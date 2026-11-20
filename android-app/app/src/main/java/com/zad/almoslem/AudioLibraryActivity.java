package com.zad.almoslem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.net.URLEncoder;
import java.util.*;

public class AudioLibraryActivity extends Activity {
    private final int green=Color.rgb(8,48,40);
    private final String[] artists={"نصر الدين طوبار","سيد النقشبندي"};
    private final String[][] tobar={
        {"جل المنادي","https://archive.org/download/20201209_20201209_0854/%D8%AC%D9%84%20%D8%A7%D9%84%D9%85%D9%86%D8%A7%D8%AF%D9%89.mp3"},
        {"يا مالك الملك","https://archive.org/download/20201209_20201209_0854/%D9%8A%D8%A7%20%D9%85%D8%A7%D9%84%D9%83%20%D8%A7%D9%84%D9%85%D9%84%D9%83.mp3"},
        {"حين يهدي الصبح إشراق سناه","https://archive.org/download/20201209_20201209_0854/%D8%AD%D9%8A%D9%86%20%D9%8A%D9%87%D8%AF%D9%8A%20%D8%A7%D9%84%D8%B5%D8%A8%D8%AD%20%D8%A5%D8%B4%D8%B1%D8%A7%D9%82%20%D8%B3%D9%86%D8%A7%D9%87.mp3"},
        {"يارب عدت إلى رحابك","https://www.nourelquran.com/force_download.php?id=5690"},
        {"تواشيح وابتهالات","https://www.nourelquran.com/force_download.php?id=5691"},
        {"سبحانك","https://www.nourelquran.com/force_download.php?id=5692"},
        {"يا من رضيت لنا","https://www.nourelquran.com/force_download.php?id=5693"},
        {"بك أستجير","https://www.nourelquran.com/force_download.php?id=5694"},
        {"يا بارئ الكون","https://www.nourelquran.com/force_download.php?id=5695"},
        {"تواشيح","https://www.nourelquran.com/force_download.php?id=5696"},
        {"الحوت والعنكبوت","https://www.nourelquran.com/force_download.php?id=5697"},
        {"يا ليلة القدر","https://www.nourelquran.com/force_download.php?id=5698"},
        {"يا من ملكت قلوبنا","https://www.nourelquran.com/force_download.php?id=5699"},
        {"يا منقذي في شدتي","https://www.nourelquran.com/force_download.php?id=5700"},
        {"يا ودودا","https://www.nourelquran.com/force_download.php?id=5701"},
        {"الواحد المعبود","https://www.nourelquran.com/force_download.php?id=5702"},
        {"الله رب الوجود","https://www.nourelquran.com/force_download.php?id=5703"},
        {"الأمر أمرك","https://www.nourelquran.com/force_download.php?id=5704"}
    };
    private final String[] naqshTitles={"مولاي إني ببابك","لحظات يزهى بهن الزمان","الله يا الله","نفسي يا رب","يا رب كرمك علينا","رباه ارفع مقتك عنا","في حبك ربي نتفانى","أقول أمتي","الكون صلاة وخشوع","ندخل بيتك","دعاء السائل المحروم","يا من يعلم","سبحانك يا كريم","أيها الناس","ربنا","أيها الساهر","أتيت إليك يا رب العباد","أدعوك في غمضي","إذا ما تلاقى الأفق بالأرض في المدى","أسعى لخلاقي"};
    private final String[] naqshFiles={"01 - مولاي.mp3","02 - لحظات يزهى بهن الزمان -ابتهال للشيخ سيد النقشبندى.mp3","03 - الله يا الله.mp3","04 - نفسي يا رب.mp3","05 - يا رب كرمك علينا.mp3","06 - رباه ارفع مقتك عنا.mp3","07 - في حبك ربي نتفاني.mp3","08 - أقول امتي.mp3","09 - الكون صلاة وخشوع.mp3","10 - ندخل بيتك.mp3","11 - دعاء السائل المحروم.mp3","12 - يا من يعلم.mp3","12 - سبحانك يا كريم.mp3","13 - أيها الناس.mp3","14 - ربنا.mp3","15 - أيها الساهر.mp3","أتيت إليك يارب العباد.mp3","أدعوك في غمضي.mp3","إذا ماتلاقى الأفق بالأرض في المدى.mp3","أسعى لخلاقي.mp3"};
    private final String ruqyahUrl="https://archive.org/download/20240228_20240228_1849/041%20%2011%20%20%20%D8%A7%D9%84%D8%B1%D9%82%D9%8A%D8%A9%20%D8%A7%D9%84%D8%B4%D8%B1%D8%B9%D9%8A%D8%A9%20%D9%84%D8%B9%D9%84%D8%A7%D8%AC%20%D8%A7%D9%84%D8%B3%D8%AD%D8%B1%20%D8%A7%D9%84%D9%85%D8%B3%20%D8%A7%D9%84%D8%AD%D8%B3%D8%AF%20%D8%A8%D8%B5%D9%88%D8%AA%20%D8%A7%D9%84%D8%B4%D9%8A%D8%AE%20%D9%85%D8%AD%D9%85%D9%88%D8%AF%20%D8%AE%D9%84%D9%8A%D9%84%20%D8%A7%D9%84%D8%AD%D8%B5%D8%B1%D9%89%20%D8%B1%D8%AD%D9%85%D9%87%20%D8%A7%D9%84%D9%84%D9%87%20%D8%AC%D9%88%D8%AF%D8%A9%20%D8%B9%D8%A7%D9%84%D9%8A%D8%A9.mp3";
    private Spinner artistPicker,trackPicker;
    private TextView status;
    private String selectedKind="nasheed";

    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(15),dp(15),dp(25));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);setContentView(AppTabs.wrap(this,scroll,"more"));
        TextView title=label("الإنشاد والرقية الشرعية",24,Color.WHITE,true);title.setGravity(Gravity.CENTER);
        TextView sub=label("استماع من مشغّل Android في الخلفية",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(14),dp(18),dp(14),dp(18));hero.setBackground(bg(green,18));hero.addView(title);hero.addView(sub);root.addView(hero);
        TextView section=label("الابتهالات الدينية",18,green,true);section.setPadding(0,dp(16),0,dp(6));root.addView(section);
        artistPicker=new Spinner(this);artistPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,artists));root.addView(artistPicker);
        trackPicker=new Spinner(this);root.addView(trackPicker);
        artistPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){fillTracks(pos);}
        });
        Button play=new Button(this);play.setText("تشغيل الابتهال المختار");root.addView(play);
        play.setOnClickListener(v->playSelected());
        Button stop=new Button(this);stop.setText("إيقاف الصوت");root.addView(stop);
        stop.setOnClickListener(v->stopAudio());
        TextView ruqTitle=label("الرقية الشرعية — اختر القارئ",18,green,true);ruqTitle.setPadding(0,dp(16),0,dp(8));root.addView(ruqTitle);
        addRuqyahCard(root,"الشيخ محمود خليل الحصري",ruqyahUrl);
        addRuqyahCard(root,"الشيخ مشاري راشد العفاسي","https://ia601709.us.archive.org/13/items/e3fasi-Rokia/e3fasi-seher.mp3");
        addRuqyahCard(root,"الشيخ ناصر القطامي","https://ia600909.us.archive.org/4/items/alqtami-Roqiaa/alqtami-seher.mp3");
        addRuqyahCard(root,"الشيخ ياسر الدوسري","https://ia601705.us.archive.org/34/items/dosari-Roqia/dosari-seher.mp3");
        addRuqyahCard(root,"الشيخ علي الحذيفي","https://ia601808.us.archive.org/32/items/hothife-Roqia/hothife-seher.mp3");
        addRuqyahCard(root,"الشيخ هاني الرفاعي","https://ia601708.us.archive.org/29/items/rifa3i-Roqia/rifa3i-seher.mp3");
        status=label("اختر المنشد والتسجيل ثم اضغط تشغيل.",12,Color.DKGRAY,false);status.setPadding(0,dp(12),0,dp(4));root.addView(status);
    }
    private void addRuqyahCard(LinearLayout root,String title,String url){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(11),dp(14),dp(10));card.setBackground(bg(Color.WHITE,15));
        TextView name=label(title,16,green,true);card.addView(name);
        TextView detail=label("تسجيل رقية شرعية — تشغيل من مشغل التطبيق",12,Color.DKGRAY,false);card.addView(detail);
        Button play=new Button(this);play.setText("▶ تشغيل الرقية");card.addView(play);
        play.setOnClickListener(v->{selectedKind="ruqyah";startAudio(url,"الرقية الشرعية — "+title);});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(9);root.addView(card,lp);
    }
    private void fillTracks(int artist){
        ArrayList<String> names=new ArrayList<>();
        if(artist==0){for(String[] t:tobar)names.add(t[0]);}
        else names.addAll(Arrays.asList(naqshTitles));
        trackPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
    }
    private void playSelected(){
        int artist=artistPicker.getSelectedItemPosition(),index=trackPicker.getSelectedItemPosition();
        if(index<0)return;
        String url,title;
        if(artist==0){title=tobar[index][0];url=tobar[index][1];}
        else{
            title=naqshTitles[index];
            try{url="https://archive.org/download/qr_20201208/"+URLEncoder.encode(naqshFiles[index],"UTF-8").replace("+","%20");}
            catch(Exception e){status.setText("تعذر تجهيز رابط التسجيل.");return;}
        }
        selectedKind="nasheed";startAudio(url,title);
    }
    private void startAudio(String url,String title){
        Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(RadioPlaybackService.ACTION_PLAY);
        i.putExtra("url",url);i.putExtra("kind",selectedKind);i.putExtra("label",title);
        try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("تم إرسال طلب التشغيل: "+title+" — إذا لم يبدأ الصوت فتحقق من الاتصال بالمصدر.");}
        catch(Exception e){status.setText("تعذر بدء التشغيل: "+e.getMessage());}
    }
    private void stopAudio(){
        Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(RadioPlaybackService.ACTION_STOP);
        try{startService(i);status.setText("تم إرسال طلب إيقاف الصوت.");}catch(Exception e){status.setText("تعذر إيقاف الصوت: "+e.getMessage());}
    }
}