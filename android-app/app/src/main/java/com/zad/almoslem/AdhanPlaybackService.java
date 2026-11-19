package com.zad.almoslem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import java.io.File;

public class AdhanPlaybackService extends Service {
    private static final String CHANNEL = "zad_adhan_playback";
    private static final int NOTIFICATION_ID = 7530;
    private MediaPlayer player;

    public static void play(Context context, String prayer, String voice, int volume) {
        Intent i = new Intent(context, AdhanPlaybackService.class);
        i.setAction("PLAY");
        i.putExtra("prayer", prayer);
        i.putExtra("voice", voice);
        i.putExtra("volume", volume);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i);
        else context.startService(i);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        createChannel();
        String prayer = intent == null ? "الصلاة" : intent.getStringExtra("prayer");
        String voice = intent == null ? "" : intent.getStringExtra("voice");
        int volume = intent == null ? 80 : intent.getIntExtra("volume", 80);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("زاد المسلم")
                .setContentText("أذان " + (prayer == null ? "" : prayer))
                .setOngoing(true);
        startForeground(NOTIFICATION_ID, b.build());
        stopPlayer();
        try {
            String safe = (voice == null ? "" : voice).replaceAll("[^\\p{L}\\p{N}]+", "_");
            File file = new File(new File(getFilesDir(), "adhan"), safe + ".mp3");
            if (!file.isFile() || file.length() < 10000) { stopSelf(); return START_NOT_STICKY; }
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            float gain = Math.max(0, Math.min(100, volume)) / 100f;
            player.setVolume(gain, gain);
            player.setOnCompletionListener(mp -> { stopPlayer(); stopForeground(true); stopSelf(); });
            player.setOnErrorListener((mp, what, extra) -> { stopPlayer(); stopForeground(true); stopSelf(); return true; });
            player.setDataSource(file.getAbsolutePath());
            player.prepareAsync();
            player.setOnPreparedListener(MediaPlayer::start);
        } catch (Exception e) { stopPlayer(); stopForeground(true); stopSelf(); }
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(CHANNEL, "تشغيل الأذان", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }
    }
    private void stopPlayer() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }
    @Override public void onDestroy() { stopPlayer(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
