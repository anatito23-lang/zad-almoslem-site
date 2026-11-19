package com.zad.almoslem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import java.io.File;
import java.io.FileInputStream;

public class AdhanPlaybackService extends Service {
    public static final String ACTION_PLAY = "com.zad.almoslem.PLAY_ADHAN";
    public static final String ACTION_STOP = "com.zad.almoslem.STOP_ADHAN";
    public static final String ACTION_SET_VOLUME = "com.zad.almoslem.SET_ADHAN_VOLUME";
    public static final String ACTION_SET_PRAYER_VOLUME = "com.zad.almoslem.SET_PRAYER_ADHAN_VOLUME";
    private static final String CHANNEL_ID = "zad_adhan_playback";
    private static final int NOTIFICATION_ID = 7310;
    private MediaPlayer player;
    private boolean playDuaAfterCompletion;
    private String activePrayer;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SET_PRAYER_VOLUME.equals(intent.getAction())) {
            String prayer = intent.getStringExtra("prayer");
            int level = Math.max(0, Math.min(100, intent.getIntExtra("volume", 100)));
            if (player != null && prayer != null && prayer.equals(activePrayer)) {
                float value = level / 100f;
                try { player.setVolume(value, value); } catch (Exception ignored) {}
            }
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_SET_VOLUME.equals(intent.getAction())) {
            int level = Math.max(0, Math.min(100, intent.getIntExtra("volume", 100)));
            if (player != null) {
                float value = level / 100f;
                try { player.setVolume(value, value); } catch (Exception ignored) {}
            }
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        playDuaAfterCompletion = intent != null && intent.getBooleanExtra("play_dua_after", false);
        getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_active", true).apply();
        ReminderAudioService.stop(this);
        String prayer = intent == null ? "الصلاة" : intent.getStringExtra("prayer");
        activePrayer = prayer;
        createChannel();
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 7311, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, AdhanPlaybackService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 7312, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان الآن وقت الصلاة")
                .setContentText("يرفع الآن أذان " + prayer)
                .setContentIntent(content)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "إيقاف الأذان", stopAction)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        String voice = intent == null ? "basit" : intent.getStringExtra("voice");
        playAdhan(voice, prayer);
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "تشغيل الأذان", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("إشعار مستمر أثناء تشغيل الأذان");
            nm.createNotificationChannel(channel);
        }
    }

    private void playAdhan(String voice, String prayer) {
        stopPlayerOnly();
        try {
            player = new MediaPlayer();
            player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            android.content.SharedPreferences prefs = getSharedPreferences("zad_reminders", MODE_PRIVATE);
            String volumePrayerKey = "dhuhr";
            if (prayer != null && prayer.contains("الفجر")) volumePrayerKey = "fajr";
            else if (prayer != null && prayer.contains("العصر")) volumePrayerKey = "asr";
            else if (prayer != null && prayer.contains("المغرب")) volumePrayerKey = "maghrib";
            else if (prayer != null && prayer.contains("العشاء")) volumePrayerKey = "isha";
            int savedVolume = prefs.getInt("adhan_volume_" + volumePrayerKey, prefs.getInt("adhan_volume", 100));
            float adhanVolume = Math.max(0, Math.min(100, savedVolume)) / 100f;
            player.setVolume(adhanVolume, adhanVolume);
            String safeVoice = ("mustafa".equals(voice)||"husary".equals(voice)||"banna".equals(voice)||"basit".equals(voice)||"haram".equals(voice)||"doha".equals(voice)) ? voice : "doha";
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
                stopPlayback();
                showMissingAudioNotification();
                return;
            }
            player.setDataSource(audioFile.getAbsolutePath());
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(mp -> {
                boolean playDua = playDuaAfterCompletion;
                playDuaAfterCompletion = false;
                stopPlayback();
                if (playDua) ReminderAudioService.play(this, "dua");
            });
            player.setOnErrorListener((mp, what, extra) -> {
                showPlaybackErrorNotification("تعذر فك ترميز ملف الأذان أو تشغيله. أعد تنزيل التسجيل المختار.");
                stopPlayback();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            showPlaybackErrorNotification("تعذر تشغيل الأذان: " + (e.getMessage() == null ? "خطأ في ملف الصوت" : e.getMessage()));
            stopPlayback();
        }
    }

    private void showPlaybackErrorNotification(String message) {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("تعذر تشغيل الأذان")
                    .setContentText(message)
                    .setAutoCancel(true);
            nm.notify(NOTIFICATION_ID + 2, b.build());
        } catch (Exception ignored) {}
    }

    private boolean isPlayableAudioFile(File file) {
        if (file == null || !file.isFile() || file.length() < 10000) return false;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] header = new byte[4];
            int count = in.read(header);
            boolean id3 = count >= 3 && header[0] == 0x49 && header[1] == 0x44 && header[2] == 0x33;
            boolean mpegFrame = count >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0;
            boolean ogg = count >= 4 && header[0] == 0x4F && header[1] == 0x67 && header[2] == 0x67 && header[3] == 0x53;
            return id3 || mpegFrame || ogg;
        } catch (Exception e) {
            return false;
        }
    }

    private void showMissingAudioNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("ملف الأذان غير موجود")
                .setContentText("افتح زاد المسلم واتصل بالإنترنت لتحميل صوت الأذان مرة واحدة.")
                .setAutoCancel(true);
        nm.notify(NOTIFICATION_ID + 1, b.build());
    }

    private void stopPlayerOnly() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    private void stopPlayback() {
        getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_active", false).apply();
        stopPlayerOnly();
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        stopPlayerOnly();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
