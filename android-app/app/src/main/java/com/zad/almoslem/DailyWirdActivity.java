package com.zad.almoslem;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DailyWirdActivity extends Activity {
 private final int green=Color.rgb(8,48,40);
 private final String[] tasks={"صلاة الفجر في وقتها","أذكار الصباح","قراءة ورد من القرآن","أذكار المساء","صلة رحم أو صدقة أو إحسان","الوتر والاستغفار"};
 private SharedPreferences prefs;
 private CheckBox[] checks;
 private TextView percent;
 private ProgressBar progress;
 @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("zad_daily_wird",MODE_PRIVATE);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);resetIfNewDay();build();}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(android.view.View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 private void resetIfNewDay(){String today=new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date());String saved=prefs.getString("date","");if(!today.equals(saved)){SharedPreferences.Editor e=prefs.edit().putString("date",today);for(int i=0;i<tasks.length;i++)e.putBoolean("task_"+i,false);e.apply();}}
 private void build(){
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(15),dp(15),dp(25));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);setContentView(scroll);
  TextView title=label("وردي اليومي",24,Color.WHITE,true);title.setGravity(Gravity.CENTER);TextView sub=label("تابع إنجازك اليومي في أعمال الخير",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(14),dp(18),dp(14),dp(18));hero.setBackground(bg(green,18));hero.addView(title);hero.addView(sub);root.addView(hero);
  LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(12),dp(8),dp(12),dp(8));list.setBackground(bg(Color.WHITE,15));root.addView(list);
  checks=new CheckBox[tasks.length];
  for(int i=0;i<tasks.length;i++){final int n=i;CheckBox cb=new CheckBox(this);cb.setText(tasks[i]);cb.setTextSize(15);cb.setTextColor(Color.rgb(45,60,50));cb.setChecked(prefs.getBoolean("task_"+i,false));checks[i]=cb;list.addView(cb);cb.setOnCheckedChangeListener((b,checked)->{prefs.edit().putBoolean("task_"+n,checked).apply();update();});}
  percent=label("",14,green,true);percent.setPadding(0,dp(12),0,dp(8));root.addView(percent);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(tasks.length);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(10)));
  Button reset=new Button(this);reset.setText("إعادة ضبط ورد اليوم");root.addView(reset);reset.setOnClickListener(v->{for(CheckBox cb:checks)cb.setChecked(false);});
  root.addView(label("يُحفظ تقدمك على هذا الجهاز، ويُعاد ضبطه تلقائيًا عند بداية يوم جديد.",12,Color.GRAY,false));update();
 }
 private void update(){if(checks==null)return;int n=0;for(CheckBox c:checks)if(c.isChecked())n++;progress.setProgress(n);percent.setText("إنجاز اليوم: "+(n*100/tasks.length)+"% ("+n+"/"+tasks.length+")");}
}