package com.zad.almoslem;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "zad_reminders";
    public static void scheduleDaily(Context context, String key, int hour, int minute, String title) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders", Context.MODE_PRIVATE);
        p.edit().putInt(key+"_hour",hour).putInt(key+"_minute",minute).putString(key+"_title",title).putBoolean(key+"_enabled",true).apply();
        Calendar c=Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,hour); c.set(Calendar.MINUTE,minute); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        if(c.getTimeInMillis()<=System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR,1);
        schedule(context,key,c.getTimeInMillis(),title,true);
    }
    public static void cancelDaily(Context context,String key) {
        context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE).edit().putBoolean(key+"_enabled",false).apply();
        cancel(context,key);
    }
    public static void schedulePrayer(Context context,String name,long at) {
        if(at<=System.currentTimeMillis()) return;
        String key="prayer_"+name;
        context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE).edit().putLong(key,at).apply();
        schedule(context,key,at,"حان الآن وقت صلاة "+name,false);
    }
    private static void schedule(Context context,String key,long at,String title,boolean daily) {
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i=new Intent(context,ReminderReceiver.class);
        i.putExtra("key",key); i.putExtra("title",title); i.putExtra("daily",daily);
        int id=key.hashCode();
        PendingIntent pi=PendingIntent.getBroadcast(context,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23) {
            if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        } else am.setExact(AlarmManager.RTC_WAKEUP,at,pi);
    }
    private static void cancel(Context context,String key) {
        Intent i=new Intent(context,ReminderReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(context,key.hashCode(),i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
        if(pi!=null){((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).cancel(pi);pi.cancel();}
    }
    @Override public void onReceive(Context context,Intent intent) {
        String key=intent.getStringExtra("key"),title=intent.getStringExtra("title");
        if(key==null)return;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL_ID,"تنبيهات زاد المسلم",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("تنبيهات الصلاة والأذكار وقيام الليل");nm.createNotificationChannel(ch);}
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL_ID):new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("قلبي اطمأن – زاد المسلم").setContentText(title==null?"تذكير من زاد المسلم":title).setAutoCancel(true).setContentIntent(content).setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);
        nm.notify(key.hashCode(),b.build());
        if(intent.getBooleanExtra("daily",false)){
            android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
            String k=key.replace("daily_","");
            if(p.getBoolean(k+"_enabled",false)) scheduleDaily(context,k,p.getInt(k+"_hour",7),p.getInt(k+"_minute",0),p.getString(k+"_title","تذكير من زاد المسلم"));
        }
    }
}