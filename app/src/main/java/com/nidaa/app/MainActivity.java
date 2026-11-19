package com.nidaa.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.data.DateComponents;
import com.batoulapps.adhan.PrayerTimes;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
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
    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("nidaa",MODE_PRIVATE);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);render();}
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
        String[] tabs={"الرئيسية","الأذان","القبلة","الإعدادات"};
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
            TextView top=txt(date+"\n"+cities[cityIndex()]+" · التوقيت المحلي",14,muted,true);card(top);
            LinearLayout hero=vertical();hero.setGravity(Gravity.CENTER);hero.addView(txt("الصلاة القادمة",15,muted,false));int n=nextIndex();hero.addView(txt(prayerNames[n],31,gold,true));hero.addView(txt(fmt(dates(times())[n]),25,white,true));hero.addView(txt("طريقة الحساب: الهيئة المصرية العامة للمساحة",12,muted,false));card(hero);
            TextView title=txt("مواقيت الصلاة اليوم",19,white,true);title.setGravity(Gravity.RIGHT);content.addView(title);
            PrayerTimes p=times();Date[] ds=dates(p);
            for(int i=0;i<6;i++){final int ix=i;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView name=txt(prayerNames[i],16,white,true);name.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);TextView time=txt(fmt(ds[i]),16,gold,true);time.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));row.addView(time);card(row);}
            TextView city=txt("تغيير المدينة",15,gold,true);city.setOnClickListener(v->chooseCity());card(city);
            card(txt("المواقيت محسوبة داخل التطبيق ولا تحتاج إلى إنترنت.",13,muted,false));
        }else if(tab.equals("الأذان")){
            card(txt("إعدادات المؤذّن",23,gold,true));
            card(txt("اختر الصوت المفضل لكل صلاة",15,white,true));
            for(int i:new int[]{0,2,3,4,5}){
                LinearLayout box=vertical();TextView label=txt(prayerNames[i],15,white,true);label.setGravity(Gravity.RIGHT);
                Spinner sp=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,muadhins);sp.setAdapter(a);
                sp.setSelection(prefs.getInt("mu_"+prayers[i],0));final int ix=i;sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){prefs.edit().putInt("mu_"+prayers[ix],pos).apply();}});
                box.addView(label);box.addView(sp);card(box);
            }
            card(txt("تنبيه مهم: أسماء المؤذنين هنا لا تعني أن التسجيلات الصوتية مرفقة بعد. سنضيف ملفات الأذان المرخصة أو التي توفرها أنت، ثم نفعّل التشغيل دون إنترنت.",13,muted,false));
            Switch reminder=new Switch(this);reminder.setText("تفعيل تذكيرات الصلاة");reminder.setTextColor(white);reminder.setChecked(prefs.getBoolean("reminders",false));reminder.setPadding(8,12,8,12);reminder.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("reminders",on).apply());card(reminder);
            Switch silent=new Switch(this);silent.setText("طلب تفعيل الصامت قبل الصلاة");silent.setTextColor(white);silent.setChecked(prefs.getBoolean("silent",false));silent.setPadding(8,12,8,12);silent.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("silent",on).apply());card(silent);
            card(txt("خيارات التذكير تُحفظ الآن؛ جدولة التنبيهات وتشغيل الصوت في الخلفية هي المرحلة البرمجية التالية.",13,muted,false));
        }else if(tab.equals("القبلة")){
            card(txt("اتجاه القبلة",24,gold,true));
            double[] c=coords[cityIndex()];double bearing=qibla(c[0],c[1]);
            card(txt(String.format(Locale.US,"اتجاه القبلة من %s\n%.1f° من الشمال",cities[cityIndex()],bearing),22,white,true));
            card(txt("هذه زاوية الاتجاه الجغرافي المحسوبة من موقع المدينة المختارة إلى الكعبة. عرض البوصلة الحية يحتاج معايرة مستشعر الهاتف وسيضاف في التحديث التالي.",14,muted,false));
            TextView change=txt("تغيير المدينة",15,gold,true);change.setOnClickListener(v->chooseCity());card(change);
        }else{
            card(txt("الإعدادات",24,gold,true));
            settingRow("المدينة الحالية",cities[cityIndex()],()->chooseCity());
            settingRow("طريقة الحساب","الهيئة المصرية العامة للمساحة",()->new AlertDialog.Builder(this).setTitle("طريقة الحساب").setMessage("المعتمدة حاليًا: الهيئة المصرية العامة للمساحة.").setPositiveButton("حسنًا",null).show());
            settingRow("صيغة الوقت","12 ساعة",()->new AlertDialog.Builder(this).setTitle("صيغة الوقت").setItems(new String[]{"12 ساعة","24 ساعة"},(d,w)->prefs.edit().putBoolean("24h",w==1).apply()).show());
            card(txt("نِداء · الإصدار 1.1\nيعمل حساب المواقيت دون اتصال بالإنترنت.",13,muted,false));
        }
    }
    private void settingRow(String title,String value,Runnable action){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView t=txt(title+"\n"+value,15,white,true);t.setGravity(Gravity.RIGHT);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));TextView b=txt("تغيير",14,gold,true);b.setOnClickListener(v->action.run());r.addView(b);card(r);}
    private void chooseCity(){new AlertDialog.Builder(this).setTitle("اختر مدينتك").setSingleChoiceItems(cities,cityIndex(),(d,w)->{prefs.edit().putInt("city",w).apply();d.dismiss();render();}).setNegativeButton("إلغاء",null).show();}
    private double qibla(double lat,double lon){double la=Math.toRadians(lat),lo=Math.toRadians(lon),ka=Math.toRadians(21.4225),ko=Math.toRadians(39.8262);double y=Math.sin(ko-lo);double x=Math.cos(la)*Math.tan(ka)-Math.sin(la)*Math.cos(ko-lo);return (Math.toDegrees(Math.atan2(y,x))+360)%360;}
}