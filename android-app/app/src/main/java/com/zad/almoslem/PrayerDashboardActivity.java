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
    private static final String[] COUNTRIES;
    private static final String[] COUNTRY_CODES;
    private static final String[] METHODS;
    private static final String[] SCHOOLS;
    private static final String[] DEFAULT_CITIES;
    static {
        // Build the selector from the complete ISO country list, not a fixed shortlist.
        Locale arabic = new Locale("ar");
        Locale english = Locale.ENGLISH;
        TreeMap<String, String> sorted = new TreeMap<>();
        for (String iso : Locale.getISOCountries()) {
            Locale countryLocale = new Locale("", iso);
            String arabicName = countryLocale.getDisplayCountry(arabic);
            if (arabicName == null || arabicName.trim().isEmpty()) arabicName = countryLocale.getDisplayCountry(english);
            String apiName = countryLocale.getDisplayCountry(english);
            if ("PS".equals(iso)) apiName = "Palestine";
            sorted.put(arabicName, apiName);
        }
        // Keep familiar Arabic country labels and API names for the countries already offered.
        String[] preferredIso = {"EG","SA","AE","KW","QA","JO","LB","PS","MA","DZ","TN","TR","PK","IN","ID","MY","GB","US","CA","DE","FR","BH","OM","IQ","SY","YE","LY","SD","BD","SG","JP","AU","ZA","NG","RU","ES","IT","NL","SE","NO"};
        String[] preferredArabic = {"مصر","السعودية","الإمارات","الكويت","قطر","الأردن","لبنان","فلسطين","المغرب","الجزائر","تونس","تركيا","باكستان","الهند","إندونيسيا","ماليزيا","المملكة المتحدة","الولايات المتحدة","كندا","ألمانيا","فرنسا","البحرين","عُمان","العراق","سوريا","اليمن","ليبيا","السودان","بنغلاديش","سنغافورة","اليابان","أستراليا","جنوب أفريقيا","نيجيريا","روسيا","إسبانيا","إيطاليا","هولندا","السويد","النرويج"};
        String[] preferredApi = {"Egypt","Saudi Arabia","United Arab Emirates","Kuwait","Qatar","Jordan","Lebanon","Palestine","Morocco","Algeria","Tunisia","Turkey","Pakistan","India","Indonesia","Malaysia","United Kingdom","United States","Canada","Germany","France","Bahrain","Oman","Iraq","Syria","Yemen","Libya","Sudan","Bangladesh","Singapore","Japan","Australia","South Africa","Nigeria","Russia","Spain","Italy","Netherlands","Sweden","Norway"};
        for (int i=0;i<preferredIso.length;i++) {
            String localized = new Locale("", preferredIso[i]).getDisplayCountry(arabic);
            sorted.remove(localized);
            sorted.put(preferredArabic[i],preferredApi[i]);
        }
        COUNTRIES = sorted.keySet().toArray(new String[0]);
        COUNTRY_CODES = sorted.values().toArray(new String[0]);
        METHODS = new String[COUNTRY_CODES.length];
        SCHOOLS = new String[COUNTRY_CODES.length];
        DEFAULT_CITIES = new String[COUNTRY_CODES.length];
        Arrays.fill(METHODS,"3"); // Muslim World League default for countries without a country-specific preset.
        Arrays.fill(SCHOOLS,"0");
        Map<String,String> methodByIso = new HashMap<>();
        String[] isoMethod={"EG:5","SA:4","AE:8","KW:9","QA:10","JO:23","MA:21","DZ:19","TN:18","TR:13","PK:1","IN:1","ID:20","MY:17","US:2","CA:2","FR:12","BH:8","OM:8","BD:1","SG:11","RU:14"};
        for(String item:isoMethod){String[] pair=item.split(":");methodByIso.put(pair[0],pair[1]);}
        Set<String> hanafi = new HashSet<>(Arrays.asList("PK","IN","BD"));
        Map<String,String> cities = new HashMap<>();
        String[] cityPairs={"EG:Cairo","SA:Riyadh","AE:Dubai","KW:Kuwait City","QA:Doha","JO:Amman","LB:Beirut","PS:Jerusalem","MA:Rabat","DZ:Algiers","TN:Tunis","TR:Istanbul","PK:Islamabad","IN:New Delhi","ID:Jakarta","MY:Kuala Lumpur","GB:London","US:New York","CA:Toronto","DE:Berlin","FR:Paris","BH:Manama","OM:Muscat","IQ:Baghdad","SY:Damascus","YE:Sanaa","LY:Tripoli","SD:Khartoum","BD:Dhaka","SG:Singapore","JP:Tokyo","AU:Sydney","ZA:Johannesburg","NG:Lagos","RU:Moscow","ES:Madrid","IT:Rome","NL:Amsterdam","SE:Stockholm","NO:Oslo"};
        Map<String,String> isoCities=new HashMap<>();
        for(String item:cityPairs){String[] pair=item.split(":",2);isoCities.put(pair[0],pair[1]);}
        for(int i=0;i<COUNTRY_CODES.length;i++){
            String api=COUNTRY_CODES[i], iso="";
            for(String code:Locale.getISOCountries()) if(new Locale("",code).getDisplayCountry(english).equals(api) || ("PS".equals(code)&&"Palestine".equals(api))){iso=code;break;}
            METHODS[i]=methodByIso.getOrDefault(iso,"3");
            if(hanafi.contains(iso)) SCHOOLS[i]="1";
            DEFAULT_CITIES[i]=isoCities.getOrDefault(iso,"");
        }
    }
    private static final String[] EGYPT_GOVERNORATES={"القاهرة","الجيزة","الإسكندرية","القليوبية","الشرقية","الدقهلية","البحيرة","الغربية","المنوفية","كفر الشيخ","دمياط","بورسعيد","الإسماعيلية","السويس","شمال سيناء","جنوب سيناء","بني سويف","الفيوم","المنيا","أسيوط","سوهاج","قنا","الأقصر","أسوان","البحر الأحمر","الوادي الجديد","مطروح"};
    private static final String[] EGYPT_CITIES={"Cairo","Giza","Alexandria","Banha","Zagazig","Mansoura","Damanhur","Tanta","Shebin El-Kom","Kafr El Sheikh","Damietta","Port Said","Ismailia","Suez","Arish","El Tor","Beni Suef","Fayoum","Minya","Asyut","Sohag","Qena","Luxor","Aswan","Hurghada","Kharga","Marsa Matruh"};
    private android.content.SharedPreferences prefs;
    private Spinner countrySpinner, egyptGovernorateSpinner;
    private Button updateButton;
    private EditText cityInput;
    private String selectedCountryCode;
    private TextView result,nextCountdown,trackerCount,adhanNow;
    private final CheckBox[] trackerChecks=new CheckBox[5];
    private LinearLayout prayerHero;
    private final TextView[] timeViews=new TextView[5];
    private final TextView[] remainingViews=new TextView[5];
    private TextView sunriseView;
    private final Handler handler=new Handler();
    private final Runnable ticker=new Runnable(){@Override public void run(){refreshCountdowns();handler.postDelayed(this,1000);}};
    private int green;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences("zad_reminders",MODE_PRIVATE);green=AppThemeColors.primary(this);AppThemeColors.apply(this);
        build();handler.post(ticker);NextPrayerWidget.refreshAll(this);if(prefs.getBoolean("sunrise_alarm_enabled",false))SunriseAlarmScheduler.scheduleNext(this);
        String today=todayInCityZone();
        boolean missing=false;for(String prayer:PRAYERS)if(prefs.getInt("native_time_hour_"+prayer,-1)<0)missing=true;
        int currentOffset=cityTimeZone().getOffset(System.currentTimeMillis())/60000;
        boolean timezoneOffsetChanged=currentOffset!=prefs.getInt("native_times_utc_offset",Integer.MIN_VALUE);
        if(missing||timezoneOffsetChanged||!today.equals(prefs.getString("native_times_date","")))updateButton.post(()->fetchTimes(updateButton));
    }
    private TimeZone cityTimeZone(){String id=prefs.getString("native_city_timezone","");if(id.isEmpty()&&"Egypt".equals(prefs.getString("native_country","Egypt")))id="Africa/Cairo";if(id.isEmpty())id=TimeZone.getDefault().getID();return TimeZone.getTimeZone(id);}
    private String todayInCityZone(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd",Locale.US);f.setTimeZone(cityTimeZone());return f.format(new Date());}
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
        egyptGovernorateSpinner=new Spinner(this);
        ArrayAdapter<String> governorateAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,EGYPT_GOVERNORATES);
        governorateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        egyptGovernorateSpinner.setAdapter(governorateAdapter);
        root.addView(egyptGovernorateSpinner,full());
        cityInput=new EditText(this);cityInput.setSingleLine(true);cityInput.setHint("اكتب اسم المدينة بالإنجليزية مثل Riyadh أو Dubai");cityInput.setText(prefs.getString("native_prayer_city",DEFAULT_CITIES[countryIndex]));cityInput.setTextDirection(View.TEXT_DIRECTION_LTR);root.addView(cityInput,full());
        boolean isEgypt="Egypt".equals(selectedCountryCode);
        egyptGovernorateSpinner.setVisibility(isEgypt?View.VISIBLE:View.GONE);
        cityInput.setVisibility(isEgypt?View.GONE:View.VISIBLE);
        String savedPrayerCity=prefs.getString("native_prayer_city",DEFAULT_CITIES[countryIndex]);
        if(isEgypt){int governorateIndex=0;for(int i=0;i<EGYPT_CITIES.length;i++)if(EGYPT_CITIES[i].equalsIgnoreCase(savedPrayerCity)){governorateIndex=i;break;}egyptGovernorateSpinner.setSelection(governorateIndex);}
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
            if(!COUNTRY_CODES[pos].equals(selectedCountryCode)){
                selectedCountryCode=COUNTRY_CODES[pos];
                boolean egypt="Egypt".equals(selectedCountryCode);
                egyptGovernorateSpinner.setVisibility(egypt?View.VISIBLE:View.GONE);
                cityInput.setVisibility(egypt?View.GONE:View.VISIBLE);
                if(egypt)egyptGovernorateSpinner.setSelection(0);else cityInput.setText(DEFAULT_CITIES[pos]);
            }
        }});
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

        TextView widgetTitle=label("إعدادات ويدج الصلاة على الشاشة",17,AppThemeColors.text(this),true);
        widgetTitle.setPadding(0,dp(16),0,dp(5));root.addView(widgetTitle,full());
        TextView widgetHint=label("تحكم في المعلومات التي تظهر في الويدج، واضغط على الويدج لفتح مواقيت الصلاة.",12,AppThemeColors.muted(this),false);
        root.addView(widgetHint,full());
        Switch showWidgetCity=new Switch(this);showWidgetCity.setText("إظهار اسم المدينة في الويدج");
        showWidgetCity.setChecked(prefs.getBoolean("widget_show_city",true));root.addView(showWidgetCity,full());
        showWidgetCity.setOnCheckedChangeListener((button,checked)->{
            prefs.edit().putBoolean("widget_show_city",checked).apply();NextPrayerWidget.refreshAll(this);
        });
        Switch showWidgetTime=new Switch(this);showWidgetTime.setText("إظهار موعد الصلاة القادمة");
        showWidgetTime.setChecked(prefs.getBoolean("widget_show_prayer_time",true));root.addView(showWidgetTime,full());
        showWidgetTime.setOnCheckedChangeListener((button,checked)->{
            prefs.edit().putBoolean("widget_show_prayer_time",checked).apply();NextPrayerWidget.refreshAll(this);
        });
        Button refreshWidget=new Button(this);refreshWidget.setText("تحديث الويدج الآن");root.addView(refreshWidget,full());
        refreshWidget.setOnClickListener(v->{NextPrayerWidget.refreshAll(this);Toast.makeText(this,"تم تحديث بيانات الويدج",Toast.LENGTH_SHORT).show();});
        Button addWidget=new Button(this);addWidget.setText("إضافة ويدج الصلاة إلى الشاشة الرئيسية");root.addView(addWidget,full());
        addWidget.setOnClickListener(v->{
            if(android.os.Build.VERSION.SDK_INT>=26){
                android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(this);
                if(manager.isRequestPinAppWidgetSupported()){
                    manager.requestPinAppWidget(new android.content.ComponentName(this,NextPrayerWidget.class),null,null);
                }else Toast.makeText(this,"افتح قائمة الويدج من الشاشة الرئيسية وأضف ويدج الصلاة يدويًا.",Toast.LENGTH_LONG).show();
            }else Toast.makeText(this,"اضغط مطولًا على الشاشة الرئيسية ثم اختر الويدج وأضف الصلاة القادمة.",Toast.LENGTH_LONG).show();
        });

        TextView fajrAlarmTitle=label("منبه الفجر",19,AppThemeColors.text(this),true);
        fajrAlarmTitle.setPadding(0,dp(18),0,dp(5));root.addView(fajrAlarmTitle,full());
        TextView fajrAlarmHint=label("يرن المنبه على موعد الفجر المسجل لمدينتك، ولا يتوقف إلا بعد الإجابة الصحيحة عن سؤال بسيط. ويمكنك طلب غفوة 5 دقائق.",12,AppThemeColors.muted(this),false);
        root.addView(fajrAlarmHint,full());
        Switch fajrAlarmEnabled=new Switch(this);fajrAlarmEnabled.setText("تفعيل منبه الفجر");
        fajrAlarmEnabled.setChecked(prefs.getBoolean("fajr_alarm_enabled",false));root.addView(fajrAlarmEnabled,full());
        TextView fajrAlarmStatus=label("",12,AppThemeColors.muted(this),false);root.addView(fajrAlarmStatus,full());
        Runnable updateFajrAlarmStatus=()->{
            int fh=prefs.getInt("native_time_hour_الفجر",-1),fm=prefs.getInt("native_time_minute_الفجر",0);
            if(fh<0)fajrAlarmStatus.setText("حدّث مواقيت الصلاة أولًا حتى نضبط المنبه على موعد الفجر.");
            else fajrAlarmStatus.setText("موعد المنبه حسب مواقيت مدينتك: "+String.format(Locale.getDefault(),"%02d:%02d",fh,fm));
        };
        updateFajrAlarmStatus.run();
        fajrAlarmEnabled.setOnCheckedChangeListener((button,enabled)->{
            prefs.edit().putBoolean("fajr_alarm_enabled",enabled).apply();
            if(enabled){
                if(prefs.getInt("native_time_hour_الفجر",-1)<0){
                    fajrAlarmEnabled.setOnCheckedChangeListener(null);fajrAlarmEnabled.setChecked(false);
                    prefs.edit().putBoolean("fajr_alarm_enabled",false).apply();
                    fajrAlarmEnabled.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("fajr_alarm_enabled",on).apply();if(on){if(!FajrAlarmScheduler.scheduleNext(this)){Toast.makeText(this,"تعذر ضبط المنبه؛ حدّث مواقيت الصلاة وتأكد من إذن المنبهات الدقيقة.",Toast.LENGTH_LONG).show();b.setChecked(false);prefs.edit().putBoolean("fajr_alarm_enabled",false).apply();}}else FajrAlarmScheduler.cancel(this);});
                    Toast.makeText(this,"حدّث مواقيت الصلاة أولًا",Toast.LENGTH_LONG).show();return;
                }
                if(!FajrAlarmScheduler.scheduleNext(this)){
                    Toast.makeText(this,"تعذر ضبط المنبه. اسمح للتطبيق بالمنبهات الدقيقة من إعدادات الهاتف.",Toast.LENGTH_LONG).show();
                    prefs.edit().putBoolean("fajr_alarm_enabled",false).apply();button.setChecked(false);
                }else Toast.makeText(this,"تم تفعيل منبه الفجر",Toast.LENGTH_SHORT).show();
            }else{FajrAlarmScheduler.cancel(this);Toast.makeText(this,"تم إيقاف منبه الفجر",Toast.LENGTH_SHORT).show();}
            updateFajrAlarmStatus.run();
        });

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

        TextView sunriseAlarmTitle=label("تنبيه الشروق",19,AppThemeColors.text(this),true);
        sunriseAlarmTitle.setPadding(0,dp(12),0,dp(4));root.addView(sunriseAlarmTitle,full());
        TextView sunriseAlarmHint=label("تنبيه هادئ وقت الشروق بصوت تغريد العصافير، وليس أذانًا. يُضبط تلقائيًا حسب مواقيت مدينتك.",12,AppThemeColors.muted(this),false);
        root.addView(sunriseAlarmHint,full());
        Switch sunriseAlertSwitch=new Switch(this);sunriseAlertSwitch.setText("تشغيل صوت العصافير عند الشروق");
        sunriseAlertSwitch.setChecked(prefs.getBoolean("sunrise_alarm_enabled",false));root.addView(sunriseAlertSwitch,full());
        TextView sunriseAlarmStatus=label("",12,AppThemeColors.muted(this),false);root.addView(sunriseAlarmStatus,full());
        Runnable updateSunriseStatus=()->{
            int sh=prefs.getInt("native_sunrise_hour",-1),sm=prefs.getInt("native_sunrise_minute",0);
            if(sh<0)sunriseAlarmStatus.setText("حدّث مواقيت الصلاة أولًا لضبط تنبيه الشروق.");
            else sunriseAlarmStatus.setText("موعد التنبيه حسب مدينتك: "+String.format(Locale.getDefault(),"%02d:%02d",sh,sm));
        };
        updateSunriseStatus.run();
        sunriseAlertSwitch.setOnCheckedChangeListener((button,enabled)->{
            prefs.edit().putBoolean("sunrise_alarm_enabled",enabled).apply();
            if(enabled){
                if(prefs.getInt("native_sunrise_hour",-1)<0||!SunriseAlarmScheduler.scheduleNext(this)){
                    prefs.edit().putBoolean("sunrise_alarm_enabled",false).apply();
                    button.setOnCheckedChangeListener(null);button.setChecked(false);
                    button.setOnCheckedChangeListener((b,on)->{
                        prefs.edit().putBoolean("sunrise_alarm_enabled",on).apply();
                        if(on){
                            if(prefs.getInt("native_sunrise_hour",-1)<0||!SunriseAlarmScheduler.scheduleNext(this)){
                                prefs.edit().putBoolean("sunrise_alarm_enabled",false).apply();b.setChecked(false);
                                Toast.makeText(this,"حدّث مواقيت الصلاة وتأكد من إذن المنبهات الدقيقة.",Toast.LENGTH_LONG).show();
                            }
                        }else SunriseAlarmScheduler.cancel(this);
                    });
                    Toast.makeText(this,"حدّث مواقيت الصلاة أولًا وتأكد من إذن المنبهات الدقيقة.",Toast.LENGTH_LONG).show();
                }else Toast.makeText(this,"تم تفعيل تنبيه الشروق بصوت العصافير 🌿",Toast.LENGTH_SHORT).show();
            }else{SunriseAlarmScheduler.cancel(this);Toast.makeText(this,"تم إيقاف تنبيه الشروق",Toast.LENGTH_SHORT).show();}
            updateSunriseStatus.run();
        });
        TextView trackerTitle=label("متتبع الصلوات اليومي",19,AppThemeColors.text(this),true);trackerTitle.setPadding(0,dp(16),0,dp(5));root.addView(trackerTitle);
        TextView trackerHint=label("علّم الصلاة بعد أدائها، وسيُحفظ إنجازك لهذا اليوم على الهاتف.",12,AppThemeColors.muted(this),false);root.addView(trackerHint);
        trackerCount=label("",14,green,true);trackerCount.setPadding(0,dp(7),0,dp(7));root.addView(trackerCount);
        for(int i=0;i<PRAYERS.length;i++){
            final String p=PRAYERS[i];
            CheckBox cb=new CheckBox(this);trackerChecks[i]=cb;cb.setText(p+" — صليت");cb.setTextSize(16);cb.setTextColor(AppThemeColors.text(this));cb.setChecked(prefs.getBoolean(trackerKey(p),false));
            bindTrackerCheckbox(i);
            root.addView(cb,full());
        }
        Button reset=new Button(this);reset.setText("إعادة تعيين متتبع اليوم");root.addView(reset,full());reset.setOnClickListener(v->{for(String p:PRAYERS)prefs.edit().remove(trackerKey(p)).apply();recreate();});
        Button settings=new Button(this);settings.setText("إعدادات الأذان وأصوات المؤذنين");root.addView(settings,full());settings.setOnClickListener(v->finish());
        refreshTracker();refreshCountdowns();
    }
    private String trackerKey(String p){return "prayer_tracker_"+todayInCityZone()+"_"+p;}
    private void bindTrackerCheckbox(int index){
        CheckBox cb=trackerChecks[index];if(cb==null)return;final String prayer=PRAYERS[index];
        cb.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean(trackerKey(prayer),checked).apply();refreshTrackerCount();});
    }
    private void refreshTrackerCount(){
        int done=0;for(String prayer:PRAYERS)if(prefs.getBoolean(trackerKey(prayer),false))done++;
        if(trackerCount!=null)trackerCount.setText("أنجزت "+done+" من 5 صلوات اليوم");
    }
    private void refreshTracker(){
        int done=0;
        for(int i=0;i<PRAYERS.length;i++){
            boolean checked=prefs.getBoolean(trackerKey(PRAYERS[i]),false);
            if(checked)done++;
            CheckBox cb=trackerChecks[i];
            if(cb!=null&&cb.isChecked()!=checked){cb.setOnCheckedChangeListener(null);cb.setChecked(checked);bindTrackerCheckbox(i);}
        }
        if(trackerCount!=null)trackerCount.setText("أنجزت "+done+" من 5 صلوات اليوم");
    }
    @Override protected void onResume(){super.onResume();if(prefs!=null)refreshTracker();}
    private String savedSunrise(){int h=prefs.getInt("native_sunrise_hour",-1),m=prefs.getInt("native_sunrise_minute",0);if(h<0)return "—";if(prefs.getBoolean("prayer_time_format_24",true))return String.format(Locale.getDefault(),"%02d:%02d",h,m);int dh=h%12;if(dh==0)dh=12;return String.format(Locale.getDefault(),"%d:%02d %s",dh,m,h<12?"ص":"م");}
    private String savedTime(String p){
        int h=prefs.getInt("native_time_hour_"+p,-1),m=prefs.getInt("native_time_minute_"+p,0);
        if(h<0)return "—";
        if(prefs.getBoolean("prayer_time_format_24",true))return String.format(Locale.getDefault(),"%02d:%02d",h,m);
        int displayHour=h%12;if(displayHour==0)displayHour=12;
        return String.format(Locale.getDefault(),"%d:%02d %s",displayHour,m,h<12?"ص":"م");
    }
    private long nextTime(String p){int h=prefs.getInt("native_time_hour_"+p,-1);if(h<0)return -1;Calendar c=Calendar.getInstance(cityTimeZone());c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,prefs.getInt("native_time_minute_"+p,0));c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);return c.getTimeInMillis();}
    private void refreshCountdowns(){
        if(sunriseView!=null)sunriseView.setText(savedSunrise());
        long now=System.currentTimeMillis(),soon=Long.MAX_VALUE;int next=-1;long[] times=new long[5];
        for(int i=0;i<5;i++){String p=PRAYERS[i];timeViews[i].setText(savedTime(p));times[i]=nextTime(p);if(times[i]>0&&times[i]<soon){soon=times[i];next=i;}}
        for(int i=0;i<5;i++){if(times[i]>0){long diff=Math.max(0,times[i]-now)/1000;remainingViews[i].setText("المتبقي: "+format(diff)+(i==next?"  • الصلاة القادمة":""));}else remainingViews[i].setText("حدّث المواقيت لعرض الوقت المتبقي");}
        int active=-1;
        for(int i=0;i<5;i++){
            int h=prefs.getInt("native_time_hour_"+PRAYERS[i],-1);
            if(h>=0){Calendar prayer=Calendar.getInstance(cityTimeZone());prayer.set(Calendar.HOUR_OF_DAY,h);prayer.set(Calendar.MINUTE,prefs.getInt("native_time_minute_"+PRAYERS[i],0));prayer.set(Calendar.SECOND,0);prayer.set(Calendar.MILLISECOND,0);long elapsed=now-prayer.getTimeInMillis();if(elapsed>=0&&elapsed<15*60*1000L)active=i;}
        }
        if(active>=0||prefs.getBoolean("adhan_active",false)){adhanNow.setText(active>=0?"🕌 حان الآن وقت صلاة "+PRAYERS[active]+" — تقبّل الله":"🕌 يرفع الأذان الآن — تقبّل الله");prayerHero.setBackgroundResource(R.drawable.azhar_mosque_bg);}
        else {if(next>=0)adhanNow.setText("الصلاة القادمة: "+PRAYERS[next]);prayerHero.setBackground(bg(green,18));}
        if(next>=0)nextCountdown.setText("متبقي على "+PRAYERS[next]+": "+format(Math.max(0,soon-now)/1000));
        else nextCountdown.setText("حدّث مواقيت الصلاة لبدء العد التنازلي");
    }
    private String format(long seconds){long h=seconds/3600,m=(seconds%3600)/60,s=seconds%60;return String.format(Locale.getDefault(),"%02d:%02d:%02d",h,m,s);}
    private void fetchTimes(Button button){
        int pos=countrySpinner.getSelectedItemPosition();String country=COUNTRY_CODES[pos];
        String city="Egypt".equals(country)?EGYPT_CITIES[Math.max(0,Math.min(egyptGovernorateSpinner.getSelectedItemPosition(),EGYPT_CITIES.length-1))]:cityInput.getText().toString().trim();
        if(city.isEmpty()){result.setText("اكتب اسم المدينة أولاً.");return;}
        String method=METHODS[pos];prefs.edit().putString("native_country",country).putString("native_prayer_city",city).putString("native_prayer_method",method).apply();
        button.setEnabled(false);result.setText("جارٍ جلب المواقيت...");
        new Thread(()->{HttpURLConnection c=null;try{
            String address="https://api.aladhan.com/v1/timingsByCity?city="+URLEncoder.encode(city,"UTF-8")+"&country="+URLEncoder.encode(country,"UTF-8")+"&method="+method+"&school="+SCHOOLS[pos];
            c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","ZadAlmoslem Android");
            if(c.getResponseCode()!=200)throw new Exception("خدمة المواقيت لم تستجب");
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            JSONObject data=new JSONObject(out.toString("UTF-8")).getJSONObject("data");JSONObject timings=data.getJSONObject("timings");String[] api={"Fajr","Dhuhr","Asr","Maghrib","Isha"};String cityTimezone=data.getJSONObject("meta").optString("timezone",TimeZone.getDefault().getID());prefs.edit().putString("native_city_timezone",cityTimezone).apply();
            String sunriseRaw=timings.getString("Sunrise").split(" ")[0];String[] sunriseHm=sunriseRaw.split(":");prefs.edit().putInt("native_sunrise_hour",Integer.parseInt(sunriseHm[0])).putInt("native_sunrise_minute",Integer.parseInt(sunriseHm[1])).apply();if(prefs.getBoolean("sunrise_alarm_enabled",false))SunriseAlarmScheduler.scheduleNext(this);
            for(int i=0;i<5;i++){String raw=timings.getString(api[i]).split(" ")[0];String[] hm=raw.split(":");int h=Integer.parseInt(hm[0]),m=Integer.parseInt(hm[1]);prefs.edit().putInt("native_time_hour_"+PRAYERS[i],h).putInt("native_time_minute_"+PRAYERS[i],m).apply();Calendar next=Calendar.getInstance(TimeZone.getTimeZone(cityTimezone));next.set(Calendar.HOUR_OF_DAY,h);next.set(Calendar.MINUTE,m);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);if(prefs.getBoolean("prayer_enabled_"+PRAYERS[i],true))ReminderReceiver.schedulePrayer(this,PRAYERS[i],next.getTimeInMillis());}
            prefs.edit().putString("native_times_date",todayInCityZone()).putInt("native_times_utc_offset",TimeZone.getTimeZone(cityTimezone).getOffset(System.currentTimeMillis())/60000).apply();NextPrayerWidget.refreshAll(this);RamadanReminderReceiver.scheduleAll(this);if(prefs.getBoolean("fajr_alarm_enabled",false))FajrAlarmScheduler.scheduleNext(this);
            runOnUiThread(()->{result.setText("تم تحديث مواقيت "+COUNTRIES[pos]+" — "+city+" بطريقة الحساب رقم "+method+"؛ وقد تختلف الدقائق حسب الجهة المحلية المعتمدة.");refreshTracker();refreshCountdowns();});
        }catch(Exception e){runOnUiThread(()->result.setText("تعذر تحديث المواقيت: "+(e.getMessage()==null?"تحقق من اسم المدينة والإنترنت":e.getMessage())));}
        finally{if(c!=null)c.disconnect();runOnUiThread(()->button.setEnabled(true));}}).start();
    }
    @Override protected void onDestroy(){handler.removeCallbacks(ticker);super.onDestroy();}
}