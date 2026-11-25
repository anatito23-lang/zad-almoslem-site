package com.zad.almoslem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AdhanPlaybackService extends Service {
    public static final String ACTION_PLAY = "com.zad.almoslem.PLAY_ADHAN";
    public static final String ACTION_STOP = "com.zad.almoslem.STOP_ADHAN";
    public static final String ACTION_SET_VOLUME = "com.zad.almoslem.SET_ADHAN_VOLUME";
    public static final String ACTION_SET_PRAYER_VOLUME = "com.zad.almoslem.SET_PRAYER_ADHAN_VOLUME";
    private static final String CHANNEL_ID = "zad_adhan_playback_sakina_v4";
    private static final int NOTIFICATION_ID = 7310;
    private ExoPlayer player;
    private boolean playDuaAfterCompletion;
    private boolean deferBackgroundAudioResume;
    private volatile boolean audioDownloadInProgress;
    private String activePrayer;
    public static volatile long uiPositionMs = 0L;
    public static volatile long uiDurationMs = 0L;
    public static volatile String uiVoiceKey = "";
    public static volatile boolean uiTakbeerOnly = false;
    private final android.os.Handler uiProgressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable uiProgressTicker = new Runnable() {
        @Override public void run() {
            ExoPlayer current = player;
            if (current != null) {
                try {
                    uiPositionMs = Math.max(0L, current.getCurrentPosition());
                    long duration = current.getDuration();
                    uiDurationMs = duration > 0L && duration < Long.MAX_VALUE ? duration : 0L;
                } catch (Exception ignored) {}
                uiProgressHandler.postDelayed(this, 100L);
            }
        }
    };

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SET_PRAYER_VOLUME.equals(intent.getAction())) {
            String prayer = intent.getStringExtra("prayer");
            int level = Math.max(0, Math.min(100, intent.getIntExtra("volume", 100)));
            if (player != null && prayer != null && prayer.equals(activePrayer)) player.setVolume(level / 100f);
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_SET_VOLUME.equals(intent.getAction())) {
            int level = Math.max(0, Math.min(100, intent.getIntExtra("volume", 100)));
            if (player != null) player.setVolume(level / 100f);
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        String prayer = intent == null ? "الصلاة" : intent.getStringExtra("prayer");
        // AlarmManager can deliver the same prayer alarm more than once on some devices.
        // Do not reset an already-running recording when a duplicate start arrives.
        if (intent != null && ACTION_PLAY.equals(intent.getAction())
                && prayer != null && prayer.equals(activePrayer)
                && (audioDownloadInProgress || (player != null &&
                    (player.isPlaying()
                     || player.getPlaybackState() == Player.STATE_BUFFERING
                     || player.getPlaybackState() == Player.STATE_READY)))) {
            android.util.Log.i("ZadAlmoslem", "Ignoring duplicate adhan start for " + prayer);
            return START_NOT_STICKY;
        }
        playDuaAfterCompletion = intent != null && intent.getBooleanExtra("play_dua_after", false);
        String selectedVoice = intent == null ? "basit" : intent.getStringExtra("voice");
        activePrayer = prayer;
        uiPositionMs = 0L;
        uiDurationMs = 0L;
        uiVoiceKey = selectedVoice == null ? "basit" : selectedVoice;
        android.content.SharedPreferences audioPrefs = getSharedPreferences("zad_reminders", MODE_PRIVATE);
        uiTakbeerOnly = audioPrefs.getBoolean("power_saver_enabled", false)
                || audioPrefs.getString("adhan_mode", "adhan").equals("takbeer");
        createChannel();
        Intent screen = new Intent(this, AdhanFullScreenActivity.class);
        screen.putExtra("prayer", prayer);
        screen.putExtra("voice", uiVoiceKey);
        screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(this, 7313, screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Promote the service immediately with a lightweight notification. Do not perform
        // other service starts or bitmap rendering before startForeground().
        Notification.Builder initialBuilder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        initialBuilder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان الآن وقت الصلاة")
                .setContentText("يتم تجهيز أذان " + prayer)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreen, true);
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, initialBuilder.build(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, initialBuilder.build());
            }
        } catch (Exception startupError) {
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit()
                    .putBoolean("adhan_active", false)
                    .putString("adhan_last_start_error", startupError.getMessage() == null
                            ? startupError.getClass().getSimpleName() : startupError.getMessage()).apply();
            android.util.Log.e("ZadAlmoslem", "Could not promote adhan service immediately", startupError);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_active", true).apply();
        ReminderAudioService.stop(this);
        RamadanAudioService.pauseForAdhan(this);
        // Keep the current radio/Quran player alive and pause it until the adhan finishes.
        Intent pauseAudio = new Intent(this, RadioPlaybackService.class);
        pauseAudio.setAction(RadioPlaybackService.ACTION_PAUSE_FOR_ADHAN);
        try { startService(pauseAudio); }
        catch (Exception error) {
            android.util.Log.w("ZadAlmoslem", "Could not pause background audio for adhan", error);
        }
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 7311, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, AdhanPlaybackService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 7312, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        Bitmap mosqueImage = createAzharMosqueBitmap();
        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان الآن وقت الصلاة")
                .setContentText("يرفع الآن أذان " + prayer)
                .setContentIntent(content)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreen, true)
                .addAction(android.R.drawable.ic_media_pause, "إيقاف الأذان", stopAction);
        if (mosqueImage != null) {
            builder.setLargeIcon(mosqueImage);
            builder.setStyle(new Notification.BigPictureStyle()
                    .bigPicture(mosqueImage)
                    .bigLargeIcon((Bitmap) null)
                    .setSummaryText("سكينة ودعاء وصلاة — تقبّل الله صلاتكم"));
        }
        Notification notification = builder.build();
        try {
            // Replace the lightweight foreground notification with the full Azhar artwork.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) notificationManager.notify(NOTIFICATION_ID, notification);
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().remove("adhan_last_start_error").remove("adhan_last_playback_error").putBoolean("adhan_playback_started", false).apply();
            playAdhan(selectedVoice, prayer);
        } catch (Exception error) {
            String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            android.util.Log.e("ZadAlmoslem", "Adhan foreground playback startup failed", error);
            showPlaybackErrorNotification("تعذر بدء خدمة الأذان: " + detail);
            stopPlayback();
        }
        return START_NOT_STICKY;
    }

    private Bitmap createAzharMosqueBitmap() {
        try {
            Drawable drawable = getDrawable(R.drawable.azhar_mosque_bg);
            if (drawable == null) return null;
            int width = 720, height = 360;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception ignored) { return null; }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "سكينة ودعاء وصلاة", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("إظهار شاشة الأذان على شاشة الهاتف عند دخول وقت الصلاة");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setBypassDnd(false);
            nm.createNotificationChannel(channel);
        }
    }

    private void playAdhan(String voice, String prayer) {
        stopPlayerOnly();
        try {
            player = new ExoPlayer.Builder(this).setWakeMode(androidx.media3.common.C.WAKE_MODE_LOCAL).build();
            player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_ALARM)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
                    // Route adhan through the phone's alarm-volume stream rather than
                    // the often-muted media stream. Background Quran/radio playback is
                    // paused explicitly by this service before the adhan starts.
                    .build(), false);
            android.content.SharedPreferences prefs = getSharedPreferences("zad_reminders", MODE_PRIVATE);
            String volumePrayerKey = "dhuhr";
            if (prayer != null && prayer.contains("الفجر")) volumePrayerKey = "fajr";
            else if (prayer != null && prayer.contains("العصر")) volumePrayerKey = "asr";
            else if (prayer != null && prayer.contains("المغرب")) volumePrayerKey = "maghrib";
            else if (prayer != null && prayer.contains("العشاء")) volumePrayerKey = "isha";
            int savedVolume = prefs.getInt("adhan_volume_" + volumePrayerKey, prefs.getInt("adhan_volume", 100));
            if (prefs.getBoolean("power_saver_enabled", false)) savedVolume = Math.min(savedVolume, 25);
            player.setVolume(Math.max(0, Math.min(100, savedVolume)) / 100f);
            String safeVoice = ("mustafa".equals(voice)||"husary".equals(voice)||"banna".equals(voice)||"basit".equals(voice)||"rifaat".equals(voice)||"haram".equals(voice)||"doha".equals(voice)||"fajr_cairo".equals(voice)||"fajr_haram".equals(voice)||"mishary".equals(voice)||"shuaisha".equals(voice)||"nuinaa".equals(voice)) ? voice : "doha";
            uiVoiceKey = safeVoice;
            uiPositionMs = 0L;
            uiDurationMs = 0L;
            File audioDir = new File(getFilesDir(), "adhan");
            if ("doha".equals(safeVoice)) {
                String prayerKey = "dhuhr";
                if (prayer != null && prayer.contains("الفجر")) prayerKey = "fajr";
                else if (prayer != null && prayer.contains("العصر")) prayerKey = "asr";
                else if (prayer != null && prayer.contains("المغرب")) prayerKey = "maghrib";
                else if (prayer != null && prayer.contains("العشاء")) prayerKey = "isha";
                safeVoice = "doha_" + prayerKey;
            }
            File mp3File = new File(audioDir, safeVoice + ".mp3");
            File oggFile = new File(audioDir, safeVoice + ".ogg");
            File audioFile = isPlayableAudioFile(mp3File) ? mp3File
                    : (isPlayableAudioFile(oggFile) ? oggFile : null);
            if (audioFile == null) {
                getSharedPreferences("zad_reminders", MODE_PRIVATE).edit()
                        .putBoolean("adhan_playback_started", false)
                        .putString("adhan_last_playback_error", "جاري تنزيل التسجيل الناقص: " + safeVoice).apply();
                stopPlayerOnly();
                downloadMissingAudio(safeVoice, voice, prayer, audioDir);
                return;
            }
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_playback_started", true).remove("adhan_last_playback_error").apply();
                    }
                    if (state == Player.STATE_ENDED) {
                        boolean playDua = playDuaAfterCompletion;
                        playDuaAfterCompletion = false;
                        boolean duaServiceRequested = false;
                        if (playDua) {
                            // Hand off to the dua foreground service before this adhan
                            // service drops foreground status; otherwise Android may reject it.
                            try {
                                ReminderAudioService.play(AdhanPlaybackService.this, "dua");
                                duaServiceRequested = true;
                            } catch (Exception error) {
                                android.util.Log.e("ZadAlmoslem", "Could not start post-adhan dua playback", error);
                            }
                        }
                        deferBackgroundAudioResume = duaServiceRequested;
                        stopPlayback();
                    }
                }
                @Override public void onPlayerError(PlaybackException error) {
                    getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_playback_started", false).putString("adhan_last_playback_error", error.getMessage() == null ? "صيغة الملف غير مدعومة" : error.getMessage()).apply();
                    showPlaybackErrorNotification("تعذر تشغيل ملف الأذان: " +
                            (error.getMessage() == null ? "صيغة الملف غير مدعومة" : error.getMessage()));
                    stopPlayback();
                }
            });
            boolean takbeerOnly = getSharedPreferences("zad_reminders", MODE_PRIVATE)
                    .getString("adhan_mode", "adhan").equals("takbeer");
            MediaItem.Builder mediaBuilder = new MediaItem.Builder().setUri(Uri.fromFile(audioFile));
            if (takbeerOnly) {
                // Play the opening takbeers from the selected adhan recording.
                mediaBuilder.setClippingConfiguration(new MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0)
                        .setEndPositionMs(15000)
                        .build());
            }
            player.setMediaItem(mediaBuilder.build());
            player.prepare();
            player.play();
            uiProgressHandler.removeCallbacks(uiProgressTicker);
            uiProgressHandler.post(uiProgressTicker);
        } catch (Exception e) {
            getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_playback_started", false).putString("adhan_last_playback_error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()).apply();
            showPlaybackErrorNotification("تعذر سكينة ودعاء وصلاة: " + (e.getMessage() == null ? "خطأ في ملف الصوت" : e.getMessage()));
            stopPlayback();
        }
    }

    private void downloadMissingAudio(String safeVoice, String originalVoice, String prayer, File audioDir) {
        audioDownloadInProgress = true;
        new Thread(() -> {
            HttpURLConnection connection = null;
            File target = new File(audioDir, safeVoice + ".mp3");
            File temp = new File(audioDir, safeVoice + ".mp3.part");
            try {
                if (!audioDir.exists() && !audioDir.mkdirs()) throw new Exception("تعذر إنشاء مجلد التسجيلات");
                String address = adhanDownloadUrl(safeVoice);
                if (address == null) throw new Exception("لا يوجد رابط تنزيل لهذا المؤذن");
                connection = (HttpURLConnection) new URL(address).openConnection();
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(90000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "ZadAlmoslem Android Native");
                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) throw new Exception("مصدر التسجيل أعاد " + code);
                long received = 0;
                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        received += count;
                        if (received > 25L * 1024 * 1024) throw new Exception("حجم التسجيل أكبر من المسموح");
                        output.write(buffer, 0, count);
                    }
                    output.getFD().sync();
                }
                if (!isPlayableAudioFile(temp)) throw new Exception("التسجيل الذي وصل من المصدر غير صالح أو غير مكتمل");
                if (target.exists() && !target.delete()) throw new Exception("تعذر استبدال التسجيل القديم");
                if (!temp.renameTo(target)) throw new Exception("تعذر حفظ التسجيل داخل التطبيق");
                new android.os.Handler(getMainLooper()).post(() -> {
                    audioDownloadInProgress = false;
                    getSharedPreferences("zad_reminders", MODE_PRIVATE).edit()
                            .remove("adhan_last_playback_error").putString("adhan_last_playback_error", "تم تنزيل التسجيل، جارٍ تشغيله").apply();
                    playAdhan(originalVoice, prayer);
                });
            } catch (Exception error) {
                String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                getSharedPreferences("zad_reminders", MODE_PRIVATE).edit()
                        .putBoolean("adhan_playback_started", false)
                        .putString("adhan_last_playback_error", "فشل تنزيل " + safeVoice + ": " + detail).apply();
                new android.os.Handler(getMainLooper()).post(() -> {
                    audioDownloadInProgress = false;
                    showPlaybackErrorNotification("تعذر تنزيل صوت الأذان: " + detail);
                    stopPlayback();
                });
            } finally {
                if (connection != null) connection.disconnect();
                if (temp.exists()) temp.delete();
            }
        }, "adhan-audio-download").start();
    }

    private String adhanDownloadUrl(String voice) throws Exception {
        if (voice != null && voice.startsWith("doha_")) {
            String prayerKey = voice.substring("doha_".length());
            String number;
            if ("fajr".equals(prayerKey)) number = "01";
            else if ("dhuhr".equals(prayerKey)) number = "02";
            else if ("asr".equals(prayerKey)) number = "03";
            else if ("maghrib".equals(prayerKey)) number = "04";
            else if ("isha".equals(prayerKey)) number = "05";
            else return null;
            String title = "fajr".equals(prayerKey) ? "Fajr" : ("dhuhr".equals(prayerKey) ? "Dhuhr" : ("asr".equals(prayerKey) ? "Asr" : ("maghrib".equals(prayerKey) ? "Maghrib" : "Isha")));
            return "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_" + number + "_" + title + "_Adhan.mp3";
        }
        String file;
        switch (voice) {
            case "fajr_cairo": file = "Adhan_Fajr_Cairo_Egypt_(أذان_الفجر_القاهرة_مصر).mp3"; break;
            case "fajr_haram": file = "Adhan_Fajr_Al_Haram_Al_Maki_(أذان_الفجر_الحرم_المكي).mp3"; break;
            case "mishary": file = "Mishary_Rashid_Alafasy_1_-_Kuwait_(مشاري_راشد_العفاسي_-_الكويت).mp3"; break;
            case "shuaisha": file = "Abul_Ainain_Shuaisha_-_Cairo_(أبو_العينين_شعيشع_-_القاهرة).mp3"; break;
            case "nuinaa": file = "Ahmed_Nuinaa_1_-_Egypt_(أحمد_نعينع_-_مصر).mp3"; break;
            case "basit": file = "Abdulbasit_Abdusamad_1_-_Egypt_(عبد_الباسط_عبد_الصمد_-_مصر).mp3"; break;
            case "mustafa": file = "Mustafa_Ismail_-_Egypt_(مصطفى_إسماعيل_-_مصر).mp3"; break;
            case "husary": file = "Mahmoud_Khalil_Al_Hussary_-_Cairo_(محمود_خليل_الحصري_-_القاهرة).mp3"; break;
            case "banna": file = "Mahmoud_Ali_Al_Banna_-_Cairo_(محمود_علي_البنا_-_القاهرة).mp3"; break;
            case "rifaat": file = "Muhammad_Refaat_-_Cairo_(محمد_رفعت_-_القاهرة).mp3"; break;
            case "haram": file = "Ali_Ibn_Ahmad_Mala_1_-_Al_Haram_Al_Maki_(علي_بن_أحمد_ملا_-_الحرم_المكي).mp3"; break;
            default: return null;
        }
        String encoded = java.net.URLEncoder.encode(file, "UTF-8").replace("+", "%20").replace("%28", "(").replace("%29", ")");
        return "https://cdn.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/" + encoded;
    }

    private void showPlaybackErrorNotification(String message) {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("تعذر سكينة ودعاء وصلاة")
                    .setContentText(message)
                    .setAutoCancel(true);
            nm.notify(NOTIFICATION_ID + 2, b.build());
        } catch (Exception ignored) {}
    }

    private boolean isPlayableAudioFile(File file) {
        if (file == null || !file.isFile() || file.length() < 10000) return false;
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            byte[] header = new byte[4];
            int count = in.read(header);
            boolean id3 = count >= 3 && header[0] == 0x49 && header[1] == 0x44 && header[2] == 0x33;
            boolean mpegFrame = count >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0;
            boolean ogg = count >= 4 && header[0] == 0x4F && header[1] == 0x67 && header[2] == 0x67 && header[3] == 0x53;
            return id3 || mpegFrame || ogg;
        } catch (Exception e) { return false; }
    }

    private void showMissingAudioNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("ملف الأذان غير موجود")
                .setContentText("افتح سكينة ودعاء وصلاة واتصل بالإنترنت لتحميل صوت الأذان مرة واحدة.")
                .setAutoCancel(true);
        nm.notify(NOTIFICATION_ID + 1, b.build());
    }

    private void stopPlayerOnly() {
        uiProgressHandler.removeCallbacks(uiProgressTicker);
        uiPositionMs = 0L;
        uiDurationMs = 0L;
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    private void resumeBackgroundAudioAfterAdhan() {
        Intent resumeAudio = new Intent(this, RadioPlaybackService.class);
        resumeAudio.setAction(RadioPlaybackService.ACTION_RESUME_AFTER_ADHAN);
        try { startService(resumeAudio); }
        catch (Exception error) {
            android.util.Log.w("ZadAlmoslem", "Could not resume background audio after adhan", error);
        }
    }

    private void stopPlayback() {
        audioDownloadInProgress = false;
        getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_active", false).apply();
        stopPlayerOnly();
        // Request radio/Quran resumption while this service is still foreground.
        // This avoids starting another background service after dropping foreground status.
        if (!deferBackgroundAudioResume) resumeBackgroundAudioAfterAdhan();
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        stopPlayerOnly();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
