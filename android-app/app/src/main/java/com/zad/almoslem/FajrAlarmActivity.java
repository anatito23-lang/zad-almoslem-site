package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import java.util.Random;

public class FajrAlarmActivity extends Activity {
    private Ringtone ringtone;
    private int answer;
    private LinearLayout root;
    private TextView feedback;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=27)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        playAlarm();buildQuestion();
    }
    private void playAlarm(){
        try{Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);ringtone=RingtoneManager.getRingtone(this,uri);if(ringtone!=null){if(Build.VERSION.SDK_INT>=28)ringtone.setLooping(true);ringtone.play();}}catch(Exception ignored){}
    }
    private void buildQuestion(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(28),dp(24),dp(28));root.setBackgroundColor(Color.rgb(7,39,34));
        TextView title=text("🌙 منبه صلاة الفجر",25,Color.WHITE,true);root.addView(title);
        TextView sub=text("صباحٌ يبدأ بذكر الله",15,Color.rgb(225,210,160),false);sub.setPadding(0,dp(12),0,dp(24));root.addView(sub);
        Random random=new Random();int a=2+random.nextInt(9),b=2+random.nextInt(9);answer=a+b;
        TextView question=text("أجب عن السؤال لإيقاف المنبه",18,Color.WHITE,true);root.addView(question);
        TextView sum=text(a+" + "+b+" = ؟",34,Color.rgb(245,220,155),true);sum.setPadding(0,dp(20),0,dp(20));root.addView(sum);
        feedback=text("",14,Color.WHITE,false);feedback.setPadding(0,dp(10),0,dp(10));root.addView(feedback);
        int[] choices={answer,answer+1,answer-1,answer+2};for(int i=choices.length-1;i>0;i--){int j=random.nextInt(i+1),v=choices[i];choices[i]=choices[j];choices[j]=v;}
        for(int value:choices){Button choice=new Button(this);choice.setText(String.valueOf(value));choice.setTextSize(20);choice.setOnClickListener(v->{if(value==answer)stopAlarm();else feedback.setText("الإجابة غير صحيحة، حاول مرة أخرى.");});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.topMargin=dp(7);root.addView(choice,lp);}
        Button snooze=new Button(this);snooze.setText("غفوة 5 دقائق");snooze.setOnClickListener(v->{FajrAlarmScheduler.scheduleAt(this,System.currentTimeMillis()+5*60*1000L);stopSound();finish();});LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(52));slp.topMargin=dp(20);root.addView(snooze,slp);
        setContentView(root);
    }
    private void stopSound(){try{if(ringtone!=null&&ringtone.isPlaying())ringtone.stop();}catch(Exception ignored){}}
    private void stopAlarm(){stopSound();android.app.NotificationManager nm=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.cancel(4818);Toast.makeText(this,"صباح الخير، تقبل الله طاعتك",Toast.LENGTH_SHORT).show();finish();}
    @Override protected void onDestroy(){stopSound();super.onDestroy();}
}