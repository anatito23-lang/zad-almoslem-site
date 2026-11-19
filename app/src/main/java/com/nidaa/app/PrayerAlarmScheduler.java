package com.nidaa.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;
import java.util.Calendar;
import java.util.Date;

public final class PrayerAlarmScheduler {
    private static final int[] IDS={0,2,3,4,5};
    private static final String[] NAMES={"الفجر","الظهر","العصر","المغرب","العشاء"};
    private static final double[][] COORDS={{31.2001,29.9187},{30.0444,31.2357},{30.5877,31.5020},{30.2285,31.4810},{30.5965,32.2715},{31.0409,31.3785},{30.7865,31.0004},{27.1809,31.1837}};
    private PrayerAlarmScheduler(){}
    public static void schedule(Context context,int city){
        double[] c=COORDS[Math.max(0,Math.min(COORDS.length-1,city))];
        PrayerTimes p=new PrayerTimes(new Coordinates(c[0],c[1]),DateComponents.from(new Date()),CalculationMethod.EGYPTIAN.getParameters());
        Date[] times={p.fajr,p.dhuhr,p.asr,p.maghrib,p.isha};
        for(int i=0;i<IDS.length;i++){
            Date when=times[i];
            if(!when.after(new Date())){Calendar next=Calendar.getInstance();next.add(Calendar.DAY_OF_YEAR,1);PrayerTimes tomorrow=new PrayerTimes(new Coordinates(c[0],c[1]),DateComponents.from(next.getTime()),CalculationMethod.EGYPTIAN.getParameters());Date[] t={tomorrow.fajr,tomorrow.dhuhr,tomorrow.asr,tomorrow.maghrib,tomorrow.isha};when=t[i];}
            set(context,IDS[i],NAMES[i],when.getTime());
        }
    }
    static void scheduleNextDay(Context context,int id,String name){
        int pos=-1;for(int i=0;i<IDS.length;i++)if(IDS[i]==id)pos=i;if(pos<0)return;
        int city=context.getSharedPreferences("nidaa",Context.MODE_PRIVATE).getInt("city",0);
        double[] c=COORDS[Math.max(0,Math.min(COORDS.length-1,city))];
        Calendar next=Calendar.getInstance();next.add(Calendar.DAY_OF_YEAR,1);
        PrayerTimes p=new PrayerTimes(new Coordinates(c[0],c[1]),DateComponents.from(next.getTime()),CalculationMethod.EGYPTIAN.getParameters());
        Date[] t={p.fajr,p.dhuhr,p.asr,p.maghrib,p.isha};set(context,id,name,t[pos].getTime());
    }
    private static void set(Context context,int id,String name,long at){
        AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(context,PrayerAlarmReceiver.class);intent.putExtra("prayer_id",id);intent.putExtra("prayer_name",name);
        PendingIntent pi=PendingIntent.getBroadcast(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(manager!=null)manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
    }
    public static void cancel(Context context){
        AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        for(int id:IDS){Intent i=new Intent(context,PrayerAlarmReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(context,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(manager!=null)manager.cancel(pi);pi.cancel();}
    }
}