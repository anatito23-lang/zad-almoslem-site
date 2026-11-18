package com.zad.almoslem;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import java.io.IOException;

public class RadioPlaybackService extends Service {
    public static final String ACTION_PLAY="com.zad.almoslem.PLAY_RADIO";
    public static final String ACTION_STOP="com.zad.almoslem.STOP_RADIO";
    private static final String CHANNEL="zad_radio_playback";
    private static final int NOTIFICATION=7410;
    private MediaPlayer player;

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && ACTION_STOP.equals(intent.getAction())) { stopRadio(); return START_NOT_STICKY; }
        createChannel();
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(this,7411,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,RadioPlaybackService.class); stop.setAction(ACTION_STOP);
        PendingIntent stopAction=PendingIntent.getService(this,7412,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("إذاعة القرآن الكريم")
          .setContentText("البث مستمر في الخلفية").setContentIntent(content).setOngoing(true)
          .addAction(android.R.drawable.ic_media_pause,"إيقاف الإذاعة",stopAction);
        Notification n=b.build();
        if(Build.VERSION.SDK_INT>=29) startForeground(NOTIFICATION,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NOTIFICATION,n);
        String url=intent==null?null:intent.getStringExtra("url");
        String kind=intent==null?"radio":intent.getStringExtra("kind");
        boolean allowedRadio="https://qurango.net/radio/tarateel".equals(url) ||
             "https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8".equals(url);
        boolean allowedQuran=false;
        try {
            android.net.Uri parsed=android.net.Uri.parse(url);
            String host=parsed.getHost();
            allowedQuran="quran".equals(kind) && "https".equalsIgnoreCase(parsed.getScheme()) && host!=null &&
                (host.toLowerCase().endsWith("mp3quran.net") || host.toLowerCase().endsWith("mp3quran.com"));
        } catch(Exception ignored) {}
        if(!allowedRadio && !allowedQuran) { stopRadio(); return START_NOT_STICKY; }
        String label=intent==null?null:intent.getStringExtra("label");
        if(label!=null) {
            Notification.Builder updated=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
            updated.setSmallIcon(android.R.drawable.ic_media_play)
              .setContentTitle("quran".equals(kind)?"استماع القرآن الكريم":"إذاعة القرآن الكريم")
              .setContentText(label+" — يعمل في الخلفية").setContentIntent(content).setOngoing(true)
              .addAction(android.R.drawable.ic_media_pause,"إيقاف الصوت",stopAction);
            Notification n2=updated.build();
            if(Build.VERSION.SDK_INT>=29) startForeground(NOTIFICATION,n2,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(NOTIFICATION,n2);
        }
        play(url);
        return START_NOT_STICKY;
    }
    private void createChannel() {
        if(Build.VERSION.SDK_INT>=26) {
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel c=new NotificationChannel(CHANNEL,"إذاعة القرآن الكريم",NotificationManager.IMPORTANCE_LOW);
            c.setDescription("يظهر أثناء استمرار تشغيل الإذاعة في الخلفية"); nm.createNotificationChannel(c);
        }
    }
    private void play(String url) {
        releasePlayer();
        try {
            player=new MediaPlayer();
            player.setWakeMode(getApplicationContext(),PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnErrorListener((mp,what,extra)->{stopRadio();return true;});
            player.setOnCompletionListener(mp->stopRadio());
            player.setDataSource(url);
            player.prepareAsync();
        } catch(Exception e) { stopRadio(); }
    }
    private void releasePlayer() {
        if(player!=null) { try { player.reset(); } catch(Exception ignored){} try { player.release(); } catch(Exception ignored){} player=null; }
    }
    private void stopRadio() { releasePlayer(); stopForeground(true); stopSelf(); }
    @Override public void onDestroy(){releasePlayer();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}