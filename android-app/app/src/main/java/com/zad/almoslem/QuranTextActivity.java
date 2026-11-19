package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

public class QuranTextActivity extends Activity {
    private final String[] names={"الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"};
    private int green=Color.rgb(8,48,40);
    private Spinner surahPicker;
    private Button openButton,saveButton;
    private TextView status,content;
    private boolean busy=false;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void build(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(28));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);setContentView(scroll);
        TextView title=label("المصحف الشريف",25,Color.WHITE,true);title.setGravity(Gravity.CENTER);
        TextView sub=label("قراءة القرآن الكريم وحفظ السور على الهاتف",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(16),dp(18),dp(16),dp(18));hero.setBackground(bg(green,18));hero.addView(title);hero.addView(sub);root.addView(hero);
        TextView choose=label("اختر السورة",15,green,true);choose.setPadding(0,dp(16),0,dp(5));root.addView(choose);
        surahPicker=new Spinner(this);surahPicker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));root.addView(surahPicker);
        openButton=new Button(this);openButton.setText("فتح السورة وقراءتها");root.addView(openButton);
        saveButton=new Button(this);saveButton.setText("حفظ السورة للقراءة دون إنترنت");root.addView(saveButton);
        status=label("اختر سورة ثم افتحها. يمكن حفظ السورة لتعمل لاحقًا دون اتصال.",13,Color.DKGRAY,false);status.setPadding(0,dp(10),0,dp(10));root.addView(status);
        content=label("",22,Color.rgb(45,55,45),false);content.setLineSpacing(dp(8),1.15f);content.setPadding(dp(4),dp(12),dp(4),dp(25));root.addView(content);
        openButton.setOnClickListener(v->loadSelected(false));saveButton.setOnClickListener(v->loadSelected(true));
        surahPicker.setSelection(0);
        showCached(1);
    }
    private File cacheFile(int n){File dir=new File(getFilesDir(),"mushaf");if(!dir.exists())dir.mkdirs();return new File(dir,"surah_"+n+".json");}
    private void loadSelected(boolean save){
        if(busy)return;int n=surahPicker.getSelectedItemPosition()+1;File cached=cacheFile(n);
        if(cached.isFile()&&cached.length()>100){readAndShow(cached,n,save?"السورة محفوظة على الهاتف وتعمل دون إنترنت.":"تم فتح السورة من الذاكرة المحلية.");return;}
        fetch(n,save);
    }
    private void showCached(int n){File f=cacheFile(n);if(f.isFile()&&f.length()>100)readAndShow(f,n,"يمكنك قراءة السورة المحفوظة دون إنترنت.");}
    private void fetch(int n,boolean save){
        busy=true;openButton.setEnabled(false);saveButton.setEnabled(false);status.setText("جارٍ جلب سورة "+names[n-1]+"...");
        new Thread(()->{
            HttpURLConnection c=null;File temp=new File(cacheFile(n).getParentFile(),"surah_"+n+".part");
            try{
                c=(HttpURLConnection)new URL("https://api.alquran.cloud/v1/surah/"+n+"/quran-uthmani").openConnection();
                c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","ZadAlmoslem-Android");
                int code=c.getResponseCode();if(code!=200)throw new Exception("تعذر الاتصال بمصدر المصحف ("+code+")");
                ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int k;while((k=in.read(b))!=-1)out.write(b,0,k);}
                String json=new String(out.toByteArray(),StandardCharsets.UTF_8);JSONObject root=new JSONObject(json);if(root.optInt("code")!=200)throw new Exception("لم يصل نص السورة بصورة صحيحة");
                JSONObject data=root.getJSONObject("data");JSONArray ayahs=data.getJSONArray("ayahs");if(ayahs.length()==0)throw new Exception("السورة لا تحتوي على آيات");
                if(save){try(FileOutputStream fos=new FileOutputStream(temp)){fos.write(json.getBytes(StandardCharsets.UTF_8));fos.getFD().sync();}File target=cacheFile(n);if(target.exists())target.delete();if(!temp.renameTo(target))throw new Exception("تعذر حفظ السورة على الهاتف");}
                runOnUiThread(()->{render(data,n);status.setText(save?"تم حفظ السورة داخل التطبيق، ويمكن قراءتها دون إنترنت.":"تم فتح السورة. اضغط حفظ إذا أردت قراءتها دون إنترنت.");});
            }catch(Exception e){runOnUiThread(()->status.setText("تعذر تحميل السورة: "+(e.getMessage()==null?"تحقق من الإنترنت":e.getMessage())));}
            finally{if(c!=null)c.disconnect();if(temp.exists())temp.delete();runOnUiThread(()->{busy=false;openButton.setEnabled(true);saveButton.setEnabled(true);});}
        }).start();
    }
    private void readAndShow(File f,int n,String message){
        try(FileInputStream in=new FileInputStream(f)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int k;while((k=in.read(b))!=-1)out.write(b,0,k);JSONObject root=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));render(root.getJSONObject("data"),n);status.setText(message);}
        catch(Exception e){status.setText("تعذر قراءة النسخة المحفوظة؛ أعد تنزيل السورة.");}
    }
    private void render(JSONObject data,int n){
        StringBuilder text=new StringBuilder();JSONArray ayahs=data.optJSONArray("ayahs");if(ayahs!=null)for(int i=0;i<ayahs.length();i++){JSONObject a=ayahs.optJSONObject(i);if(a!=null)text.append(a.optString("text","")).append(" ﴿").append(a.optInt("numberInSurah",i+1)).append("﴾  ");}
        content.setText("سُورَةُ "+names[n-1]+"\n\n"+text.toString());
        content.setTypeface(Typeface.create("serif",Typeface.NORMAL));
    }
}
