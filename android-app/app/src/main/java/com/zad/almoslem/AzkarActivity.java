package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class AzkarActivity extends Activity {
    private final int green=Color.rgb(8,48,40), ink=Color.rgb(40,58,48);
    private static class Zikr {
        String text, source, category; int count;
        Zikr(String c,String t,String s,int n){category=c;text=t;source=s;count=n;}
    }
    private final ArrayList<Zikr> items=new ArrayList<>();
    private final ArrayList<Zikr> shown=new ArrayList<>();
    private TextView zikrText, sourceText, countText, positionText;
    private Spinner categorySpinner;
    private int index=0, remaining=0;
    private String currentCategory="أذكار الصباح";

    @Override public void onCreate(Bundle state){super.onCreate(state);AppThemeColors.apply(this);load();build();RemoteContentRepository.sync(this, "azkar", this::applyRemoteAzkar);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER);t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLineSpacing(dp(5),1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void add(String c,String t,String s,int n){items.add(new Zikr(c,t,s,n));}
    private void load(){
        String morning="أذكار الصباح", evening="أذكار المساء", sleep="أذكار النوم", prayer="أذكار بعد الصلاة";
        add(morning,"أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير. رب أسألك خير ما في هذا اليوم وخير ما بعده، وأعوذ بك من شر ما في هذا اليوم وشر ما بعده. رب أعوذ بك من الكسل وسوء الكبر، رب أعوذ بك من عذاب في النار وعذاب في القبر.","رواه مسلم",1);
        add(morning,"اللهم بك أصبحنا، وبك أمسينا، وبك نحيا، وبك نموت، وإليك النشور.","رواه أبو داود والترمذي، وصححه أهل العلم",1);
        add(morning,"اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي، وأبوء بذنبي، فاغفر لي؛ فإنه لا يغفر الذنوب إلا أنت.","سيد الاستغفار — رواه البخاري",1);
        add(morning,"اللهم إني أسألك العفو والعافية في الدنيا والآخرة، اللهم إني أسألك العفو والعافية في ديني ودنياي وأهلي ومالي.","رواه أبو داود وابن ماجه، وصححه الألباني",1);
        add(morning,"رضيت بالله ربًّا، وبالإسلام دينًا، وبمحمد ﷺ نبيًّا.","رواه أبو داود والترمذي",3);
        add(morning,"بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم.","رواه أبو داود والترمذي، وصححه الألباني",3);
        add(morning,"سبحان الله وبحمده.","رواه مسلم",100);
        add(morning,"اللهم صل وسلم على نبينا محمد.","ثبت الأمر بالإكثار من الصلاة عليه ﷺ، ولا يثبت لهذا الذكر عدد مخصوص في الصباح",1);
        add(evening,"أمسينا وأمسى الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير. رب أسألك خير ما في هذه الليلة وخير ما بعدها، وأعوذ بك من شر ما في هذه الليلة وشر ما بعدها.","رواه مسلم",1);
        add(evening,"اللهم بك أمسينا، وبك أصبحنا، وبك نحيا، وبك نموت، وإليك المصير.","رواه أبو داود والترمذي",1);
        add(evening,"اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي، وأبوء بذنبي، فاغفر لي؛ فإنه لا يغفر الذنوب إلا أنت.","سيد الاستغفار — رواه البخاري",1);
        add(evening,"رضيت بالله ربًّا، وبالإسلام دينًا، وبمحمد ﷺ نبيًّا.","رواه أبو داود والترمذي",3);
        add(evening,"بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم.","رواه أبو داود والترمذي، وصححه الألباني",3);
        add(evening,"أعوذ بكلمات الله التامات من شر ما خلق.","رواه مسلم",3);
        add(evening,"سبحان الله وبحمده.","رواه مسلم",100);
        add(sleep,"باسمك اللهم أموت وأحيا.","رواه البخاري",1);
        add(sleep,"اللهم قني عذابك يوم تبعث عبادك.","رواه أبو داود والترمذي، وصححه الألباني",3);
        add(sleep,"يجمع كفيه ثم ينفث فيهما، ويقرأ: قل هو الله أحد، وقل أعوذ برب الفلق، وقل أعوذ برب الناس، ثم يمسح بهما ما استطاع من جسده، يبدأ برأسه ووجهه وما أقبل من جسده.","رواه البخاري",3);
        add(sleep,"آمَنَ الرَّسُولُ بِمَا أُنْزِلَ إِلَيْهِ مِنْ رَبِّهِ وَالْمُؤْمِنُونَ... إلى آخر سورة البقرة.","رواه البخاري ومسلم في فضل قراءة الآيتين من آخر سورة البقرة",1);
        add(sleep,"سبحان الله (33)، والحمد لله (33)، والله أكبر (34).","متفق عليه؛ وصية النبي ﷺ لفاطمة وعلي رضي الله عنهما",1);
        add(sleep,"آية الكرسي: ﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...﴾ إلى آخر الآية.","رواه البخاري في قصة أبي هريرة مع الشيطان",1);
        add(prayer,"أستغفر الله، أستغفر الله، أستغفر الله.","رواه مسلم",1);
        add(prayer,"اللهم أنت السلام، ومنك السلام، تباركت يا ذا الجلال والإكرام.","رواه مسلم",1);
        add(prayer,"سبحان الله، والحمد لله، والله أكبر؛ ثلاثًا وثلاثين، ثم تمام المائة: لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.","رواه مسلم",1);
        add(prayer,"اللهم أعني على ذكرك وشكرك وحسن عبادتك.","رواه أبو داود والنسائي، وصححه أهل العلم",1);
        add(prayer,"قراءة آية الكرسي: ﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...﴾ إلى آخر الآية.","ورد في فضلها حديث صحيح رواه النسائي في عمل اليوم والليلة وغيره",1);
    }
    private void applyRemoteAzkar(){
        JSONArray remote=RemoteContentRepository.cached(this,"azkar");
        if(remote.length()==0)return;
        ArrayList<Zikr> updated=new ArrayList<>();
        for(int i=0;i<remote.length();i++){
            JSONObject row=remote.optJSONObject(i);if(row==null)continue;
            String category=row.optString("category","").trim();
            String text=row.optString("text","").trim();
            String source=row.optString("source","").trim();
            int count=row.optInt("count",1);
            if(category.isEmpty()||text.isEmpty()||source.isEmpty()||count<1)continue;
            updated.add(new Zikr(category,text,source,count));
        }
        if(updated.isEmpty())return;
        items.clear();items.addAll(updated);
        String previous=currentCategory;
        ArrayList<String> categories=new ArrayList<>();
        for(Zikr z:items)if(!categories.contains(z.category))categories.add(z.category);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,categories);
        categorySpinner.setAdapter(adapter);
        int selected=categories.indexOf(previous);
        categorySpinner.setSelection(selected>=0?selected:0);
        currentCategory=categories.get(selected>=0?selected:0);
        filter();
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);
        root.setPadding(dp(16),dp(16),dp(16),dp(28));root.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);
        TextView title=label("مكتبة الأذكار",24,AppThemeColors.text(this),true);root.addView(title);
        TextView intro=label("أذكار مأثورة مرتبة حسب الوقت والحال، مع المصدر والعدد الوارد.",13,AppThemeColors.muted(this),false);
        intro.setPadding(0,dp(8),0,dp(12));root.addView(intro);
        categorySpinner=new Spinner(this);
        String[] categories={"أذكار الصباح","أذكار المساء","أذكار النوم","أذكار بعد الصلاة"};
        categorySpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,categories));root.addView(categorySpinner);
        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18),dp(24),dp(18),dp(20));card.setBackground(bg(AppThemeColors.surface(this),18));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(14);root.addView(card,cp);
        zikrText=label("",20,AppThemeColors.text(this),true);zikrText.setPadding(0,dp(8),0,dp(18));card.addView(zikrText);
        sourceText=label("",12,AppThemeColors.muted(this),false);card.addView(sourceText);
        countText=label("",16,AppThemeColors.primary(this),true);countText.setPadding(0,dp(18),0,dp(8));card.addView(countText);
        Button count=new Button(this);count.setText("تسبيح / تم");card.addView(count);
        Button reset=new Button(this);reset.setText("إعادة الذكر");card.addView(reset);
        LinearLayout nav=new LinearLayout(this);Button prev=new Button(this),next=new Button(this);
        prev.setText("السابق");next.setText("التالي");nav.addView(prev,new LinearLayout.LayoutParams(0,dp(48),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(nav);
        positionText=label("",12,AppThemeColors.muted(this),false);root.addView(positionText);
        TextView note=label("تنبيه: تختلف أعداد بعض الأذكار باختلاف الروايات؛ التزمنا بالعدد المشهور في المصدر المذكور، وما لم يثبت له عدد محدد نُبّه عليه.",11,AppThemeColors.muted(this),false);note.setPadding(0,dp(14),0,0);root.addView(note);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){currentCategory=categories[pos];filter();}});
        count.setOnClickListener(v->{if(remaining>0){remaining--;render();}if(remaining==0&&index<shown.size()-1){index++;remaining=shown.get(index).count;render();}});
        reset.setOnClickListener(v->{if(!shown.isEmpty()){remaining=shown.get(index).count;render();}});
        prev.setOnClickListener(v->{if(index>0){index--;remaining=shown.get(index).count;render();}});
        next.setOnClickListener(v->{if(index<shown.size()-1){index++;remaining=shown.get(index).count;render();}});
        setContentView(AppTabs.wrap(this,scroll,"more"));
    }
    private void filter(){shown.clear();for(Zikr z:items)if(z.category.equals(currentCategory))shown.add(z);index=0;remaining=shown.isEmpty()?0:shown.get(0).count;render();}
    private void render(){
        if(shown.isEmpty())return;Zikr z=shown.get(index);zikrText.setText(z.text);sourceText.setText("المصدر: "+z.source);
        countText.setText("المتبقي: "+remaining+" من "+z.count);positionText.setText("الذكر "+(index+1)+" من "+shown.size());
    }
}
