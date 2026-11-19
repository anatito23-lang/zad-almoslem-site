package com.zad.almoslem;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

public class FaithLibraryActivity extends Activity {
 private final int green=Color.rgb(8,48,40);
 private final String[][] hadiths={
  {"إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى","متفق عليه"},
  {"لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه","متفق عليه"},
  {"من كان يؤمن بالله واليوم الآخر فليقل خيرًا أو ليصمت","متفق عليه"},
  {"من لا يرحم لا يُرحم","متفق عليه"},
  {"المسلم من سلم المسلمون من لسانه ويده","متفق عليه"},
  {"من سلك طريقًا يلتمس فيه علمًا سهل الله له به طريقًا إلى الجنة","رواه مسلم"},
  {"انظروا إلى من أسفل منكم، ولا تنظروا إلى من هو فوقكم؛ فهو أجدر ألا تزدروا نعمة الله عليكم","رواه مسلم (2963)"}
 };
 private final String[][] duas={
  {"رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ","سورة البقرة: 201"},
  {"رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً إِنَّكَ أَنْتَ الْوَهَّابُ","سورة آل عمران: 8"},
  {"رَبَّنَا ظَلَمْنَا أَنْفُسَنَا وَإِنْ لَمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ","سورة الأعراف: 23"},
  {"رَبِّ إِنِّي لِمَا أَنْزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ","سورة القصص: 24"},
  {"رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا","سورة الفرقان: 74"}
 };
 private int hadithIndex=0,duaIndex=0;
 private TextView hadithText,hadithSource,duaText,duaSource,khatmaResult;
 private EditText daysInput;
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(android.view.View.TEXT_DIRECTION_RTL);t.setLineSpacing(dp(4),1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 private LinearLayout card(LinearLayout root,String title){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(13),dp(14),dp(13));box.setBackground(bg(Color.WHITE,15));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);root.addView(box,lp);box.addView(label(title,18,green,true));return box;}
 private void build(){
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(15),dp(14),dp(25));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);setContentView(scroll);
  TextView title=label("السنة النبوية والأدعية والمكتبة الإيمانية",22,Color.WHITE,true);title.setGravity(Gravity.CENTER);
  TextView sub=label("أحاديث صحيحة وأدعية قرآنية ووقفات نافعة",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(13),dp(18),dp(13),dp(18));hero.setBackground(bg(green,18));hero.addView(title);hero.addView(sub);root.addView(hero);
  LinearLayout ayah=card(root,"۞ آية اليوم");
  ayah.addView(label("﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾",21,green,true));
  ayah.addView(label("سورة الرعد: 28",12,Color.GRAY,false));
  LinearLayout hadith=card(root,"ﷺ حديث نبوي");
  hadithText=label("",19,green,true);hadithText.setGravity(Gravity.CENTER);hadithText.setPadding(0,dp(12),0,dp(8));hadith.addView(hadithText);
  hadithSource=label("",12,Color.GRAY,false);hadithSource.setGravity(Gravity.CENTER);hadith.addView(hadithSource);
  Button nextHadith=new Button(this);nextHadith.setText("حديث آخر");hadith.addView(nextHadith);nextHadith.setOnClickListener(v->{hadithIndex=(hadithIndex+1)%hadiths.length;renderHadith();});renderHadith();
  LinearLayout dua=card(root,"🤲 دعاء من القرآن الكريم");
  duaText=label("",19,green,true);duaText.setGravity(Gravity.CENTER);duaText.setPadding(0,dp(12),0,dp(8));dua.addView(duaText);
  duaSource=label("",12,Color.GRAY,false);duaSource.setGravity(Gravity.CENTER);dua.addView(duaSource);
  Button nextDua=new Button(this);nextDua.setText("دعاء آخر");dua.addView(nextDua);nextDua.setOnClickListener(v->{duaIndex=(duaIndex+1)%duas.length;renderDua();});renderDua();
  LinearLayout khatma=card(root,"📖 خطة ختم القرآن");
  khatma.addView(label("اختر عدد الأيام، على أساس مصحف من 604 صفحات.",13,Color.DKGRAY,false));
  daysInput=new EditText(this);daysInput.setInputType(2);daysInput.setText("30");daysInput.setSingleLine(true);khatma.addView(daysInput);
  Button calculate=new Button(this);calculate.setText("احسب الورد اليومي");khatma.addView(calculate);
  khatmaResult=label("وردك اليومي: 21 صفحة تقريبًا",14,green,true);khatmaResult.setPadding(0,dp(8),0,0);khatma.addView(khatmaResult);
  calculate.setOnClickListener(v->{try{int days=Integer.parseInt(daysInput.getText().toString().trim());if(days<1||days>365)throw new Exception();int pages=(604+days-1)/days;khatmaResult.setText("وردك اليومي: "+pages+" صفحة تقريبًا.");}catch(Exception e){khatmaResult.setText("أدخل عدد أيام من 1 إلى 365.");}});
  LinearLayout reflection=card(root,"♡ قلبي اطمأن — وقفة إيمانية");
  reflection.addView(label("لا تُطفئ فرحة النعمة بالمقارنة",17,green,true));
  reflection.addView(label("قد تكون بين يديك نعم اعتدت وجودها حتى لم تعد تلتفت إليها: بيت يأويك، وصحة تعينك، ووجه تحبه، ورزق يكفي يومك. ثم تنظر إلى ما في أيدي الآخرين، فتنسى ما عندك وتظن أنك محروم. لا تجعل المقارنة تسرق منك شكر النعمة؛ فليس معنى أن غيرك يملك أكثر أن ما عندك قليل، ولا أن رزقك بلا بركة. انظر إلى عطايا الله بعين الامتنان، واستعملها فيما يرضيه.",15,Color.rgb(45,62,50),false));
  reflection.addView(label("﴿لَئِنْ شَكَرْتُمْ لَأَزِيدَنَّكُمْ﴾ — إبراهيم: 7",18,green,true));
  reflection.addView(label("تذكير عملي: اكتب اليوم ثلاث نعم أنعم الله بها عليك، ثم استخدم واحدة منها في مساعدة إنسان أو إدخال السرور عليه.",13,Color.GRAY,false));
  LinearLayout topics=card(root,"من المكتبة الإيمانية");
  topics.addView(label("﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾ — الرعد: 28",18,green,true));
  topics.addView(label("القناعة لا تعني أن تتوقف عن السعي، بل أن تسعى من غير حسد، وتطلب الرزق من غير سخط، وتفرح بما أعطاك الله وأنت تعمل لتحسين حالك بالحلال.",14,Color.rgb(50,65,55),false));
  topics.addView(label("المحافظة على الصلاة: اجعل الصلاة أول ما تهتم به في يومك، وحافظ عليها في وقتها.\\n\\nبر الوالدين وصلة الرحم: من أعظم أبواب البر الإحسان إلى الوالدين والسؤال عن الأقارب وجبر الخواطر.\\n\\nالتوبة والاستغفار: باب التوبة مفتوح، فلا يقنط عبد من رحمة الله، وليتبع السيئة حسنة تمحها.\\n\\nالرفق بالأبناء: الطفل يحتاج إلى من يسمعه ويحتويه قبل أن يوجهه؛ فالكلمة الطيبة والقدوة الحسنة تبني قلبًا آمنًا.\\n\\nضبط الغضب قبل الكلام: إذا احتد النقاش، توقف لحظة واستعذ بالله ولا تتعجل الرد. اترك لنفسك وقتًا تهدأ فيه، ثم ارجع إلى الحوار بصوت هادئ؛ فالهدف حل المشكلة لا كسب الجولة.\\n\\nشكر النعمة بالفعل: سمّ النعمة واعترف بأنها من الله، واحفظها من الحرام، واستعملها في الخير؛ فشكر الصحة يكون بالطاعة، وشكر المال بالإنفاق الحلال ومساعدة المحتاج.",14,Color.rgb(50,65,55),false));
 }
 private void renderHadith(){hadithText.setText("«"+hadiths[hadithIndex][0]+"»");hadithSource.setText(hadiths[hadithIndex][1]);}
 private void renderDua(){duaText.setText("﴿"+duas[duaIndex][0]+"﴾");duaSource.setText(duas[duaIndex][1]);}
}