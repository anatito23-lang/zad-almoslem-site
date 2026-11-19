package com.nidaa.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.DateComponents;
import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(10, 22, 37), panel = Color.rgb(20, 38, 56), gold = Color.rgb(218, 181, 111), white = Color.rgb(244, 241, 232), muted = Color.rgb(164, 177, 188);
    private LinearLayout root, content;
    private String currentTab = "الرئيسية";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        render();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(8, 8, 8, 8); return t;
    }
    private void render() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg);
        setContentView(root);
        LinearLayout header = new LinearLayout(this); header.setPadding(20,18,20,12); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("نِداء", 27, gold, true); brand.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        TextView subtitle = text("مواقيت الصلاة والأذان", 12, muted, false); subtitle.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(0,-2,1)); header.addView(subtitle);
        root.addView(header);
        ScrollView scroll = new ScrollView(this); content = new LinearLayout(this); content.setPadding(18,8,18,20); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        showTab(currentTab);
        LinearLayout nav = new LinearLayout(this); nav.setPadding(8,8,8,12); nav.setBackgroundColor(panel);
        String[] tabs = {"الرئيسية","الأذان","القبلة","الإعدادات"};
        for(String tab:tabs){ TextView b=text(tab,13,tab.equals(currentTab)?gold:white,true); b.setOnClickListener(v->{currentTab=tab;render();}); nav.addView(b,new LinearLayout.LayoutParams(0,-2,1)); }
        root.addView(nav);
    }
    private void card(View v) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(14,14,14,14); box.setBackground(shape(panel,22));
        box.addView(v); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=14; content.addView(box,p);
    }
    private void showTab(String tab) {
        content.removeAllViews();
        if(tab.equals("الرئيسية")) {
            String date = new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar")).format(new Date());
            TextView dateView=text(date+"\nمصر · التوقيت المحلي",14,muted,false); card(dateView);
            TextView hero=text("الصلاة القادمة",15,muted,false);
            LinearLayout heroBox=new LinearLayout(this); heroBox.setOrientation(LinearLayout.VERTICAL); heroBox.setGravity(Gravity.CENTER);
            heroBox.addView(hero); heroBox.addView(text(nextPrayerName(),30,gold,true)); heroBox.addView(text(nextPrayerTime(),24,white,true)); heroBox.addView(text("نحسب المواقيت محليًا دون الحاجة للإنترنت",12,muted,false)); card(heroBox);
            TextView heading=text("مواقيت اليوم",19,white,true); heading.setGravity(Gravity.RIGHT); content.addView(heading);
            PrayerTimes pt = prayerTimes();
            String[] names={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};
            Date[] times={pt.fajr,pt.sunrise,pt.dhuhr,pt.asr,pt.maghrib,pt.isha};
            for(int i=0;i<names.length;i++){
                LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(8,4,8,4);
                TextView n=text(names[i],16,white,true); n.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                TextView tm=text(new SimpleDateFormat("hh:mm a",Locale.getDefault()).format(times[i]),16,gold,true); tm.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
                row.addView(n,new LinearLayout.LayoutParams(0,-2,1)); row.addView(tm); card(row);
            }
        } else if(tab.equals("الأذان")) {
            card(text("إعدادات الأذان",23,gold,true));
            card(text("اختيار مؤذن مستقل لكل صلاة\nتفعيل الأذان عند حلول الوقت\nتنزيل التسجيلات لتعمل دون إنترنت\nاختبار الصوت والتحكم في مستوى الصوت",16,white,false));
            card(text("سيتم ربط التسجيلات الصوتية والتنبيهات في المرحلة التالية.",13,muted,false));
        } else if(tab.equals("القبلة")) {
            card(text("اتجاه القبلة",23,gold,true)); card(text("سيتم تفعيل البوصلة بعد إضافة إذن المستشعرات والموقع.",15,white,false));
        } else {
            card(text("إعدادات نداء",23,gold,true));
            card(text("طريقة حساب المواقيت: الهيئة المصرية العامة للمساحة\nالموقع الحالي: مصر (إحداثيات القاهرة مؤقتًا)\nاللغة: العربية",15,white,false));
            card(text("سيضاف اختيار المدينة وتحديد الموقع تلقائيًا في المرحلة التالية.",13,muted,false));
        }
    }
    private PrayerTimes prayerTimes() {
        Coordinates c = new Coordinates(30.0444,31.2357);
        return new PrayerTimes(c,DateComponents.from(new Date()),CalculationMethod.EGYPTIAN.getParameters());
    }
    private String nextPrayerName() {
        PrayerTimes p=prayerTimes(); Date now=new Date();
        Prayer[] prayers={Prayer.FAJR,Prayer.DHUHR,Prayer.ASR,Prayer.MAGHRIB,Prayer.ISHA};
        String[] names={"الفجر","الظهر","العصر","المغرب","العشاء"};
        Date[] times={p.fajr,p.dhuhr,p.asr,p.maghrib,p.isha};
        for(int i=0;i<times.length;i++) if(times[i].after(now)) return names[i];
        return "الفجر";
    }
    private String nextPrayerTime() {
        PrayerTimes p=prayerTimes(); Date now=new Date();
        Date[] times={p.fajr,p.dhuhr,p.asr,p.maghrib,p.isha};
        for(Date d:times) if(d.after(now)) return new SimpleDateFormat("hh:mm a",Locale.getDefault()).format(d);
        return new SimpleDateFormat("hh:mm a",Locale.getDefault()).format(p.fajr);
    }
}
