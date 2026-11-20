package com.zad.almoslem;
import android.app.Activity;import android.content.Intent;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.os.Bundle;import android.view.Gravity;import android.widget.*;
import java.util.Locale;
public class HomeActivity extends Activity{
String[] prayers={"الفجر","الظهر","العصر","المغرب","العشاء"};TextView[] times=new TextView[5];int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
TextView label(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
void open(Class<?> c){startActivity(new Intent(this,c));}
@Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(8,48,40));getWindow().setNavigationBarColor(Color.rgb(8,48,40));build();}
void build(){ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(14),dp(14),dp(14),dp(22));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);
TextView heading=label("زاد المسلم",24,Color.WHITE,true);TextView ayah=label("﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",14,Color.rgb(232,211,158),true);TextView ref=label("سورة الرعد • الآية ٢٨",11,Color.rgb(222,239,229),false);TextClock clock=new TextClock(this);clock.setFormat12Hour("hh:mm a");clock.setFormat24Hour("HH:mm");clock.setTextSize(27);clock.setTextColor(Color.WHITE);clock.setGravity(Gravity.CENTER);clock.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
LinearLayout hero=new LinearLayout(this);hero.setOrientation(1);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(14),dp(14),dp(14),dp(14));hero.setBackground(shape(Color.rgb(8,48,40),20));hero.addView(heading);hero.addView(clock);hero.addView(ayah);hero.addView(ref);root.addView(hero,new LinearLayout.LayoutParams(-1,-2));
TextView prayerTitle=label("مواقيت الصلاة",19,Color.rgb(20,55,43),true);prayerTitle.setGravity(Gravity.RIGHT);prayerTitle.setPadding(0,dp(16),0,dp(8));root.addView(prayerTitle);
LinearLayout prayerCard=new LinearLayout(this);prayerCard.setOrientation(0);prayerCard.setPadding(dp(7),dp(12),dp(7),dp(12));prayerCard.setBackground(shape(Color.WHITE,16));
for(int i=0;i<prayers.length;i++){LinearLayout cell=new LinearLayout(this);cell.setOrientation(1);cell.setGravity(Gravity.CENTER);cell.addView(label(new String[]{"🌅","☀️","🌤️","🌇","🌙"}[i],17,Color.rgb(25,90,70),false));cell.addView(label(prayers[i],11,Color.rgb(70,85,75),true));times[i]=label("--:--",13,Color.rgb(8,48,40),true);cell.addView(times[i]);prayerCard.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}root.addView(prayerCard);
Button details=new Button(this);details.setText("عرض مواقيت الصلاة بالتفصيل");details.setOnClickListener(v->open(PrayerDashboardActivity.class));root.addView(details,new LinearLayout.LayoutParams(-1,-2));
TextView quick=label("وصول سريع",18,Color.rgb(20,55,43),true);quick.setGravity(Gravity.RIGHT);quick.setPadding(0,dp(14),0,dp(5));root.addView(quick);
addShortcut(root,"📖 المصحف الشريف",MushafPageActivity.class);addShortcut(root,"🎧 استماع القرآن الكريم",RadioActivity.class);addShortcut(root,"۞ السبحة الإلكترونية",TasbeehActivity.class);
setContentView(AppTabs.wrap(this,scroll,"home"));refreshTimes();}
void addShortcut(LinearLayout root,String title,Class<?> c){Button b=new Button(this);b.setText(title);b.setOnClickListener(v->open(c));root.addView(b,new LinearLayout.LayoutParams(-1,-2));}
void refreshTimes(){android.content.SharedPreferences p=getSharedPreferences("zad_reminders",MODE_PRIVATE);for(int i=0;i<prayers.length;i++){int h=p.getInt("native_time_hour_"+prayers[i],-1),m=p.getInt("native_time_minute_"+prayers[i],0);times[i].setText(h<0?"--:--":String.format(Locale.getDefault(),"%02d:%02d",h,m));}}
@Override protected void onResume(){super.onResume();if(times[0]!=null)refreshTimes();}
}