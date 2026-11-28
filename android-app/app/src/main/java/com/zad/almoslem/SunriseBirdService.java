package com.zad.almoslem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

public class SunriseBirdService extends Service {
    private static final String CHANNEL="zad_sunrise_birds";
    private MediaPlayer player;

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        createChannel();
        Notification.Builder builder=Build.VERSION.SDK_INT>=26
                ?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        Notification notification=builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت الشروق")
                .setContentText("صباح هادئ مع تغريد العصافير 🌿")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true).build();
        startForeground(5822,notification);
        if(player!=null) return START_NOT_STICKY;
        try {
            player=MediaPlayer.create(this,R.raw.sunrise_birds);
            if(player==null){stopSelf(startId);return START_NOT_STICKY;}
            if(Build.VERSION.SDK_INT>=21) player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setVolume(0.68f,0.68f);
            player.setOnCompletionListener(mp->stopSelf(startId));
            player.setOnErrorListener((mp,what,extra)->{stopSelf(startId);return true;});
            player.start();
        } catch(Exception e) {
            android.util.Log.e("ZadAlmoslem","Sunrise birdsong playback failed",e);
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if(Build.VERSION.SDK_INT>=26) {
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel channel=new NotificationChannel(CHANNEL,"تنبيه الشروق بصوت العصافير",NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("تنبيه هادئ عند الشروق مع صوت تغريد العصافير");
            channel.setSound(null,null);
            channel.enableVibration(false);
            nm.createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        if(player!=null){try{if(player.isPlaying())player.stop();}catch(Exception ignored){}player.release();player=null;}
        if(Build.VERSION.SDK_INT>=24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}