package com.zad.almoslem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        for(String key:new String[]{"morning","qiyam"}) {
            if(p.getBoolean(key+"_enabled",false))
                ReminderReceiver.scheduleDaily(context,key,p.getInt(key+"_hour",7),p.getInt(key+"_minute",0),p.getString(key+"_title","تذكير من زاد المسلم"));
        }
        for(String name:new String[]{"الفجر","الظهر","العصر","المغرب","العشاء"}) {
            long at=p.getLong("prayer_"+name,0L);
            long now=System.currentTimeMillis();
            while(at>0L && at<=now) at+=24L*60L*60L*1000L;
            if(at>now) ReminderReceiver.schedulePrayer(context,name,at);
        }
    }
}