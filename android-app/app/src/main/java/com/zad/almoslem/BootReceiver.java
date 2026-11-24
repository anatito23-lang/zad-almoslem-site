package com.zad.almoslem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    private android.content.SharedPreferences prefs(Context context){return context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);}
    @Override public void onReceive(Context context,Intent intent) {
        ReminderReceiver.ensureDefaultDailyReminders(context);
        RamadanReminderReceiver.scheduleAll(context);
        if(prefs(context).getBoolean("gentle_enabled",false)) ReminderReceiver.scheduleGentleReminder(context);
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        for(String key:new String[]{"morning","evening","qiyam"}) {
            if(p.getBoolean(key+"_enabled",false))
                ReminderReceiver.scheduleDaily(context,key,p.getInt(key+"_hour",7),p.getInt(key+"_minute",0),p.getString(key+"_title","تذكير من سكينة"));
        }
        // Rebuild prayer alarms from their saved local wall-clock times.
        // This keeps them aligned after reboot, manual clock changes, and DST/time-zone changes.
        for(String name:new String[]{"الفجر","الظهر","العصر","المغرب","العشاء"}) {
            if(!p.getBoolean("prayer_enabled_"+name,true)) continue;
            int hour=p.getInt("native_time_hour_"+name,-1);
            int minute=p.getInt("native_time_minute_"+name,0);
            long now=System.currentTimeMillis();
            if(hour>=0 && hour<=23 && minute>=0 && minute<=59) {
                String zoneId=p.getString("native_city_timezone","");if(zoneId.isEmpty()&&"Egypt".equals(p.getString("native_country","Egypt")))zoneId="Africa/Cairo";if(zoneId.isEmpty())zoneId=java.util.TimeZone.getDefault().getID();
                Calendar next=Calendar.getInstance(java.util.TimeZone.getTimeZone(zoneId));
                next.set(Calendar.HOUR_OF_DAY,hour);
                next.set(Calendar.MINUTE,minute);
                next.set(Calendar.SECOND,0);
                next.set(Calendar.MILLISECOND,0);
                if(next.getTimeInMillis()<=now) next.add(Calendar.DAY_OF_YEAR,1);
                ReminderReceiver.schedulePrayer(context,name,next.getTimeInMillis());
            } else {
                long at=p.getLong("prayer_"+name,0L);
                if(at>now) ReminderReceiver.schedulePrayer(context,name,at);
            }
        }
    }
}