package com.zad.almoslem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    private android.content.SharedPreferences prefs(Context context){return context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);}\n    @Override public void onReceive(Context context,Intent intent) {
        ReminderReceiver.ensureDefaultDailyReminders(context);\n        if(prefs(context).getBoolean("gentle_enabled",false)) ReminderReceiver.scheduleGentleReminder(context);
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        for(String key:new String[]{"morning","evening","qiyam"}) {
            if(p.getBoolean(key+"_enabled",false))
                ReminderReceiver.scheduleDaily(context,key,p.getInt(key+"_hour",7),p.getInt(key+"_minute",0),p.getString(key+"_title","تذكير من زاد المسلم"));
        }
        for(String name:new String[]{"الفجر","الظهر","العصر","المغرب","العشاء"}) {
            long at=p.getLong("prayer_"+name,0L);
            long now=System.currentTimeMillis();
            if (at > 0L) {
                Calendar next = Calendar.getInstance();
                next.setTimeInMillis(at);
                while (next.getTimeInMillis() <= now) next.add(Calendar.DAY_OF_YEAR, 1);
                at = next.getTimeInMillis();
            }
            if(at>now) ReminderReceiver.schedulePrayer(context,name,at);
        }
    }
}