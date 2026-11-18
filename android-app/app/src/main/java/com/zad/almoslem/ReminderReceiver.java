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
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "zad_reminders";
    private static final String PRAYER_CHANNEL_ID = "zad_prayer_alerts";

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
        boolean prayer=key.startsWith("prayer_");
        android.content.SharedPreferences prefs=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        boolean adhanEnabled=prefs.getBoolean("adhan_enabled",true);
        if(prayer && adhanEnabled) {
            String prayerName=key.substring("prayer_".length());
            Intent audio=new Intent(context,AdhanPlaybackService.class);
            audio.setAction(AdhanPlaybackService.ACTION_PLAY);
            audio.putExtra("prayer",prayerName);
            try {
                if(Build.VERSION.SDK_INT>=26) context.startForegroundService(audio);
                else context.startService(audio);
            } catch(Exception ignored) {}
        }
        if(prayer) {\n            long next=prefs.getLong(key,0L)+24L*60L*60L*1000L;\n            prefs.edit().putLong(key,next).apply();\n            schedule(context,key,next,title,false);\n        }\n        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId=prayer?PRAYER_CHANNEL_ID:CHANNEL_ID;
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch;
            if(prayer){
                ch=new NotificationChannel(PRAYER_CHANNEL_ID,"مواعيد الصلاة",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("إشعار دخول وقت الصلاة؛ صوت الأذان يعمل بشكل مستقل");
                ch.setSound(null,null);
            } else {
                ch=new NotificationChannel(CHANNEL_ID,"تنبيهات زاد المسلم",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("تنبيهات الأذكار وقيام الليل");
            }
            nm.createNotificationChannel(ch);
        }
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channelId):new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("قلبي اطمأن – زاد المسلم").setContentText(title==null?"تذكير من زاد المسلم":title).setAutoCancel(true).setContentIntent(content);
        if(!prayer) b.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);
        nm.notify(key.hashCode(),b.build());
        if(intent.getBooleanExtra("daily",false)){
            String k=key.replace("daily_","");
            if(prefs.getBoolean(k+"_enabled",false)) scheduleDaily(context,k,prefs.getInt(k+"_hour",7),prefs.getInt(k+"_minute",0),prefs.getString(k+"_title","تذكير من زاد المسلم"));
        }
    }
}
