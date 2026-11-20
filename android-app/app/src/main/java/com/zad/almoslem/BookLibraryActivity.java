package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class BookLibraryActivity extends Activity {
    private final String[][] books = {
        {"الفقه الميسر في ضوء الكتاب والسنة","وزارة الشؤون الإسلامية والدعوة والإرشاد","مدخل واضح إلى أبواب الطهارة والصلاة وأحكام العبادات، مناسب للقراءة والتعلّم.","فقه","FP93430"},
        {"منهاج المسلم","أبو بكر جابر الجزائري","كتاب جامع في العقيدة والآداب والعبادات والمعاملات، مرتب في أبواب سهلة الرجوع إليها.","فقه","FP170221"},
        {"المغني","الإمام ابن قدامة المقدسي","من الموسوعات الفقهية المعروفة، يعرض المسائل وأدلتها وأقوال أهل العلم.","فقه","FPelmoghni"},
        {"الرحيق المختوم","صفي الرحمن المباركفوري","عرض مرتب لأحداث السيرة النبوية من أحوال العرب قبل البعثة إلى وفاة النبي ﷺ.","السيرة","waq34663"},
        {"السيرة النبوية — ابن هشام","عبد الملك بن هشام","من أمهات المصادر التراثية في أخبار السيرة ومغازي النبي ﷺ.","السيرة","FP30626"},
        {"زاد المعاد في هدي خير العباد","ابن قيم الجوزية","يتناول هدي النبي ﷺ في عباداته ومعاملاته وأخلاقه وسفره وغزواته.","السيرة","80184s"},
        {"الشمائل المحمدية","الإمام الترمذي","كتاب في صفات النبي ﷺ وأخلاقه ولباسه وطعامه وعبادته وتواضعه.","السيرة","3840waq"},
        {"تفسير القرآن العظيم — ابن كثير","الحافظ ابن كثير","تفسير مشهور يجمع الآيات والأحاديث والآثار وأقوال أهل العلم في بيان معاني القرآن.","التفسير","72411"},
        {"تيسير الكريم الرحمن — تفسير السعدي","الشيخ عبد الرحمن بن ناصر السعدي","تفسير واضح العبارة، يقرّب معاني الآيات ويبرز الهدايات والفوائد.","التفسير","WAQ100585"},
        {"جامع البيان — تفسير الطبري","الإمام ابن جرير الطبري","من أمهات كتب التفسير بالمأثور، يعرض الروايات وأقوال السلف في تفسير الآيات.","التفسير","FP59561"},
        {"رياض الصالحين","الإمام النووي — مراجعة ماهر ياسين الفحل","كتاب يجمع أبوابًا من العبادات والآداب والأخلاق والرقائق، بأحاديث مرتبة تسهّل القراءة اليومية.","الحديث",""},
        {"الأذكار النووية","الإمام النووي — مراجعة عامر بن علي ياسين","مرجع واسع لأذكار اليوم والليلة وأدعية الأحوال المختلفة، مع أبواب في آداب الذكر وأحكامه.","الحديث",""}
    };
    private final int green = Color.rgb(8,48,40);
    private LinearLayout list;
    private EditText search;
    private Spinner category;
    private TextView count;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(green);
        getWindow().setNavigationBarColor(green);
        build();
        render();
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15),dp(15),dp(15),dp(28));root.setBackgroundColor(Color.rgb(244,247,243));
        scroll.addView(root);setContentView(scroll);
        TextView title=text("مكتبة الكتب الإسلامية",24,Color.WHITE,true);title.setGravity(Gravity.CENTER);
        TextView sub=text("فهرس الكتب الموجود في موقع زاد المسلم",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(15),dp(18),dp(15),dp(18));hero.setBackground(shape(green,18));hero.addView(title);hero.addView(sub);
        root.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        search=new EditText(this);search.setSingleLine(true);search.setHint("ابحث باسم الكتاب أو المؤلف");
        search.setTextSize(15);search.setPadding(dp(12),dp(8),dp(12),dp(8));search.setBackground(shape(Color.WHITE,12));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(50));sp.topMargin=dp(14);root.addView(search,sp);
        category=new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"كل الأقسام","فقه","السيرة","التفسير","الحديث"}));
        root.addView(category,new LinearLayout.LayoutParams(-1,dp(48)));
        count=text("",12,Color.DKGRAY,false);count.setPadding(dp(3),dp(8),dp(3),dp(8));root.addView(count);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){render();}
            public void afterTextChanged(Editable e){}
        });
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){render();}
            public void onNothingSelected(AdapterView<?> p){}
        });
    }
    private void render(){
        if(list==null)return;
        list.removeAllViews();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        String cat=category.getSelectedItem()==null?"كل الأقسام":category.getSelectedItem().toString();
        int shown=0;
        for(String[] b:books){
            if(!"كل الأقسام".equals(cat)&&!cat.equals(b[3]))continue;
            if(!q.isEmpty()&&!b[0].toLowerCase(Locale.ROOT).contains(q)&&!b[1].toLowerCase(Locale.ROOT).contains(q))continue;
            shown++;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(shape(Color.WHITE,15));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(10);list.addView(card,cp);
            card.addView(text(b[0],18,green,true));
            TextView author=text("المؤلف: "+b[1],12,Color.rgb(110,120,112),false);author.setPadding(0,dp(4),0,dp(5));card.addView(author);
            card.addView(text(b[2],14,Color.rgb(65,78,68),false));
            TextView tag=text("القسم: "+b[3],12,Color.rgb(150,118,54),true);tag.setPadding(0,dp(7),0,dp(2));card.addView(tag);
            Button open=new Button(this);
            if(b[4].isEmpty()){
                open.setText("رابط القراءة غير محدد في الموقع");open.setEnabled(false);
            }else{
                open.setText("فتح الكتاب للقراءة");open.setOnClickListener(v->{
                    String url="https://archive.org/embed/"+Uri.encode(b[4])+"?ui=embed";
                    try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
                    catch(Exception e){Toast.makeText(this,"تعذر فتح رابط الكتاب",Toast.LENGTH_LONG).show();}
                });
            }
            card.addView(open,new LinearLayout.LayoutParams(-1,dp(48)));
        }
        count.setText("عدد الكتب الظاهرة: "+shown+" من "+books.length+
                " — القراءة تفتح من مصدر الكتاب الأصلي عبر المتصفح.");
        if(shown==0)list.addView(text("لا توجد نتائج مطابقة.",14,Color.DKGRAY,false));
    }
}