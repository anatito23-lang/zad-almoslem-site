package com.zad.almoslem;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
public class AdhanFullScreenActivity extends Activity {
 private final String[] ids={"azhar_mosque_bg","kaaba_wallpaper","madinah_wallpaper","aqsa_wallpaper","quran_wallpaper"};
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private void hideSystemBars(){
  getWindow().getDecorView().setSystemUiVisibility(
   View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
   View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
   View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
  );
 }
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
  getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
  String prayer=getIntent().getStringExtra("prayer");if(prayer==null)prayer="الصلاة";
  int choice=getSharedPreferences("zad_reminders",MODE_PRIVATE).getInt("adhan_wallpaper",0);if(choice<0||choice>=ids.length)choice=0;
  FrameLayout frame=new FrameLayout(this);
  ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);AdhanWallpaperImages.load(this,image,ids[choice]);frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
  View shade=new View(this);shade.setBackgroundColor(0x66000000);frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setGravity(Gravity.CENTER);content.setPadding(dp(24),dp(36),dp(24),dp(36));
  TextView brand=new TextView(this);brand.setText("زاد وطمأنينة");brand.setTextColor(Color.WHITE);brand.setTextSize(19);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);brand.setGravity(Gravity.CENTER);
  TextView title=new TextView(this);title.setText("حان الآن وقت صلاة");title.setTextColor(Color.WHITE);title.setTextSize(24);title.setGravity(Gravity.CENTER);title.setPadding(0,dp(24),0,dp(8));
  TextView name=new TextView(this);name.setText(prayer);name.setTextColor(Color.WHITE);name.setTextSize(38);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);name.setGravity(Gravity.CENTER);
  TextView call=new TextView(this);call.setText("الصلاة نور وطمأنينة");call.setTextColor(Color.WHITE);call.setTextSize(18);call.setGravity(Gravity.CENTER);call.setPadding(0,dp(12),0,dp(26));
  Button stop=new Button(this);stop.setText("إيقاف الأذان");stop.setTextSize(17);stop.setAllCaps(false);stop.setTextColor(Color.WHITE);
  if(Build.VERSION.SDK_INT>=21)stop.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(8,72,54)));
  stop.setOnClickListener(v->{
   Intent action=new Intent(this,AdhanPlaybackService.class);action.setAction(AdhanPlaybackService.ACTION_STOP);
   try{startService(action);}catch(Exception error){android.util.Log.e("ZadAlmoslem","Could not stop adhan from full-screen screen",error);}
   finish();
  });
  content.addView(brand);content.addView(title);content.addView(name);content.addView(call);content.addView(stop,new LinearLayout.LayoutParams(-1,dp(54)));
  frame.addView(content,new FrameLayout.LayoutParams(-1,-1));setContentView(frame);hideSystemBars();
 }
 @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus)hideSystemBars();}
}