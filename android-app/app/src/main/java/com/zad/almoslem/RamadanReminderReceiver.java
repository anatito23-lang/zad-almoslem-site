package com.zad.almoslem;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class RamadanReminderReceiver extends BroadcastReceiver {
    public static void scheduleAll(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences("zad_ramadan",Context.MODE_PRIVATE);
        if(!p.getBoolean("enabled",false)){cancel(context,"ramadan_iftar_voice");cancel(context,"ramadan_iftar_cannon");cancel(context,"ramadan_suhoor");return;}
        android.content.SharedPreferences times=context.getSharedPreferences("zad_reminders",Context.MODE_PRIVATE);
        int maghrib=times.getInt("native_time_hour_المغرب",-1), maghribMinute=times.getInt("native_time_minute_المغرب",0);
        int fajr=times.getInt("native_time_hour_الفجر",-1), fajrMinute=times.getInt("native_time_minute_الفجر",0);
        if(maghrib>=0&&maghrib<=23){
            if(p.getBoolean("iftar_voice_enabled",true))scheduleDaily(context,"ramadan_iftar_voice",maghrib,maghribMinute,p.getInt("iftar_lead_minutes",5));
            else cancel(context,"ramadan_iftar_voice");
            if(p.getBoolean("cannon_enabled",true))scheduleDaily(context,"ramadan_iftar_cannon",maghrib,maghribMinute,0);
            else cancel(context,"ramadan_iftar_cannon");
        }
        if(fajr>=0&&fajr<=23&&p.getBoolean("suhoor_enabled",true))
            scheduleDaily(context,"ramadan_suhoor",fajr,fajrMinute,p.getInt("suhoor_lead_minutes",45));
        else cancel(context,"ramadan_suhoor");
    }
    private static void scheduleDaily(Context context,String key,int hour,int minute,int lead){
        Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,hour);c.set(Calendar.MINUTE,minute);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);c.add(Calendar.MINUTE,-Math.max(0,lead));
        if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i=new Intent(context,RamadanReminderReceiver.class);i.putExtra("key",key);
        PendingIntent pi=PendingIntent.getBroadcast(context,key.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
        else if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
        else am.setExact(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
    }
    private static void cancel(Context context,String key){
        Intent i=new Intent(context,RamadanReminderReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(context,key.hashCode(),i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
        if(pi!=null){((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).cancel(pi);pi.cancel();}
    }
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null)return;String key=intent.getStringExtra("key");if(key==null)return;
        android.content.SharedPreferences p=context.getSharedPreferences("zad_ramadan",Context.MODE_PRIVATE);
        if(!p.getBoolean("enabled",false))return;
        if("ramadan_iftar_voice".equals(key)&&p.getBoolean("iftar_voice_enabled",true)
                ||"ramadan_iftar_cannon".equals(key)&&p.getBoolean("cannon_enabled",true)
                ||"ramadan_suhoor".equals(key)&&p.getBoolean("suhoor_enabled",true)){
            Intent play=new Intent(context,RamadanAudioService.class);play.putExtra("key",key);
            try{if(Build.VERSION.SDK_INT>=26)context.startForegroundService(play);else context.startService(play);}
            catch(Exception e){android.util.Log.e("ZadRamadan","Could not start Ramadan audio",e);}
        }
        scheduleAll(context);
    }
}
