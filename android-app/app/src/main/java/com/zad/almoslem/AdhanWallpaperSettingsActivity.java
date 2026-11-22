package com.zad.almoslem;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.widget.*;
public class AdhanWallpaperSettingsActivity extends Activity {
 private final String[] ids={"azhar_mosque_bg","kaaba_wallpaper","madinah_wallpaper","aqsa_wallpaper","quran_wallpaper"};
 private final String[] names={"الجامع الأزهر","المسجد الحرام","المسجد النبوي","المسجد الأقصى","المصحف الشريف"};
 private final int green=Color.rgb(8,48,40);
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.RIGHT);t.setTextDirection(android.view.View.TEXT_DIRECTION_RTL);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();}
 private void build(){
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(15),dp(15),dp(15),dp(25));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);
  TextView title=label("خلفيات شاشة الأذان",24,Color.WHITE,true);title.setGravity(Gravity.CENTER);
  TextView sub=label("اختر الخلفية التي تحبها، وستظهر عند دخول وقت الصلاة.",14,Color.rgb(225,239,230),false);sub.setGravity(Gravity.CENTER);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(1);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(14),dp(18),dp(14),dp(18));hero.setBackground(bg(green,18));hero.addView(title);hero.addView(sub);root.addView(hero);
  int selected=getSharedPreferences("zad_reminders",MODE_PRIVATE).getInt("adhan_wallpaper",0);
  for(int i=0;i<ids.length;i++){final int index=i;
   LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackground(bg(Color.WHITE,15));
   ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);AdhanWallpaperImages.load(this,image,ids[i]);card.addView(image,new LinearLayout.LayoutParams(-1,dp(145)));
   TextView name=label(names[i]+(selected==i?"  ✓":""),17,green,true);name.setPadding(dp(4),dp(9),dp(4),dp(4));card.addView(name);
   TextView choose=label(selected==i?"الخلفية الحالية":"اختيار هذه الخلفية",13,selected==i?green:Color.DKGRAY,false);card.addView(choose);
   card.setOnClickListener(v->{getSharedPreferences("zad_reminders",MODE_PRIVATE).edit().putInt("adhan_wallpaper",index).apply();Toast.makeText(this,"تم اختيار خلفية "+names[index],Toast.LENGTH_SHORT).show();build();});
   LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(12);root.addView(card,cp);
  }
  Button access=new Button(this);
  boolean fullScreenAllowed=Build.VERSION.SDK_INT<34 || ((android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE)).canUseFullScreenIntent();
  access.setText(fullScreenAllowed?"صلاحية شاشة الأذان الكاملة مفعّلة ✓":"تفعيل صلاحية شاشة الأذان الكاملة");
  access.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=34){try{
   android.app.NotificationManager manager=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
   if(manager!=null&&!manager.canUseFullScreenIntent()){
    Intent i=new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName()));startActivity(i);
   }else Toast.makeText(this,"صلاحية شاشة الأذان الكاملة مفعّلة بالفعل.",Toast.LENGTH_SHORT).show();
  }catch(Exception e){Toast.makeText(this,"افتح إعدادات التطبيق ثم فعّل إشعارات الشاشة الكاملة إن كانت متاحة.",Toast.LENGTH_LONG).show();}}
  else Toast.makeText(this,"تأكد من تفعيل إشعارات زاد وطمأنينة على الهاتف.",Toast.LENGTH_LONG).show();});
  LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(14);root.addView(access,ap);
  TextView note=label("إذا لم تظهر الخلفية على شاشة الهاتف، فعّل صلاحية شاشة الأذان الكاملة من الزر أعلاه، وتأكد من السماح بإشعارات التطبيق. الصور مضمّنة وتعمل دون إنترنت.",12,Color.GRAY,false);note.setPadding(dp(4),dp(10),dp(4),0);root.addView(note);
  TextView credits=label("مصادر الصور: Wikimedia Commons. الجامع الأزهر: Mohamed Ouda (CC BY-SA 3.0)؛ الكعبة: MohmmadRjab (CC0)؛ المسجد النبوي: Aymanzaid2 (CC BY-SA 4.0)؛ الأقصى: جمان المحتسب (CC0)؛ المصحف: Ali139099 (CC0).",11,Color.DKGRAY,false);credits.setPadding(dp(4),dp(10),dp(4),0);root.addView(credits);setContentView(scroll);
 }
}