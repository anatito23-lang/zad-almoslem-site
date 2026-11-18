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
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 2401;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF0C3B32);
        getWindow().setNavigationBarColor(0xFF0C3B32);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF0C3B32);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadsImagesAutomatically(true);
        s.setAllowFileAccess(false);
        webView.addJavascriptInterface(new ReminderBridge(), "ZadAndroid");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);
        webView.loadUrl("https://anatito23-lang.github.io/zad-almoslem-site/");
        requestNotificationPermission();
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
    }
    private class ReminderBridge {
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
            String safe = "beautiful".equals(voice) ? "beautiful" : "sabah";
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putString("adhan_voice", safe).apply();
        }
        @JavascriptInterface public void setPrayerAdhanEnabled(String prayer, boolean enabled) {
            String key = "prayer_enabled_" + prayer;
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean(key, enabled).apply();
        }
        @JavascriptInterface public void setAdhanEnabled(boolean enabled) {
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_enabled", enabled).apply();
        }
        @JavascriptInterface public void requestNotificationPermission() {
            runOnUiThread(() -> MainActivity.this.requestNotificationPermission());
        }
        @JavascriptInterface public boolean isAdhanDownloaded(String voice) {
            return adhanFile(voice).isFile() && adhanFile(voice).length() > 10000;
        }
        @JavascriptInterface public void downloadAdhan(String voice) {
            final String safe = "beautiful".equals(voice) ? "beautiful" : "sabah";
            new Thread(() -> {
                boolean ok = false;
                HttpURLConnection connection = null;
                File dir = new File(getFilesDir(), "adhan");
                File temp = new File(dir, safe + ".download");
                File target = new File(dir, safe + ".ogg");
                try {
                    if (!dir.exists() && !dir.mkdirs()) throw new Exception("تعذر إنشاء مجلد الصوت");
                    String source = "beautiful".equals(safe)
                            ? "https://upload.wikimedia.org/wikipedia/commons/b/b0/Beautiful_adhan.ogg"
                            : "https://upload.wikimedia.org/wikipedia/commons/e/e7/Adhan.ogg";
                    connection = (HttpURLConnection) new URL(source).openConnection();
                    connection.setConnectTimeout(20000);
                    connection.setReadTimeout(30000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", "ZadAlmoslemAndroid/1.0");
                    int status = connection.getResponseCode();
                    if (status != HttpURLConnection.HTTP_OK) throw new Exception("HTTP " + status);
                    String type = connection.getContentType();
                    if (type == null || !(type.contains("audio") || type.contains("octet-stream"))) {
                        throw new Exception("نوع الملف غير صحيح");
                    }
                    int length = connection.getContentLength();
                    if (length > 8 * 1024 * 1024) throw new Exception("حجم الملف أكبر من المسموح");
                    try (InputStream in = connection.getInputStream();
                         FileOutputStream out = new FileOutputStream(temp)) {
                        byte[] buffer = new byte[8192];
                        int count;
                        long total = 0;
                        while ((count = in.read(buffer)) != -1) {
                            total += count;
                            if (total > 8L * 1024L * 1024L) throw new Exception("حجم الملف أكبر من المسموح");
                            out.write(buffer, 0, count);
                        }
                        out.getFD().sync();
                        if (total < 10000) throw new Exception("ملف الصوت غير مكتمل");
                    }
                    if (target.exists() && !target.delete()) throw new Exception("تعذر استبدال الملف القديم");
                    if (!temp.renameTo(target)) throw new Exception("تعذر حفظ ملف الصوت");
                    ok = true;
                } catch (Exception e) {
                    if (temp.exists()) temp.delete();
                } finally {
                    if (connection != null) connection.disconnect();
                }
                final boolean result = ok;
                runOnUiThread(() -> {
                    if (webView != null) webView.evaluateJavascript(
                            "window.onAdhanDownloadComplete && window.onAdhanDownloadComplete('" + safe + "'," + result + ")", null);
                });
            }).start();
        }
        private File adhanFile(String voice) {
            String safe = "beautiful".equals(voice) ? "beautiful" : "sabah";
            return new File(new File(getFilesDir(), "adhan"), safe + ".ogg");
        }
        @JavascriptInterface public void previewAdhan(String voice) {
            if (!isAdhanDownloaded(voice)) return;
            Intent audio = new Intent(MainActivity.this, AdhanPlaybackService.class);
            audio.setAction(AdhanPlaybackService.ACTION_PLAY);
            audio.putExtra("prayer", "تجربة الأذان");
            audio.putExtra("voice", "beautiful".equals(voice) ? "beautiful" : "sabah");
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(audio); else startService(audio);
        }

    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) { webView.goBack(); return true; }
        return super.onKeyDown(keyCode, event);
    }
}