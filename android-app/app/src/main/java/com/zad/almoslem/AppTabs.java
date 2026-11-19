package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public final class AppTabs {
    private AppTabs() {}
    public static void add(Activity activity, LinearLayout root, String active) {
        String[] keys={"home","prayer","mushaf","radio","library"};
        String[] labels={"الرئيسية","الصلاة","المصحف","الإذاعة","المكتبة"};
        Class<?>[] screens={MainActivity.class,PrayerDashboardActivity.class,MushafPageActivity.class,RadioActivity.class,FaithLibraryActivity.class};
        LinearLayout bar=new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        for(int i=0;i<keys.length;i++){
            final Class<?> target=screens[i];
            Button button=new Button(activity);
            button.setText(labels[i]);
            button.setTextSize(10);
            button.setAllCaps(false);
            button.setPadding(2,0,2,0);
            button.setTextColor(Color.WHITE);
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(keys[i].equals(active)?Color.rgb(8,78,57):Color.rgb(118,137,125)));
            button.setEnabled(!keys[i].equals(active));
            button.setOnClickListener(v->{if(activity.getClass()!=target)activity.startActivity(new Intent(activity,target));});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,(int)(46*activity.getResources().getDisplayMetrics().density),1);
            lp.setMargins(2,2,2,8);
            bar.addView(button,lp);
        }
        LinearLayout.LayoutParams barParams=new LinearLayout.LayoutParams(-1,-2);
        root.addView(bar,0,barParams);
    }
}