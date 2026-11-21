package com.zad.almoslem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads verified Wikimedia Commons photographs once and keeps them for offline use. */
public final class AdhanWallpaperImages {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final String[] IDS = {
        "azhar_mosque_bg", "kaaba_wallpaper", "madinah_wallpaper",
        "aqsa_wallpaper", "quran_wallpaper"
    };

    // 960px Commons thumbnails (Quran is portrait and uses a 500px thumbnail).
    private static final String[] URLS = {
        "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e6/AlAzhar_Mosque.jpg/960px-AlAzhar_Mosque.jpg",
        "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5a/Holy_Kaaba.jpg/960px-Holy_Kaaba.jpg",
        "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/96/Al_Masjid_an_Nabawi.jpg/960px-Al_Masjid_an_Nabawi.jpg",
        "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/ba/Al_Aqsa_mosque.jpg/960px-Al_Aqsa_mosque.jpg",
        "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/52/A_photo_of_a_Quran.jpg/500px-A_photo_of_a_Quran.jpg"
    };

    private AdhanWallpaperImages() {}

    public static void load(Context context, ImageView target, String id) {
        int index = -1;
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) { index = i; break; }
        }
        if (index < 0) return;

        final int imageIndex = index;
        final String imageId = id;
        target.setTag(imageId);
        target.setBackgroundColor(Color.rgb(12, 35, 31));

        File directory = new File(context.getFilesDir(), "adhan-wallpapers");
        if (!directory.exists() && !directory.mkdirs()) return;
        File cached = new File(directory, imageId + ".jpg");

        if (cached.isFile() && cached.length() > 0) {
            Bitmap bitmap = BitmapFactory.decodeFile(cached.getAbsolutePath());
            if (bitmap != null) {
                target.setImageBitmap(bitmap);
                return;
            }
            // A truncated cache is discarded and downloaded again.
            //noinspection ResultOfMethodCallIgnored
            cached.delete();
        }

        final Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            File temporary = new File(directory, imageId + ".download");
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(URLS[imageIndex]).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(18000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "ZadAlMoslemAndroid/3.1 (adhan wallpaper photo cache)");
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return;
                int length = connection.getContentLength();
                if (length > 8 * 1024 * 1024) return;

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    long total = 0;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > 8L * 1024L * 1024L) {
                            //noinspection ResultOfMethodCallIgnored
                            temporary.delete();
                            return;
                        }
                        output.write(buffer, 0, count);
                    }
                }

                Bitmap bitmap = BitmapFactory.decodeFile(temporary.getAbsolutePath());
                if (bitmap == null) return;
                if (cached.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    cached.delete();
                }
                if (!temporary.renameTo(cached)) {
                    try (FileOutputStream output = new FileOutputStream(cached);
                         InputStream input = new java.io.FileInputStream(temporary)) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    }
                    //noinspection ResultOfMethodCallIgnored
                    temporary.delete();
                }
                MAIN.post(() -> {
                    if (imageId.equals(target.getTag())) target.setImageBitmap(bitmap);
                });
            } catch (Exception ignored) {
                // Keep the neutral placeholder if the device is offline or Commons is unreachable.
            } finally {
                if (connection != null) connection.disconnect();
                if (temporary.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    temporary.delete();
                }
            }
        });
    }
}
