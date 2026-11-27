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
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

public class AudioLibraryActivity extends Activity {
    private final int green=Color.rgb(8,48,40);
    private static final String[] artists={"نصر الدين طوبار","سيد النقشبندي"};
    // Direct MP3 download endpoints from the Sheikh's recording catalogue.
    private static final String[][] tobar={
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
    private static final String[] naqshTitles={"مولاي إني ببابك","لحظات يزهى بهن الزمان","الله يا الله","نفسي يا رب","يا رب كرمك علينا","رباه ارفع مقتك عنا","في حبك ربي نتفانى","أقول أمتي","الكون صلاة وخشوع","ندخل بيتك","دعاء السائل المحروم","يا من يعلم","سبحانك يا كريم","أيها الناس","ربنا","أيها الساهر","أتيت إليك يا رب العباد","أدعوك في غمضي","إذا ما تلاقى الأفق بالأرض في المدى","أسعى لخلاقي"};
    private static final String[] naqshFiles={"01 - مولاي.mp3","02 - لحظات يزهى بهن الزمان -ابتهال للشيخ سيد النقشبندى.mp3","03 - الله يا الله.mp3","04 - نفسي يا رب.mp3","05 - يا رب كرمك علينا.mp3","06 - رباه ارفع مقتك عنا.mp3","07 - في حبك ربي نتفاني.mp3","08 - أقول امتي.mp3","09 - الكون صلاة وخشوع.mp3","10 - ندخل بيتك.mp3","11 - دعاء السائل المحروم.mp3","12 - يا من يعلم.mp3","12 - سبحانك يا كريم.mp3","13 - أيها الناس.mp3","14 - ربنا.mp3","15 - أيها الساهر.mp3","أتيت إليك يارب العباد.mp3","أدعوك في غمضي.mp3","إذا ماتلاقى الأفق بالأرض في المدى.mp3","أسعى لخلاقي.mp3"};
    public static int getArtistCount(){return artists.length;}
    public static String getArtistName(int artist){return artists[Math.max(0,Math.min(artists.length-1,artist))];}
    public static int getTrackCount(int artist){return artist==0?tobar.length:naqshTitles.length;}
    public static String getTrackName(int artist,int track){
        if(artist==0)return tobar[track][0];
        return naqshTitles[track];
    }
    public static String getTrackUrl(int artist,int track){
        if(artist==0)return tobar[track][1];
        try{return "https://archive.org/download/qr_20201208/"+URLEncoder.encode(naqshFiles[track],"UTF-8").replace("+","%20");}
        catch(Exception e){return "";}
    }
    private final String ruqyahUrl="https://archive.org/download/20240228_20240228_1849/041%20%2011%20%20%20%D8%A7%D9%84%D8%B1%D9%82%D9%8A%D8%A9%20%D8%A7%D9%84%D8%B4%D8%B1%D8%B9%D9%8A%D8%A9%20%D9%84%D8%B9%D9%84%D8%A7%D8%AC%20%D8%A7%D9%84%D8%B3%D8%AD%D8%B1%20%D8%A7%D9%84%D9%85%D8%B3%20%D8%A7%D9%84%D8%AD%D8%B3%D8%AF%20%D8%A8%D8%B5%D9%88%D8%AA%20%D8%A7%D9%84%D8%B4%D9%8A%D8%AE%20%D9%85%D8%AD%D9%85%D9%88%D8%AF%20%D8%AE%D9%84%D9%8A%D9%84%20%D8%A7%D9%84%D8%AD%D8%B5%D8%B1%D9%89%20%D8%B1%D8%AD%D9%85%D9%87%20%D8%A7%D9%84%D9%84%D9%87%20%D8%AC%D9%88%D8%AF%D8%A9%20%D8%B9%D8%A7%D9%84%D9%8A%D8%A9.mp3";
    private Spinner artistPicker,trackPicker;
    private TextView status;
    private final ArrayList<String> naqshDynamicFiles=new ArrayList<>();
    private final ArrayList<String> naqshDynamicTitles=new ArrayList<>();
    private boolean naqshCatalogLoading=false;
    private String selectedKind="nasheed";

    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();loadNaqshbandiCatalog();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackground(new IslamicWallpaperDrawable(this,"recordings"));LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(15),dp(15),dp(25));root.setBackground(new IslamicWallpaperDrawable(this,"recordings"));scroll.addView(root);setContentView(AppTabs.wrap(this,scroll,"more","recordings"));
        TextView title=label("الابتهالات والإنشاد الديني",24,Color.WHITE,true);title.setGravity(Gravity.CENTER);
        TextView sub=label("استماع من مشغّل Android في الخلفية",13,Color.rgb(222,239,229),false);sub.setGravity(Gravity.CENTER);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(14),dp(18),dp(14),dp(18));hero.setBackground(new IslamicWallpaperDrawable(this,"recordings"));hero.addView(title);hero.addView(sub);root.addView(hero);
        TextView section=label("الابتهالات الدينية",18,green,true);section.setPadding(0,dp(16),0,dp(6));root.addView(section);
        artistPicker=new Spinner(this);artistPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,artists));root.addView(artistPicker);
        trackPicker=new Spinner(this);root.addView(trackPicker);
        artistPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){fillTracks(pos);if(pos==1&&naqshDynamicFiles.isEmpty()&&!naqshCatalogLoading)loadNaqshbandiCatalog();}
        });
        fillTracks(0);
        Button play=new Button(this);play.setText("تشغيل الابتهال المختار");root.addView(play);
        play.setOnClickListener(v->playSelected());
        Button stop=new Button(this);stop.setText("■ إيقاف الصوت");root.addView(stop);
        stop.setOnClickListener(v->stopAudio());
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
    private int activeTrackCount(int artist){
        if(artist==0)return tobar.length;
        return naqshDynamicFiles.isEmpty()?naqshTitles.length:naqshDynamicFiles.size();
    }
    private String activeTrackName(int artist,int track){
        if(artist==0)return tobar[track][0];
        return naqshDynamicFiles.isEmpty()?naqshTitles[track]:naqshDynamicTitles.get(track);
    }
    private String activeTrackUrl(int artist,int track){
        if(artist==0)return tobar[track][1];
        String file=naqshDynamicFiles.isEmpty()?naqshFiles[track]:naqshDynamicFiles.get(track);
        try{return "https://archive.org/download/qr_20201208/"+URLEncoder.encode(file,"UTF-8").replace("+","%20");}
        catch(Exception e){return "";}
    }
    private void fillTracks(int artist){
        ArrayList<String> names=new ArrayList<>();
        if(artist==0){for(String[] t:tobar)names.add(t[0]);}
        else if(!naqshDynamicTitles.isEmpty())names.addAll(naqshDynamicTitles);
        else names.addAll(Arrays.asList(naqshTitles));
        trackPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        if(artist==1&&naqshCatalogLoading&&status!=null)status.setText("جارٍ تحميل قائمة التسجيلات الصحيحة للشيخ سيد النقشبندي...");
    }
    private void loadNaqshbandiCatalog(){
        if(naqshCatalogLoading||!naqshDynamicFiles.isEmpty())return;
        naqshCatalogLoading=true;
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL("https://archive.org/metadata/qr_20201208").openConnection();
                connection.setConnectTimeout(15000);connection.setReadTimeout(25000);
                connection.setRequestProperty("User-Agent","ZadAlmoslem-Android");
                if(connection.getResponseCode()!=200)throw new Exception("مصدر التسجيلات غير متاح");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=connection.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
                org.json.JSONArray files=new org.json.JSONObject(out.toString("UTF-8")).optJSONArray("files");
                if(files==null)throw new Exception("لم تصل قائمة التسجيلات");
                ArrayList<String> actualFiles=new ArrayList<>();
                for(int i=0;i<files.length();i++){
                    org.json.JSONObject item=files.optJSONObject(i);if(item==null)continue;
                    String name=item.optString("name","");
                    if(name.toLowerCase(Locale.ROOT).endsWith(".mp3"))actualFiles.add(name);
                }
                if(actualFiles.isEmpty())throw new Exception("لا توجد ملفات MP3 في المصدر");
                Collections.sort(actualFiles,String.CASE_INSENSITIVE_ORDER);
                ArrayList<String> actualTitles=new ArrayList<>();
                for(String name:actualFiles){
                    String title=name.substring(0,name.length()-4).replace('_',' ').replaceAll("\\s+"," ").trim();
                    actualTitles.add(title);
                }
                runOnUiThread(()->{
                    naqshDynamicFiles.clear();naqshDynamicFiles.addAll(actualFiles);
                    naqshDynamicTitles.clear();naqshDynamicTitles.addAll(actualTitles);
                    if(artistPicker!=null&&artistPicker.getSelectedItemPosition()==1){fillTracks(1);status.setText("تم تحميل "+naqshDynamicFiles.size()+" ابتهالًا للشيخ سيد النقشبندي.");}
                    naqshCatalogLoading=false;
                });
            }catch(Exception error){
                runOnUiThread(()->{naqshCatalogLoading=false;if(artistPicker!=null&&artistPicker.getSelectedItemPosition()==1&&status!=null)status.setText("تعذر تحميل قائمة النقشبندي من المصدر. تحقق من الإنترنت ثم أعد المحاولة.");});
            }finally{if(connection!=null)connection.disconnect();}
        }).start();
    }
    private void playSelected(){
        int artist=artistPicker.getSelectedItemPosition(),index=trackPicker.getSelectedItemPosition();
        if(artist<0||index<0||index>=activeTrackCount(artist)){
            status.setText("اختر الشيخ والابتهال أولًا.");
            return;
        }
        ArrayList<String> queueUrls=new ArrayList<>();
        ArrayList<String> queueTitles=new ArrayList<>();
        for(int track=index;track<activeTrackCount(artist);track++){
            String url=activeTrackUrl(artist,track);
            if(url==null||url.trim().isEmpty()){
                status.setText("تعذر تجهيز رابط الابتهال: "+activeTrackName(artist,track));
                return;
            }
            queueUrls.add(url);
            queueTitles.add(activeTrackName(artist,track));
        }
        selectedKind="nasheed";
        startAudio(queueUrls.get(0),queueTitles.get(0),queueUrls,queueTitles);
    }
    private void startAudio(String url,String title){
        startAudio(url,title,null,null);
    }
    private void startAudio(String url,String title,ArrayList<String> queueUrls,ArrayList<String> queueTitles){
        Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(RadioPlaybackService.ACTION_PLAY);
        i.putExtra("url",url);i.putExtra("kind",selectedKind);i.putExtra("label",title);
        if(queueUrls!=null&&!queueUrls.isEmpty()){
            i.putStringArrayListExtra("queue_urls",queueUrls);
            i.putStringArrayListExtra("queue_titles",queueTitles);
        }
        try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("بدأ طلب تشغيل "+title+"، وسيعمل التالي تلقائيًا من قائمة الشيخ نفسه.");}
        catch(Exception e){status.setText("تعذر بدء التشغيل: "+e.getMessage());}
    }
    private void stopAudio(){
        Intent i=new Intent(this,RadioPlaybackService.class);i.setAction(RadioPlaybackService.ACTION_STOP);
        try{startService(i);status.setText("تم إرسال طلب إيقاف الصوت.");}catch(Exception e){status.setText("تعذر إيقاف الصوت: "+e.getMessage());}
    }
}