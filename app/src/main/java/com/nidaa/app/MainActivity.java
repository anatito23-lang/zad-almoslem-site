package com.nidaa.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.data.DateComponents;
import com.batoulapps.adhan.PrayerTimes;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity implements SensorEventListener {
    private final int bg=Color.rgb(10,22,37), panel=Color.rgb(20,38,56), gold=Color.rgb(218,181,111), white=Color.rgb(244,241,232), muted=Color.rgb(164,177,188);
    private LinearLayout root,content;
    private SharedPreferences prefs;
    private String tab="الرئيسية";
    private final String[] prayerNames={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};
    private final String[] prayers={"fajr","sunrise","dhuhr","asr","maghrib","isha"};
    private final String[] cities={"الإسكندرية","القاهرة","الزقازيق","مدينة العبور","الإسماعيلية","المنصورة","طنطا","أسيوط"};
    private final double[][] coords={{31.2001,29.9187},{30.0444,31.2357},{30.5877,31.5020},{30.2285,31.4810},{30.5965,32.2715},{31.0409,31.3785},{30.7865,31.0004},{27.1809,31.1837}};
    private final String[] muadhins={"بدون اختيار","عبد الباسط عبد الصمد","محمود خليل الحصري","مصطفى إسماعيل","محمود علي البنا","محمد صديق المنشاوي","محمد رفعت","النقشبندي","نصر الدين طوبار","أذان الحرم المكي"};
    private TextView status;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private CompassView compassView;
    private float azimuth=0;
    private boolean compassAvailable=false;
    private final Handler clockHandler=new Handler();
    private TextView countdownView;
    private final Runnable clockTick=new Runnable(){@Override public void run(){updateCountdown();clockHandler.postDelayed(this,1000);}};
    private int tasbeehCount=0;
    private int dhikrIndex=0;
    private final String[] adhkar={"سبحان الله","الحمد لله","الله أكبر","أستغفر الله","لا إله إلا الله","اللهم صل وسلم على نبينا محمد"};
    private final String[] adhkarDetails={"من قال سبحان الله وبحمده مائة مرة حُطّت خطاياه وإن كانت مثل زبد البحر.","كلمتان خفيفتان على اللسان، ثقيلتان في الميزان، حبيبتان إلى الرحمن.","التكبير من أحب الذكر إلى الله.","استغفر الله وأتوب إليه.","أفضل الذكر لا إله إلا الله.","الصلاة على النبي ﷺ من أجلّ القربات."};
    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("nidaa",MODE_PRIVATE);sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);rotationSensor=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);compassAvailable=rotationSensor!=null;getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);render();}
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(r);d.setStroke(1,Color.rgb(42,62,80));return d;}
    private TextView txt(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);t.setPadding(10,10,10,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void render(){
        root=vertical();root.setBackgroundColor(bg);setContentView(root);
        LinearLayout head=new LinearLayout(this);head.setPadding(18,14,18,10);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=txt("نِداء",28,gold,true);brand.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        TextView sub=txt("رفيق صلاتك",13,muted,false);
        head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));head.addView(sub);root.addView(head);
        ScrollView sc=new ScrollView(this);content=vertical();content.setPadding(16,8,16,18);sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        showTab();
        LinearLayout nav=new LinearLayout(this);nav.setPadding(4,8,4,10);nav.setBackgroundColor(panel);
        String[] tabs={"الرئيسية","الصلاة","القرآن","الأذكار","القبلة","المزيد"};
        for(String s:tabs){TextView b=txt(s,13,s.equals(tab)?gold:white,true);b.setOnClickListener(v->{tab=s;render();});nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);
    }
    private void card(View v){LinearLayout c=vertical();c.setPadding(12,12,12,12);c.setBackground(shape(panel,18));c.addView(v);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=12;content.addView(c,p);}
    private int cityIndex(){return Math.max(0,Math.min(cities.length-1,prefs.getInt("city",0)));}
    private PrayerTimes times(){double[] c=coords[cityIndex()];return new PrayerTimes(new Coordinates(c[0],c[1]),DateComponents.from(new Date()),CalculationMethod.EGYPTIAN.getParameters());}
    private Date[] dates(PrayerTimes p){return new Date[]{p.fajr,p.sunrise,p.dhuhr,p.asr,p.maghrib,p.isha};}
    private String fmt(Date d){return new SimpleDateFormat("hh:mm a",new Locale("ar")).format(d);}
    private int nextIndex(){Date[] ds=dates(times());Date now=new Date();for(int i:new int[]{0,2,3,4,5})if(ds[i].after(now))return i;return 0;}
    private void showTab(){
        content.removeAllViews();
        if(tab.equals("الرئيسية")){
            String date=new SimpleDateFormat("EEEE، d MMMM yyyy",new Locale("ar")).format(new Date());
            LinearLayout dateRow=new LinearLayout(this);dateRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView city=txt(cities[cityIndex()],14,gold,true);city.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            TextView today=txt(date,13,muted,false);today.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            dateRow.addView(city,new LinearLayout.LayoutParams(0,-2,1));dateRow.addView(today);card(dateRow);

            LinearLayout hero=vertical();hero.setGravity(Gravity.CENTER);hero.setPadding(8,18,8,18);
            hero.addView(txt("الصلاة القادمة",15,muted,false));
            int n=nextIndex();
            hero.addView(txt(prayerNames[n],34,gold,true));
            hero.addView(txt(fmt(dates(times())[n]),25,white,true));
            countdownView=txt("--:--:--",30,white,true);
            countdownView.setPadding(8,14,8,14);hero.addView(countdownView);
            hero.addView(txt("الوقت المتبقي للصلاة",12,muted,false));
            card(hero);

            LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
            TextView title=txt("مواقيت الصلاة",20,white,true);title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            TextView method=txt("حساب مصر",12,muted,false);
            header.addView(title,new LinearLayout.LayoutParams(0,-2,1));header.addView(method);content.addView(header);
            PrayerTimes p=times();Date[] ds=dates(p);
            for(int i=0;i<6;i++){
                final int ix=i;
                LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(8,3,8,3);
                TextView name=txt(prayerNames[i],16,i==n?gold:white,true);name.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                TextView time=txt(fmt(ds[i]),16,i==n?gold:white,true);time.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
                row.addView(name,new LinearLayout.LayoutParams(0,-2,1));row.addView(time);
                card(row);
            }
            LinearLayout quick=vertical();
            quick.addView(txt("وصول سريع",18,white,true));
            String[] quickNames={"القرآن الكريم","أذكار الصباح والمساء","المسبحة الإلكترونية","اتجاه القبلة"};
            String[] destinations={"القرآن","الأذكار","الأذكار","القبلة"};
            for(int i=0;i<quickNames.length;i+=2){
                LinearLayout line=new LinearLayout(this);
                for(int j=i;j<Math.min(i+2,quickNames.length);j++){
                    final String dest=destinations[j];
                    TextView q=txt(quickNames[j],14,gold,true);q.setBackground(shape(bg,14));q.setOnClickListener(v->{tab=dest;render();});
                    LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(0,76,1);qp.setMargins(4,4,4,4);line.addView(q,qp);
                }
                quick.addView(line);
            }
            card(quick);
            TextView cityChange=txt("تغيير المدينة وإعدادات الموقع",14,gold,true);cityChange.setOnClickListener(v->chooseCity());card(cityChange);
        }        }else if(tab.equals("الصلاة")){
            card(txt("إعدادات المؤذّن",23,gold,true));
            card(txt("اختر الصوت المفضل لكل صلاة",15,white,true));
            for(int i:new int[]{0,2,3,4,5}){
                LinearLayout box=vertical();TextView label=txt(prayerNames[i],15,white,true);label.setGravity(Gravity.RIGHT);
                Spinner sp=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,muadhins);sp.setAdapter(a);
                sp.setSelection(prefs.getInt("mu_"+prayers[i],0));final int ix=i;sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){prefs.edit().putInt("mu_"+prayers[ix],pos).apply();}});
                box.addView(label);box.addView(sp);card(box);
            }
            card(txt("تنبيه مهم: أسماء المؤذنين هنا لا تعني أن التسجيلات الصوتية مرفقة بعد. سنضيف ملفات الأذان المرخصة أو التي توفرها أنت، ثم نفعّل التشغيل دون إنترنت.",13,muted,false));
            Switch reminder=new Switch(this);reminder.setText("تفعيل تنبيهات الصلاة");reminder.setTextColor(white);reminder.setChecked(prefs.getBoolean("reminders",false));reminder.setPadding(8,12,8,12);reminder.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("reminders",on).apply();if(on){if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},71);}else{PrayerAlarmScheduler.schedule(this,cityIndex());Toast.makeText(this,"تم تفعيل تنبيهات الصلاة",Toast.LENGTH_SHORT).show();}}else{PrayerAlarmScheduler.cancel(this);Toast.makeText(this,"تم إيقاف تنبيهات الصلاة",Toast.LENGTH_SHORT).show();}});card(reminder);
            Switch silent=new Switch(this);silent.setText("طلب تفعيل الصامت قبل الصلاة");silent.setTextColor(white);silent.setChecked(prefs.getBoolean("silent",false));silent.setPadding(8,12,8,12);silent.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("silent",on).apply());card(silent);
            card(txt("خيارات التذكير تُحفظ الآن؛ جدولة التنبيهات وتشغيل الصوت في الخلفية هي المرحلة البرمجية التالية.",13,muted,false));
        }else if(tab.equals("الأذكار")){
            card(txt("الأذكار اليومية",24,gold,true));
            card(txt("أذكار مختارة للتسبيح والاستغفار والصلاة على النبي ﷺ",14,muted,false));
            for(int i=0;i<adhkar.length;i++){final int ix=i;LinearLayout item=vertical();TextView phrase=txt(adhkar[i],21,white,true);TextView detail=txt(adhkarDetails[i],13,muted,false);Button count= new Button(this);count.setText("اضغط للتسبيح · "+prefs.getInt("dhikr_"+i,0));count.setOnClickListener(v->{int n=prefs.getInt("dhikr_"+ix,0)+1;prefs.edit().putInt("dhikr_"+ix,n).apply();count.setText("اضغط للتسبيح · "+n);});item.addView(phrase);item.addView(detail);item.addView(count);card(item);}
        }else if(tab.equals("القرآن")){
            card(txt("القرآن الكريم",24,gold,true));
            card(txt("المصحف الشريف",21,white,true));
            card(txt("هذه النسخة لم يُضمّن فيها ملف المصحف بعد. لن نعرض نصًا ناقصًا أو غير موثوق على أنه مصحف كامل. ستُضاف صفحات المصحف بالرسم العثماني مع القراءة دون إنترنت في مرحلة المحتوى.",15,muted,false));
        }else if(tab.equals("القبلة")){
            card(txt("اتجاه القبلة",24,gold,true));
            double[] c=coords[cityIndex()];double bearing=qibla(c[0],c[1]);
            card(txt(String.format(Locale.US,"اتجاه القبلة من %s\\n%.1f° من الشمال",cities[cityIndex()],bearing),20,white,true));
            compassView=new CompassView();
            LinearLayout compassBox=vertical();compassBox.setGravity(Gravity.CENTER);compassBox.addView(compassView,new LinearLayout.LayoutParams(280,280));card(compassBox);
            status=txt(compassAvailable?"حرّك الهاتف بشكل أفقي لمعايرة البوصلة":"هذا الهاتف لا يوفر مستشعر اتجاه مناسبًا",13,muted,false);card(status);
            card(txt("اتجه بحيث يشير السهم الذهبي إلى أعلى الشاشة؛ عندها يكون الهاتف باتجاه القبلة.",14,white,false));
            TextView change=txt("تغيير المدينة",15,gold,true);change.setOnClickListener(v->chooseCity());card(change);
        }else{
            card(txt("الإعدادات",24,gold,true));
            settingRow("المدينة الحالية",cities[cityIndex()],()->chooseCity());
            settingRow("طريقة الحساب","الهيئة المصرية العامة للمساحة",()->new AlertDialog.Builder(this).setTitle("طريقة الحساب").setMessage("المعتمدة حاليًا: الهيئة المصرية العامة للمساحة.").setPositiveButton("حسنًا",null).show());
            settingRow("صيغة الوقت","12 ساعة",()->new AlertDialog.Builder(this).setTitle("صيغة الوقت").setItems(new String[]{"12 ساعة","24 ساعة"},(d,w)->prefs.edit().putBoolean("24h",w==1).apply()).show());
            card(txt("نِداء · الإصدار 1.1\nيعمل حساب المواقيت دون اتصال بالإنترنت.",13,muted,false));
        }
    }
    private void updateCountdown(){
        if(countdownView==null||!tab.equals("الرئيسية"))return;
        int n=nextIndex();long diff=dates(times())[n].getTime()-System.currentTimeMillis();
        if(diff<0)diff=0;
        long h=diff/3600000,m=(diff/60000)%60,s=(diff/1000)%60;
        countdownView.setText(String.format(Locale.US,"%02d:%02d:%02d",h,m,s));
    }
    private void settingRow(String title,String value,Runnable action){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView t=txt(title+"\n"+value,15,white,true);t.setGravity(Gravity.RIGHT);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));TextView b=txt("تغيير",14,gold,true);b.setOnClickListener(v->action.run());r.addView(b);card(r);}
    private void chooseCity(){new AlertDialog.Builder(this).setTitle("اختر مدينتك").setSingleChoiceItems(cities,cityIndex(),(d,w)->{prefs.edit().putInt("city",w).apply();if(prefs.getBoolean("reminders",false))PrayerAlarmScheduler.schedule(this,w);d.dismiss();render();}).setNegativeButton("إلغاء",null).show();}

    @Override protected void onResume(){super.onResume();if(rotationSensor!=null)sensorManager.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_UI);clockHandler.removeCallbacks(clockTick);clockHandler.post(clockTick);}
    @Override protected void onPause(){super.onPause();clockHandler.removeCallbacks(clockTick);if(sensorManager!=null)sensorManager.unregisterListener(this);}
    @Override public void onSensorChanged(SensorEvent event){
        if(event.sensor.getType()!=Sensor.TYPE_ROTATION_VECTOR)return;
        float[] matrix=new float[9];float[] orientation=new float[3];
        SensorManager.getRotationMatrixFromVector(matrix,event.values);SensorManager.getOrientation(matrix,orientation);
        float degrees=(float)Math.toDegrees(orientation[0]);azimuth=(azimuth*0.82f)+(degrees*0.18f);
        if(compassView!=null){compassView.invalidate();if(status!=null)status.setText("اتجاه الهاتف: "+Math.round((azimuth+360)%360)+"° · حرّك الهاتف ببطء للمعايرة");}
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){if(status!=null&&accuracy==SensorManager.SENSOR_STATUS_UNRELIABLE)status.setText("دقة البوصلة منخفضة؛ أبعد الهاتف عن المعادن وأعد المعايرة.");}
    private class CompassView extends View{
        private final Paint paint=new Paint(3);
        CompassView(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-18;
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);paint.setColor(gold);canvas.drawCircle(cx,cy,r,paint);
            paint.setStrokeWidth(2);paint.setColor(muted);
            for(int i=0;i<36;i++){canvas.save();canvas.rotate(i*10,cx,cy);canvas.drawLine(cx,cy-r+5,cx,cy-r+(i%3==0?22:12),paint);canvas.restore();}
            paint.setStyle(Paint.Style.FILL);paint.setColor(white);paint.setTextSize(22);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("ش",cx,cy-r+45,paint);
            double[] loc=coords[cityIndex()];float relative=(float)qibla(loc[0],loc[1])-azimuth;
            canvas.save();canvas.rotate(relative,cx,cy);
            Path arrow=new Path();arrow.moveTo(cx,cy-r+55);arrow.lineTo(cx-17,cy+18);arrow.lineTo(cx,cy+5);arrow.lineTo(cx+17,cy+18);arrow.close();
            paint.setColor(gold);canvas.drawPath(arrow,paint);paint.setColor(white);canvas.drawCircle(cx,cy,8,paint);canvas.restore();
            paint.setColor(muted);paint.setTextSize(17);canvas.drawText("اتجاه القبلة",cx,cy+r-5,paint);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==71){if(grantResults.length>0&&grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){PrayerAlarmScheduler.schedule(this,cityIndex());Toast.makeText(this,"تم تفعيل تنبيهات الصلاة",Toast.LENGTH_SHORT).show();}else{prefs.edit().putBoolean("reminders",false).apply();Toast.makeText(this,"اسمح بالإشعارات لتفعيل التنبيهات",Toast.LENGTH_LONG).show();render();}}}
    private double qibla(double lat,double lon){double la=Math.toRadians(lat),lo=Math.toRadians(lon),ka=Math.toRadians(21.4225),ko=Math.toRadians(39.8262);double y=Math.sin(ko-lo);double x=Math.cos(la)*Math.tan(ka)-Math.sin(la)*Math.cos(ko-lo);return (Math.toDegrees(Math.atan2(y,x))+360)%360;}
}