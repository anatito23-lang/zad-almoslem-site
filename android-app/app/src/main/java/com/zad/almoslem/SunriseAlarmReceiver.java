package com.zad.almoslem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class SunriseAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if(!p.getBoolean("sunrise_alarm_enabled",false)) return;
        // Schedule the next local sunrise before starting today's short birdsong.
        SunriseAlarmScheduler.scheduleNext(context);
        try {
            Intent play=new Intent(context,SunriseBirdService.class);
            if(Build.VERSION.SDK_INT>=26) context.startForegroundService(play);
            else context.startService(play);
        } catch(Exception e) {
            Log.e("ZadAlmoslem","Unable to start sunrise birdsong",e);
        }
    }
}