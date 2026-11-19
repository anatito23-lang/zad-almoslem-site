package com.zad.almoslem;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

public class DailyReminderSettingsActivity extends Activity {
 private final int green=Color.rgb(8,48,40);
 private SharedPreferences prefs;
 private final String[] keys={"morning","evening","qiyam"};
 private final String[] names={"أذكار الصباح","أذكار المساء","قيام الليل"};
 private final int[] defaultHours={7,17,4};
 private TextView[] timeLabels=new TextView[3];
 @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("zad_reminders",MODE_PRIVATE);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(android.view.View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 private String timeText(String key,int i){return String.format(Locale.getDefault(),"%02d:%02d",prefs.getInt(key+"_hour",defaultHours[i]),prefs.getInt(key+"_minute",0));}
 private void schedule(int i){String key=keys[i];ReminderReceiver.scheduleDaily(this,key,prefs.getInt(key+"_hour",defaultHours[i]),prefs.getInt(key+"_minute",0),"حان وقت "+names[i]+"، لا تنس ذكر الله.");}
 private void build(){
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(15),dp(15),dp(25));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);setContentView(scroll);
  TextView title=label("تنبيهاتك اليومية",24,Color.WHITE,true);title.setGravity(Gravity.CENTER);TextView sub=label("اختر مواعيد التذكير التي تناسب يومك",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(14),dp(18),dp(14),dp(18));hero.setBackground(bg(green,18));hero.addView(title);hero.addView(sub);root.addView(hero);
  for(int i=0;i<keys.length;i++)addReminder(root,i);
  addPrayerApproachSettings(root);
  TextView note=label("تحتاج التنبيهات إلى السماح بإشعارات التطبيق. وإذا لم يتوفر إذن المنبهات الدقيقة، يستخدم Android تنبيهًا بديلًا قد يتأخر قليلًا.",12,Color.GRAY,false);note.setPadding(0,dp(10),0,0);root.addView(note);
 }

 private void addPrayerApproachSettings(LinearLayout root){
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
  card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(bg(Color.WHITE,15));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);root.addView(card,lp);
  card.addView(label("التذكير باقتراب الصلاة",18,green,true));
  Switch enabled=new Switch(this);enabled.setText("تنبيه صوتي قبل الصلاة");enabled.setChecked(prefs.getBoolean("approach_alert_enabled",true));card.addView(enabled);
  TextView value=label("قبل الصلاة بـ "+prefs.getInt("approach_alert_minutes",15)+" دقيقة",14,green,false);card.addView(value);
  SeekBar minutes=new SeekBar(this);minutes.setMax(59);minutes.setProgress(Math.max(1,Math.min(60,prefs.getInt("approach_alert_minutes",15)))-1);card.addView(minutes);
  enabled.setOnCheckedChangeListener((button,checked)->{
   prefs.edit().putBoolean("approach_alert_enabled",checked).apply();
   ReminderReceiver.rescheduleApproachAlerts(this);
  });
  minutes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
    int m=progress+1;value.setText("قبل الصلاة بـ "+m+" دقيقة");
    if(fromUser){prefs.edit().putInt("approach_alert_minutes",m).apply();ReminderReceiver.rescheduleApproachAlerts(DailyReminderSettingsActivity.this);}
   }
   public void onStartTrackingTouch(SeekBar bar){}
   public void onStopTrackingTouch(SeekBar bar){}
  });
 }
 private void addReminder(LinearLayout root,int i){
  String key=keys[i];LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(bg(Color.WHITE,15));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);root.addView(card,lp);
  card.addView(label(names[i],18,green,true));
  Switch enabled=new Switch(this);enabled.setText("تفعيل التنبيه");enabled.setTextSize(14);enabled.setChecked(prefs.getBoolean(key+"_enabled",false));card.addView(enabled);
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
  timeLabels[i]=label(timeText(key,i),18,green,true);Button choose=new Button(this);choose.setText("تغيير الوقت");
  row.addView(timeLabels[i],new LinearLayout.LayoutParams(0,dp(48),1));row.addView(choose,new LinearLayout.LayoutParams(dp(130),dp(48)));card.addView(row);
  choose.setOnClickListener(v->{int h=prefs.getInt(key+"_hour",defaultHours[i]),m=prefs.getInt(key+"_minute",0);new TimePickerDialog(this,(picker,hour,minute)->{prefs.edit().putInt(key+"_hour",hour).putInt(key+"_minute",minute).apply();timeLabels[i].setText(timeText(key,i));if(enabled.isChecked())schedule(i);},h,m,true).show();});
  enabled.setOnCheckedChangeListener((button,checked)->{if(checked)schedule(i);else ReminderReceiver.cancelDaily(this,key);});
 }
}