package com.zad.almoslem;
import android.app.Activity;import android.content.Intent;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.os.Build;import android.os.Bundle;import android.view.Gravity;import android.widget.*;
public class RuqyahActivity extends Activity{
private TextView status;private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
private TextView label(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.RIGHT);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
@Override public void onCreate(Bundle b){super.onCreate(b);AppThemeColors.apply(this);build();}
private void build(){ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(15),dp(16),dp(15),dp(24));root.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);
TextView title=label("الرقية الشرعية",24,AppThemeColors.text(this),true);title.setGravity(Gravity.CENTER);root.addView(title);
TextView intro=label("اختر تسجيل الرقية والقارئ. يمكنك إيقاف الصوت من الزر الموجود أسفل كل تسجيل.",14,AppThemeColors.muted(this),false);intro.setPadding(0,dp(8),0,dp(16));root.addView(intro);
addCard(root,"الشيخ محمود خليل الحصري","https://archive.org/download/20240228_20240228_1849/041%20%2011%20%20%20%D8%A7%D9%84%D8%B1%D9%82%D9%8A%D8%A9%20%D8%A7%D9%84%D8%B4%D8%B1%D8%B9%D9%8A%D8%A9%20%D9%84%D8%B9%D9%84%D8%A7%D8%AC%20%D8%A7%D9%84%D8%B3%D8%AD%D8%B1%20%D8%A7%D9%84%D9%85%D8%B3%20%D8%A7%D9%84%D8%AD%D8%B3%D8%AF%20%D8%A8%D8%B5%D9%88%D8%AA%20%D8%A7%D9%84%D8%B4%D9%8A%D8%AE%20%D9%85%D8%AD%D9%85%D9%88%D8%AF%20%D8%AE%D9%84%D9%8A%D9%84%20%D8%A7%D9%84%D8%AD%D8%B5%D8%B1%D9%89%20%D8%B1%D8%AD%D9%85%D9%87%20%D8%A7%D9%84%D9%84%D9%87%20%D8%AC%D9%88%D8%AF%D8%A9%20%D8%B9%D8%A7%D9%84%D9%8A%D8%A9.mp3");
addCard(root,"الشيخ مشاري راشد العفاسي","https://archive.org/download/e3fasi-Rokia/e3fasi-seher.mp3");
addCard(root,"الشيخ ناصر القطامي","https://archive.org/download/alqtami-Roqiaa/alqtami-seher.mp3");
addCard(root,"الشيخ ياسر الدوسري","https://archive.org/download/dosari-Roqia/dosari-seher.mp3");
addCard(root,"الشيخ علي الحذيفي","https://archive.org/download/hothife-Roqia/hothife-seher.mp3");
addCard(root,"الشيخ هاني الرفاعي","https://archive.org/download/rifa3i-Roqia/rifa3i-seher.mp3");
status=label("إذا لم يبدأ أحد التسجيلات، ستظهر رسالة توضح سبب تعذر الاتصال بالمصدر.",12,AppThemeColors.muted(this),false);status.setPadding(0,dp(12),0,0);root.addView(status);
setContentView(AppTabs.wrap(this,scroll,"more"));}
private void addCard(LinearLayout root,String name,String url){LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(bg(AppThemeColors.surface(this),16));TextView title=label(name,16,AppThemeColors.text(this),true);card.addView(title);
Button play=new Button(this);play.setText("▶ تشغيل الرقية");card.addView(play);play.setOnClickListener(v->{Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(RadioPlaybackService.ACTION_PLAY);i.putExtra("url",url);i.putExtra("kind","ruqyah");i.putExtra("label","الرقية الشرعية — "+name);try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("طلب تشغيل تسجيل "+name);startActivity(new Intent(this,NowPlayingActivity.class));}catch(Exception e){status.setText("تعذر بدء التشغيل: "+e.getMessage());}});
Button stop=new Button(this);stop.setText("■ إيقاف الصوت");card.addView(stop);stop.setOnClickListener(v->stopAudio());
LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);root.addView(card,lp);}
private void stopAudio(){Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(RadioPlaybackService.ACTION_STOP);try{startService(i);if(status!=null)status.setText("تم إرسال طلب إيقاف الصوت.");}catch(Exception e){if(status!=null)status.setText("تعذر الإيقاف: "+e.getMessage());}}
}