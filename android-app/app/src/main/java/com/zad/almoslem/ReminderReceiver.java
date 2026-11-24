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
    private static final String PRAYER_CHANNEL_ID = "zad_prayer_alerts_v3";
    private static final String PRAYER_TONE_CHANNEL_ID = "zad_prayer_tone";
    private static final String GENTLE_CHANNEL_ID = "zad_gentle_reminders_v3";
    private static final String AUDIO_FALLBACK_CHANNEL_ID = "zad_reminder_audio_fallback_v3";
    private static final String POWER_SAVER_CHANNEL_ID = "zad_power_saver_visual_v1";
    private static final String POWER_SAVER_PRAYER_CHANNEL_ID = "zad_power_saver_prayer_visual_v1";

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

    public static void setGentleRemindersEnabled(Context context, boolean enabled) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        p.edit().putBoolean("gentle_enabled",enabled).apply();
        if(enabled) scheduleGentleReminder(context); else cancel(context,"gentle_reminder");
    }
    public static void setGentleReminderInterval(Context context,int hours) {
        int safe=Math.max(1,Math.min(6,hours));
        context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE).edit().putInt("gentle_interval_hours",safe).apply();
        scheduleGentleReminder(context);
    }
    public static void scheduleGentleReminder(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if(!p.getBoolean("gentle_enabled",false)){cancel(context,"gentle_reminder");return;}
        int hours=Math.max(1,Math.min(6,p.getInt("gentle_interval_hours",3)));
        int startHour=Math.max(0,Math.min(23,p.getInt("gentle_start_hour",9)));
        int startMinute=Math.max(0,Math.min(59,p.getInt("gentle_start_minute",0)));
        Calendar next=Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY,startHour);
        next.set(Calendar.MINUTE,startMinute);
        next.set(Calendar.SECOND,0);
        next.set(Calendar.MILLISECOND,0);
        long intervalMillis=hours*60L*60L*1000L;
        if(next.getTimeInMillis()<=System.currentTimeMillis()){
            long elapsed=System.currentTimeMillis()-next.getTimeInMillis();
            long steps=(elapsed/intervalMillis)+1L;
            next.setTimeInMillis(next.getTimeInMillis()+steps*intervalMillis);
        }
        long at=next.getTimeInMillis();
        p.edit().putLong("gentle_next_at",at).apply();
        schedule(context,"gentle_reminder",at,"تذكير لطيف من سكينة",false);
    }

    public static void ensureDefaultDailyReminders(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if(!p.contains("morning_enabled"))
            scheduleDaily(context,"morning",7,0,"حان وقت أذكار الصباح، لا تنس وردك من ذكر الله.");
        if(!p.contains("evening_enabled"))
            scheduleDaily(context,"evening",17,0,"حان وقت أذكار المساء، لا تنس وردك من ذكر الله.");
        if(!p.contains("qiyam_enabled"))
            scheduleDaily(context,"qiyam",4,0,"حان وقت قيام الليل، قم إلى الصلاة والذكر.");
        if(!p.contains("gentle_enabled")) p.edit().putBoolean("gentle_enabled",true).putInt("gentle_interval_hours",3).putInt("gentle_start_hour",9).putInt("gentle_start_minute",0).apply();
        // Rebuild saved alarms whenever the app is opened. Android removes alarms after
        // force-stop/restart scenarios; checking only whether preferences exist leaves
        // enabled reminders silently unscheduled.
        for(String key:new String[]{"morning","evening","qiyam"}) {
            if(p.getBoolean(key+"_enabled",false)) {
                scheduleDaily(context,key,p.getInt(key+"_hour",7),p.getInt(key+"_minute",0),
                        p.getString(key+"_title","تذكير من سكينة"));
            }
        }
        if(p.getBoolean("gentle_enabled",false)) scheduleGentleReminder(context);
        rescheduleApproachAlerts(context);
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
    public static void cancelPrayer(Context context,String name) {
        String key="prayer_"+name;
        cancel(context,key);
        cancel(context,"approach_"+name);
        context.getSharedPreferences(SILENT_PREFS,Context.MODE_PRIVATE).edit().remove(key).apply();
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
        if ("gentle_reminder".equals(key)) {
            if(!prefs.getBoolean("gentle_enabled",false)) return;
            String[] messages={
                "تذكير من القرآن: ألا بذكر الله تطمئن القلوب.",
                "تذكير من القرآن: فاذكروني أذكركم.",
                "تذكير من القرآن: يا أيها الذين آمنوا اذكروا الله ذكرا كثيرا.",
                "تذكير من القرآن: الذين يذكرون الله قياما وقعودا وعلى جنوبهم."
            };
            int index=prefs.getInt("gentle_message_index",0)%messages.length;
            if(index<0) index+=messages.length;
            title=messages[index];
            prefs.edit().putInt("gentle_message_index",(index+1)%messages.length).apply();
            scheduleGentleReminder(context);
        }
        boolean exactAlarmAudioAllowed = Build.VERSION.SDK_INT < 31
                || ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();
        boolean audioStartFailed = false;
        boolean powerSaverEnabled = prefs.getBoolean("power_saver_enabled", false);
        boolean reminderAudioMasterEnabled = prefs.getBoolean("reminder_audio_master_enabled", true) && !powerSaverEnabled;
        if (reminderAudioMasterEnabled && !prayer && ("morning".equals(key) || "evening".equals(key) || "qiyam".equals(key))) {
            if (prefs.getBoolean(key + "_audio_enabled", true) && exactAlarmAudioAllowed) {
                try { ReminderAudioService.play(context, key); } catch (Exception error) {
                    audioStartFailed = true;
                    android.util.Log.e("ZadAlmoslem", "Unable to start daily reminder audio", error);
                }
            }
        } else if (reminderAudioMasterEnabled && key.startsWith("approach_")) {
            if (prefs.getBoolean("approach_alert_enabled", true) && exactAlarmAudioAllowed) {
                try { ReminderAudioService.play(context, "approach", title, "approach"); } catch (Exception error) {
                    audioStartFailed = true;
                    android.util.Log.e("ZadAlmoslem", "Unable to start prayer approach audio", error);
                }
            }
        } else if (reminderAudioMasterEnabled && "gentle_reminder".equals(key)) {
            if (prefs.getBoolean("gentle_audio_enabled", true) && exactAlarmAudioAllowed) {
                try {
                    int audioIndex=prefs.getInt("gentle_message_index",1)-1;
                    if(audioIndex<0)audioIndex=3;
                    ReminderAudioService.play(context, "gentle", title, "gentle_"+audioIndex);
                } catch (Exception error) {
                    audioStartFailed = true;
                    android.util.Log.e("ZadAlmoslem", "Unable to start gentle reminder audio", error);
                }
            }
        }
        boolean adhanEnabled=prefs.getBoolean("adhan_enabled",true);
        String adhanMode=powerSaverEnabled ? "takbeer" : prefs.getString("adhan_mode","adhan");
        String prayerName = prayer ? key.substring("prayer_".length()) : "";
        boolean thisPrayerEnabled = prayer && prefs.getBoolean("prayer_enabled_" + prayerName, true);
        if (prayer && prefs.getBoolean("prayer_silent_" + prayerName, false)) activateSilentMode(context);
        if(prayer && adhanEnabled && thisPrayerEnabled && ("adhan".equals(adhanMode) || "takbeer".equals(adhanMode))) {
            Intent audio=new Intent(context,AdhanPlaybackService.class);
            audio.setAction(AdhanPlaybackService.ACTION_PLAY);
            audio.putExtra("prayer",prayerName);
            String selectedVoice = "الفجر".equals(prayerName)
                    ? prefs.getString("adhan_fajr_voice", "doha")
                    : prefs.getString("adhan_voice_" + prayerName, prefs.getString("adhan_voice","basit"));
            audio.putExtra("voice", selectedVoice);
            audio.putExtra("play_dua_after",prefs.getBoolean("dua_after_adhan_enabled",true));
            try {
                if (!exactAlarmAudioAllowed) {
                    prefs.edit().putString("adhan_last_start_error",
                            "فعّل إذن المنبهات الدقيقة من إعدادات التذكيرات لتشغيل الأذان صوتيًا").apply();
                } else {
                    prefs.edit().remove("adhan_last_start_error").apply();
                    if(Build.VERSION.SDK_INT>=26) context.startForegroundService(audio);
                    else context.startService(audio);
                }
            } catch(Exception error) {
                String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                prefs.edit().putString("adhan_last_start_error", detail).apply();
                android.util.Log.e("ZadAlmoslem", "Unable to start adhan playback service for " + prayerName, error);
            }
        }
        if(prayer) {
            // Advance by one local calendar day, not a fixed 24-hour duration.
            String zoneId=prefs.getString("native_city_timezone","");
            if(zoneId.isEmpty() && "Egypt".equals(prefs.getString("native_country","Egypt"))) zoneId="Africa/Cairo";
            if(zoneId.isEmpty()) zoneId=java.util.TimeZone.getDefault().getID();
            Calendar nextPrayer = Calendar.getInstance(java.util.TimeZone.getTimeZone(zoneId));
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
        boolean gentle="gentle_reminder".equals(key);
        boolean gentleAudioEnabled = prefs.getBoolean("gentle_audio_enabled", true);
        boolean approachAlert = key.startsWith("approach_");
        boolean dailyAudioReminder = "morning".equals(key) || "evening".equals(key) || "qiyam".equals(key);
        boolean fallbackAudioNotification = !powerSaverEnabled && reminderAudioMasterEnabled && !prayer && (!exactAlarmAudioAllowed || audioStartFailed) && (
                (gentle && gentleAudioEnabled)
                || (approachAlert && prefs.getBoolean("approach_alert_enabled", true))
                || (dailyAudioReminder && prefs.getBoolean(key + "_audio_enabled", true)));
        String channelId=prayer?(powerSaverEnabled?POWER_SAVER_PRAYER_CHANNEL_ID:
                ((adhanEnabled && thisPrayerEnabled && "tone".equals(adhanMode))?PRAYER_TONE_CHANNEL_ID:PRAYER_CHANNEL_ID)):
                (powerSaverEnabled?POWER_SAVER_CHANNEL_ID:
                (gentle?(fallbackAudioNotification?AUDIO_FALLBACK_CHANNEL_ID:GENTLE_CHANNEL_ID):
                (fallbackAudioNotification?AUDIO_FALLBACK_CHANNEL_ID:CHANNEL_ID)));
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch;
            if(prayer && powerSaverEnabled) {
                ch=new NotificationChannel(POWER_SAVER_PRAYER_CHANNEL_ID,"إشعارات الصلاة — توفير الطاقة",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("تنبيه مرئي عند دخول وقت الصلاة دون صوت أو اهتزاز");
                ch.setSound(null,null);
                ch.enableVibration(false);
            } else if(prayer){
                ch=new NotificationChannel(PRAYER_CHANNEL_ID,"إشعارات الصلاة الصامتة",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("إشعار دخول وقت الصلاة بدون صوت");
                ch.setSound(null,null);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
                NotificationChannel toneCh=new NotificationChannel(PRAYER_TONE_CHANNEL_ID,"تنبيه صوتي للصلاة",NotificationManager.IMPORTANCE_HIGH);
                toneCh.setDescription("نغمة قصيرة عند دخول وقت الصلاة");
                toneCh.enableVibration(true);
                nm.createNotificationChannel(toneCh);
            } else if(powerSaverEnabled) {
                ch=new NotificationChannel(POWER_SAVER_CHANNEL_ID,"إشعارات توفير الطاقة",NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("إشعارات مرئية فقط دون صوت أو اهتزاز عند تفعيل وضع توفير الطاقة");
                ch.setSound(null,null);
                ch.enableVibration(false);
            } else if(fallbackAudioNotification) {
                // If exact alarms are unavailable, Android may reject the background
                // audio foreground-service launch. Use a distinct audible notification
                // channel so the reminder is not silently lost.
                ch=new NotificationChannel(AUDIO_FALLBACK_CHANNEL_ID,"تنبيهات صوتية احتياطية من زاد وطمأنينة",NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("صوت إشعار بديل للتذكيرات عند عدم توفر إذن تشغيل الصوت في الخلفية");
                ch.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                        new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build());
                ch.enableVibration(true);
            } else if(gentle) {
                ch=new NotificationChannel(GENTLE_CHANNEL_ID,"تذكيرات إيمانية خفيفة",NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("تذكيرات قصيرة بالصلاة على النبي والذكر والاستغفار، دون صوت أو اهتزاز");
                ch.setSound(null,null);
                ch.enableVibration(false);
            } else {
                ch=new NotificationChannel(CHANNEL_ID,"تنبيهات زاد وطمأنينة",NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("تنبيهات الأذكار وقيام الليل");
            }
            nm.createNotificationChannel(ch);
        }
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent fullScreen=null;
        if(prayer && adhanEnabled && thisPrayerEnabled && !"off".equals(adhanMode)) {
            Intent screen=new Intent(context,AdhanFullScreenActivity.class);
            screen.putExtra("prayer",prayerName);
            screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            fullScreen=PendingIntent.getActivity(context,7300+Math.abs(prayerName.hashCode()%1000),screen,
                    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        }
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channelId):new Notification.Builder(context);
        String startError = prayer ? prefs.getString("adhan_last_start_error", "") : "";
        String notificationText = title==null?"تذكير من سكينة":title;
        if (!startError.isEmpty()) notificationText = "تعذر بدء الأذان: " + startError;
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(gentle?"تذكير لطيف من سكينة":"سكينة – أذان وصلاة وقرآن").setContentText(notificationText).setAutoCancel(true).setContentIntent(content);
        if(prayer) {
            b.setCategory(Notification.CATEGORY_ALARM)
             .setVisibility(Notification.VISIBILITY_PUBLIC)
             .setPriority(Notification.PRIORITY_MAX);
        }
        if(fullScreen!=null) b.setFullScreenIntent(fullScreen,true);
        if(prayer && adhanEnabled && thisPrayerEnabled && "tone".equals(adhanMode)) b.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);
        else if(prayer && adhanEnabled && thisPrayerEnabled && "off".equals(adhanMode)) { /* no alert */ }
        else if(powerSaverEnabled || (gentle && exactAlarmAudioAllowed) || fallbackAudioNotification) b.setDefaults(0);
        else b.setDefaults(Notification.DEFAULT_VIBRATE);
        // "off" means silent prayer alert, not suppressing the notification itself.
        if(!prayer || (adhanEnabled && thisPrayerEnabled)) nm.notify(key.hashCode(),b.build());
        if(intent.getBooleanExtra("daily",false)){
            String k=key.replace("daily_","");
            if(prefs.getBoolean(k+"_enabled",false)) scheduleDaily(context,k,prefs.getInt(k+"_hour",7),prefs.getInt(k+"_minute",0),prefs.getString(k+"_title","تذكير من سكينة"));
        }
    }
}
