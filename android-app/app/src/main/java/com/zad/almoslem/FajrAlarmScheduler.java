package com.zad.almoslem;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;
import java.util.Calendar;
import java.util.TimeZone;

public final class FajrAlarmScheduler {
    private FajrAlarmScheduler() {}
    private static final int REQUEST_CODE=4817;

    public static boolean scheduleNext(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        if(!p.getBoolean("fajr_alarm_enabled",false)) return false;
        int hour=p.getInt("native_time_hour_الفجر",-1);
        int minute=p.getInt("native_time_minute_الفجر",0);
        if(hour<0||hour>23||minute<0||minute>59) return false;
        String zone=p.getString("native_city_timezone","");
        if(zone.isEmpty() && "Egypt".equals(p.getString("native_country","Egypt"))) zone="Africa/Cairo";
        if(zone.isEmpty()) zone=TimeZone.getDefault().getID();
        Calendar at=Calendar.getInstance(TimeZone.getTimeZone(zone));
        at.set(Calendar.HOUR_OF_DAY,hour);at.set(Calendar.MINUTE,minute);at.set(Calendar.SECOND,0);at.set(Calendar.MILLISECOND,0);
        if(at.getTimeInMillis()<=System.currentTimeMillis()) at.add(Calendar.DAY_OF_YEAR,1);
        return scheduleAt(context,at.getTimeInMillis(),REQUEST_CODE);
    }

    public static boolean scheduleSnooze(Context context,long when) { return scheduleAt(context,when,REQUEST_CODE+1); }\n\n    private static boolean scheduleAt(Context context,long when,int requestCode) {
        try {
            AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent intent=new Intent(context,FajrAlarmReceiver.class).setAction("com.zad.almoslem.FAJR_ALARM");
            PendingIntent pi=PendingIntent.getBroadcast(context,requestCode,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            if(Build.VERSION.SDK_INT>=31 && !manager.canScheduleExactAlarms()) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            else if(Build.VERSION.SDK_INT>=23) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            else manager.setExact(AlarmManager.RTC_WAKEUP,when,pi);
            return true;
        } catch(Exception e) { return false; }
    }

    public static void cancel(Context context) {
        AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(context,FajrAlarmReceiver.class).setAction("com.zad.almoslem.FAJR_ALARM");
        PendingIntent pi=PendingIntent.getBroadcast(context,REQUEST_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pi);pi.cancel();
    }
}