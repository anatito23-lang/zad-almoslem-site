package com.zad.almoslem;
import android.app.Activity;import android.content.Intent;import android.content.res.Configuration;import android.graphics.Color;import android.os.Bundle;import android.view.Gravity;import android.widget.*;import java.util.Locale;
public class LanguageSettingsActivity extends Activity{
String[] names={"العربية","English","Français","اردو","Türkçe"};String[] codes={"ar","en","fr","ur","tr"};
String[] themeNames={"الأخضر الإسلامي","فاتح","داكن"};String[] themeCodes={"green","light","dark"};
int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}
@Override public void onCreate(Bundle b){super.onCreate(b);AppThemeColors.apply(this);build();}
void build(){int bg=AppThemeColors.bg(this),surface=AppThemeColors.surface(this),fg=AppThemeColors.text(this),muted=AppThemeColors.muted(this),primary=AppThemeColors.primary(this);
ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(22),dp(18),dp(24));root.setBackgroundColor(bg);scroll.addView(root);
TextView title=text("إعدادات المظهر واللغة",23,fg,true);title.setGravity(Gravity.CENTER);root.addView(title);
TextView languageTitle=text("لغة التطبيق",17,primary,true);languageTitle.setPadding(0,dp(20),0,dp(8));root.addView(languageTitle);
TextView desc=text("اختر لغة واجهة التطبيق.",13,muted,false);root.addView(desc);
Spinner language=new Spinner(this);language.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));String saved=getSharedPreferences("zad_settings",MODE_PRIVATE).getString("app_language","ar");int li=0;for(int i=0;i<codes.length;i++)if(codes[i].equals(saved))li=i;language.setSelection(li);root.addView(language);
TextView themeTitle=text("ثيم التطبيق",17,primary,true);themeTitle.setPadding(0,dp(22),0,dp(8));root.addView(themeTitle);
TextView themeDesc=text("غيّر ألوان التطبيق حسب راحتك. سيتم حفظ اختيارك.",13,muted,false);root.addView(themeDesc);
Spinner theme=new Spinner(this);theme.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,themeNames));String savedTheme=AppThemeColors.get(this);int ti=0;for(int i=0;i<themeCodes.length;i++)if(themeCodes[i].equals(savedTheme))ti=i;theme.setSelection(ti);root.addView(theme);
TextView preview=text("معاينة: الرئيسية • السبحة • المصحف • القرآن • المزيد",13,fg,true);preview.setGravity(Gravity.CENTER);preview.setPadding(dp(8),dp(18),dp(8),dp(18));preview.setBackgroundColor(surface);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=dp(18);root.addView(preview,pp);
Button apply=new Button(this);apply.setText("حفظ الإعدادات وتطبيقها");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(18);root.addView(apply,bp);
apply.setOnClickListener(v->{String code=codes[language.getSelectedItemPosition()];String selectedTheme=themeCodes[theme.getSelectedItemPosition()];getSharedPreferences("zad_settings",MODE_PRIVATE).edit().putString("app_language",code).putString("app_theme",selectedTheme).apply();Locale locale=new Locale(code);Locale.setDefault(locale);Configuration config=new Configuration(getResources().getConfiguration());config.setLocale(locale);getResources().updateConfiguration(config,getResources().getDisplayMetrics());Intent i=new Intent(this,HomeActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);finish();});
TextView note=text("اللغات المتاحة: العربية والإنجليزية والفرنسية والأردية والتركية. ترجمة جميع محتويات التطبيق يجري استكمالها. الثيم يغيّر ألوان الشاشات الأساسية وشريط التنقل.",12,muted,false);note.setPadding(0,dp(16),0,0);root.addView(note);setContentView(AppTabs.wrap(this,scroll,"more"));}
}