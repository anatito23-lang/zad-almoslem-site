package com.nidaa.app;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            android.content.SharedPreferences p=context.getSharedPreferences("nidaa",Context.MODE_PRIVATE);
            if(p.getBoolean("reminders",false))PrayerAlarmScheduler.schedule(context,p.getInt("city",0));
        }
    }
}