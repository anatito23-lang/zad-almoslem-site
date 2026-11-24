package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public class AdhanFullScreenActivity extends Activity {
    private final String[] ids={"azhar_mosque_bg","kaaba_wallpaper","madinah_wallpaper","aqsa_wallpaper","quran_wallpaper"};
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView lyricHint;
    private ProgressBar progress;
    private ScrollView lyricScroll;
    private LinearLayout phraseContainer;
    private TextView[] phraseViews=new TextView[0];
    private int lastLine=-1;
    private final Runnable lyricTicker=new Runnable(){
        @Override public void run(){
            updateLyrics();
            handler.postDelayed(this,100L);
        }
    };

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void hideSystemBars(){
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }
    private String[] phrases(boolean fajr){
        if(fajr){
            return new String[]{
                "الله أكبر، الله أكبر",
                "الله أكبر، الله أكبر",
                "أشهد أن لا إله إلا الله",
                "أشهد أن لا إله إلا الله",
                "أشهد أن محمدًا رسول الله",
                "أشهد أن محمدًا رسول الله",
                "حي على الصلاة",
                "حي على الصلاة",
                "حي على الفلاح",
                "حي على الفلاح",
                "الصلاة خير من النوم",
                "الصلاة خير من النوم",
                "الله أكبر، الله أكبر",
                "لا إله إلا الله"
            };
        }
        return new String[]{
            "الله أكبر، الله أكبر",
            "الله أكبر، الله أكبر",
            "أشهد أن لا إله إلا الله",
            "أشهد أن لا إله إلا الله",
            "أشهد أن محمدًا رسول الله",
            "أشهد أن محمدًا رسول الله",
            "حي على الصلاة",
            "حي على الصلاة",
            "حي على الفلاح",
            "حي على الفلاح",
            "الله أكبر، الله أكبر",
            "لا إله إلا الله"
        };
    }
    private int activePhraseIndex(long position,long duration,int count){
        if(duration<=0L)return 0;
        float fraction=Math.max(0f,Math.min(0.9999f,(float)position/(float)duration));
        // Phrase progression follows the selected recording's actual playback clock.
        // The per-phrase intervals are normalized to each recording's measured duration.
        float[] standard={0f,.075f,.15f,.235f,.32f,.405f,.49f,.575f,.66f,.745f,.86f,.955f};
        float[] fajr={0f,.065f,.13f,.205f,.28f,.355f,.43f,.505f,.58f,.655f,.73f,.805f,.89f,.96f};
        float[] points=count==14?fajr:standard;
        int selected=0;
        for(int i=1;i<points.length;i++){
            if(fraction>=points[i])selected=i;else break;
        }
        return Math.min(count-1,selected);
    }
    private void updateLyrics(){
        if(phraseContainer==null)return;
        long duration=AdhanPlaybackService.uiDurationMs;
        long position=AdhanPlaybackService.uiPositionMs;
        boolean takbeer=AdhanPlaybackService.uiTakbeerOnly;
        String prayer=getIntent().getStringExtra("prayer");
        boolean fajr=prayer!=null&&prayer.contains("الفجر");
        String[] lines=takbeer
                ?new String[]{"الله أكبر، الله أكبر","الله أكبر، الله أكبر"}
                :phrases(fajr);
        if(phraseViews.length!=lines.length){
            phraseContainer.removeAllViews();
            phraseViews=new TextView[lines.length];
            for(int i=0;i<lines.length;i++){
                TextView lineView=new TextView(this);
                lineView.setText(lines[i]);
                lineView.setTextSize(20);
                lineView.setGravity(Gravity.CENTER);
                lineView.setPadding(dp(10),dp(9),dp(10),dp(9));
                lineView.setTextColor(0xFFD8D5CB);
                lineView.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);
                phraseViews[i]=lineView;
                phraseContainer.addView(lineView,new LinearLayout.LayoutParams(-1,-2));
            }
            lastLine=-1;
        }
        int active=takbeer?(duration>0L&&position*2L>=duration?1:0)
                :activePhraseIndex(position,duration,lines.length);
        for(int i=0;i<phraseViews.length;i++){
            TextView item=phraseViews[i];
            boolean selected=i==active;
            item.setTextColor(selected?0xFFFFE3A0:0xFFE5E1D6);
            item.setTypeface(Typeface.DEFAULT,selected?Typeface.BOLD:Typeface.NORMAL);
            GradientDrawable bg=new GradientDrawable();
            bg.setColor(selected?0x665E9B79:Color.TRANSPARENT);
            bg.setCornerRadius(dp(12));
            if(selected)bg.setStroke(dp(1),0x99E5C77A);
            item.setBackground(bg);
            item.setAlpha(selected?1f:.72f);
        }
        if(active!=lastLine){
            lastLine=active;
            if(lyricScroll!=null&&active>=0&&active<phraseViews.length){
                final TextView target=phraseViews[active];
                lyricScroll.post(()->{
                    if(target.getParent()==phraseContainer){
                        int top=target.getTop()-dp(48);
                        lyricScroll.smoothScrollTo(0,Math.max(0,top));
                    }
                });
            }
        }
        if(lyricHint!=null)lyricHint.setText(takbeer?"تكبيرات الأذان":"كلمات الأذان كاملة — تمييز العبارة مع صوت المؤذن");
        if(progress!=null)progress.setProgress(duration>0?(int)Math.min(100L,position*100L/duration):0);
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        String prayer=getIntent().getStringExtra("prayer");
        if(prayer==null)prayer="الصلاة";
        int choice=getSharedPreferences("zad_reminders",MODE_PRIVATE).getInt("adhan_wallpaper",0);
        if(choice<0||choice>=ids.length)choice=0;

        FrameLayout frame=new FrameLayout(this);
        ImageView image=new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        AdhanWallpaperImages.load(this,image,ids[choice]);
        frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);
        shade.setBackgroundColor(0x77000000);
        frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24),dp(30),dp(24),dp(30));

        TextView brand=new TextView(this);
        brand.setText("سكينة – أذان وصلاة وقرآن");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(19);
        brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        brand.setGravity(Gravity.CENTER);

        TextView title=new TextView(this);
        title.setText("حان الآن وقت صلاة");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,dp(18),0,dp(4));

        TextView name=new TextView(this);
        name.setText(prayer);
        name.setTextColor(Color.WHITE);
        name.setTextSize(36);
        name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        name.setGravity(Gravity.CENTER);

        LinearLayout lyricCard=new LinearLayout(this);
        lyricCard.setOrientation(LinearLayout.VERTICAL);
        lyricCard.setGravity(Gravity.CENTER);
        lyricCard.setPadding(dp(14),dp(14),dp(14),dp(12));
        GradientDrawable cardBg=new GradientDrawable();
        cardBg.setColor(0xB20A3028);
        cardBg.setCornerRadius(dp(22));
        cardBg.setStroke(dp(1),0x88D5BD82);
        lyricCard.setBackground(cardBg);
        LinearLayout.LayoutParams lyricCardParams=new LinearLayout.LayoutParams(-1,dp(280));
        lyricCardParams.topMargin=dp(20);

        lyricHint=new TextView(this);
        lyricHint.setText("كلمات الأذان كاملة — تمييز العبارة مع صوت المؤذن");
        lyricHint.setTextColor(0xFFE6D8B5);
        lyricHint.setTextSize(13);
        lyricHint.setGravity(Gravity.CENTER);
        lyricHint.setPadding(0,0,0,dp(8));

        lyricScroll=new ScrollView(this);
        lyricScroll.setFillViewport(false);
        lyricScroll.setVerticalScrollBarEnabled(false);
        phraseContainer=new LinearLayout(this);
        phraseContainer.setOrientation(LinearLayout.VERTICAL);
        phraseContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        lyricScroll.addView(phraseContainer,new ScrollView.LayoutParams(-1,-2));

        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        LinearLayout.LayoutParams progressParams=new LinearLayout.LayoutParams(-1,dp(5));
        progressParams.topMargin=dp(8);

        lyricCard.addView(lyricHint);
        lyricCard.addView(lyricScroll,new LinearLayout.LayoutParams(-1,0,1f));
        lyricCard.addView(progress,progressParams);

        TextView footer=new TextView(this);
        footer.setText("الصلاة نور وطمأنينة");
        footer.setTextColor(Color.WHITE);
        footer.setTextSize(17);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0,dp(20),0,dp(22));

        Button stop=new Button(this);
        stop.setText("إيقاف الأذان");
        stop.setTextSize(17);
        stop.setAllCaps(false);
        stop.setTextColor(Color.WHITE);
        if(Build.VERSION.SDK_INT>=21)stop.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(8,72,54)));
        stop.setOnClickListener(v->{
            Intent action=new Intent(this,AdhanPlaybackService.class);
            action.setAction(AdhanPlaybackService.ACTION_STOP);
            try{startService(action);}catch(Exception error){android.util.Log.e("ZadAlmoslem","Could not stop adhan from full-screen screen",error);}
            finish();
        });

        content.addView(brand);
        content.addView(title);
        content.addView(name);
        content.addView(lyricCard,lyricCardParams);
        content.addView(footer);
        content.addView(stop,new LinearLayout.LayoutParams(-1,dp(54)));
        frame.addView(content,new FrameLayout.LayoutParams(-1,-1));
        setContentView(frame);
        hideSystemBars();
        updateLyrics();
    }

    @Override protected void onResume(){super.onResume();handler.removeCallbacks(lyricTicker);handler.post(lyricTicker);}
    @Override protected void onPause(){handler.removeCallbacks(lyricTicker);super.onPause();}
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus)hideSystemBars();}
}