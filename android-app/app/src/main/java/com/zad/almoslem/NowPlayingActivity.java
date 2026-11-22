package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NowPlayingActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView title, subtitle, state, toggle;
    private final Runnable ticker = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 900); }
    };

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    @Override public void onCreate(Bundle stateBundle) {
        super.onCreate(stateBundle);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(AppThemeColors.bg(this));
        String kind = getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", "quran");
        String photoTitle = "radio".equals(kind) ? "إذاعة القرآن الكريم" :
                "ruqyah".equals(kind) ? "الرقية الشرعية" :
                "quran".equals(kind) ? "استماع القرآن الكريم" : "زاد وطمأنينة";
        String photoSubtitle = "radio".equals(kind) ? "بث مباشر" :
                "ruqyah".equals(kind) ? "سكينة وطمأنينة" : "أنصت بقلبك";
        PhotoHeaderView photo = new PhotoHeaderView(this, kind, photoTitle, photoSubtitle);
        frame.addView(photo, new FrameLayout.LayoutParams(-1, dp(220), Gravity.TOP));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(26),dp(30),dp(26),dp(30));
        panel.setBackground(rounded(0xF01C2925,20));
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        panelLp.setMargins(dp(18),dp(8),dp(18),dp(22));
        frame.addView(panel,panelLp);

        TextView emblem=text("♫",42,AppThemeColors.accent(this),true);
        panel.addView(emblem,new LinearLayout.LayoutParams(-1,dp(88)));
        panel.addView(text("مشغل الصوت",15,AppThemeColors.muted(this),true));
        title=text("مجالس القرآن",23,AppThemeColors.text(this),true);
        LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(-1,-2);titleLp.topMargin=dp(10);
        panel.addView(title,titleLp);
        subtitle=text("أنصت بقلبك… واطمئن بذكر الله",14,AppThemeColors.muted(this),false);
        LinearLayout.LayoutParams subLp=new LinearLayout.LayoutParams(-1,-2);subLp.topMargin=dp(9);
        panel.addView(subtitle,subLp);
        state=text("جارٍ تجهيز الصوت",13,AppThemeColors.muted(this),false);
        LinearLayout.LayoutParams stateLp=new LinearLayout.LayoutParams(-1,-2);stateLp.topMargin=dp(24);
        panel.addView(state,stateLp);
        AudioEqualizerView equalizer=new AudioEqualizerView(this,"radio","quran","ruqyah","nasheed");
        panel.addView(equalizer,new LinearLayout.LayoutParams(-1,dp(52)));

        LinearLayout controls=new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams controlsLp=new LinearLayout.LayoutParams(-1,-2);
        controlsLp.topMargin=dp(24);
        panel.addView(controls,controlsLp);
        toggle=new TextView(this);
        toggle.setTextColor(AppThemeColors.text(this));toggle.setTextSize(16);toggle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        toggle.setGravity(Gravity.CENTER);toggle.setPadding(dp(28),dp(14),dp(28),dp(14));
        toggle.setBackground(rounded(AppThemeColors.accent(this),30));
        controls.addView(toggle,new LinearLayout.LayoutParams(-1,-2));
        toggle.setOnClickListener(v->sendControl(RadioPlaybackService.ACTION_TOGGLE));

        Button stop=new Button(this);stop.setText("إيقاف الصوت نهائيًا");
        LinearLayout.LayoutParams stopLp=new LinearLayout.LayoutParams(-1,-2);stopLp.topMargin=dp(12);
        panel.addView(stop,stopLp);
        stop.setOnClickListener(v->{
            sendControl(RadioPlaybackService.ACTION_STOP);
            finish();
        });
        setContentView(frame);
        refresh();
    }

    private void sendControl(String action) {
        Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(action);
        try { startService(i); } catch(Exception ignored) {}
        handler.postDelayed(ticker,250);
    }

    private void refresh() {
        android.content.SharedPreferences p=getSharedPreferences("zad_audio",MODE_PRIVATE);
        String kind=p.getString("active_kind","radio");
        String name=p.getString("radio_title","");
        if(name.isEmpty()) name="quran".equals(kind)?"استماع القرآن الكريم":
                "ruqyah".equals(kind)?"الرقية الشرعية":
                "nasheed".equals(kind)?"الإنشاد والابتهالات":"إذاعة القرآن الكريم";
        title.setText(name);
        subtitle.setText("quran".equals(kind)?"تلاوة من كتاب الله":
                "ruqyah".equals(kind)?"رقية شرعية مسموعة":
                "radio".equals(kind)?"بث إذاعة القرآن الكريم":"استماع إيماني");
        boolean waiting=p.getBoolean("waiting_for_audio_focus",false);
        boolean playing=p.getBoolean("radio_playing",false)&&!p.getBoolean("radio_paused",false);
        state.setText(waiting?"الصوت الآخر يعمل — سيعود الاستماع تلقائيًا":(playing?"● الصوت يعمل الآن":"◌ الصوت متوقف مؤقتًا"));
        toggle.setText(waiting?"إلغاء الاستئناف التلقائي":(playing?"Ⅱ   إيقاف مؤقت":"▶   استئناف الاستماع"));
    }

    @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);ticker.run();}
    @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}

    private static class ViewOverlay extends android.view.View {
        private final android.graphics.Paint paint=new android.graphics.Paint(3);
        ViewOverlay(Activity a){super(a);paint.setShader(new android.graphics.LinearGradient(0,0,0,1,
                new int[]{0x55000000,0x99000000,0x77000000},null,android.graphics.Shader.TileMode.CLAMP));}
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);paint.setShader(new android.graphics.LinearGradient(0,0,0,getHeight(),
                new int[]{0x55000000,0x99000000,0x77000000},null,android.graphics.Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),paint);}
    }
}