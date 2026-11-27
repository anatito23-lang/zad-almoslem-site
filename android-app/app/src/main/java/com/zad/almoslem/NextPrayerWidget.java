package com.zad.almoslem;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class NextPrayerWidget extends AppWidgetProvider {
    private static final String ACTION_REFRESH = "com.zad.almoslem.NEXT_PRAYER_WIDGET_REFRESH";
    private static final String[] NAMES = {"الفجر","الظهر","العصر","المغرب","العشاء"};

    @Override public void onEnabled(Context context) { scheduleRefresh(context); }
    @Override public void onDisabled(Context context) {
        Intent i = new Intent(context, NextPrayerWidget.class).setAction(ACTION_REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(context, 7741, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
        scheduleRefresh(context);
    }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) refreshAll(context);
    }
    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, NextPrayerWidget.class));
        for (int id : ids) updateOne(context, manager, id);
    }
    private static void scheduleRefresh(Context context) {
        Intent i = new Intent(context, NextPrayerWidget.class).setAction(ACTION_REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(context, 7741, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + 60000L, 60000L, pi);
    }
    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.next_prayer_widget);
        SharedPreferences p = context.getSharedPreferences("zad_reminders", Context.MODE_PRIVATE);
        String city = p.getString("native_prayer_city", "");
        if (city.isEmpty()) city = "مواقيت الصلاة";
        v.setTextViewText(R.id.next_widget_city, city);
        TimeZone zone = TimeZone.getTimeZone(p.getString("native_city_timezone",
                "Egypt".equals(p.getString("native_country", "Egypt")) ? "Africa/Cairo" : TimeZone.getDefault().getID()));
        Calendar now = Calendar.getInstance(zone);
        long current = System.currentTimeMillis();
        long best = Long.MAX_VALUE;
        String nextName = null;
        int nextHour = 0, nextMinute = 0;
        for (String name : NAMES) {
            int hour = p.getInt("native_time_hour_" + name, -1);
            int minute = p.getInt("native_time_minute_" + name, 0);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) continue;
            Calendar candidate = Calendar.getInstance(zone);
            candidate.setTimeInMillis(current);
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);
            if (candidate.getTimeInMillis() <= current) candidate.add(Calendar.DAY_OF_YEAR, 1);
            if (candidate.getTimeInMillis() < best) {
                best = candidate.getTimeInMillis();
                nextName = name;
                nextHour = hour;
                nextMinute = minute;
            }
        }
        if (nextName == null) {
            v.setTextViewText(R.id.next_widget_prayer, "افتح مواقيت الصلاة");
            v.setTextViewText(R.id.next_widget_time, "لتحديث المواعيد");
            v.setTextViewText(R.id.next_widget_countdown, "—");
        } else {
            v.setTextViewText(R.id.next_widget_prayer, "الصلاة القادمة: " + nextName);
            v.setTextViewText(R.id.next_widget_time, String.format(Locale.getDefault(), "موعدها %02d:%02d", nextHour, nextMinute));
            long left = Math.max(0, best - current);
            long seconds = left / 1000L;
            String countdown = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    seconds / 3600L, (seconds % 3600L) / 60L, seconds % 60L);
            v.setTextViewText(R.id.next_widget_countdown, countdown);
            if (Build.VERSION.SDK_INT >= 24) {
                v.setChronometer(R.id.next_widget_chronometer, SystemClock.elapsedRealtime() + left, "%s", true);
                v.setViewVisibility(R.id.next_widget_chronometer, android.view.View.VISIBLE);
                v.setViewVisibility(R.id.next_widget_countdown, android.view.View.GONE);
            } else {
                v.setViewVisibility(R.id.next_widget_chronometer, android.view.View.GONE);
                v.setViewVisibility(R.id.next_widget_countdown, android.view.View.VISIBLE);
            }
        }
        Intent open = new Intent(context, PrayerDashboardActivity.class);
        PendingIntent click = PendingIntent.getActivity(context, 7742, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.next_widget_root, click);
        manager.updateAppWidget(id, v);
    }
}