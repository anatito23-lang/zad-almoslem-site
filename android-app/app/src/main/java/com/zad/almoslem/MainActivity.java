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
import android.content.ContentValues;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
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
        s.setAllowFileAccess(true);
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
        webView.loadUrl("file:///android_asset/site/index.html?appbuild=20261119clean-no-adhan1&native=1");
        requestNotificationPermission();
    }
    @Override protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 31) {
            android.app.AlarmManager alarms = (android.app.AlarmManager)getSystemService(ALARM_SERVICE);
            if (alarms != null && alarms.canScheduleExactAlarms()) {
                android.content.SharedPreferences prefs = getSharedPreferences("zad_reminders", MODE_PRIVATE);
                for (String key : new String[]{"morning", "evening", "qiyam"}) {
                    if (prefs.getBoolean(key + "_enabled", false)) {
                        ReminderReceiver.scheduleDaily(this, key,
                            prefs.getInt(key + "_hour", 7), prefs.getInt(key + "_minute", 0),
                            prefs.getString(key + "_title", "تذكير من زاد المسلم"));
                    }
                }
            }
        }
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
        @JavascriptInterface public boolean isReminderAudioDownloaded() {
            File dir = new File(getFilesDir(), "reminders");
            for (String name : new String[]{"morning", "evening", "dua"}) {
                if (!isValidReminderAudio(new File(dir, name + ".mp3"))) return false;
            }
            return true;
        }
        private boolean isValidReminderAudio(File file) {
            if (!file.isFile() || file.length() < 10000) return false;
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                int a = in.read(), b = in.read();
                int c = in.read();
                return (a == 'I' && b == 'D' && c == '3') || (a == 0xFF && (b & 0xE0) == 0xE0);
            } catch (Exception ignored) { return false; }
        }
        @JavascriptInterface public void downloadReminderAudio() {
            new Thread(() -> {
                String[][] sources = {
                    {"morning", "https://archive.org/download/makkah-live.-net-athkar-01/MakkahLive.Net_athkar_03.mp3"},
                    {"evening", "https://archive.org/download/makkah-live.-net-athkar-01/MakkahLive.Net_athkar_04.mp3"},
                    {"dua", "https://archive.org/download/Islamic_Tape-258_uP_bY_mUSLEm/sharawy_doaa_24.mp3"}
                };
                File dir = new File(getFilesDir(), "reminders");
                StringBuilder report = new StringBuilder();
                boolean allOk = true;
                if (!dir.exists() && !dir.mkdirs()) {
                    allOk = false;
                    report.append("تعذر إنشاء مجلد الملفات الصوتية.");
                } else {
                    for (String[] item : sources) {
                        HttpURLConnection connection = null;
                        File temp = new File(dir, item[0] + ".download");
                        File target = new File(dir, item[0] + ".mp3");
                        try {
                            if (isValidReminderAudio(target)) {
                                report.append(item[0]).append(": محفوظ بالفعل. ");
                                continue;
                            }
                            connection = (HttpURLConnection) new URL(item[1]).openConnection();
                            connection.setConnectTimeout(25000);
                            connection.setReadTimeout(90000);
                            connection.setInstanceFollowRedirects(true);
                            connection.setRequestProperty("User-Agent", "ZadAlmoslem Android");
                            int status = connection.getResponseCode();
                            if (status != HttpURLConnection.HTTP_OK) throw new Exception("استجابة المصدر " + status);
                            String type = connection.getContentType();
                            String mime = type == null ? "" : type.toLowerCase(java.util.Locale.ROOT);
                            if (!(mime.contains("audio") || mime.contains("mpeg") || mime.contains("octet-stream"))) {
                                throw new Exception("المصدر لم يُرجع ملفًا صوتيًا (" + (type == null ? "نوع غير معروف" : type) + ")");
                            }
                            long total = 0;
                            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                                byte[] buffer = new byte[8192];
                                int count;
                                while ((count = in.read(buffer)) != -1) {
                                    total += count;
                                    if (total > 30L * 1024L * 1024L) throw new Exception("حجم الملف أكبر من 30 ميجابايت");
                                    out.write(buffer, 0, count);
                                }
                                out.getFD().sync();
                            }
                            if (total < 10000 || !isValidReminderAudio(temp)) throw new Exception("الملف الذي وصل ليس تسجيل MP3 مكتملًا");
                            if (target.exists() && !target.delete()) throw new Exception("تعذر استبدال النسخة القديمة");
                            if (!temp.renameTo(target)) throw new Exception("تعذر حفظ الملف على الجهاز");
                            report.append(item[0]).append(": تم التنزيل. ");
                        } catch (Exception e) {
                            allOk = false;
                            if (temp.exists()) temp.delete();
                            report.append(item[0]).append(": ").append(e.getMessage() == null ? "فشل التنزيل" : e.getMessage()).append(". ");
                        } finally {
                            if (connection != null) connection.disconnect();
                        }
                    }
                }
                final boolean result = allOk && isReminderAudioDownloaded();
                final String details = report.toString();
                runOnUiThread(() -> {
                    if (webView != null) webView.evaluateJavascript(
                        "window.onReminderAudioDownload && window.onReminderAudioDownload(" + result + "," + org.json.JSONObject.quote(details) + ")", null);
                });
            }).start();
        }
        @JavascriptInterface public void schedulePrayer(String name,long timestamp) {
            ReminderReceiver.schedulePrayer(MainActivity.this,name,timestamp);
        }
        @JavascriptInterface public boolean canScheduleExactAlarms() {
            if (Build.VERSION.SDK_INT < 31) return true;
            android.app.AlarmManager alarms = (android.app.AlarmManager)getSystemService(ALARM_SERVICE);
            return alarms != null && alarms.canScheduleExactAlarms();
        }
        @JavascriptInterface public void requestExactAlarmPermission() {
            if (Build.VERSION.SDK_INT >= 31 && !canScheduleExactAlarms()) {
                try {
                    Intent settings = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName()));
                    settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(settings);
                } catch (Exception ignored) {}
            }
        }
        @JavascriptInterface public void requestNotificationPermission() {
            runOnUiThread(() -> MainActivity.this.requestNotificationPermission());
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