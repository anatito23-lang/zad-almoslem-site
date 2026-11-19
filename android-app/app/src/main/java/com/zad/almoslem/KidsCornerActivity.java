package com.zad.almoslem;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class KidsCornerActivity extends Activity {
 private final int green=Color.rgb(8,48,40);
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);t.setLineSpacing(dp(4),1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 private void card(LinearLayout root,String title,String intro,String story,String[] steps,String footer){
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(13),dp(14),dp(13));box.setBackground(bg(Color.WHITE,15));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);root.addView(box,lp);
  box.addView(text(title,18,green,true));if(intro!=null)box.addView(text(intro,13,Color.GRAY,false));
  if(story!=null){TextView st=text(story,14,Color.rgb(78,67,42),false);st.setPadding(dp(10),dp(9),dp(10),dp(9));st.setBackground(bg(Color.rgb(248,245,235),11));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(8);box.addView(st,sp);}
  if(steps!=null)for(int i=0;i<steps.length;i++){TextView step=text((i+1)+". "+steps[i],14,Color.rgb(48,65,54),false);step.setPadding(0,dp(5),0,dp(5));box.addView(step);}
  if(footer!=null){TextView ft=text(footer,12,Color.rgb(120,120,110),false);ft.setPadding(0,dp(7),0,0);box.addView(ft);}
 }
 private void build(){
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(15),dp(14),dp(25));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);setContentView(scroll);
  TextView title=text("ركن الأطفال — نتعلم ونمرح",23,Color.WHITE,true);title.setGravity(Gravity.CENTER);
  TextView sub=text("نتعلم الطهارة والصلاة بالرفق والخطوات السهلة",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(13),dp(18),dp(13),dp(18));hero.setBackground(bg(green,18));hero.addView(title);hero.addView(sub);root.addView(hero);
  card(root,"💧 حكاية الوضوء مع آدم","تعال نتعلم كيف نتوضأ قبل الصلاة خطوة خطوة.","نادى الأب آدم وقال له: هيا يا بُني نتوضأ؛ فالله يحب المتطهرين. وقف آدم عند الماء وتعلّم الوضوء بهدوء.",new String[]{"أنوي الوضوء بقلبي، وأقول: بسم الله.","أغسل كفّيّ ثلاث مرات.","أتمضمض وأستنشق الماء ثم أستنثره.","أغسل وجهي جيدًا.","أغسل يديّ إلى المرفقين، وأبدأ باليمنى ثم اليسرى.","أمسح رأسي وأذنيّ.","أغسل قدميّ إلى الكعبين، وأبدأ باليمنى ثم اليسرى."},"نتعلم الوضوء من غير إسراف في الماء، ونتأكد أن الماء وصل إلى الأعضاء.");
  card(root,"🕌 قصة سليم والصلاة","قصة طفل أحب الصلاة وحرص أن يصلي مع أسرته.","كان سليم يسمع الأذان كل يوم، فسأل أمه: لماذا نذهب للصلاة؟ ابتسمت أمه وقالت: لأن الصلاة صلتنا بربنا، وهي من أعظم العبادات. توضأ سليم ووقف بجوار أبيه، وتعلّم أن يحافظ على صلاته في وقتها.",new String[]{"عندما يحين وقت الصلاة أترك ما يشغلني وأستعد لها.","أتوضأ وأرتدي ملابس طاهرة ونظيفة.","أستقبل القبلة وأنوي الصلاة بقلبي.","أبدأ الصلاة بتكبيرة الإحرام: الله أكبر.","أقرأ الفاتحة وما تيسر من القرآن، ثم أركع وأسجد بخشوع.","أكمل ركعات الصلاة، وأختمها بالتشهد والسلام."},"يتعلم الطفل الصلاة بالتدريب والرفق، ويشجعه أهله دون تعنيف.");
  card(root,"📖 كيف أصلي؟ ترتيب الصلاة","تعرف على الحركات الأساسية للصلاة بالترتيب.",null,new String[]{"القيام والتكبير: أقف مستقبل القبلة وأقول: الله أكبر.","القراءة: أقرأ سورة الفاتحة وما تيسر من القرآن.","الركوع: أنحني وأقول: سبحان ربي العظيم.","الاعتدال: أرفع من الركوع وأحمد الله.","السجود: أسجد وأقول: سبحان ربي الأعلى.","الجلوس: أجلس بين السجدتين، ثم أسجد مرة ثانية.","إكمال الصلاة: أكرر الركعات حسب الصلاة، ثم أجلس للتشهد وأسلم."},"الصلاة لها أوقات وعدد ركعات محدد، ونتعلم صفتها كاملة مع الأهل أو المعلم.");
  TextView note=text("يا بطل: اسأل والديك أو معلمك إذا لم تفهم خطوة، فالتعلم يكون بالسؤال والتكرار والرفق.",13,green,true);note.setPadding(dp(8),dp(10),dp(8),dp(10));root.addView(note);
 }
}