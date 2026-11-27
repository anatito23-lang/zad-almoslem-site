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
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RadioActivity extends Activity {
    private static final String GREEN = "#083028";
    private TextView status;
    private final Handler handler = new Handler();
    private final ExecutorService radioLoader = Executors.newSingleThreadExecutor();
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

        PhotoHeaderView header = new PhotoHeaderView(this,"radio",
                "إذاعة القرآن الكريم","بث مباشر من محطات القرآن الكريم");
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(190)));
        View headerGap = new View(this);
        root.addView(headerGap,new LinearLayout.LayoutParams(1,dp(12)));

        // Minimal audio player; the equalizer moves only while the live stream is ready.
        LinearLayout deck = new LinearLayout(this);
        deck.setOrientation(LinearLayout.VERTICAL);
        deck.setGravity(Gravity.CENTER);
        deck.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable deckBg = new GradientDrawable();
        deckBg.setColor(AppThemeColors.surface(this));
        deckBg.setCornerRadius(dp(18));
        deck.setBackground(deckBg);
        TextView deckTitle = new TextView(this);
        deckTitle.setText("مشغل الإذاعة المباشرة");
        deckTitle.setTextSize(21);
        deckTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        deckTitle.setTextColor(AppThemeColors.text(this));
        deckTitle.setGravity(Gravity.CENTER);
        deck.addView(deckTitle, match());
        TextView deckSub = new TextView(this);
        deckSub.setText("البث يستمر حتى عند مغادرة التبويب");
        deckSub.setTextSize(13);
        deckSub.setTextColor(AppThemeColors.muted(this));
        deckSub.setGravity(Gravity.CENTER);
        deckSub.setPadding(0, dp(5), 0, dp(12));
        deck.addView(deckSub, match());
        AudioEqualizerView equalizer = new AudioEqualizerView(this,"radio");
        equalizer.setContentDescription("مؤشر متحرك للبث المباشر");
        deck.addView(equalizer, new LinearLayout.LayoutParams(-1, dp(42)));
        Button toggleAudio = new Button(this);
        toggleAudio.setText("Ⅱ  إيقاف مؤقت / ▶  استئناف");
        styleAction(toggleAudio,0xFF244D61);
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
        liveHint.setTextColor(AppThemeColors.muted(this));
        liveHint.setGravity(Gravity.CENTER);
        liveHint.setPadding(0, dp(8), 0, 0);
        deck.addView(liveHint, match());
        LinearLayout.LayoutParams deckLp = match();
        deckLp.bottomMargin = dp(16);
        root.addView(deck, deckLp);

        TextView listTitle = new TextView(this);
        listTitle.setText("📻 إذاعات MP3Quran المباشرة");
        listTitle.setTextSize(19);
        listTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        listTitle.setTextColor(AppThemeColors.text(this));
        root.addView(listTitle, match());
        LinearLayout stationsList = new LinearLayout(this);
        stationsList.setOrientation(LinearLayout.VERTICAL);
        root.addView(stationsList, match());
        addStation(stationsList, "🇪🇬 إذاعة القرآن الكريم من القاهرة",
                "البث المصري", "https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8",
                "إذاعة القرآن الكريم من القاهرة", "egypt", "https://misrquran.gov.eg/home");
        addStation(stationsList, "🇸🇦 إذاعة القرآن الكريم السعودية",
                "هيئة الإذاعة والتلفزيون السعودية", "https://live.kwikmotion.com/sbrksaquranradiolive/srpksaquranradio/playlist.m3u8",
                "إذاعة القرآن الكريم السعودية", "saudi", "https://radioplus.sba.sa/ar/live/4");
        loadMp3QuranRadios(stationsList);

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
        play.setText("▶  تشغيل المحطة");
        styleAction(play,0xFF244D61);
        card.addView(play, match());
        play.setOnClickListener(v -> start(url, label, station));
        Button stopStation = new Button(this);
        stopStation.setText("■  إيقاف المحطة");
        styleAction(stopStation,0xFF53636B);
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


    private void loadMp3QuranRadios(LinearLayout target) {
        TextView loading = new TextView(this);
        loading.setText("جارٍ تحميل الإذاعات من MP3Quran...");
        loading.setTextColor(AppThemeColors.muted(this));
        target.addView(loading, match());
        radioLoader.execute(() -> {
            JSONArray radios = null;
            String failure = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("https://www.mp3quran.net/api/v3/radios?language=ar").openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");
                connection.connect();
                if (connection.getResponseCode() != 200) throw new Exception("HTTP " + connection.getResponseCode());
                InputStream in = connection.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                in.close();
                JSONObject response = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
                radios = response.optJSONArray("radios");
                if (radios == null) throw new Exception("لم تصل قائمة الإذاعات");
            } catch (Exception e) { failure = e.getMessage(); }
            finally { if (connection != null) connection.disconnect(); }
            final JSONArray result = radios;
            final String error = failure;
            runOnUiThread(() -> {
                target.removeAllViews();
                if (result == null) {
                    TextView problem = new TextView(this);
                    problem.setText("تعذر تحميل إذاعات MP3Quran. تحقق من الإنترنت وأعد فتح التبويب. " + (error == null ? "" : error));
                    problem.setTextColor(AppThemeColors.muted(this));
                    target.addView(problem, match());
                    return;
                }
                int added = 0;
                for (int index = 0; index < result.length(); index++) {
                    JSONObject item = result.optJSONObject(index);
                    if (item == null) continue;
                    String name = item.optString("name", "").trim();
                    String stream = item.optString("url", "").trim();
                    if (name.isEmpty() || !(stream.startsWith("https://") || stream.startsWith("http://"))) continue;
                    addStation(target, "📻 " + name, "بث مباشر عبر MP3Quran", stream,
                            name, "mp3quran_" + item.optString("id", String.valueOf(index)), "https://www.mp3quran.net/");
                    added++;
                }
                if (added == 0) {
                    TextView empty = new TextView(this);
                    empty.setText("لا توجد إذاعات متاحة حاليًا من المصدر.");
                    empty.setTextColor(AppThemeColors.muted(this));
                    target.addView(empty, match());
                }
            });
        });
    }

    private void styleAction(Button button, int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(16));
        button.setBackground(shape);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
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

    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(statusTicker);
        radioLoader.shutdownNow();
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
