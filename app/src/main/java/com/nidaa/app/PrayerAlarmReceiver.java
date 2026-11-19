package com.nidaa.app;

import android.app.BroadcastOptions;
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
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"تنبيهات الصلاة",NotificationManager.IMPORTANCE_HIGH));
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        Notification notification=builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان موعد صلاة "+name)
            .setContentText("حيّ على الصلاة")
            .setAutoCancel(true).build();
        nm.notify((int)(System.currentTimeMillis()%Integer.MAX_VALUE),notification);
        int id=intent.getIntExtra("prayer_id",-1);
        PrayerAlarmScheduler.scheduleNextDay(context,id,name);
    }
}
