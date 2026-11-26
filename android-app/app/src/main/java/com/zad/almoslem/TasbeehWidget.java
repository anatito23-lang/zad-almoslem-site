package com.zad.almoslem;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class TasbeehWidget extends AppWidgetProvider {
    public static final String ACTION_INCREMENT = "com.zad.almoslem.TASBEEH_WIDGET_INCREMENT";
    private static final String[] DHIKR = {
        "سبحان الله", "الحمد لله", "الله أكبر", "لا إله إلا الله",
        "أستغفر الله وأتوب إليه", "اللهم صل وسلم على نبينا محمد",
        "سبحان الله وبحمده، سبحان الله العظيم", "لا حول ولا قوة إلا بالله"
    };

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_INCREMENT.equals(intent.getAction())) {
            android.content.SharedPreferences p = context.getSharedPreferences("zad_reminders", Context.MODE_PRIVATE);
            int goal = p.getInt("native_tasbeeh_goal", 33);
            if (goal != 33 && goal != 66 && goal != 99) goal = 33;
            int count = Math.max(0, p.getInt("native_tasbeeh_count", 0));
            int index = Math.floorMod(p.getInt("native_tasbeeh_dhikr", 0), DHIKR.length);
            count++;
            if (count >= goal) {
                count = 0;
                index = (index + 1) % DHIKR.length;
            }
            p.edit()
                    .putInt("native_tasbeeh_count", count)
                    .putInt("native_tasbeeh_dhikr", index)
                    .apply();
            refreshAll(context);
        }
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, TasbeehWidget.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) updateOne(context, manager, id);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id) {
        android.content.SharedPreferences p = context.getSharedPreferences("zad_reminders", Context.MODE_PRIVATE);
        int count = Math.max(0, p.getInt("native_tasbeeh_count", 0));
        int goal = p.getInt("native_tasbeeh_goal", 33);
        int index = Math.floorMod(p.getInt("native_tasbeeh_dhikr", 0), DHIKR.length);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tasbeeh_widget);
        views.setTextViewText(R.id.tasbeeh_widget_phrase, DHIKR[index]);
        views.setTextViewText(R.id.tasbeeh_widget_count, count + " / " + goal);
        views.setTextViewText(R.id.tasbeeh_widget_button, count >= goal ? "اكتمل الورد" : "سبّح +١");

        Intent increment = new Intent(context, TasbeehWidget.class).setAction(ACTION_INCREMENT);
        PendingIntent add = PendingIntent.getBroadcast(context, id, increment,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.tasbeeh_widget_button, add);
        Intent open = new Intent(context, TasbeehActivity.class);
        PendingIntent launch = PendingIntent.getActivity(context, id + 10000, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.tasbeeh_widget_root, launch);
        manager.updateAppWidget(id, views);
    }
}