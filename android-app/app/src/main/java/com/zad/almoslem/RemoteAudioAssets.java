package com.zad.almoslem;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RemoteAudioAssets {
    private static final String TAG = "ZadRemoteAudio";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static volatile boolean callbackRegistered = false;

    private static final class Asset {
        final String url;
        final File target;
        Asset(String url, File target) { this.url = url; this.target = target; }
    }

    public interface RefreshCallback { void onComplete(String message); }

    private RemoteAudioAssets() {}

    public static void watchConnectivity(Context context) {
        Context app = context.getApplicationContext();
        refresh(app);
        if (callbackRegistered) return;
        synchronized (RemoteAudioAssets.class) {
            if (callbackRegistered) return;
            ConnectivityManager manager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return;
            try {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
                manager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                    @Override public void onAvailable(Network network) { refresh(app); }
                });
                callbackRegistered = true;
            } catch (Exception error) {
                Log.w(TAG, "Could not register network callback", error);
            }
        }
    }

    public static void refreshWithResult(Context context, RefreshCallback callback) {
        Context app = context.getApplicationContext();
        FirebaseFirestore.getInstance().collection("app_audio_assets").document("ramadan").get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists() || snapshot.getData() == null) {
                        postResult(callback, "لم تُنشر روابط تسجيلات رمضان بعد. ارفع التسجيل من لوحة الإدارة أولًا.");
                        return;
                    }
                    ArrayList<Asset> pending = new ArrayList<>();
                    SharedPreferences prefs = app.getSharedPreferences("zad_ramadan", Context.MODE_PRIVATE);
                    Map<String, Object> values = snapshot.getData();
                    String[] fields = {"cannonUrl", "iftarVoiceUrl", "suhoorVoiceUrl"};
                    String[] keys = {"ramadan_iftar_cannon", "ramadan_iftar_voice", "ramadan_suhoor"};
                    String[] files = {"iftar_cannon.mp3", "iftar_announcement.mp3", "suhoor_voice.mp3"};
                    int configured = 0;
                    for (int i = 0; i < fields.length; i++) {
                        Object raw = values.get(fields[i]);
                        if (raw instanceof String && ((String) raw).trim().startsWith("https://")) {
                            configured++;
                            syncOne(app, prefs, values, fields[i], keys[i], files[i], pending);
                        }
                    }
                    if (configured == 0) {
                        postResult(callback, "لم أجد روابط صوت صالحة في قاعدة البيانات. راجع نشر التسجيل من لوحة الإدارة.");
                        return;
                    }
                    if (pending.isEmpty()) {
                        postResult(callback, "✓ روابط التسجيلات محفوظة، والملفات موجودة بالفعل على الجهاز.");
                        return;
                    }
                    WORKER.execute(() -> {
                        int downloaded = 0;
                        ArrayList<String> errors = new ArrayList<>();
                        for (Asset asset : pending) {
                            String error = download(asset.url, asset.target);
                            if (error == null) downloaded++;
                            else errors.add(asset.target.getName() + ": " + error);
                        }
                        StringBuilder message = new StringBuilder(downloaded + " من " + pending.size() + " تسجيلات تم تنزيلها بنجاح.");
                        if (!errors.isEmpty()) {
                            message.append(" تعذر تنزيل: ");
                            for (int i = 0; i < errors.size(); i++) {
                                if (i > 0) message.append("؛ ");
                                message.append(errors.get(i));
                            }
                        }
                        postResult(callback, message.toString());
                    });
                })
                .addOnFailureListener(error -> postResult(callback,
                        "تعذر قراءة روابط التسجيلات من قاعدة البيانات: " + error.getMessage()));
    }

    private static void postResult(RefreshCallback callback, String message) {
        if (callback != null) new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(message));
    }

    public static void refresh(Context context) { refresh(context, null); }

    public static void refresh(Context context, Runnable onComplete) {
        Context app = context.getApplicationContext();
        FirebaseFirestore.getInstance().collection("app_audio_assets").document("ramadan").get()
                .addOnSuccessListener(snapshot -> {
                    ArrayList<Asset> pending = new ArrayList<>();
                    if (snapshot.exists() && snapshot.getData() != null) {
                        SharedPreferences prefs = app.getSharedPreferences("zad_ramadan", Context.MODE_PRIVATE);
                        Map<String, Object> values = snapshot.getData();
                        syncOne(app, prefs, values, "cannonUrl", "ramadan_iftar_cannon", "iftar_cannon.mp3", pending);
                        syncOne(app, prefs, values, "iftarVoiceUrl", "ramadan_iftar_voice", "iftar_announcement.mp3", pending);
                        syncOne(app, prefs, values, "suhoorVoiceUrl", "ramadan_suhoor", "suhoor_voice.mp3", pending);
                    }
                    finishRefresh(pending, onComplete);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Could not read remote audio configuration", error);
                    finishRefresh(new ArrayList<>(), onComplete);
                });
    }

    private static void syncOne(Context context, SharedPreferences prefs, Map<String, Object> data,
                                String field, String key, String fileName, ArrayList<Asset> pending) {
        Object raw = data.get(field);
        if (!(raw instanceof String)) return;
        String url = ((String) raw).trim();
        if (!url.startsWith("https://")) return;

        String prefKey = "remote_" + key;
        String previous = prefs.getString(prefKey, "");
        // Keep the remote URL available as a fallback if offline caching fails.
        prefs.edit().putString(prefKey, url).apply();

        File dir = new File(context.getFilesDir(), "ramadan_audio");
        File target = new File(dir, fileName);
        if (url.equals(previous) && target.isFile() && target.length() > 1000) return;
        pending.add(new Asset(url, target));
    }

    private static void finishRefresh(ArrayList<Asset> pending, Runnable onComplete) {
        if (pending.isEmpty()) {
            postComplete(onComplete);
            return;
        }
        WORKER.execute(() -> {
            for (Asset asset : pending) download(asset.url, asset.target);
            postComplete(onComplete);
        });
    }

    private static void postComplete(Runnable callback) {
        if (callback != null) new Handler(Looper.getMainLooper()).post(callback);
    }

    private static String download(String url, File target) {
        HttpURLConnection connection = null;
        File dir = target.getParentFile();
        File temp = new File(dir, target.getName() + ".part");
        try {
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create audio cache directory");
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "ZadAlmoslem-Android");
            connection.connect();

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("HTTP " + status);
            String contentType = connection.getContentType();
            if (contentType != null && (contentType.toLowerCase().contains("text/html")
                    || contentType.toLowerCase().contains("application/json"))) {
                throw new IllegalStateException("The audio URL returned a web page, not an audio file");
            }
            long length = connection.getContentLengthLong();
            if (length > 25L * 1024L * 1024L) throw new IllegalStateException("Audio file exceeds 25 MB");

            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[16384];
                int count;
                long total = 0;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > 25L * 1024L * 1024L) throw new IllegalStateException("Audio file exceeds 25 MB");
                    out.write(buffer, 0, count);
                }
                out.getFD().sync();
            }
            if (temp.length() < 1000) throw new IllegalStateException("Audio file is empty or invalid");
            if (target.exists() && !target.delete()) throw new IllegalStateException("Cannot replace cached audio");
            if (!temp.renameTo(target)) throw new IllegalStateException("Cannot finalize cached audio");
            Log.i(TAG, "Downloaded " + target.getName() + " (" + target.length() + " bytes)");
            return null;
        } catch (Exception error) {
            Log.w(TAG, "Audio download failed for " + target.getName() + ": " + error.getMessage(), error);
            if (temp.exists()) temp.delete();
            return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
