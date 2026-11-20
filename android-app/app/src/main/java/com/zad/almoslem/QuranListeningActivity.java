package com.zad.almoslem;
import android.app.Activity;import android.content.Intent;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.os.Build;import android.os.Bundle;import android.view.Gravity;import android.view.View;import android.widget.*;import java.io.*;import java.net.HttpURLConnection;import java.net.URL;import java.util.Locale;import java.util.ArrayList;
public class QuranListeningActivity extends Activity{
private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
private TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.RIGHT);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(-1,-2);}
private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
private void addCard(LinearLayout root,View card){if(card instanceof LinearLayout){card.setBackground(shape(Color.WHITE,16));card.setPadding(dp(14),dp(12),dp(14),dp(12));}LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(10);root.addView(card,lp);}
@Override public void onCreate(Bundle b){super.onCreate(b);AppThemeColors.apply(this);build();}
private void build(){ScrollView scroll=new ScrollView(this);LinearLayout root=column();root.setPadding(dp(16),dp(16),dp(16),dp(24));root.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);
TextView title=text("استماع القرآن الكريم",24,AppThemeColors.text(this),true);title.setGravity(Gravity.CENTER);root.addView(title);
TextView intro=text("اختر القارئ والسورة، واستمع مباشرة أو نزّلها للاستماع دون إنترنت. هذه الصفحة للتلاوات فقط وليست إذاعة مباشرة.",14,AppThemeColors.muted(this),false);intro.setPadding(0,dp(8),0,dp(16));root.addView(intro);
buildPlayer(root);setContentView(AppTabs.wrap(this,scroll,"quran"));}
    private void buildPlayer(LinearLayout root) {
        TextView section=text("استماع القرآن الكريم",18,Color.rgb(20,55,43),true);
        section.setPadding(0,dp(18),0,dp(8));root.addView(section);
        LinearLayout card=column();card.setBackground(shape(Color.WHITE,16));card.setPadding(dp(14),dp(12),dp(14),dp(12));
        card.addView(text("اختر القارئ والسورة، وسيعمل الصوت في مشغل Android بالخلفية دون فتح الموقع.",13,Color.DKGRAY,false));
        Spinner reciterSpinner=new Spinner(this),surahSpinner=new Spinner(this);
        reciterSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"جارٍ تحميل القراء..."}));
        String[] surahs={"الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"};
        surahSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,surahs));
        card.addView(text("القارئ",12,Color.DKGRAY,true));card.addView(reciterSpinner,matchWrap());
        card.addView(text("السورة",12,Color.DKGRAY,true));card.addView(surahSpinner,matchWrap());
        Button play=new Button(this);play.setText("استمع الآن");card.addView(play,matchWrap());
        Button stop=new Button(this);stop.setText("■ إيقاف التلاوة");card.addView(stop,matchWrap());
        stop.setOnClickListener(v->{Intent stopIntent=new Intent(this,RadioPlaybackService.class);stopIntent.setAction(RadioPlaybackService.ACTION_STOP);try{startService(stopIntent);Toast.makeText(this,"تم إيقاف التلاوة",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"تعذر إيقاف التلاوة",Toast.LENGTH_SHORT).show();}});
        Button download=new Button(this);download.setText("تنزيل السورة للاستماع دون إنترنت");card.addView(download,matchWrap());
        ProgressBar quranProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);quranProgress.setMax(100);card.addView(quranProgress,matchWrap());
        TextView state=text("جارٍ تحميل قائمة القراء من مكتبة التسجيلات.",12,Color.rgb(80,100,90),false);card.addView(state);
        addCard(root,card);
        final ArrayList<org.json.JSONObject> reciters=new ArrayList<>();
        final ArrayList<String> reciterNames=new ArrayList<>();
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL("https://mp3quran.net/api/v3/reciters?language=ar").openConnection();
                connection.setConnectTimeout(15000);connection.setReadTimeout(20000);
                if(connection.getResponseCode()!=200)throw new Exception("تعذر تحميل قائمة القراء");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=connection.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
                org.json.JSONArray array=new org.json.JSONObject(out.toString("UTF-8")).getJSONArray("reciters");
                for(int i=0;i<array.length();i++){org.json.JSONObject item=array.getJSONObject(i);if(item.has("moshaf")&&item.getJSONArray("moshaf").length()>0){reciters.add(item);reciterNames.add(item.optString("name","قارئ"));}}
                runOnUiThread(()->{
                    if(reciterNames.isEmpty()){state.setText("لا توجد قائمة قراء متاحة حاليًا.");return;}
                    reciterSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,reciterNames));
                    state.setText("تم تحميل "+reciterNames.size()+" قارئًا. اختر القارئ والسورة.");
                });
            }catch(Exception e){runOnUiThread(()->state.setText("تعذر تحميل القراء. تحقق من الإنترنت ثم أعد فتح التطبيق."));}
            finally{if(connection!=null)connection.disconnect();}
        }).start();
        play.setOnClickListener(v->{
            int ri=reciterSpinner.getSelectedItemPosition(),si=surahSpinner.getSelectedItemPosition()+1;
            if(ri<0||ri>=reciters.size()){state.setText("انتظر حتى تكتمل قائمة القراء.");return;}
            try{
                org.json.JSONObject reciter=reciters.get(ri);
                String audioUrl=resolveQuranAudioUrl(reciter,si);
                if(audioUrl==null){state.setText("السورة غير متاحة لهذا القارئ. اختر قارئًا آخر.");return;}
                String label="سورة "+surahs[si-1]+" — "+reciter.optString("name","القارئ");
                getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putString("last_quran_url",audioUrl).putString("last_quran_title",label).putBoolean("radio_playing",true).putString("active_kind","quran").apply();
                Intent intent=new Intent(this,RadioPlaybackService.class);intent.setAction(RadioPlaybackService.ACTION_PLAY);
                intent.putExtra("url",audioUrl);intent.putExtra("kind","quran");intent.putExtra("label",label);
                File offline=new File(new File(getFilesDir(),"quran"),"reciter_"+ri+"_surah_"+si+".mp3");
                if(offline.isFile()&&offline.length()>10000)intent.putExtra("local_path",offline.getAbsolutePath());
                if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);
                state.setText("يتم تشغيل "+label+" في الخلفية.");
            }catch(Exception e){state.setText("تعذر بدء التلاوة: "+e.getMessage());}
        });
        download.setOnClickListener(v->{
            int ri=reciterSpinner.getSelectedItemPosition(),si=surahSpinner.getSelectedItemPosition()+1;
            if(ri<0||ri>=reciters.size()){state.setText("انتظر حتى تكتمل قائمة القراء.");return;}
            try{
                String url=resolveQuranAudioUrl(reciters.get(ri),si);
                if(url==null){state.setText("السورة غير متاحة لهذا القارئ.");return;}
                String label="سورة "+surahs[si-1]+" — "+reciterNames.get(ri);
                download.setEnabled(false);quranProgress.setProgress(0);state.setText("جارٍ تنزيل "+label+"...");
                new Thread(()->{
                    HttpURLConnection connection=null;File temp=null;
                    try{
                        File dir=new File(getFilesDir(),"quran");if(!dir.exists()&&!dir.mkdirs())throw new Exception("تعذر إنشاء مجلد القرآن");
                        File target=new File(dir,"reciter_"+ri+"_surah_"+si+".mp3");temp=new File(dir,target.getName()+".part");
                        connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(20000);connection.setReadTimeout(90000);connection.setInstanceFollowRedirects(true);
                        if(connection.getResponseCode()!=200)throw new Exception("مصدر التلاوة غير متاح");
                        long total=connection.getContentLength(),got=0;
                        try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                            byte[] buffer=new byte[16384];int n;
                            while((n=in.read(buffer))!=-1){got+=n;if(got>100L*1024*1024)throw new Exception("حجم الملف تجاوز الحد المسموح");out.write(buffer,0,n);
                                final long current=got,expected=total;
                                runOnUiThread(()->quranProgress.setProgress(expected>0?(int)Math.min(99,current*100/expected):0));
                            }out.getFD().sync();
                        }
                        if(temp.length()<10000)throw new Exception("ملف التلاوة غير مكتمل");
                        try(FileInputStream in=new FileInputStream(temp)){byte[] h=new byte[3];int n=in.read(h);if(!(n>=3&&h[0]=='I'&&h[1]=='D'&&h[2]=='3')&&!(n>=2&&(h[0]&255)==255&&(h[1]&224)==224))throw new Exception("الملف الذي وصل ليس MP3 صالحًا");}
                        if(target.exists()&&!target.delete())throw new Exception("تعذر استبدال الملف السابق");
                        if(!temp.renameTo(target))throw new Exception("تعذر حفظ الملف داخل التطبيق");
                        runOnUiThread(()->{quranProgress.setProgress(100);state.setText("تم تنزيل "+label+" وحفظه داخل التطبيق. سيعمل دون إنترنت.");});
                    }catch(Exception e){runOnUiThread(()->state.setText("فشل التنزيل: "+(e.getMessage()==null?"خطأ غير معروف":e.getMessage())));}
                    finally{if(connection!=null)connection.disconnect();if(temp!=null&&temp.exists())temp.delete();runOnUiThread(()->download.setEnabled(true));}
                }).start();
            }catch(Exception e){state.setText("تعذر تجهيز التنزيل: "+e.getMessage());}
        });
    }

    private String resolveQuranAudioUrl(org.json.JSONObject reciter,int surahId) throws Exception {
        org.json.JSONArray moshafs=reciter.getJSONArray("moshaf");
        for(int i=0;i<moshafs.length();i++){
            org.json.JSONObject m=moshafs.getJSONObject(i);String list=m.optString("surah_list","");
            boolean includes=false;
            for(String id:list.split(",")){try{if(Integer.parseInt(id.trim())==surahId){includes=true;break;}}catch(Exception ignored){}}
            String server=m.optString("server","");
            if(includes&&!server.isEmpty()){if(!server.endsWith("/"))server+="/";return server+String.format(Locale.US,"%03d.mp3",surahId);}
        }
        return null;
    }





}