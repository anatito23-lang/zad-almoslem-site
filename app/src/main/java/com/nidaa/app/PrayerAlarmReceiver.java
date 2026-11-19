package com.nidaa.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL="nidaa_prayers";
    @Override public void onReceive(Context context,Intent intent){
        if(!context.getSharedPreferences("nidaa",Context.MODE_PRIVATE).getBoolean("reminders",false))return;
        String name=intent.getStringExtra("prayer_name");int id=intent.getIntExtra("prayer_id",-1);
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"تنبيهات الصلاة",NotificationManager.IMPORTANCE_HIGH));
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("حان وقت صلاة "+name).setContentText("حيّ على الصلاة").setAutoCancel(true).setContentIntent(content).setCategory(Notification.CATEGORY_ALARM);
        nm.notify(100+id,b.build());
        PrayerAlarmScheduler.scheduleNextDay(context,id,name);
    }
}