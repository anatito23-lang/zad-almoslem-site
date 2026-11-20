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
    private static final String PRAYER_TONE_CHANNEL_ID = "zad_prayer_tone";

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

    public static void ensureDefaultDailyReminders(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if(!p.contains("morning_enabled"))
            scheduleDaily(context,"morning",7,0,"حان وقت أذكار الصباح، لا تنس وردك من ذكر الله.");
        if(!p.contains("evening_enabled"))
            scheduleDaily(context,"evening",17,0,"حان وقت أذكار المساء، لا تنس وردك من ذكر الله.");
    }
    private static void scheduleApproachAlert(Context context,String name,long prayerAt) {
        String alertKey="approach_"+name;
        android.content.SharedPreferences prefs=context.getSharedPreferences(SILENT_PREFS,Context.MODE_PRIVATE);
        if(!prefs.getBoolean("approach_alert_enabled",true)){cancel(context,alertKey);return;}
        int minutes=Math.max(1,Math.min(60,prefs.getInt("approach_alert_minutes",15)));
        long alertAt=prayerAt-minutes*60L*1000L;
        if(alertAt>System.currentTimeMillis()) {
            schedule(context,alertKey,alertAt,"اقتربت صلاة "+name+"، بقي على الصلاة "+minutes+" دقيقة. استعد للصلاة.",false);
        } else {
            cancel(context,alertKey);
        }
    }
    public static void schedulePrayer(Context context,String name,long at) {
        if(at<=System.currentTimeMillis()) return;
        String key="prayer_"+name;
        context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE).edit().putLong(key,at).apply();
        schedule(context,key,at,"حان الآن وقت صلاة "+name,false);
        scheduleApproachAlert(context,name,at);
    }
    public static void rescheduleApproachAlerts(Context context) {
        android.content.SharedPreferences prefs=context.getSharedPreferences(SILENT_PREFS,Context.MODE_PRIVATE);
        long now=System.currentTimeMillis();
        for(String name:new String[]{"الفجر","الظهر","العصر","المغرب","العشاء"}) {
            long at=prefs.getLong("prayer_"+name,0L);
            if(at>now) scheduleApproachAlert(context,name,at);
            else cancel(context,"approach_"+name);
        }
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
    private static final String SILENT_PREFS = "zad_reminders";
    private static void activateSilentMode(Context context) {
        if (Build.VERSION.SDK_INT < 23) return;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        android.media.AudioManager audio=(android.media.AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (nm == null || audio == null || !nm.isNotificationPolicyAccessGranted()) return;
        android.content.SharedPreferences prefs=context.getSharedPreferences(SILENT_PREFS,Context.MODE_PRIVATE);
        int current=audio.getRingerMode();
        if (current != android.media.AudioManager.RINGER_MODE_NORMAL) return;
        prefs.edit().putInt("silent_previous_ringer",current).putBoolean("silent_restore_pending",true).apply();
        audio.setRingerMode(android.media.AudioManager.RINGER_MODE_SILENT);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent restore=new Intent(context,ReminderReceiver.class);
        restore.putExtra("action","restore_silent");
        PendingIntent pi=PendingIntent.getBroadcast(context,987654,restore,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        long at=System.currentTimeMillis()+15L*60L*1000L;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        else if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        else am.setExact(AlarmManager.RTC_WAKEUP,at,pi);
    }
    private static void restoreSilentMode(Context context) {
        if (Build.VERSION.SDK_INT < 23) return;
        android.content.SharedPreferences prefs=context.getSharedPreferences(SILENT_PREFS,Context.MODE_PRIVATE);
        if (!prefs.getBoolean("silent_restore_pending",false)) return;
        android.media.AudioManager audio=(android.media.AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (audio != null && nm != null && nm.isNotificationPolicyAccessGranted()
                && audio.getRingerMode()==android.media.AudioManager.RINGER_MODE_SILENT) {
            audio.setRingerMode(prefs.getInt("silent_previous_ringer",android.media.AudioManager.RINGER_MODE_NORMAL));
        }
        prefs.edit().putBoolean("silent_restore_pending",false).remove("silent_previous_ringer").apply();
    }

    @Override public void onReceive(Context context,Intent intent) {
        if (intent != null && "restore_silent".equals(intent.getStringExtra("action"))) {
            restoreSilentMode(context);
            return;
        }
        if (intent == null) return;
        String key=intent.getStringExtra("key"),title=intent.getStringExtra("title");
        if(key==null)return;
        boolean prayer=key.startsWith("prayer_");
        android.content.SharedPreferences prefs=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if (!prayer && ("morning".equals(key) || "evening".equals(key) || "qiyam".equals(key))) {
            if (prefs.getBoolean(key + "_audio_enabled", true)) {
                try { ReminderAudioService.play(context, key); } catch (Exception ignored) {}
            }
        } else if (key.startsWith("approach_")) {
            try { ReminderAudioService.play(context, "approach", title); } catch (Exception ignored) {}
        }
        boolean adhanEnabled=prefs.getBoolean("adhan_enabled",true);
        String adhanMode=prefs.getString("adhan_mode","adhan");
        String prayerName = prayer ? key.substring("prayer_".length()) : "";
        boolean thisPrayerEnabled = prayer && prefs.getBoolean("prayer_enabled_" + prayerName, true);
        if (prayer && prefs.getBoolean("prayer_silent_" + prayerName, false)) activateSilentMode(context);
        if(prayer && adhanEnabled && thisPrayerEnabled && "adhan".equals(adhanMode)) {
            Intent audio=new Intent(context,AdhanPlaybackService.class);
            audio.setAction(AdhanPlaybackService.ACTION_PLAY);
            audio.putExtra("prayer",prayerName);
            String selectedVoice = "الفجر".equals(prayerName)
                    ? prefs.getString("adhan_fajr_voice", "doha")
                    : prefs.getString("adhan_voice_" + prayerName, prefs.getString("adhan_voice","basit"));
            audio.putExtra("voice", selectedVoice);
            audio.putExtra("play_dua_after",prefs.getBoolean("dua_after_adhan_enabled",true));
            try {
                prefs.edit().remove("adhan_last_start_error").apply();
                if(Build.VERSION.SDK_INT>=26) context.startForegroundService(audio);
                else context.startService(audio);
            } catch(Exception error) {
                String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                prefs.edit().putString("adhan_last_start_error", detail).apply();
                android.util.Log.e("ZadAlmoslem", "Unable to start adhan playback service for " + prayerName, error);
            }
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
            scheduleApproachAlert(context,prayerName,next);
        }
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId=prayer?((adhanEnabled && thisPrayerEnabled && "tone".equals(adhanMode))?PRAYER_TONE_CHANNEL_ID:PRAYER_CHANNEL_ID):CHANNEL_ID;
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch;
            if(prayer){
                ch=new NotificationChannel(PRAYER_CHANNEL_ID,"إشعارات الصلاة الصامتة",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("إشعار دخول وقت الصلاة بدون صوت");
                ch.setSound(null,null);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
                NotificationChannel toneCh=new NotificationChannel(PRAYER_TONE_CHANNEL_ID,"تنبيه صوتي للصلاة",NotificationManager.IMPORTANCE_HIGH);
                toneCh.setDescription("نغمة قصيرة عند دخول وقت الصلاة");
                toneCh.enableVibration(true);
                nm.createNotificationChannel(toneCh);
            } else {
                ch=new NotificationChannel(CHANNEL_ID,"تنبيهات زاد المسلم",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("تنبيهات الأذكار وقيام الليل");
            }
            nm.createNotificationChannel(ch);
        }
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channelId):new Notification.Builder(context);
        String startError = prayer ? prefs.getString("adhan_last_start_error", "") : "";
        String notificationText = title==null?"تذكير من زاد المسلم":title;
        if (!startError.isEmpty()) notificationText = "تعذر بدء الأذان: " + startError;
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("قلبي اطمأن – زاد المسلم").setContentText(notificationText).setAutoCancel(true).setContentIntent(content);
        if(prayer && adhanEnabled && thisPrayerEnabled && "tone".equals(adhanMode)) b.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);
        else if(prayer && adhanEnabled && thisPrayerEnabled && "off".equals(adhanMode)) { /* no alert */ }
        else b.setDefaults(Notification.DEFAULT_VIBRATE);
        if(!prayer || (adhanEnabled && thisPrayerEnabled && !"off".equals(adhanMode))) nm.notify(key.hashCode(),b.build());
        if(intent.getBooleanExtra("daily",false)){
            String k=key.replace("daily_","");
            if(prefs.getBoolean(k+"_enabled",false)) scheduleDaily(context,k,prefs.getInt(k+"_hour",7),prefs.getInt(k+"_minute",0),prefs.getString(k+"_title","تذكير من زاد المسلم"));
        }
    }
}
