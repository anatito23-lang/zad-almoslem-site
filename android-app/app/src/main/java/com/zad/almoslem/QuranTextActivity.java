package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class QuranTextActivity extends Activity {
    private final String[] names={"الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"};
    private static final String[] RECITER_IDS={"ar.abdulbasitmurattal","ar.husary"};
    private static final String[] RECITER_NAMES={"الشيخ عبدالباسط عبدالصمد","الشيخ محمود خليل الحصري"};
    private int green=Color.rgb(8,48,40), pageNumber=0, surahNumber=1, currentIndex=-1;
    private boolean busy=false, pageMode=false, autoAdvance=false;
    private Spinner reciterPicker;
    private TextView status, heading;
    private LinearLayout verses;
    private final ArrayList<JSONObject> ayahs=new ArrayList<>();
    private MediaPlayer player;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);
        pageMode=getIntent().hasExtra("page_number");
        pageNumber=getIntent().getIntExtra("page_number",1);
        surahNumber=Math.max(1,Math.min(114,getIntent().getIntExtra("surah_number",1)));
        build();
        loadContent();
    }
    private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void build(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(14),dp(14),dp(28));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);
        heading=label(pageMode?"القراءة والتصحيح — صفحة "+pageNumber:"القراءة والتصحيح",22,green,true);heading.setGravity(Gravity.CENTER);root.addView(heading);
        TextView hint=label("اختر القارئ، ثم اضغط على الآية لسماعها. بعد انتهاء الآية ينتقل التشغيل تلقائيًا إلى الآية التالية. اضغط الآية مرة أخرى لإعادتها.",13,Color.DKGRAY,false);hint.setPadding(0,dp(10),0,dp(10));root.addView(hint);
        root.addView(label("القارئ",13,green,true));
        reciterPicker=new Spinner(this);
        reciterPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,RECITER_NAMES));
        root.addView(reciterPicker);
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);
        Button stop=new Button(this);stop.setText("إيقاف");
        Button repeat=new Button(this);repeat.setText("إعادة الآية");
        controls.addView(repeat,new LinearLayout.LayoutParams(0,dp(44),1));
        controls.addView(stop,new LinearLayout.LayoutParams(0,dp(44),1));root.addView(controls);
        stop.setOnClickListener(v->stopAudio());
        repeat.setOnClickListener(v->{if(currentIndex>=0)playAyah(currentIndex,false);});
        status=label("جارٍ تحميل الآيات...",12,Color.DKGRAY,false);status.setPadding(0,dp(8),0,dp(8));root.addView(status);
        verses=new LinearLayout(this);verses.setOrientation(LinearLayout.VERTICAL);root.addView(verses);
        setContentView(AppTabs.wrap(this,scroll,"mushaf"));
    }
    private void loadContent(){
        if(busy)return;busy=true;status.setText("جارٍ تحميل نص الآيات...");
        String endpoint=pageMode?"page/"+Math.max(1,Math.min(604,pageNumber)):"surah/"+surahNumber;
        new Thread(()->{
            HttpURLConnection c=null;
            try{
                c=(HttpURLConnection)new URL("https://api.alquran.cloud/v1/"+endpoint+"/quran-uthmani").openConnection();
                c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","ZadAlmoslem-Android");
                if(c.getResponseCode()!=200)throw new Exception("تعذر الاتصال بمصدر نص القرآن");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
                JSONObject response=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
                if(response.optInt("code")!=200)throw new Exception("لم يصل نص الآيات بصورة صحيحة");
                JSONArray data=response.getJSONObject("data").getJSONArray("ayahs");
                ArrayList<JSONObject> result=new ArrayList<>();
                for(int i=0;i<data.length();i++){JSONObject a=data.optJSONObject(i);if(a!=null&&!a.optString("text","").trim().isEmpty())result.add(a);}
                if(result.isEmpty())throw new Exception("لا توجد آيات في هذا الموضع");
                runOnUiThread(()->{ayahs.clear();ayahs.addAll(result);renderVerses();status.setText("اضغط على أي آية لسماعها، وسيكمل التشغيل إلى الآية التالية تلقائيًا.");});
            }catch(Exception e){runOnUiThread(()->status.setText("تعذر تحميل الآيات: "+(e.getMessage()==null?"تحقق من الإنترنت":e.getMessage())));}
            finally{if(c!=null)c.disconnect();runOnUiThread(()->busy=false);}
        }).start();
    }
    private void renderVerses(){
        verses.removeAllViews();
        for(int i=0;i<ayahs.size();i++){
            final int index=i;JSONObject a=ayahs.get(i);
            int surah=a.optJSONObject("surah")==null?surahNumber:a.optJSONObject("surah").optInt("number",surahNumber);
            int local=a.optInt("numberInSurah",i+1);
            Button verse=new Button(this);
            verse.setAllCaps(false);verse.setTextDirection(View.TEXT_DIRECTION_RTL);verse.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            verse.setText(a.optString("text","")+"  ۞ "+local);
            verse.setTextSize(20);verse.setPadding(dp(12),dp(12),dp(12),dp(12));
            verse.setBackground(bg(Color.WHITE,12));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(7);verses.addView(verse,lp);
            verse.setOnClickListener(v->{currentIndex=index;playAyah(index,true);});
        }
    }
    private void playAyah(int index,boolean continueAfter){
        if(index<0||index>=ayahs.size())return;
        stopAudio();currentIndex=index;autoAdvance=continueAfter;
        JSONObject ayah=ayahs.get(index);
        int global=ayah.optInt("number",0);
        if(global<1){status.setText("تعذر تحديد رقم الآية الصوتي.");return;}
        String edition=RECITER_IDS[Math.max(0,Math.min(RECITER_IDS.length-1,reciterPicker.getSelectedItemPosition()))];
        String audioUrl="https://cdn.islamic.network/quran/audio/"+("ar.abdulbasitmurattal".equals(edition)?"192":"128")+"/"+edition+"/"+global+".mp3";
        status.setText("جارٍ تشغيل الآية "+ayah.optInt("numberInSurah",index+1)+" بصوت "+RECITER_NAMES[reciterPicker.getSelectedItemPosition()]);
        try{
            player=new MediaPlayer();
            player.setOnPreparedListener(mp->{if(player==mp){mp.start();}});
            player.setOnCompletionListener(mp->{if(player==mp){mp.release();player=null;if(autoAdvance&&currentIndex+1<ayahs.size())playAyah(currentIndex+1,true);else status.setText("انتهى تشغيل الآيات المعروضة.");}});
            player.setOnErrorListener((mp,what,extra)->{if(player==mp){mp.release();player=null;}status.setText("تعذر تشغيل هذه الآية. جرّب الاتصال أو اختر القارئ الآخر.");return true;});
            player.setDataSource(audioUrl);player.prepareAsync();
        }catch(Exception e){stopAudio();status.setText("تعذر بدء صوت الآية: "+(e.getMessage()==null?"خطأ في الصوت":e.getMessage()));}
    }
    private void stopAudio(){autoAdvance=false;if(player!=null){try{player.setOnCompletionListener(null);player.stop();}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}player=null;}}
    @Override protected void onStop(){stopAudio();super.onStop();}
    @Override protected void onDestroy(){stopAudio();super.onDestroy();}
}