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
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.io.File;
import java.util.Locale;

public class ReminderAudioService extends Service {
    public static final String ACTION_PLAY = "com.zad.almoslem.PLAY_REMINDER_AUDIO";
    public static final String ACTION_STOP = "com.zad.almoslem.STOP_REMINDER_AUDIO";
    private static final String CHANNEL = "zad_reminder_audio";
    private static final int NOTIFICATION_ID = 7520;
    private MediaPlayer player;
    private TextToSpeech tts;

    public static void play(Context context, String kind) {
        Intent i = new Intent(context, ReminderAudioService.class);
        i.setAction(ACTION_PLAY);
        i.putExtra("kind", kind);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i);
        else context.startService(i);
    }

    public static void stop(Context context) {
        Intent i = new Intent(context, ReminderAudioService.class);
        i.setAction(ACTION_STOP);
        try { context.startService(i); } catch (Exception ignored) {}
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            finishPlayback();
            return START_NOT_STICKY;
        }
        String kind = intent == null ? "qiyam" : intent.getStringExtra("kind");
        if (!"morning".equals(kind) && !"evening".equals(kind) && !"dua".equals(kind)) kind = "qiyam";
        createChannel();
        String title = "morning".equals(kind) ? "أذكار الصباح" :
                "evening".equals(kind) ? "أذكار المساء" :
                "dua".equals(kind) ? "دعاء ما بعد الأذان" : "قيام الليل";
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("زاد المسلم")
                .setContentText("يتم تشغيل " + title)
                .setOngoing(true);
        startForeground(NOTIFICATION_ID, b.build());
        boolean adhanActive = getSharedPreferences("zad_reminders", MODE_PRIVATE).getBoolean("adhan_active", false);
        if (adhanActive && !"dua".equals(kind)) { finishPlayback(); return START_NOT_STICKY; }
        Intent stopRadio = new Intent(this, RadioPlaybackService.class);
        stopRadio.setAction(RadioPlaybackService.ACTION_STOP);
        try { startService(stopRadio); } catch (Exception ignored) {}
        stopCurrentPlayers();
        if ("qiyam".equals(kind)) {
            speak("حان وقت قيام الليل، لا تنسَ نصيبك من صلاة الليل");
        } else {
            playFile(kind);
        }
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel c = new NotificationChannel(CHANNEL, "التذكيرات الصوتية", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("تشغيل أذكار الصباح والمساء ودعاء ما بعد الأذان");
            nm.createNotificationChannel(c);
        }
    }

    private void playFile(String kind) {
        try {
            File file = new File(new File(getFilesDir(), "reminders"), kind + ".mp3");
            if (!file.isFile() || file.length() < 10000) {
                if ("dua".equals(kind)) { finishPlayback(); return; }
                speak("حان وقت " + ("morning".equals(kind) ? "أذكار الصباح" : "أذكار المساء") + "، لا تنسَ ذكر الله");
                return;
            }
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setOnCompletionListener(mp -> finishPlayback());
            player.setOnErrorListener((mp, what, extra) -> { finishPlayback(); return true; });
            player.setOnPreparedListener(MediaPlayer::start);
            player.setDataSource(file.getAbsolutePath());
            player.prepareAsync();
        } catch (Exception e) { finishPlayback(); }
    }

    private void speak(String message) {
        try {
            tts = new TextToSpeech(this, status -> {
                if (status != TextToSpeech.SUCCESS || tts == null) { finishPlayback(); return; }
                tts.setLanguage(new Locale("ar"));
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {}
                    @Override public void onDone(String id) { finishPlayback(); }
                    @Override public void onError(String id) { finishPlayback(); }
                });
                if (Build.VERSION.SDK_INT >= 21)
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "zad-reminder");
                else
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            });
        } catch (Exception e) { finishPlayback(); }
    }

    private void stopCurrentPlayers() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Exception ignored) {}
            tts = null;
        }
    }

    private void finishPlayback() {
        stopCurrentPlayers();
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() { stopCurrentPlayers(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
