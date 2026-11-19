package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

public class RadioActivity extends Activity {
    private static final String GREEN="#083028";
    private TextView status;
    private final Handler handler=new Handler();
    private final Runnable statusTicker=new Runnable(){@Override public void run(){refreshStatus();handler.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(8,48,40));getWindow().setNavigationBarColor(Color.rgb(8,48,40));build();handler.post(statusTicker);}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,30,24,24);root.setBackgroundColor(Color.rgb(244,247,243));AppTabs.add(this,root,"radio");setContentView(root);
        TextView title=new TextView(this);title.setText("إذاعة القرآن الكريم");title.setTextSize(25);title.setTextColor(Color.WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);title.setPadding(12,28,12,28);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(8,48,40));bg.setCornerRadius(28);title.setBackground(bg);root.addView(title);
        TextView desc=new TextView(this);desc.setText("استمع إلى إذاعة القرآن الكريم من القاهرة، مع استمرار التشغيل في الخلفية.");desc.setTextSize(15);desc.setTextColor(Color.DKGRAY);desc.setPadding(0,22,0,18);root.addView(desc);
        Button play=new Button(this);play.setText("▶ تشغيل إذاعة القرآن");root.addView(play,new LinearLayout.LayoutParams(-1,-2));play.setOnClickListener(v->start("https://qurango.net/radio/tarateel","إذاعة القرآن الكريم"));
        Button cairo=new Button(this);cairo.setText("▶ إذاعة القرآن الكريم من القاهرة");root.addView(cairo,new LinearLayout.LayoutParams(-1,-2));cairo.setOnClickListener(v->start("https://stream.radiojar.com/8s5u5tpdtwzuv","إذاعة القرآن الكريم من القاهرة"));
        Button stop=new Button(this);stop.setText("■ إيقاف الإذاعة");root.addView(stop,new LinearLayout.LayoutParams(-1,-2));stop.setOnClickListener(v->{Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(RadioPlaybackService.ACTION_STOP);try{startService(i);}catch(Exception e){status.setText("تعذر الإيقاف: "+e.getMessage());}});
        status=new TextView(this);status.setText("حالة البث تظهر في إشعار التشغيل أعلى الهاتف.");status.setTextColor(Color.rgb(30,70,50));status.setPadding(0,20,0,20);root.addView(status);
        Button back=new Button(this);back.setText("العودة");root.addView(back);back.setOnClickListener(v->finish());
    }
    private void refreshStatus(){
        if(status==null)return;
        android.content.SharedPreferences p=getSharedPreferences("zad_audio",MODE_PRIVATE);
        boolean ready=p.getBoolean("radio_ready",false),playing=p.getBoolean("radio_playing",false);
        String error=p.getString("radio_last_error","");
        if(ready)status.setText("● البث يعمل الآن — الصوت مستمر من الإذاعة.");
        else if(playing)status.setText("جارٍ الاتصال بالبث أو تجربة مصدر احتياطي...");
        else if(error!=null&&!error.isEmpty())status.setText("تعذر تشغيل الإذاعة: "+error);
        else status.setText("اضغط تشغيل لبدء إذاعة القرآن الكريم.");
    }
    @Override protected void onDestroy(){handler.removeCallbacks(statusTicker);super.onDestroy();}
    private void start(String url,String label){Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(RadioPlaybackService.ACTION_PLAY);i.putExtra("url",url);i.putExtra("kind","radio");i.putExtra("label",label);try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("تم إرسال طلب تشغيل البث. انتظر إشعار حالة الاتصال.");}catch(Exception e){status.setText("تعذر بدء البث: "+e.getMessage());}}
}