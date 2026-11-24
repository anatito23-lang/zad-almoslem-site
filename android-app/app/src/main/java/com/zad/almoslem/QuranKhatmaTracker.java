package com.zad.almoslem;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Tracks distinct canonical Quran mushaf pages actually opened while tracking is enabled. */
public final class QuranKhatmaTracker {
    private static final String PREFS = "zad_ramadan";
    private static final String KEY_ACTIVE = "khatma_tracking";
    private static final String KEY_READ_PAGES = "khatma_read_pages";

    private QuranKhatmaTracker() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isTracking(Context context) {
        return prefs(context).getBoolean(KEY_ACTIVE, false);
    }

    public static void start(Context context) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, true).apply();
    }

    public static void stop(Context context) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, false).apply();
    }

    public static void markPageRead(Context context, int page) {
        if (page < 1 || page > 604 || !isTracking(context)) return;
        SharedPreferences p = prefs(context);
        Set<String> pages = new HashSet<>(p.getStringSet(KEY_READ_PAGES, new HashSet<>()));
        if (pages.add(String.valueOf(page))) {
            p.edit().putStringSet(KEY_READ_PAGES, pages).apply();
        }
    }

    public static int getReadPageCount(Context context) {
        return prefs(context).getStringSet(KEY_READ_PAGES, new HashSet<>()).size();
    }

    public static void reset(Context context) {
        prefs(context).edit().remove(KEY_READ_PAGES).apply();
    }
}
