package com.zad.almoslem;
import android.app.Activity;import android.content.Intent;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.os.Bundle;import android.view.Gravity;import android.widget.*;
public class MoreActivity extends Activity{
private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
private void open(Class<?> c){startActivity(new Intent(this,c));}
private TextView label(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.RIGHT);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
private void section(LinearLayout root,String title){TextView h=label(title,18,AppThemeColors.primary(this),true);h.setPadding(dp(3),dp(18),dp(3),dp(9));root.addView(h);}
private void add(LinearLayout root,String title,String desc,Class<?> target){LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(15),dp(12),dp(15),dp(12));GradientDrawable bg=new GradientDrawable();bg.setColor(AppThemeColors.surface(this));bg.setCornerRadius(dp(15));card.setBackground(bg);card.addView(label(title,16,AppThemeColors.text(this),true));TextView d=label(desc,12,AppThemeColors.muted(this),false);d.setPadding(0,dp(4),0,0);card.addView(d);card.setOnClickListener(v->open(target));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(9);root.addView(card,lp);}
@Override public void onCreate(Bundle b){super.onCreate(b);AppThemeColors.apply(this);build();}
private void build(){ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(14),dp(14),dp(14),dp(24));root.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);
TextView title=label("المزيد من خدمات زاد المسلم",23,AppThemeColors.text(this),true);title.setGravity(Gravity.CENTER);title.setPadding(0,0,0,dp(4));root.addView(title);
section(root,"🕌 الصلاة والأذان");
add(root,"مواقيت الصلاة","عرض المواقيت وتحديثها واختيار المدينة",PrayerDashboardActivity.class);
add(root,"إعدادات الأذان","اختيار صوت كل صلاة والتحكم في مستوى الصوت والتنزيل",MainActivity.class);
add(root,"التنبيهات اليومية","إدارة التذكيرات والأذكار والتنبيهات",DailyReminderSettingsActivity.class);
section(root,"📿 الرقية والابتهالات");
add(root,"الرقية الشرعية","اختيار القارئ وتشغيل الرقية أو إيقافها",RuqyahActivity.class);
add(root,"الابتهالات والإنشاد","تسجيلات الابتهالات الدينية",AudioLibraryActivity.class);
section(root,"🎞️ محتوى إيماني");
add(root,"ريلز إسلامي","بطاقات إيمانية رأسية: آيات وأحاديث صحيحة وأدعية، اسحب للتنقل بينها",IslamicReelsActivity.class);
section(root,"🧭 خدمات إسلامية");
add(root,"بوصلة القبلة","تحديد اتجاه القبلة",QiblaActivity.class);

add(root,"مكتبة خطب الجمعة","الخطب والموضوعات الدعوية",KhutbahLibraryActivity.class);
add(root,"المكتبة الإسلامية","الكتب والسنة والأدعية",FaithLibraryActivity.class);
add(root,"التقويم الهجري والميلادي","المناسبات والتواريخ",CalendarActivity.class);

section(root,"☀️ الأذكار والأوراد");
add(root,"مكتبة الأذكار","أذكار الصباح والمساء والنوم وبعد الصلاة مع النص والمصدر والعداد",AzkarActivity.class);
add(root,"وردي اليومي","متابعة الورد والأذكار اليومية",DailyWirdActivity.class);
add(root,"ورد يوم الجمعة","أذكار وأعمال يوم الجمعة",FridayWirdActivity.class);
section(root,"⚙️ إعدادات التطبيق");
add(root,"اللغة والثيم","اختيار لغة الواجهة وتغيير ألوان التطبيق",LanguageSettingsActivity.class);
setContentView(AppTabs.wrap(this,scroll,"more"));}
}