package com.nidaa.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL="nidaa_prayer_times";
    @Override public void onReceive(Context context,Intent intent){
        String name=intent.getStringExtra("prayer_name");
        if(name==null||name.trim().isEmpty())name="الصلاة";
        boolean preAlert=intent.getBooleanExtra("pre_alert",false);
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"تنبيهات الصلاة",NotificationManager.IMPORTANCE_HIGH));
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        String title=preAlert?"اقترب موعد صلاة "+name:"حان موعد صلاة "+name;
        String message=preAlert?"باقي 15 دقيقة على الصلاة":"حيّ على الصلاة";
        Notification notification=builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText(message).setAutoCancel(true).build();
        int id=intent.getIntExtra("prayer_id",-1);
        nm.notify((preAlert?1000:0)+Math.max(0,id),notification);
        if(!preAlert)PrayerAlarmScheduler.scheduleNextDay(context,id,name);
    }
}