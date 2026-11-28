package com.zad.almoslem;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.TimeZone;

public final class SunriseAlarmScheduler {
    private static final int REQUEST_CODE=5821;
    private SunriseAlarmScheduler() {}

    public static boolean scheduleNext(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if(!p.getBoolean("sunrise_alarm_enabled",false)) return false;
        int hour=p.getInt("native_sunrise_hour",-1), minute=p.getInt("native_sunrise_minute",0);
        if(hour<0||hour>23||minute<0||minute>59) return false;
        String zone=p.getString("native_city_timezone","");
        if(zone.isEmpty() && "Egypt".equals(p.getString("native_country","Egypt"))) zone="Africa/Cairo";
        if(zone.isEmpty()) zone=TimeZone.getDefault().getID();
        Calendar at=Calendar.getInstance(TimeZone.getTimeZone(zone));
        at.set(Calendar.HOUR_OF_DAY,hour);at.set(Calendar.MINUTE,minute);
        at.set(Calendar.SECOND,0);at.set(Calendar.MILLISECOND,0);
        if(at.getTimeInMillis()<=System.currentTimeMillis()) at.add(Calendar.DAY_OF_YEAR,1);
        try {
            AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent intent=new Intent(context,SunriseAlarmReceiver.class).setAction("com.zad.almoslem.SUNRISE_ALARM");
            PendingIntent pi=PendingIntent.getBroadcast(context,REQUEST_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            if(Build.VERSION.SDK_INT>=31 && !manager.canScheduleExactAlarms())
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.getTimeInMillis(),pi);
            else if(Build.VERSION.SDK_INT>=23)
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.getTimeInMillis(),pi);
            else manager.setExact(AlarmManager.RTC_WAKEUP,at.getTimeInMillis(),pi);
            return true;
        } catch(Exception e) { return false; }
    }

    public static void cancel(Context context) {
        AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(context,SunriseAlarmReceiver.class).setAction("com.zad.almoslem.SUNRISE_ALARM");
        PendingIntent pi=PendingIntent.getBroadcast(context,REQUEST_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pi);pi.cancel();
    }
}