package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

public class RadioActivity extends Activity {
    private static final String GREEN = "#083028";
    private TextView status;
    private LinearLayout equalizer;
    private final int[] equalizerHeights = {8, 15, 23, 13, 20, 10, 17};
    private int equalizerFrame = 0;
    private final Handler handler = new Handler();
    private final Runnable statusTicker = new Runnable() {
        @Override public void run() { refreshStatus(); handler.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        AppThemeColors.apply(this);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        build();
        handler.post(statusTicker);
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(AppThemeColors.bg(this));
        setContentView(AppTabs.wrap(this, root, "radio"));

        TextView title = new TextView(this);
        title.setText("إذاعة القرآن الكريم");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(12), dp(22), dp(12), dp(22));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppThemeColors.primary(this));
        bg.setCornerRadius(28);
        title.setBackground(bg);
        root.addView(title, match());

        TextView desc = new TextView(this);
        desc.setText("اختر المحطة التي تحب الاستماع إليها. يستمر البث في الخلفية بعد مغادرة الصفحة.");
        desc.setTextSize(15);
        desc.setTextColor(AppThemeColors.muted(this));
        desc.setPadding(0, dp(14), 0, dp(14));
        root.addView(desc, match());

        // Branded radio deck: animated only while the actual stream is ready.
        LinearLayout deck = new LinearLayout(this);
        deck.setOrientation(LinearLayout.VERTICAL);
        deck.setGravity(Gravity.CENTER);
        deck.setPadding(dp(18), dp(20), dp(18), dp(20));
        GradientDrawable deckBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(8,48,40), Color.rgb(17,91,68), Color.rgb(6,37,32)});
        deckBg.setCornerRadius(dp(24));
        deck.setBackground(deckBg);
        deck.addView(new SectionArtworkView(this,"radio"), new LinearLayout.LayoutParams(-1, dp(164)));
        TextView deckTitle = new TextView(this);
        deckTitle.setText("راديو زاد المسلم");
        deckTitle.setTextSize(21);
        deckTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        deckTitle.setTextColor(Color.WHITE);
        deckTitle.setGravity(Gravity.CENTER);
        deck.addView(deckTitle, match());
        TextView deckSub = new TextView(this);
        deckSub.setText("أنصت بقلبك… واطمئن بذكر الله");
        deckSub.setTextSize(13);
        deckSub.setTextColor(Color.rgb(231,220,190));
        deckSub.setGravity(Gravity.CENTER);
        deckSub.setPadding(0, dp(5), 0, dp(12));
        deck.addView(deckSub, match());
        equalizer = new LinearLayout(this);
        equalizer.setGravity(Gravity.CENTER);
        equalizer.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 7; i++) {
            View bar = new View(this);
            GradientDrawable barBg = new GradientDrawable();
            barBg.setColor(Color.rgb(232,198,126));
            barBg.setCornerRadius(dp(4));
            bar.setBackground(barBg);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(5), dp(8));
            barLp.setMargins(dp(4), 0, dp(4), 0);
            equalizer.addView(bar, barLp);
        }
        deck.addView(equalizer, new LinearLayout.LayoutParams(-1, dp(30)));
        Button toggleAudio = new Button(this);
        toggleAudio.setText("Ⅱ إيقاف مؤقت / ▶ استئناف");
        deck.addView(toggleAudio, match());
        toggleAudio.setOnClickListener(v -> {
            Intent control = new Intent(this, RadioPlaybackService.class);
            control.setAction(RadioPlaybackService.ACTION_TOGGLE);
            try { startService(control); status.setText("تم تحديث حالة الاستماع."); }
            catch (Exception e) { status.setText("تعذر التحكم في الصوت: " + e.getMessage()); }
        });
        TextView liveHint = new TextView(this);
        liveHint.setText("اختر محطة لبدء البث المباشر");
        liveHint.setTextSize(11);
        liveHint.setTextColor(Color.rgb(220,232,220));
        liveHint.setGravity(Gravity.CENTER);
        liveHint.setPadding(0, dp(8), 0, 0);
        deck.addView(liveHint, match());
        LinearLayout.LayoutParams deckLp = match();
        deckLp.bottomMargin = dp(16);
        root.addView(deck, deckLp);

        addStation(root, "🇪🇬 إذاعة القرآن الكريم من القاهرة",
                "البث المصري", "https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8",
                "إذاعة القرآن الكريم من القاهرة", "egypt", "https://misrquran.gov.eg/home");
        addStation(root, "🇸🇦 إذاعة القرآن الكريم السعودية",
                "هيئة الإذاعة والتلفزيون السعودية", "https://live.kwikmotion.com/sbrksaquranradiolive/srpksaquranradio/playlist.m3u8",
                "إذاعة القرآن الكريم السعودية", "saudi", "https://radioplus.sba.sa/ar/live/4");

        Button stop = new Button(this);
        stop.setText("■ إيقاف الإذاعة");
        root.addView(stop, match());
        stop.setOnClickListener(v -> {
            Intent i = new Intent(this, RadioPlaybackService.class);
            i.setAction(RadioPlaybackService.ACTION_STOP);
            try { startService(i); }
            catch (Exception e) { if (status != null) status.setText("تعذر الإيقاف: " + e.getMessage()); }
        });

        status = new TextView(this);
        status.setText("اختر محطة واضغط تشغيل. حالة الاتصال ستظهر هنا.");
        status.setTextColor(AppThemeColors.text(this));
        status.setPadding(0, dp(18), 0, dp(18));
        root.addView(status, match());
    }

    private void addStation(LinearLayout root, String name, String detail, String url,
                            String label, String station, String officialUrl) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(12));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(AppThemeColors.surface(this));
        cardBg.setCornerRadius(18);
        card.setBackground(cardBg);
        LinearLayout.LayoutParams cardLp = match();
        cardLp.bottomMargin = dp(12);
        root.addView(card, cardLp);

        TextView heading = new TextView(this);
        heading.setText(name);
        heading.setTextSize(17);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(AppThemeColors.text(this));
        card.addView(heading, match());

        TextView subtitle = new TextView(this);
        subtitle.setText(detail);
        subtitle.setTextSize(12);
        subtitle.setTextColor(AppThemeColors.muted(this));
        subtitle.setPadding(0, dp(4), 0, dp(8));
        card.addView(subtitle, match());

        Button play = new Button(this);
        play.setText("▶ تشغيل المحطة");
        card.addView(play, match());
        play.setOnClickListener(v -> start(url, label, station));
        Button stopStation = new Button(this);
        stopStation.setText("■ إيقاف المحطة");
        card.addView(stopStation, match());
        stopStation.setOnClickListener(v -> stopPlayback());

        TextView official = new TextView(this);
        official.setText("الموقع الرسمي للمحطة");
        official.setTextSize(12);
        official.setTextColor(Color.rgb(25,112,80));
        official.setGravity(Gravity.CENTER);
        official.setPadding(8, dp(8), 8, dp(4));
        card.addView(official, match());
        official.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(officialUrl))); }
            catch (Exception e) { if (status != null) status.setText("تعذر فتح موقع المحطة."); }
        });
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void refreshStatus() {
        if (status == null) return;
        android.content.SharedPreferences p = getSharedPreferences("zad_audio", MODE_PRIVATE);
        boolean ready = p.getBoolean("radio_ready", false);
        boolean playing = p.getBoolean("radio_playing", false);
        String error = p.getString("radio_last_error", "");
        String station = p.getString("radio_station", "");
        String stationName = "egypt".equals(station) ? "إذاعة القرآن الكريم المصرية من القاهرة" :
                "saudi".equals(station) ? "إذاعة القرآن الكريم السعودية" :
                "palestine".equals(station) ? "إذاعة القرآن الكريم من نابلس – فلسطين" : "إذاعة القرآن الكريم";
        if (ready) status.setText("● البث يعمل الآن — " + stationName);
        else if (playing) status.setText("◌ جارٍ الاتصال بـ " + stationName + "...");
        else if (error != null && !error.isEmpty()) status.setText("تعذر تشغيل " + stationName + ": " + error);
        else status.setText("اختر محطة ثم اضغط تشغيل لبدء البث.");
        // A genuine playback indicator: bars stay still while connecting or stopped.
        if (equalizer != null) {
            boolean live = ready && playing;
            for (int i = 0; i < equalizer.getChildCount(); i++) {
                View bar = equalizer.getChildAt(i);
                int h = live ? equalizerHeights[(i + equalizerFrame) % equalizerHeights.length] : 6;
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bar.getLayoutParams();
                lp.height = dp(h);
                bar.setLayoutParams(lp);
            }
            if (live) equalizerFrame = (equalizerFrame + 1) % equalizerHeights.length;
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(statusTicker);
        super.onDestroy();
    }

    private void stopPlayback() {
        Intent i = new Intent(this, RadioPlaybackService.class);
        i.setAction(RadioPlaybackService.ACTION_STOP);
        try { startService(i); if (status != null) status.setText("تم إرسال طلب إيقاف الإذاعة."); }
        catch (Exception e) { if (status != null) status.setText("تعذر إيقاف الإذاعة: " + e.getMessage()); }
    }

    private void start(String url, String label, String station) {
        Intent i = new Intent(this, RadioPlaybackService.class);
        i.setAction(RadioPlaybackService.ACTION_PLAY);
        i.putExtra("url", url);
        i.putExtra("kind", "radio");
        i.putExtra("label", label);
        i.putExtra("station", station);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            status.setText("تم إرسال طلب تشغيل البث. ستبقى داخل تبويب الإذاعة، وحالة البث ستظهر هنا.");
        } catch (Exception e) {
            status.setText("تعذر بدء البث: " + e.getMessage());
        }
    }
}
