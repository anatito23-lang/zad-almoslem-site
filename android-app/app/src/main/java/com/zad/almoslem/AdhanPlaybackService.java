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

public class AdhanPlaybackService extends Service {
    public static final String ACTION_PLAY = "com.zad.almoslem.PLAY_ADHAN";
    public static final String ACTION_STOP = "com.zad.almoslem.STOP_ADHAN";
    private static final String CHANNEL_ID = "zad_adhan_playback";
    private static final int NOTIFICATION_ID = 7310;
    private MediaPlayer player;
    private boolean playDuaAfterCompletion;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        playDuaAfterCompletion = intent != null && intent.getBooleanExtra("play_dua_after", false);
        getSharedPreferences("zad_reminders", MODE_PRIVATE).edit().putBoolean("adhan_active", true).apply();
        ReminderAudioService.stop(this);
        String prayer = intent == null ? "الصلاة" : intent.getStringExtra("prayer");
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
        playAdhan(voice);
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

    private void playAdhan(String voice) {
        stopPlayerOnly();
        try {
            player = new MediaPlayer();
            player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            int savedVolume = getSharedPreferences("zad_reminders", MODE_PRIVATE).getInt("adhan_volume", 100);
            float adhanVolume = Math.max(0, Math.min(100, savedVolume)) / 100f;
            player.setVolume(adhanVolume, adhanVolume);
            String safeVoice = ("mustafa".equals(voice)||"husary".equals(voice)||"banna".equals(voice)||"basit".equals(voice)||"naqshbandi".equals(voice)||"tobar".equals(voice)||"haram".equals(voice)||"rifaat".equals(voice)||"fajr".equals(voice)||"beautiful".equals(voice)) ? voice : "basit";
            File audioDir = new File(getFilesDir(), "adhan");
            File audioFile = new File(audioDir, safeVoice + ".mp3");
            if (!audioFile.isFile() || audioFile.length() < 10000) audioFile = new File(audioDir, safeVoice + ".ogg");
            if (!audioFile.isFile() || audioFile.length() < 10000) {
                stopPlayback();
                showMissingAudioNotification();
                return;
            }
            player.setDataSource(audioFile.getAbsolutePath());
            player.setOnPreparedListener(mp -> {
                try {
                    mp.start();
                    getSharedPreferences("zad_reminders", MODE_PRIVATE).edit()
                            .putBoolean("adhan_active", true).apply();
                } catch (Exception startError) {
                    showPlaybackErrorNotification("تعذر بدء تشغيل ملف الأذان.");
                    stopPlayback();
                }
            });
            player.setOnCompletionListener(mp -> {
                boolean playDua = playDuaAfterCompletion;
                playDuaAfterCompletion = false;
                stopPlayback();
                if (playDua) ReminderAudioService.play(this, "dua");
            });
            player.setOnErrorListener((mp, what, extra) -> {
                showPlaybackErrorNotification("ملف الأذان تالف أو تعذر فك ترميزه ("
                        + what + "/" + extra + "). أعد تنزيل صوت المؤذن من الإعدادات.");
                stopPlayback();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            showPlaybackErrorNotification("تعذر تجهيز ملف الأذان: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            stopPlayback();
        }
    }

    private void showPlaybackErrorNotification(String message) {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("تعذر تشغيل الأذان")
                    .setContentText(message)
                    .setAutoCancel(true);
            nm.notify(NOTIFICATION_ID + 2, b.build());
        } catch (Exception ignored) {}
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
