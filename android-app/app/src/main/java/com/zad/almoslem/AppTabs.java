package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

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

        LinearLayout shell = new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.rgb(244,247,243));
        shell.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        shell.addView(createBar(activity, active), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 60)));
        return shell;
    }

    private static LinearLayout createBar(Activity activity, String active) {
        String[] keys={"home","prayer","mushaf","radio","library"};
        String[] labels={"الرئيسية","الصلاة","المصحف","الإذاعة","المكتبة"};
        Class<?>[] screens={MainActivity.class,PrayerDashboardActivity.class,MushafPageActivity.class,RadioActivity.class,FaithLibraryActivity.class};
        LinearLayout bar=new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        bar.setBackgroundColor(Color.rgb(8,48,40));
        for(int i=0;i<keys.length;i++){
            final String key=keys[i];
            final Class<?> target=screens[i];
            Button button=new Button(activity);
            button.setText(labels[i]);
            button.setTextSize(10);
            button.setSingleLine(true);
            button.setAllCaps(false);
            button.setMinWidth(0);
            button.setMinHeight(dp(activity,52));
            button.setPadding(1,0,1,0);
            button.setTextColor(Color.WHITE);
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    key.equals(active)?Color.rgb(25,112,80):Color.rgb(8,48,40)));
            button.setEnabled(!key.equals(active));
            button.setOnClickListener(v->activity.startActivity(new Intent(activity,target)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(activity,56),1f);
            lp.setMargins(1,0,1,0);
            bar.addView(button,lp);
        }
        return bar;
    }

    private static int dp(Activity activity,float value) {
        return (int)(value*activity.getResources().getDisplayMetrics().density+0.5f);
    }
}
