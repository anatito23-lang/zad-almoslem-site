package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class RuqyahActivity extends Activity {
    private TextView status;
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        AppThemeColors.apply(this);
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppThemeColors.bg(this));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15), dp(16), dp(15), dp(24));
        root.setBackgroundColor(AppThemeColors.bg(this));
        scroll.addView(root);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(12), dp(8), dp(12), dp(8));
        hero.setBackground(bg(AppThemeColors.surface(this),16));
        TextView title = label("الرقية الشرعية", 23, AppThemeColors.text(this), true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title, match());
        TextView intro = label("آيات من القرآن الكريم… وطمأنينة للقلب", 13, AppThemeColors.muted(this), false);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0, dp(5), 0, 0);
        hero.addView(intro, match());
        root.addView(hero, match());

        TextView description = label("اختر القارئ، وسيعمل الصوت داخل هذا التبويب دون فتح شاشة منبثقة.", 14, AppThemeColors.muted(this), false);
        description.setPadding(0, dp(12), 0, dp(12));
        root.addView(description, match());

        addCard(root, "الشيخ محمود خليل الحصري",
                "https://archive.org/download/20240228_20240228_1849/041%20%2011%20%20%20%D8%A7%D9%84%D8%B1%D9%82%D9%8A%D8%A9%20%D8%A7%D9%84%D8%B4%D8%B1%D8%B9%D9%8A%D8%A9%20%D9%84%D8%B9%D9%84%D8%A7%D8%AC%20%D8%A7%D9%84%D8%B3%D8%AD%D8%B1%20%D8%A7%D9%84%D9%85%D8%B3%20%D8%A7%D9%84%D8%AD%D8%B3%D8%AF%20%D8%A8%D8%B5%D9%88%D8%AA%20%D8%A7%D9%84%D8%B4%D9%8A%D8%AE%20%D9%85%D8%AD%D9%85%D9%88%D8%AF%20%D8%AE%D9%84%D9%8A%D9%84%20%D8%A7%D9%84%D8%AD%D8%B5%D8%B1%D9%89%20%D8%B1%D8%AD%D9%85%D9%87%20%D8%A7%D9%84%D9%84%D9%87%20%D8%AC%D9%88%D8%AF%D8%A9%20%D8%B9%D8%A7%D9%84%D9%8A%D8%A9.mp3");
        addCard(root, "الشيخ مشاري راشد العفاسي", "https://archive.org/download/e3fasi-Rokia/e3fasi-seher.mp3");
        addCard(root, "الشيخ ناصر القطامي", "https://archive.org/download/alqtami-Roqiaa/alqtami-seher.mp3");
        addCard(root, "الشيخ ياسر الدوسري", "https://archive.org/download/dosari-Roqia/dosari-seher.mp3");
        addCard(root, "الشيخ علي الحذيفي", "https://archive.org/download/hothife-Roqia/hothife-seher.mp3");
        addCard(root, "الشيخ هاني الرفاعي", "https://archive.org/download/rifa3i-Roqia/rifa3i-seher.mp3");

        status = label("حالة الاستماع ستظهر هنا.", 12, AppThemeColors.muted(this), false);
        status.setPadding(0, dp(12), 0, 0);
        root.addView(status, match());
        setContentView(AppTabs.wrap(this, scroll, "more", "ruqyah"));
    }

    private void addCard(LinearLayout root, String name, String url) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(bg(AppThemeColors.surface(this), 16));
        TextView title = label(name, 16, AppThemeColors.text(this), true);
        card.addView(title, match());

        Button play = new Button(this);
        play.setText("▶ تشغيل الرقية");
        card.addView(play, match());
        play.setOnClickListener(v -> {
            Intent i = new Intent(this, RadioPlaybackService.class);
            i.setAction(RadioPlaybackService.ACTION_PLAY);
            i.putExtra("url", url);
            i.putExtra("kind", "ruqyah");
            i.putExtra("label", "الرقية الشرعية — " + name);
            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
                status.setText("يتم تشغيل الرقية داخل تبويب الرقية الشرعية.");
            } catch (Exception e) {
                status.setText("تعذر بدء التشغيل: " + e.getMessage());
            }
        });

        Button pause = new Button(this);
        pause.setText("Ⅱ إيقاف مؤقت / ▶ استئناف");
        card.addView(pause, match());
        pause.setOnClickListener(v -> {
            Intent control = new Intent(this, RadioPlaybackService.class);
            control.setAction(RadioPlaybackService.ACTION_TOGGLE);
            try { startService(control); status.setText("تم تحديث حالة الاستماع."); }
            catch (Exception e) { status.setText("تعذر التحكم في الصوت: " + e.getMessage()); }
        });

        Button stop = new Button(this);
        stop.setText("■ إيقاف الصوت");
        card.addView(stop, match());
        stop.setOnClickListener(v -> stopAudio());

        LinearLayout.LayoutParams lp = match();
        lp.bottomMargin = dp(10);
        root.addView(card, lp);
    }

    private void stopAudio() {
        Intent i = new Intent(this, RadioPlaybackService.class);
        i.setAction(RadioPlaybackService.ACTION_STOP);
        try { startService(i); if (status != null) status.setText("تم إيقاف الصوت."); }
        catch (Exception e) { if (status != null) status.setText("تعذر الإيقاف: " + e.getMessage()); }
    }
}
