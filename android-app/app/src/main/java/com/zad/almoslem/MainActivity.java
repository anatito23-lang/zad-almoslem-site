package com.zad.almoslem;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.KeyEvent;
import android.content.pm.PackageManager;
import android.content.Intent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.util.Base64;
import android.net.Uri;
import android.webkit.ValueCallback;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private static final int AUDIO_FILE_REQUEST = 2402;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 2401;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF071522);
        getWindow().setNavigationBarColor(0xFF071522);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF071522);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadsImagesAutomatically(true);
        s.setAllowFileAccess(false);
        s.setGeolocationEnabled(true);
        webView.addJavascriptInterface(new ReminderBridge(), "ZadAndroid");
        // Keep WebView and service-worker caches so downloaded Quran pages remain available offline.
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.setType("audio/*");
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                try { startActivityForResult(Intent.createChooser(pick, "اختر تسجيل الأذان"), AUDIO_FILE_REQUEST); }
                catch (Exception e) { fileChooserCallback = null; callback.onReceiveValue(null); return false; }
                return true;
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
        setContentView(webView);
        webView.loadUrl("https://anatito23-lang.github.io/zad-almoslem-site/?appbuild=20261118p&native=1");
        requestNotificationPermission();
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
    }
    private class ReminderBridge {
        @JavascriptInterface public boolean isNativeApp() { return true; }
        @JavascriptInterface public void scheduleDailyReminder(String key,int hour,int minute,String title) {
            ReminderReceiver.scheduleDaily(MainActivity.this,key,hour,minute,title);
        }
        @JavascriptInterface public void cancelDailyReminder(String key) {
            ReminderReceiver.cancelDaily(MainActivity.this,key);
        }
        @JavascriptInterface public void schedulePrayer(String name,long timestamp) {
            ReminderReceiver.schedulePrayer(MainActivity.this,name,timestamp);
        }
        @JavascriptInterface public void setAdhanVoice(String voice) {
            String safe = ("mustafa".equals(voice)||"husary".equals(voice)||"banna".equals(voice)||"basit".equals(voice)||"naqshbandi".equals(voice)||"tobar".equals(voice)||"rifaat".equals(voice)||"fajr".equals(voice)||"beautiful".equals(voice)) ? voice : "basit";
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putString("adhan_voice", safe).apply();
        }
        @JavascriptInterface public void setPrayerAdhanEnabled(String prayer, boolean enabled) {
            String key = "prayer_enabled_" + prayer;
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean(key, enabled).apply();
        }
        @JavascriptInterface public void setAdhanEnabled(boolean enabled) {
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_enabled", enabled).apply();
        }
        @JavascriptInterface public void setAdhanMode(String mode) {
            String safe = ("tone".equals(mode) || "silent".equals(mode)) ? mode : "adhan";
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putString("adhan_mode", safe).apply();
        }
        @JavascriptInterface public void setPrayerSilentEnabled(String prayer, boolean enabled) {
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("prayer_silent_" + prayer, enabled).apply();
        }
        @JavascriptInterface public boolean hasSilentPermission() {
            if (Build.VERSION.SDK_INT < 23) return false;
            android.app.NotificationManager nm=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            return nm != null && nm.isNotificationPolicyAccessGranted();
        }
        @JavascriptInterface public void requestSilentPermission() {
            if (Build.VERSION.SDK_INT >= 23 && !hasSilentPermission()) {
                try { startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); }
                catch (Exception ignored) {}
            }
        }
        @JavascriptInterface public void requestNotificationPermission() {
            runOnUiThread(() -> MainActivity.this.requestNotificationPermission());
        }
        @JavascriptInterface public boolean isAdhanDownloaded(String voice) {
            File f=adhanFile(voice);
            if (f.isFile() && f.length()>10000) return true;
            File mp3=new File(f.getParentFile(),safeVoice(voice)+".mp3");
            return mp3.isFile() && mp3.length()>10000;
        }
        @JavascriptInterface public boolean saveAdhanAudio(String voice,String encoded) {
            try {
                String safe=safeVoice(voice);
                byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
                if(bytes.length<10000 || bytes.length>8*1024*1024) return false;
                File dir=new File(getFilesDir(),"adhan");
                if(!dir.exists()&&!dir.mkdirs()) return false;
                File temp=new File(dir,safe+".import");
                File target=new File(dir,safe+".mp3");
                try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}
                if(target.exists()&&!target.delete()){temp.delete();return false;}
                return temp.renameTo(target);
            } catch(Exception e){return false;}
        }
        private String safeVoice(String voice) {
            return ("mustafa".equals(voice)||"husary".equals(voice)||"banna".equals(voice)||"basit".equals(voice)||"naqshbandi".equals(voice)||"tobar".equals(voice)||"rifaat".equals(voice)||"fajr".equals(voice)||"beautiful".equals(voice))?voice:"basit";
        }
        @JavascriptInterface public void downloadAdhan(String voice) {
            final String safe = safeVoice(voice);
            final String source;
            switch (safe) {
                case "basit": source = "https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/041--.mp3"; break;
                case "mustafa": source = "https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/068-.mp3"; break;
                case "husary": source = "https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/044--.mp3"; break;
                case "rifaat": source = "https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/042--.mp3"; break;
                case "banna": source = "https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/043--.mp3"; break;
                case "naqshbandi": source = "https://cdn1.esm3.com//music/2603/esm3.com.198098.mp3"; break;
                default:
                    runOnUiThread(() -> {
                        if (webView != null) webView.evaluateJavascript(
                            "window.onAdhanDownloadComplete && window.onAdhanDownloadComplete('" + safe + "',false,'مصدر هذا التسجيل لم يُعتمد بعد')", null);
                    });
                    return;
            }
            new Thread(() -> {
                boolean ok = false;
                String error = "تعذر الاتصال بمصدر الصوت";
                HttpURLConnection connection = null;
                File dir = new File(getFilesDir(), "adhan");
                File temp = new File(dir, safe + ".download");
                File target = new File(dir, safe + ".mp3");
                try {
                    if (!dir.exists() && !dir.mkdirs()) throw new Exception("تعذر إنشاء مجلد الصوت");
                    connection = (HttpURLConnection) new URL(source).openConnection();
                    connection.setConnectTimeout(20000);
                    connection.setReadTimeout(45000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", "ZadAlmoslem Android");
                    int status = connection.getResponseCode();
                    if (status != HttpURLConnection.HTTP_OK) throw new Exception("استجابة مصدر الصوت: " + status);
                    String type = connection.getContentType();
                    if (type == null || !(type.toLowerCase().contains("audio") || type.toLowerCase().contains("mpeg") || type.toLowerCase().contains("octet-stream"))) {
                        throw new Exception("المصدر لم يُرجع ملف MP3 صالحًا");
                    }
                    long total = 0;
                    try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = in.read(buffer)) != -1) {
                            total += count;
                            if (total > 12L * 1024L * 1024L) throw new Exception("حجم الملف أكبر من المسموح");
                            out.write(buffer, 0, count);
                        }
                        out.getFD().sync();
                    }
                    if (total < 10000) throw new Exception("ملف الصوت غير مكتمل");
                    if (target.exists() && !target.delete()) throw new Exception("تعذر استبدال التسجيل القديم");
                    if (!temp.renameTo(target)) throw new Exception("تعذر حفظ التسجيل على الجهاز");
                    ok = true;
                } catch (Exception e) {
                    error = e.getMessage() == null ? "فشل تنزيل التسجيل" : e.getMessage();
                    if (temp.exists()) temp.delete();
                } finally {
                    if (connection != null) connection.disconnect();
                }
                final boolean result = ok;
                final String reason = error;
                runOnUiThread(() -> {
                    if (webView != null) webView.evaluateJavascript(
                        "window.onAdhanDownloadComplete && window.onAdhanDownloadComplete('" + safe + "'," + result + "," + org.json.JSONObject.quote(reason) + ")", null);
                });
            }).start();
        }
        private File adhanFile(String voice) {
            String safe = safeVoice(voice);
            return new File(new File(getFilesDir(), "adhan"), safe + ".ogg");
        }
        @JavascriptInterface public void playRadio(String stream, String label) {
            String url = "backup".equals(stream) ? "https://qurango.net/radio/tarateel" :
                "https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8";
            Intent radio = new Intent(MainActivity.this, RadioPlaybackService.class);
            radio.setAction(RadioPlaybackService.ACTION_PLAY);
            radio.putExtra("url", url);
            radio.putExtra("label", label == null ? "إذاعة القرآن الكريم" : label);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(radio); else startService(radio);
        }
        @JavascriptInterface public void playQuranAudio(String audioUrl, String trackTitle) {
            try {
                android.net.Uri uri = android.net.Uri.parse(audioUrl);
                String host = uri.getHost();
                if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null ||
                    !(host.toLowerCase().endsWith("mp3quran.net") || host.toLowerCase().endsWith("mp3quran.com"))) return;
                Intent audio = new Intent(MainActivity.this, RadioPlaybackService.class);
                audio.setAction(RadioPlaybackService.ACTION_PLAY);
                audio.putExtra("url", uri.toString());
                audio.putExtra("label", trackTitle == null ? "تلاوة القرآن الكريم" : trackTitle);
                audio.putExtra("kind", "quran");
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(audio); else startService(audio);
            } catch (Exception ignored) {}
        }
        @JavascriptInterface public void stopRadio() {
            Intent radio = new Intent(MainActivity.this, RadioPlaybackService.class);
            radio.setAction(RadioPlaybackService.ACTION_STOP);
            startService(radio);
        }
        @JavascriptInterface public void previewAdhan(String voice) {
            if (!isAdhanDownloaded(voice)) return;
            Intent audio = new Intent(MainActivity.this, AdhanPlaybackService.class);
            audio.setAction(AdhanPlaybackService.ACTION_PLAY);
            audio.putExtra("prayer", "تجربة الأذان");
            audio.putExtra("voice", safeVoice(voice));
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(audio); else startService(audio);
        }

    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==AUDIO_FILE_REQUEST && fileChooserCallback!=null){
            Uri[] result=null;
            if(resultCode==RESULT_OK && data!=null && data.getData()!=null) result=new Uri[]{data.getData()};
            fileChooserCallback.onReceiveValue(result);
            fileChooserCallback=null;
        }
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) { webView.goBack(); return true; }
        return super.onKeyDown(keyCode, event);
    }
}