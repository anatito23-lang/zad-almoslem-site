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
        schedulePrayerExtras(context,name,at);
    }
    public static void startSilentMode(Context context,int minutes) {
        if(Build.VERSION.SDK_INT<23 || minutes<=0) return;
        try {
            NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager==null || !manager.isNotificationPolicyAccessGranted()) return;
            android.content.SharedPreferences prefs=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
            long until=System.currentTimeMillis()+Math.max(1,Math.min(180,minutes))*60000L;
            if(prefs.getLong("adhan_silent_until",0L)<=System.currentTimeMillis())
                prefs.edit().putInt("adhan_previous_filter",manager.getCurrentInterruptionFilter()).apply();
            prefs.edit().putLong("adhan_silent_until",until).apply();
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            schedule(context,"silent_restore",until,"انتهت مدة الوضع الصامت",false);
        } catch(Exception ignored) {}
    }
    private static void schedulePrayerExtras(Context context,String name,long at) {
        try {
            android.content.SharedPreferences prefs=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
            org.json.JSONObject settings=new org.json.JSONObject(prefs.getString("adhan_settings","{}"));
            long lead=Math.max(0,Math.min(120,settings.optInt("lead",15)));
            long iqama=Math.max(0,Math.min(180,settings.optInt("iqama",20)));
            if(lead>0 && at-lead*60000L>System.currentTimeMillis())
                schedule(context,"pre_prayer_"+name,at-lead*60000L,"اقترب موعد صلاة "+name,false);
            if(iqama>0)
                schedule(context,"iqama_prayer_"+name,at+iqama*60000L,"حان وقت الإقامة لصلاة "+name,false);
        } catch(Exception ignored) {}
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
        if (intent == null) return;
        String key=intent.getStringExtra("key"),title=intent.getStringExtra("title");
        if(key==null)return;
        android.content.SharedPreferences prefs=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if("silent_restore".equals(key)) {
            try {
                if(Build.VERSION.SDK_INT>=23) {
                    NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if(manager!=null && manager.isNotificationPolicyAccessGranted())
                        manager.setInterruptionFilter(prefs.getInt("adhan_previous_filter",NotificationManager.INTERRUPTION_FILTER_ALL));
                }
                prefs.edit().putLong("adhan_silent_until",0L).apply();
            } catch(Exception ignored) {}
        }
        boolean prayer=key.startsWith("prayer_");
        boolean prePrayer=key.startsWith("pre_prayer_");
        boolean iqama=key.startsWith("iqama_prayer_");
        String prayerName=prayer?key.substring("prayer_".length()):"";
        if (!prayer && !prePrayer && !iqama && ("morning".equals(key) || "evening".equals(key) || "qiyam".equals(key))) {
            try { ReminderAudioService.play(context, key); } catch (Exception ignored) {}
        }
        if(prayer) {
            try {
                org.json.JSONObject settings=new org.json.JSONObject(prefs.getString("adhan_settings","{}"));
                org.json.JSONObject items=settings.optJSONObject("items");
                String itemKey=prayerName.equals("الفجر")?"fajr":prayerName.equals("الظهر")?"dhuhr":prayerName.equals("العصر")?"asr":prayerName.equals("المغرب")?"maghrib":"isha";
                org.json.JSONObject item=items==null?null:items.optJSONObject(itemKey);
                boolean shouldPlay=settings.optBoolean("enabled",false) && item!=null && item.optBoolean("enabled",true) && "full".equals(item.optString("style","full"));
                String voice=item==null?"":item.optString("voice","عبد الباسط عبد الصمد");
                int volume=item==null?80:item.optInt("volume",80);
                int silentDuration=settings.optBoolean("silentEnabled",false)?Math.max(1,Math.min(180,settings.optInt("silent",15))):0;
                if(shouldPlay && AdhanPlaybackService.hasAudio(context,voice))
                    AdhanPlaybackService.play(context,prayerName,voice,volume,silentDuration);
                else if(silentDuration>0) startSilentMode(context,silentDuration);
            } catch(Exception ignored) {}
        }
        if(prayer) {
            // Advance by one local calendar day, not a fixed 24-hour duration.
            Calendar nextPrayer = Calendar.getInstance();
            long previous = prefs.getLong(key, 0L);
            if (previous <= 0L) previous = System.currentTimeMillis();
            nextPrayer.setTimeInMillis(previous);
            nextPrayer.add(Calendar.DAY_OF_YEAR, 1);
            long next = nextPrayer.getTimeInMillis();
            prefs.edit().putLong(key,next).apply();
            schedule(context,key,next,title,false);
            schedulePrayerExtras(context,prayerName,next);
        }
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId=prayer?PRAYER_CHANNEL_ID:CHANNEL_ID;
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch;
            if(prayer){
                ch=new NotificationChannel(PRAYER_CHANNEL_ID,"إشعارات الصلاة الصامتة",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("إشعار دخول وقت الصلاة بدون صوت");
                ch.setSound(null,null);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
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
        b.setDefaults(Notification.DEFAULT_VIBRATE);
        nm.notify(key.hashCode(),b.build());
        if(intent.getBooleanExtra("daily",false)){
            String k=key.replace("daily_","");
            if(prefs.getBoolean(k+"_enabled",false)) scheduleDaily(context,k,prefs.getInt(k+"_hour",7),prefs.getInt(k+"_minute",0),prefs.getString(k+"_title","تذكير من زاد المسلم"));
        }
    }
}
