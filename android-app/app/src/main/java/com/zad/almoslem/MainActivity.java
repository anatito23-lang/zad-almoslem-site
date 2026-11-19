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
        webView.loadUrl("https://anatito23-lang.github.io/zad-almoslem-site/?appbuild=20261119doharecordings1&native=1");
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
        @JavascriptInterface public void setAdhanVoice(String voice) {
            String safe = ("mustafa".equals(voice)||"husary".equals(voice)||"banna".equals(voice)||"basit".equals(voice)||"naqshbandi".equals(voice)||"tobar".equals(voice)||"haram".equals(voice)||"rifaat".equals(voice)||"fajr".equals(voice)||"beautiful".equals(voice)) ? voice : "basit";
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putString("adhan_voice", safe).apply();
        }
        @JavascriptInterface public void setAdhanVolume(int volume) {
            int safeVolume = Math.max(0, Math.min(100, volume));
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putInt("adhan_volume", safeVolume).apply();
        }
        @JavascriptInterface public void setPrayerAdhanEnabled(String prayer, boolean enabled) {
            String key = "prayer_enabled_" + prayer;
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean(key, enabled).apply();
        }
        @JavascriptInterface public void setAdhanEnabled(boolean enabled) {
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_enabled", enabled).apply();
        }
        @JavascriptInterface public void setDuaAfterAdhanEnabled(boolean enabled) {
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("dua_after_adhan_enabled", enabled).apply();
        }
        @JavascriptInterface public void setAdhanMode(String mode) {
            String safe = ("tone".equals(mode) || "vibrate".equals(mode) || "off".equals(mode)) ? mode : "adhan";
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putString("adhan_mode", safe).apply();
        }
        @JavascriptInterface public void setApproachAlertEnabled(boolean enabled) {
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("approach_alert_enabled", enabled).apply();
            ReminderReceiver.rescheduleApproachAlerts(MainActivity.this);
        }
        @JavascriptInterface public void setApproachAlertMinutes(int minutes) {
            int safe = Math.max(1, Math.min(60, minutes));
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putInt("approach_alert_minutes", safe).apply();
            ReminderReceiver.rescheduleApproachAlerts(MainActivity.this);
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
        private boolean isValidAdhanBytes(byte[] header, int length) {
            boolean id3 = length >= 3 && header[0] == 0x49 && header[1] == 0x44 && header[2] == 0x33;
            boolean mpegFrame = length >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0;
            boolean ogg = length >= 4 && header[0] == 0x4F && header[1] == 0x67 && header[2] == 0x67 && header[3] == 0x53;
            return id3 || mpegFrame || ogg;
        }
        private boolean isValidAdhanFile(File file) {
            if (file == null || !file.isFile() || file.length() < 10000) return false;
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] header = new byte[4];
                return isValidAdhanBytes(header, in.read(header));
            } catch (Exception e) {
                return false;
            }
        }
        @JavascriptInterface public boolean isAdhanDownloaded(String voice) {
            File dir = new File(getFilesDir(), "adhan");
            String safe = safeVoice(voice);
            if ("doha".equals(safe)) {
                String[] prayers = {"fajr","dhuhr","asr","maghrib","isha"};
                for (String prayer : prayers) if (!isValidAdhanFile(new File(dir, "doha_" + prayer + ".mp3"))) return false;
                return true;
            }
            return isValidAdhanFile(new File(dir, safe + ".mp3"))
                    || isValidAdhanFile(new File(dir, safe + ".ogg"));
        }
        @JavascriptInterface public boolean saveAdhanAudio(String voice,String encoded) {
            File temp = null;
            try {
                String safe=safeVoice(voice);
                byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
                if(bytes.length<10000 || bytes.length>20*1024*1024
                        || !isValidAdhanBytes(bytes, bytes.length)) return false;
                File dir=new File(getFilesDir(),"adhan");
                if(!dir.exists()&&!dir.mkdirs()) return false;
                temp=new File(dir,safe+".import");
                File target=new File(dir,safe+".mp3");
                try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}
                if(!isValidAdhanFile(temp)){temp.delete();return false;}
                if(target.exists()&&!target.delete()){temp.delete();return false;}
                return temp.renameTo(target);
            } catch(Exception e) {
                if(temp!=null&&temp.exists()) temp.delete();
                return false;
            }
        }
        private String safeVoice(String voice) {
            return ("mustafa".equals(voice)||"husary".equals(voice)||"banna".equals(voice)||"basit".equals(voice)||"naqshbandi".equals(voice)||"tobar".equals(voice)||"haram".equals(voice)||"rifaat".equals(voice)||"fajr".equals(voice)||"beautiful".equals(voice)||"doha".equals(voice))?voice:"doha";
        }
        private void downloadDohaAdhan() {
            new Thread(() -> {
                String[] prayers = {"fajr","dhuhr","asr","maghrib","isha"};
                String[] urls = {
                    "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_01_Fajr_Adhan.mp3",
                    "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_02_Dhuhr_Adhan.mp3",
                    "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_03_Asr_Adhan.mp3",
                    "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_04_Maghrib_Adhan.mp3",
                    "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_05_Isha_Adhan.mp3"
                };
                boolean ok = false;
                String reason = "تعذر تنزيل تسجيلات الأذان";
                HttpURLConnection connection = null;
                File dir = new File(getFilesDir(), "adhan");
                try {
                    if (!dir.exists() && !dir.mkdirs()) throw new Exception("تعذر إنشاء مجلد الصوت");
                    for (int i = 0; i < urls.length; i++) {
                        File temp = new File(dir, "doha_" + prayers[i] + ".download");
                        File target = new File(dir, "doha_" + prayers[i] + ".mp3");
                        try {
                            connection = (HttpURLConnection) new URL(urls[i]).openConnection();
                            connection.setConnectTimeout(25000);
                            connection.setReadTimeout(60000);
                            connection.setInstanceFollowRedirects(true);
                            connection.setRequestProperty("User-Agent", "ZadAlmoslem Android");
                            int status = connection.getResponseCode();
                            if (status != HttpURLConnection.HTTP_OK) throw new Exception("مصدر تسجيل " + prayers[i] + " أعاد " + status);
                            long total = 0;
                            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                                byte[] buffer = new byte[8192]; int count;
                                while ((count = in.read(buffer)) != -1) {
                                    total += count;
                                    if (total > 15L * 1024L * 1024L) throw new Exception("حجم تسجيل الأذان أكبر من المسموح");
                                    out.write(buffer, 0, count);
                                }
                                out.getFD().sync();
                            }
                            if (!isValidAdhanFile(temp)) throw new Exception("تسجيل " + prayers[i] + " غير صالح أو غير مكتمل");
                            if (target.exists() && !target.delete()) throw new Exception("تعذر استبدال تسجيل " + prayers[i]);
                            if (!temp.renameTo(target)) throw new Exception("تعذر حفظ تسجيل " + prayers[i]);
                        } finally {
                            if (connection != null) { connection.disconnect(); connection = null; }
                            if (temp.exists()) temp.delete();
                        }
                    }
                    ok = true;
                    reason = "تم تنزيل تسجيلات الصلوات الخمس وحفظها للعمل دون إنترنت.";
                } catch (Exception e) {
                    reason = e.getMessage() == null ? "تعذر تنزيل تسجيلات الأذان" : e.getMessage();
                } finally {
                    if (connection != null) connection.disconnect();
                }
                final boolean result = ok;
                final String message = reason;
                runOnUiThread(() -> {
                    if (webView != null) webView.evaluateJavascript(
                        "window.onAdhanDownloadComplete && window.onAdhanDownloadComplete('doha'," + result + "," + org.json.JSONObject.quote(message) + ")", null);
                });
            }).start();
        }
        @JavascriptInterface public void downloadAdhan(String voice) {
            final String safe = safeVoice(voice);
            if ("doha".equals(safe)) { downloadDohaAdhan(); return; }
            final String source;
            switch (safe) {
                case "basit": source = "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Abdulbasit_Abdusamad_1_-_Egypt_(%D8%B9%D8%A8%D8%AF_%D8%A7%D9%84%D8%A8%D8%A7%D8%B3%D8%B7_%D8%B9%D8%A8%D8%AF_%D8%A7%D9%84%D8%B5%D9%85%D8%AF_-_%D9%85%D8%B5%D8%B1).mp3"; break;
                case "mustafa": source = "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Mustafa_Ismail_-_Egypt_(%D9%85%D8%B5%D8%B7%D9%81%D9%89_%D8%A5%D8%B3%D9%85%D8%A7%D8%B9%D9%8A%D9%84_-_%D9%85%D8%B5%D8%B1).mp3"; break;
                case "husary": source = "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Mahmoud_Khalil_Al_Hussary_-_Cairo_(%D9%85%D8%AD%D9%85%D9%88%D8%AF_%D8%AE%D9%84%D9%8A%D9%84_%D8%A7%D9%84%D8%AD%D8%B5%D8%B1%D9%8A_-_%D8%A7%D9%84%D9%82%D8%A7%D9%87%D8%B1%D8%A9).mp3"; break;
                case "banna": source = "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Mahmoud_Ali_Al_Banna_-_Cairo_(%D9%85%D8%AD%D9%85%D9%88%D8%AF_%D8%B9%D9%84%D9%8A_%D8%A7%D9%84%D8%A8%D9%86%D8%A7_-_%D8%A7%D9%84%D9%82%D8%A7%D9%87%D8%B1%D8%A9).mp3"; break;
                case "naqshbandi": source = "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Sheikh_Al_Naqshbandi_(%D8%A7%D9%84%D8%B4%D9%8A%D8%AE_%D8%A7%D9%84%D9%86%D9%82%D8%B4%D8%A8%D9%86%D8%AF%D9%89).mp3"; break;
                case "haram": source = "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Ali_Ibn_Ahmad_Mala_1_-_Al_Haram_Al_Maki_(%D8%B9%D9%84%D9%8A_%D8%A8%D9%86_%D8%A3%D8%AD%D9%85%D8%AF_%D9%85%D9%84%D8%A7_-_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8A).mp3"; break;
                case "tobar": source = "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/Fajr_Adhan_by_Nasreddine_Toubar_(%D8%A7%D8%B0%D8%A7%D9%86_%D8%A7%D9%84%D9%81%D8%AC%D8%B1_%D8%A8%D8%B5%D9%88%D8%AA_%D9%86%D8%B5%D8%B1_%D8%A7%D9%84%D8%AF%D9%8A%D9%86_%D8%B7%D9%88%D8%A8%D8%A7%D8%B1).mp3"; break;
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
                    String[] sourceUrls = new String[] {
                        source.replace("https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/", "https://cdn.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/"),
                        source.replace("https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/", "https://fastly.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/"),
                        source.replace("https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/", "https://raw.githack.com/Kiwifu/adhan-mp3/main/"),
                        source
                    };
                    Exception lastFailure = null;
                    boolean downloaded = false;
                    for (String sourceUrl : sourceUrls) {
                        try {
                            connection = (HttpURLConnection) new URL(sourceUrl).openConnection();
                            connection.setConnectTimeout(20000);
                            connection.setReadTimeout(45000);
                            connection.setInstanceFollowRedirects(true);
                            connection.setRequestProperty("User-Agent", "ZadAlmoslem Android");
                            int status = connection.getResponseCode();
                            if (status != HttpURLConnection.HTTP_OK) throw new Exception("استجابة مصدر الصوت: " + status);
                            // Do not trust CDN Content-Type; validate the downloaded MP3 signature instead.
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
                            try (FileInputStream check = new FileInputStream(temp)) {
                                byte[] header = new byte[3];
                                int headerLength = check.read(header);
                                boolean id3 = headerLength >= 3 && header[0] == 0x49 && header[1] == 0x44 && header[2] == 0x33;
                                boolean mpegFrame = headerLength >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0;
                                if (!id3 && !mpegFrame) throw new Exception("الملف الذي وصل ليس تسجيل MP3 صالحًا");
                            }
                            downloaded = true;
                            break;
                        } catch (Exception attemptError) {
                            lastFailure = attemptError;
                            if (temp.exists()) temp.delete();
                        } finally {
                            if (connection != null) { connection.disconnect(); connection = null; }
                        }
                    }
                    if (!downloaded) throw (lastFailure == null ? new Exception("تعذر الاتصال بمصادر الصوت") : lastFailure);
                    if (target.exists() && !target.delete()) throw new Exception("تعذر استبدال التسجيل القديم");
                    if (!temp.renameTo(target)) throw new Exception("تعذر حفظ التسجيل على الجهاز");
                    String publicPath = copyAdhanToMusicFolder(target, safe);
                    error = publicPath == null
                            ? "تم تنزيل الصوت وحفظه داخل التطبيق للعمل دون إنترنت."
                            : "تم الحفظ في: " + publicPath;
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
        private String copyAdhanToMusicFolder(File source, String voice) {
            if (Build.VERSION.SDK_INT < 29) return null;
            String folder = "Music/Zad Almoslem";
            String displayName = voice + ".mp3";
            android.content.ContentResolver resolver = getContentResolver();
            android.net.Uri collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            try {
                String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " +
                        MediaStore.MediaColumns.RELATIVE_PATH + "=?";
                String[] args = new String[]{displayName, folder + "/"};
                try (android.database.Cursor cursor = resolver.query(collection,
                        new String[]{MediaStore.MediaColumns._ID}, selection, args, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(0);
                        resolver.delete(android.content.ContentUris.withAppendedId(collection, id), null, null);
                    }
                }
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, folder);
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                android.net.Uri item = resolver.insert(collection, values);
                if (item == null) return null;
                try (InputStream in = new java.io.FileInputStream(source);
                     java.io.OutputStream out = resolver.openOutputStream(item, "w")) {
                    if (out == null) throw new Exception("تعذر فتح ملف الوجهة");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    out.flush();
                } catch (Exception e) {
                    resolver.delete(item, null, null);
                    throw e;
                }
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(item, ready, null, null);
                return "التخزين الداخلي/" + folder + "/" + displayName;
            } catch (Exception e) {
                return null;
            }
        }
        private File adhanFile(String voice) {
            String safe = safeVoice(voice);
            return new File(new File(getFilesDir(), "adhan"), safe + ".ogg");
        }
        @JavascriptInterface public void playRadio(String stream, String label) {
            String url = "backup".equals(stream) ? "https://qurango.net/radio/tarateel" :
                "https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8";
            getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putString("radio_stream", stream == null ? "main" : stream).putString("radio_label", label == null ? "إذاعة القرآن الكريم" : label).apply();
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
                getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putString("last_quran_url", uri.toString()).putString("last_quran_title", trackTitle == null ? "تلاوة القرآن الكريم" : trackTitle).apply();
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