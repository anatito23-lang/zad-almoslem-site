package com.zad.almoslem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class FajrAlarmReceiver extends android.content.BroadcastReceiver {
    private static final String CHANNEL="zad_fajr_alarm";
    @Override public void onReceive(Context context,Intent intent) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if(!p.getBoolean("fajr_alarm_enabled",false)) return;
        // Queue tomorrow's alarm immediately so dismissing today's alarm never disables recurrence.
        FajrAlarmScheduler.scheduleNext(context);
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel ch=new NotificationChannel(CHANNEL,"منبه الفجر",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("تنبيه منبه الفجر");
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
        Intent open=new Intent(context,FajrAlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent full=PendingIntent.getActivity(context,4818,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        Notification n=builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت منبه الفجر")
                .setContentText("أجب عن السؤال لإيقاف المنبه")
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(full,true)
                .setContentIntent(full)
                .setAutoCancel(true).build();
        nm.notify(4818,n);
    }
}