package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class PrayerDashboardActivity extends Activity {
    private static final String[] PRAYERS={"الفجر","الظهر","العصر","المغرب","العشاء"};
    private static final String[] KEYS={"fajr","dhuhr","asr","maghrib","isha"};
    private static final String[] COUNTRIES={"مصر","السعودية","الإمارات","الكويت","قطر","الأردن","لبنان","فلسطين","المغرب","الجزائر","تونس","تركيا","باكستان","الهند","إندونيسيا","ماليزيا","المملكة المتحدة","الولايات المتحدة","كندا","ألمانيا","فرنسا","البحرين","عُمان","العراق","سوريا","اليمن","ليبيا","السودان","بنغلاديش","سنغافورة","اليابان","أستراليا","جنوب أفريقيا","نيجيريا","روسيا","إسبانيا","إيطاليا","هولندا","السويد","النرويج"};
    private static final String[] COUNTRY_CODES={"Egypt","Saudi Arabia","United Arab Emirates","Kuwait","Qatar","Jordan","Lebanon","Palestine","Morocco","Algeria","Tunisia","Turkey","Pakistan","India","Indonesia","Malaysia","United Kingdom","United States","Canada","Germany","France","Bahrain","Oman","Iraq","Syria","Yemen","Libya","Sudan","Bangladesh","Singapore","Japan","Australia","South Africa","Nigeria","Russia","Spain","Italy","Netherlands","Sweden","Norway"};
    private static final String[] METHODS={"5","4","8","9","10","23","3","3","21","19","18","13","1","1","20","17","3","2","2","3","12","8","8","3","3","3","3","3","1","11","3","3","3","3","14","3","3","3","3","3"};
    private static final String[] DEFAULT_CITIES={"Cairo","Riyadh","Dubai","Kuwait City","Doha","Amman","Beirut","Jerusalem","Rabat","Algiers","Tunis","Istanbul","Islamabad","New Delhi","Jakarta","Kuala Lumpur","London","New York","Toronto","Berlin","Paris","Manama","Muscat","Baghdad","Damascus","Sanaa","Tripoli","Khartoum","Dhaka","Singapore","Tokyo","Sydney","Johannesburg","Lagos","Moscow","Madrid","Rome","Amsterdam","Stockholm","Oslo"};
    private android.content.SharedPreferences prefs;
    private Spinner countrySpinner;
    private Button updateButton;
    private EditText cityInput;
    private String selectedCountryCode;
    private TextView result,nextCountdown,trackerCount,adhanNow;
    private LinearLayout prayerHero;
    private final TextView[] timeViews=new TextView[5];
    private final TextView[] remainingViews=new TextView[5];
    private TextView sunriseView;
    private final Handler handler=new Handler();
    private final Runnable ticker=new Runnable(){@Override public void run(){refreshCountdowns();handler.postDelayed(this,1000);}};
    private int green;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences("zad_reminders",MODE_PRIVATE);green=AppThemeColors.primary(this);AppThemeColors.apply(this);
        build();handler.post(ticker);
        String today=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());
        boolean missing=false;for(String prayer:PRAYERS)if(prefs.getInt("native_time_hour_"+prayer,-1)<0)missing=true;
        int currentOffset=TimeZone.getDefault().getOffset(System.currentTimeMillis())/60000;
        boolean timezoneOffsetChanged=currentOffset!=prefs.getInt("native_times_utc_offset",Integer.MIN_VALUE);
        if(missing||timezoneOffsetChanged||!today.equals(prefs.getString("native_times_date","")))updateButton.post(()->fetchTimes(updateButton));
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout.LayoutParams full(){return new LinearLayout.LayoutParams(-1,-2);}
    private void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=col();root.setPadding(dp(14),dp(14),dp(14),dp(28));root.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);setContentView(AppTabs.wrap(this,scroll,"prayer"));
        LinearLayout hero=col();prayerHero=hero;hero.setGravity(Gravity.CENTER);hero.setPadding(dp(16),dp(24),dp(16),dp(24));hero.setBackground(bg(green,18));
        TextView title=label("مواقيت الصلاة",25,Color.WHITE,true);title.setGravity(Gravity.CENTER);
        adhanNow=label("الصلاة نورٌ وطمأنينة",15,Color.WHITE,true);adhanNow.setGravity(Gravity.CENTER);
        nextCountdown=label("جارٍ حساب الصلاة القادمة...",14,Color.WHITE,false);nextCountdown.setGravity(Gravity.CENTER);
        hero.addView(title);hero.addView(adhanNow);hero.addView(nextCountdown);root.addView(hero,full());

        TextView locationTitle=label("الدولة والمدينة",18,AppThemeColors.text(this),true);locationTitle.setPadding(0,dp(16),0,dp(6));root.addView(locationTitle);
        countrySpinner=new Spinner(this);countrySpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,COUNTRIES));
        String savedCountry=prefs.getString("native_country","Egypt");int countryIndex=Arrays.asList(COUNTRY_CODES).indexOf(savedCountry);if(countryIndex<0)countryIndex=0;countrySpinner.setSelection(countryIndex);selectedCountryCode=COUNTRY_CODES[countryIndex];root.addView(countrySpinner,full());
        cityInput=new EditText(this);cityInput.setSingleLine(true);cityInput.setHint("اكتب اسم المدينة بالإنجليزية مثل Cairo أو Zagazig");cityInput.setText(prefs.getString("native_prayer_city",DEFAULT_CITIES[countryIndex]));cityInput.setTextDirection(View.TEXT_DIRECTION_LTR);root.addView(cityInput,full());
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(!COUNTRY_CODES[pos].equals(selectedCountryCode)){cityInput.setText(DEFAULT_CITIES[pos]);selectedCountryCode=COUNTRY_CODES[pos];}}});
        updateButton=new Button(this);updateButton.setText("تحديث مواقيت الصلاة");root.addView(updateButton,full());
        result=label("اختر الدولة والمدينة ثم حدّث المواقيت.",12,AppThemeColors.muted(this),false);root.addView(result,full());updateButton.setOnClickListener(v->fetchTimes(updateButton));

        TextView prayersTitle=label("الصلوات الخمس",19,AppThemeColors.text(this),true);prayersTitle.setPadding(0,dp(18),0,dp(8));root.addView(prayersTitle);

        TextView clockTitle=label("تنسيق عرض الوقت",14,AppThemeColors.text(this),true);
        clockTitle.setPadding(0,dp(4),0,dp(4));root.addView(clockTitle,full());
        RadioGroup clockFormat=new RadioGroup(this);
        clockFormat.setOrientation(RadioGroup.HORIZONTAL);
        clockFormat.setGravity(Gravity.CENTER);
        RadioButton format24=new RadioButton(this);format24.setText("24 ساعة");format24.setId(View.generateViewId());
        RadioButton format12=new RadioButton(this);format12.setText("12 ساعة");format12.setId(View.generateViewId());
        clockFormat.addView(format24);clockFormat.addView(format12);
        boolean use24=prefs.getBoolean("prayer_time_format_24",true);
        clockFormat.check(use24?format24.getId():format12.getId());
        clockFormat.setOnCheckedChangeListener((group,checkedId)->{
            prefs.edit().putBoolean("prayer_time_format_24",checkedId==format24.getId()).apply();
            refreshCountdowns();
        });
        root.addView(clockFormat,full());

        String[] icons={"🌅","☀️","🌤️","🌇","🌙"};
        for(int i=0;i<5;i++){
            LinearLayout card=col();card.setPadding(dp(14),dp(10),dp(14),dp(10));card.setBackground(bg(AppThemeColors.surface(this),15));
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setOrientation(LinearLayout.HORIZONTAL);
            TextView name=label(icons[i]+"  "+PRAYERS[i],18,green,true);name.setGravity(Gravity.CENTER);row.addView(name,new LinearLayout.LayoutParams(-1,dp(42)));card.addView(row);
            timeViews[i]=label(savedTime(PRAYERS[i]),20,green,true);timeViews[i].setGravity(Gravity.CENTER);card.addView(timeViews[i]);
            remainingViews[i]=label("المتبقي: —",12,AppThemeColors.muted(this),false);remainingViews[i].setGravity(Gravity.CENTER);card.addView(remainingViews[i]);
            LinearLayout.LayoutParams cp=full();cp.bottomMargin=dp(9);root.addView(card,cp);
        }
        LinearLayout sunriseCard=col();sunriseCard.setPadding(dp(14),dp(10),dp(14),dp(10));sunriseCard.setBackground(bg(AppThemeColors.surface(this),15));TextView sunriseName=label("🌄  الشروق — دون أذان",18,green,true);sunriseName.setGravity(Gravity.CENTER);sunriseCard.addView(sunriseName,new LinearLayout.LayoutParams(-1,dp(42)));sunriseView=label(savedSunrise(),20,green,true);sunriseView.setGravity(Gravity.CENTER);sunriseCard.addView(sunriseView);LinearLayout.LayoutParams sunriseLp=full();sunriseLp.bottomMargin=dp(9);root.addView(sunriseCard,sunriseLp);
        TextView trackerTitle=label("متتبع الصلوات اليومي",19,AppThemeColors.text(this),true);trackerTitle.setPadding(0,dp(16),0,dp(5));root.addView(trackerTitle);
        TextView trackerHint=label("علّم الصلاة بعد أدائها، وسيُحفظ إنجازك لهذا اليوم على الهاتف.",12,AppThemeColors.muted(this),false);root.addView(trackerHint);
        trackerCount=label("",14,green,true);trackerCount.setPadding(0,dp(7),0,dp(7));root.addView(trackerCount);
        for(String p:PRAYERS){
            CheckBox cb=new CheckBox(this);cb.setText(p+" — صليت");cb.setTextSize(16);cb.setTextColor(AppThemeColors.text(this));cb.setChecked(prefs.getBoolean(trackerKey(p),false));
            cb.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean(trackerKey(p),checked).apply();refreshTracker();});
            root.addView(cb,full());
        }
        Button reset=new Button(this);reset.setText("إعادة تعيين متتبع اليوم");root.addView(reset,full());reset.setOnClickListener(v->{for(String p:PRAYERS)prefs.edit().remove(trackerKey(p)).apply();recreate();});
        Button settings=new Button(this);settings.setText("إعدادات الأذان وأصوات المؤذنين");root.addView(settings,full());settings.setOnClickListener(v->finish());
        refreshTracker();refreshCountdowns();
    }
    private String trackerKey(String p){return "prayer_tracker_"+new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date())+"_"+p;}
    private void refreshTracker(){int done=0;for(String p:PRAYERS)if(prefs.getBoolean(trackerKey(p),false))done++;if(trackerCount!=null)trackerCount.setText("أنجزت "+done+" من 5 صلوات اليوم");}
    private String savedSunrise(){int h=prefs.getInt("native_sunrise_hour",-1),m=prefs.getInt("native_sunrise_minute",0);if(h<0)return "—";if(prefs.getBoolean("prayer_time_format_24",true))return String.format(Locale.getDefault(),"%02d:%02d",h,m);int dh=h%12;if(dh==0)dh=12;return String.format(Locale.getDefault(),"%d:%02d %s",dh,m,h<12?"ص":"م");}
    private String savedTime(String p){
        int h=prefs.getInt("native_time_hour_"+p,-1),m=prefs.getInt("native_time_minute_"+p,0);
        if(h<0)return "—";
        if(prefs.getBoolean("prayer_time_format_24",true))return String.format(Locale.getDefault(),"%02d:%02d",h,m);
        int displayHour=h%12;if(displayHour==0)displayHour=12;
        return String.format(Locale.getDefault(),"%d:%02d %s",displayHour,m,h<12?"ص":"م");
    }
    private long nextTime(String p){int h=prefs.getInt("native_time_hour_"+p,-1);if(h<0)return -1;Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,prefs.getInt("native_time_minute_"+p,0));c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);return c.getTimeInMillis();}
    private void refreshCountdowns(){
        if(sunriseView!=null)sunriseView.setText(savedSunrise());
        long now=System.currentTimeMillis(),soon=Long.MAX_VALUE;int next=-1;long[] times=new long[5];
        for(int i=0;i<5;i++){String p=PRAYERS[i];timeViews[i].setText(savedTime(p));times[i]=nextTime(p);if(times[i]>0&&times[i]<soon){soon=times[i];next=i;}}
        for(int i=0;i<5;i++){if(times[i]>0){long diff=Math.max(0,times[i]-now)/1000;remainingViews[i].setText("المتبقي: "+format(diff)+(i==next?"  • الصلاة القادمة":""));}else remainingViews[i].setText("حدّث المواقيت لعرض الوقت المتبقي");}
        int active=-1;
        for(int i=0;i<5;i++){
            int h=prefs.getInt("native_time_hour_"+PRAYERS[i],-1);
            if(h>=0){Calendar prayer=Calendar.getInstance();prayer.set(Calendar.HOUR_OF_DAY,h);prayer.set(Calendar.MINUTE,prefs.getInt("native_time_minute_"+PRAYERS[i],0));prayer.set(Calendar.SECOND,0);prayer.set(Calendar.MILLISECOND,0);long elapsed=now-prayer.getTimeInMillis();if(elapsed>=0&&elapsed<15*60*1000L)active=i;}
        }
        if(active>=0||prefs.getBoolean("adhan_active",false)){adhanNow.setText(active>=0?"🕌 حان الآن وقت صلاة "+PRAYERS[active]+" — تقبّل الله":"🕌 يرفع الأذان الآن — تقبّل الله");prayerHero.setBackgroundResource(R.drawable.azhar_mosque_bg);}
        else {if(next>=0)adhanNow.setText("الصلاة القادمة: "+PRAYERS[next]);prayerHero.setBackground(bg(green,18));}
        if(next>=0)nextCountdown.setText("متبقي على "+PRAYERS[next]+": "+format(Math.max(0,soon-now)/1000));
        else nextCountdown.setText("حدّث مواقيت الصلاة لبدء العد التنازلي");
    }
    private String format(long seconds){long h=seconds/3600,m=(seconds%3600)/60,s=seconds%60;return String.format(Locale.getDefault(),"%02d:%02d:%02d",h,m,s);}
    private void fetchTimes(Button button){
        int pos=countrySpinner.getSelectedItemPosition();String country=COUNTRY_CODES[pos],city=cityInput.getText().toString().trim();if(city.isEmpty()){result.setText("اكتب اسم المدينة أولاً.");return;}
        String method=METHODS[pos];prefs.edit().putString("native_country",country).putString("native_prayer_city",city).putString("native_prayer_method",method).apply();
        button.setEnabled(false);result.setText("جارٍ جلب المواقيت...");
        new Thread(()->{HttpURLConnection c=null;try{
            String address="https://api.aladhan.com/v1/timingsByCity?city="+URLEncoder.encode(city,"UTF-8")+"&country="+URLEncoder.encode(country,"UTF-8")+"&method="+method+(pos==0?"&school=0":"");
            c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","ZadAlmoslem Android");
            if(c.getResponseCode()!=200)throw new Exception("خدمة المواقيت لم تستجب");
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            JSONObject data=new JSONObject(out.toString("UTF-8")).getJSONObject("data");JSONObject timings=data.getJSONObject("timings");String[] api={"Fajr","Dhuhr","Asr","Maghrib","Isha"};String cityTimezone=data.getJSONObject("meta").optString("timezone",TimeZone.getDefault().getID());prefs.edit().putString("native_city_timezone",cityTimezone).apply();
            String sunriseRaw=timings.getString("Sunrise").split(" ")[0];String[] sunriseHm=sunriseRaw.split(":");prefs.edit().putInt("native_sunrise_hour",Integer.parseInt(sunriseHm[0])).putInt("native_sunrise_minute",Integer.parseInt(sunriseHm[1])).apply();
            for(int i=0;i<5;i++){String raw=timings.getString(api[i]).split(" ")[0];String[] hm=raw.split(":");int h=Integer.parseInt(hm[0]),m=Integer.parseInt(hm[1]);prefs.edit().putInt("native_time_hour_"+PRAYERS[i],h).putInt("native_time_minute_"+PRAYERS[i],m).apply();Calendar next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,h);next.set(Calendar.MINUTE,m);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);if(prefs.getBoolean("prayer_enabled_"+PRAYERS[i],true))ReminderReceiver.schedulePrayer(this,PRAYERS[i],next.getTimeInMillis());}
            prefs.edit().putString("native_times_date",new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date())).putInt("native_times_utc_offset",TimeZone.getDefault().getOffset(System.currentTimeMillis())/60000).apply();RamadanReminderReceiver.scheduleAll(this);
            runOnUiThread(()->{result.setText(pos==0?"تم تحديث المواقيت بطريقة الحساب المصرية رقم 5؛ وقد تختلف دقائق عن جدول الهيئة الرسمي المنشور.":"تم تحديث مواقيت "+COUNTRIES[pos]+" — "+city+" بطريقة الحساب المعتمدة لهذه الدولة.");refreshCountdowns();});
        }catch(Exception e){runOnUiThread(()->result.setText("تعذر تحديث المواقيت: "+(e.getMessage()==null?"تحقق من اسم المدينة والإنترنت":e.getMessage())));}
        finally{if(c!=null)c.disconnect();runOnUiThread(()->button.setEnabled(true));}}).start();
    }
    @Override protected void onDestroy(){handler.removeCallbacks(ticker);super.onDestroy();}
}