package com.zad.almoslem;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class FridayWirdActivity extends Activity {
 private final int green=Color.rgb(8,48,40);
 private SharedPreferences prefs;
 private TextView countLabel,percentLabel;
 private ProgressBar progress;
 private CheckBox[] checks;
 private final String[] tasks={"قراءة سورة الكهف","الإكثار من الصلاة والسلام على النبي ﷺ","الاغتسال والتطيب ولبس أحسن الثياب","التبكير إلى صلاة الجمعة","الإنصات للخطبة وعدم اللغو","تحرّي ساعة الإجابة والإكثار من الدعاء"};
 @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("zad_friday_wird",MODE_PRIVATE);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(android.view.View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 private void build(){
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(15),dp(15),dp(25));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);setContentView(scroll);
  TextView title=label("ورد يوم الجمعة",24,Color.WHITE,true);title.setGravity(Gravity.CENTER);TextView sub=label("أعمال تذكيرية تعينك على اغتنام يوم الجمعة",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(14),dp(18),dp(14),dp(18));hero.setBackground(bg(green,18));hero.addView(title);hero.addView(sub);root.addView(hero);
  LinearLayout salawat=new LinearLayout(this);salawat.setOrientation(LinearLayout.VERTICAL);salawat.setPadding(dp(14),dp(13),dp(14),dp(13));salawat.setBackground(bg(Color.WHITE,15));
  salawat.addView(label("اللهم صلِّ وسلم على نبينا محمد",18,green,true));salawat.addView(label("أكثر من الصلاة والسلام على النبي ﷺ يوم الجمعة.",13,Color.DKGRAY,false));
  countLabel=label(String.valueOf(prefs.getInt("salawat_count",0)),34,green,true);countLabel.setGravity(Gravity.CENTER);salawat.addView(countLabel);
  LinearLayout buttons=new LinearLayout(this);Button add=new Button(this);add.setText("صلِّ على النبي ﷺ");Button reset=new Button(this);reset.setText("تصفير العداد");buttons.addView(add,new LinearLayout.LayoutParams(0,dp(48),1));buttons.addView(reset,new LinearLayout.LayoutParams(0,dp(48),1));salawat.addView(buttons);root.addView(salawat);
  add.setOnClickListener(v->{int n=prefs.getInt("salawat_count",0)+1;prefs.edit().putInt("salawat_count",n).apply();countLabel.setText(String.valueOf(n));});
  reset.setOnClickListener(v->{prefs.edit().putInt("salawat_count",0).apply();countLabel.setText("0");});
  Button kahf=new Button(this);kahf.setText("📖  افتح سورة الكهف في المصحف");root.addView(kahf);kahf.setOnClickListener(v->{Intent i=new Intent(this,QuranTextActivity.class);i.putExtra("surah_number",18);startActivity(i);});
  TextView checklistTitle=label("قائمة أعمال الجمعة",18,green,true);checklistTitle.setPadding(0,dp(14),0,dp(6));root.addView(checklistTitle);
  LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(12),dp(8),dp(12),dp(8));list.setBackground(bg(Color.WHITE,15));root.addView(list);
  checks=new CheckBox[tasks.length];
  for(int i=0;i<tasks.length;i++){final int index=i;CheckBox cb=new CheckBox(this);cb.setText(tasks[i]);cb.setTextSize(14);cb.setTextColor(Color.rgb(45,60,50));cb.setChecked(prefs.getBoolean("task_"+i,false));checks[i]=cb;list.addView(cb);cb.setOnCheckedChangeListener((b,checked)->{prefs.edit().putBoolean("task_"+index,checked).apply();updateProgress();});}
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);percentLabel=label("",13,green,true);Button clear=new Button(this);clear.setText("إعادة القائمة");row.addView(percentLabel,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(clear,new LinearLayout.LayoutParams(dp(125),dp(48)));root.addView(row);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(tasks.length);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(10)));
  clear.setOnClickListener(v->{for(CheckBox cb:checks)cb.setChecked(false);});
  TextView foot=label("هذه قائمة تذكيرية بالأعمال، وليست كل بنودها واجبة، ولا يلزم عدد محدد للصلاة على النبي ﷺ.",12,Color.GRAY,false);foot.setPadding(0,dp(12),0,0);root.addView(foot);updateProgress();
 }
 private void updateProgress(){if(checks==null)return;int done=0;for(CheckBox cb:checks)if(cb.isChecked())done++;progress.setProgress(done);percentLabel.setText("إنجاز ورد الجمعة: "+(done*100/tasks.length)+"% ("+done+"/"+tasks.length+")");}
}