package com.zad.almoslem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PrayerStatusNotificationReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PrayerStatusNotification.update(context);
    }
}
