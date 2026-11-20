package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class AppTabs {
    private AppTabs() {}

    public static View wrap(Activity activity, View content, String active) {
        ScrollView scroll;
        if (content instanceof ScrollView) {
            scroll = (ScrollView) content;
        } else {
            scroll = new ScrollView(activity);
            scroll.setFillViewport(true);
            scroll.addView(content);
        }
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout shell = new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.rgb(244,247,243));
        shell.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = createBar(activity, active);
        shell.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 70)));

        // Keep the navigation dock above Android's gesture/three-button navigation area.
        shell.setOnApplyWindowInsetsListener((view, insets) -> {
            int bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets nav = insets.getInsets(
                        WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout());
                bottom = nav.bottom;
            } else {
                bottom = insets.getSystemWindowInsetBottom();
            }
            int safeBottom = Math.max(0, bottom);
            bar.setPadding(dp(activity, 10), 0, dp(activity, 10), safeBottom);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
            int desired = dp(activity, 70) + safeBottom;
            if (params.height != desired) {
                params.height = desired;
                bar.setLayoutParams(params);
            }
            return insets;
        });
        shell.setFitsSystemWindows(false);
        shell.requestApplyInsets();
        return shell;
    }

    private static LinearLayout createBar(Activity activity, String active) {
        String[] keys = {"home", "prayer", "mushaf", "radio", "library"};
        String[] labels = {"الرئيسية", "الصلاة", "المصحف", "الإذاعة", "المكتبة"};
        String[] icons = {"⌂", "☾", "۞", "♫", "▤"};
        Class<?>[] screens = {MainActivity.class, PrayerDashboardActivity.class,
                MushafPageActivity.class, RadioActivity.class, FaithLibraryActivity.class};

        LinearLayout outer = new LinearLayout(activity);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(Color.rgb(244,247,243));
        outer.setPadding(dp(activity, 10), dp(activity, 5), dp(activity, 10), 0);

        LinearLayout dock = new LinearLayout(activity);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable dockBg = new GradientDrawable();
        dockBg.setColor(Color.rgb(8,48,40));
        dockBg.setCornerRadius(dp(activity, 20));
        dock.setBackground(dockBg);
        dock.setElevation(dp(activity, 5));
        dock.setPadding(dp(activity, 3), dp(activity, 4), dp(activity, 3), dp(activity, 4));
        outer.addView(dock, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 58)));

        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final Class<?> target = screens[i];
            boolean selected = key.equals(active);

            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setClickable(!selected);
            item.setFocusable(!selected);
            item.setPadding(dp(activity, 2), dp(activity, 2), dp(activity, 2), dp(activity, 2));

            if (selected) {
                GradientDrawable selectedBg = new GradientDrawable();
                selectedBg.setColor(Color.rgb(28,112,82));
                selectedBg.setCornerRadius(dp(activity, 15));
                item.setBackground(selectedBg);
            } else {
                item.setBackgroundResource(android.R.drawable.list_selector_background);
            }

            TextView icon = new TextView(activity);
            icon.setText(icons[i]);
            icon.setTextSize(19);
            icon.setGravity(Gravity.CENTER);
            icon.setTextColor(selected ? Color.WHITE : Color.rgb(215,232,222));
            icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.addView(icon, new LinearLayout.LayoutParams(-1, dp(activity, 25)));

            TextView label = new TextView(activity);
            label.setText(labels[i]);
            label.setTextSize(10);
            label.setSingleLine(true);
            label.setGravity(Gravity.CENTER);
            label.setTextColor(selected ? Color.WHITE : Color.rgb(230,239,233));
            label.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            item.addView(label, new LinearLayout.LayoutParams(-1, dp(activity, 19)));

            item.setOnClickListener(v -> activity.startActivity(new Intent(activity, target)));
            dock.addView(item, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        }
        return outer;
    }

    private static int dp(Activity activity, float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
