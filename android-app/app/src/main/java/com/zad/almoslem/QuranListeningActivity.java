package com.zad.almoslem;
import android.app.Activity;import android.content.Intent;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.os.Build;import android.os.Bundle;import android.view.Gravity;import android.view.View;import android.widget.*;import java.io.*;import java.net.HttpURLConnection;import java.net.URL;import java.util.Locale;import java.util.ArrayList;
public class QuranListeningActivity extends Activity{
private boolean mujawwadOnly=false;
private int reciterGroup=0;
private final ArrayList<org.json.JSONObject> allReciters=new ArrayList<>();
private final ArrayList<org.json.JSONObject> reciters=new ArrayList<>();
private final ArrayList<String> reciterNames=new ArrayList<>();
private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
private TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.RIGHT);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(-1,-2);}
private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
private void addCard(LinearLayout root,View card){if(card instanceof LinearLayout){card.setBackground(shape(AppThemeColors.surface(this),16));card.setPadding(dp(14),dp(12),dp(14),dp(12));}LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(10);root.addView(card,lp);}
@Override public void onCreate(Bundle b){super.onCreate(b);AppThemeColors.apply(this);build();}
private void build(){ScrollView scroll=new ScrollView(this);LinearLayout root=column();root.setPadding(dp(16),dp(16),dp(16),dp(24));root.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);
TextView title=text("استماع القرآن الكريم",24,AppThemeColors.text(this),true);title.setGravity(Gravity.CENTER);root.addView(title);
TextView intro=text("اختر القارئ والسورة، واستمع مباشرة أو نزّلها للاستماع دون إنترنت. هذه الصفحة للتلاوات فقط وليست إذاعة مباشرة.",14,AppThemeColors.muted(this),false);intro.setPadding(0,dp(8),0,dp(16));root.addView(intro);
buildPlayer(root);setContentView(AppTabs.wrap(this,scroll,"quran"));}
    private void buildPlayer(LinearLayout root) {
        TextView section=text("استماع القرآن الكريم",18,AppThemeColors.primary(this),true);
        section.setPadding(0,dp(18),0,dp(8));root.addView(section);
        LinearLayout card=column();card.setBackground(shape(Color.WHITE,16));card.setPadding(dp(14),dp(12),dp(14),dp(12));
        card.addView(text("اختر القارئ والسورة، وسيعمل الصوت في مشغل Android بالخلفية دون فتح الموقع.",13,AppThemeColors.muted(this),false));
        Spinner categorySpinner=new Spinner(this),reciterSpinner=new Spinner(this),surahSpinner=new Spinner(this);
        categorySpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"كل القراء من مختلف الدول","القراء المصريون القدامى","القراء المصريون المعاصرون","القرآن المجوّد — المصريون فقط"}));
        card.addView(text("القسم",12,AppThemeColors.text(this),true));card.addView(categorySpinner,matchWrap());
        reciterSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"جارٍ تحميل القراء..."}));
        String[] surahs={"الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"};
        surahSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,surahs));
        card.addView(text("القارئ",12,AppThemeColors.text(this),true));card.addView(reciterSpinner,matchWrap());
        card.addView(text("السورة",12,AppThemeColors.text(this),true));card.addView(surahSpinner,matchWrap());
        Button play=new Button(this);play.setText("استمع الآن");card.addView(play,matchWrap());
        Button stop=new Button(this);stop.setText("■ إيقاف التلاوة");card.addView(stop,matchWrap());
        stop.setOnClickListener(v->{Intent stopIntent=new Intent(this,RadioPlaybackService.class);stopIntent.setAction(RadioPlaybackService.ACTION_STOP);try{startService(stopIntent);Toast.makeText(this,"تم إيقاف التلاوة",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"تعذر إيقاف التلاوة",Toast.LENGTH_SHORT).show();}});
        Button download=new Button(this);download.setText("تنزيل السورة للاستماع دون إنترنت");card.addView(download,matchWrap());
        ProgressBar quranProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);quranProgress.setMax(100);card.addView(quranProgress,matchWrap());
        TextView state=text("جارٍ تحميل قائمة القراء من مكتبة التسجيلات.",12,Color.rgb(80,100,90),false);card.addView(state);
        TextView extraTitle=text("مصادر إضافية لتلاوات القرّاء",16,AppThemeColors.primary(this),true);
        extraTitle.setPadding(0,dp(18),0,dp(6));card.addView(extraTitle);
        TextView extraNote=text("بعض القراء الذين طلبتهم غير موجودين في مكتبة MP3Quran الحالية؛ أضفنا مصادر موثقة بدرجة التوفر ونوع التسجيل. الروابط تفتح صفحة المصدر، ولا تُعد تشغيلًا مدمجًا داخل المشغل.",12,AppThemeColors.muted(this),false);
        extraNote.setPadding(0,0,0,dp(8));card.addView(extraNote);
        addExternalSourceButton(card,"السيد متولي عبد العال — 36 سورة مجوّدة بحفص","https://etilawa.com/reciters/seidmetwallyabdelaal/");
        addExternalSourceButton(card,"محمد الليثي — الموقع الرسمي وتسجيلات صافور والاستوديو","https://www.sheikhleithy.com/");
        addExternalSourceButton(card,"محمد الليثي — أرشيف التلاوات والحفلات","https://alkabbah.com/scholar/2462/mohmd-allythy/recitations");
        addExternalSourceButton(card,"السيد سعيد — تسجيلات الشيخ","https://www.nourelquran.com/swars-168.html");
        addExternalSourceButton(card,"السيد سعيد — مصحف حفص وتلاوات مختارة","https://way2quran.com/ar/reciters/alsaid-saeed");
        addExternalSourceButton(card,"الشحات محمد أنور — مصحف مرتّل (114 سورة)","https://etilawa.com/reciters/al-shahat-muhammad-anwar/");
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){reciterGroup=pos;mujawwadOnly=pos==3;applyReciterFilter(reciterSpinner,state);}});
        addCard(root,card);
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL("https://mp3quran.net/api/v3/reciters?language=ar").openConnection();
                connection.setConnectTimeout(15000);connection.setReadTimeout(20000);
                if(connection.getResponseCode()!=200)throw new Exception("تعذر تحميل قائمة القراء");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=connection.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
                org.json.JSONArray array=new org.json.JSONObject(out.toString("UTF-8")).getJSONArray("reciters");
                for(int i=0;i<array.length();i++){org.json.JSONObject item=array.getJSONObject(i);if(item.has("moshaf")&&item.getJSONArray("moshaf").length()>0)allReciters.add(item);}
                runOnUiThread(()->{
                    applyReciterFilter(reciterSpinner,state);
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
                String reciterName=reciter.optString("name","القارئ");
                String label="سورة "+surahs[si-1]+" — "+reciterName;
                org.json.JSONObject selectedMoshaf=findMoshaf(reciter,si);
                if(selectedMoshaf==null){state.setText("تعذر تحديد مسار التلاوة لهذا القارئ.");return;}
                getSharedPreferences("zad_audio",MODE_PRIVATE).edit()
                    .putString("last_quran_url",audioUrl).putString("last_quran_title",label)
                    .putString("last_quran_server",selectedMoshaf.optString("server",""))
                    .putString("last_quran_surah_list",selectedMoshaf.optString("surah_list",""))
                    .putString("last_quran_reciter",reciterName).putInt("last_quran_surah_id",si)
                    .putBoolean("radio_playing",true).putString("active_kind","quran").apply();
                QuranHomeWidget.refreshAll(this);
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


    private void addExternalSourceButton(LinearLayout parent,String label,String url){
        Button button=new Button(this);button.setText(label);button.setAllCaps(false);
        button.setOnClickListener(v->{
            try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}
            catch(Exception e){Toast.makeText(this,"تعذر فتح مصدر التلاوة",Toast.LENGTH_SHORT).show();}
        });
        parent.addView(button,matchWrap());
    }

    private boolean isMujawwadMoshaf(org.json.JSONObject m){
        if(m==null)return false;
        String n=m.optString("name","").toLowerCase(Locale.ROOT);
        return n.contains("مجود")||n.contains("mujawwad")||n.contains("mujawad");
    }
    private String normalizeReciterName(String name){
        return (name==null?"":name).replace(" ","").replace("أ","ا").replace("إ","ا").replace("آ","ا").replace("ى","ي").replace("ـ","");
    }
    private boolean isEgyptianReciter(String name,boolean old){
        String n=normalizeReciterName(name);
        String[] oldNames={"عبدالباسطعبدالصمد","محمدصديقالمنشاوي","محمودخليلالحصري","محمودعليالبنا","مصطفيإسماعيل","مصطفىاسماعيل","مصطفىاللاهونى","محمدالطبلاوي","محمودالطبلاوي","عليحجاجالسويسي","احمدنعينع","محمدرفعت","كاملوسيفالبهتيمي","طهالفشني","عبدالفتاحالشعشاعي","راغبمصطفىغلوش" ,"محمودالشيمي","حمديالزامل","محمدحصان","محمدعمران","محمدالطوخي","محمداحمدشبيب","محمدعبدالعزيزحصان"};
        String[] modernNames={"محمدجبريل","حاتمفريدالواعر","ياسرسلامة","اسلامصبحي","احمدعامر","احمدعيسىالمعصراوي","محمودعبدالحكم","صابرعبدالحكم","حسنصالح","عبداللهكامل","محمودالرفاعي","احمدخليلشاهين","سيدرمضان","السيدسعيد","سيدسعد","السيدمتولي","السيدمتوليعبدالعال","سيدمتولي","محمدالليثي","محمدليثي","محمودالشحات","محمودالشحاتانور","الشحاتمحمدانور"};
        String[] names=old?oldNames:modernNames;
        for(String candidate:names)if(n.contains(normalizeReciterName(candidate)))return true;
        return false;
    }
    private void applyReciterFilter(Spinner spinner,TextView state){
        reciters.clear();reciterNames.clear();
        for(org.json.JSONObject item:allReciters){
            String name=item.optString("name","قارئ");
            boolean include;
            if(reciterGroup==0)include=true;
            else if(reciterGroup==1)include=isEgyptianReciter(name,true);
            else if(reciterGroup==2)include=isEgyptianReciter(name,false);
            else include=isEgyptianReciter(name,true)||isEgyptianReciter(name,false);
            if(!include)continue;
            if(mujawwadOnly){
                boolean has=false;org.json.JSONArray m=item.optJSONArray("moshaf");
                if(m!=null)for(int j=0;j<m.length();j++)if(isMujawwadMoshaf(m.optJSONObject(j))){has=true;break;}
                if(!has)continue;
            }
            reciters.add(item);reciterNames.add(name);
        }
        spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,reciterNames));
        String description=reciterGroup==0?"قارئًا من مختلف الدول":reciterGroup==3?"قارئًا مصريًا للتلاوات المجوّدة":"قارئًا مصريًا";
        state.setText(reciterNames.isEmpty()?(mujawwadOnly?"لا توجد تلاوات مجوّدة مصرية متاحة في مصدر المكتبة حاليًا.":"لا توجد تسجيلات متاحة لهذا القسم حاليًا."):"تم تحميل "+reciterNames.size()+" "+description+". اختر القارئ والسورة.");
    }

    private org.json.JSONObject findMoshaf(org.json.JSONObject reciter,int surahId) throws Exception {
        org.json.JSONArray moshafs=reciter.getJSONArray("moshaf");
        for(int i=0;i<moshafs.length();i++){
            org.json.JSONObject m=moshafs.getJSONObject(i);
            if(mujawwadOnly&&!isMujawwadMoshaf(m))continue;
            boolean includes=false;
            for(String id:m.optString("surah_list","").split(",")){
                try{if(Integer.parseInt(id.trim())==surahId){includes=true;break;}}catch(Exception ignored){}
            }
            if(includes&&!m.optString("server","").isEmpty())return m;
        }
        return null;
    }

    private String resolveQuranAudioUrl(org.json.JSONObject reciter,int surahId) throws Exception {
        org.json.JSONArray moshafs=reciter.getJSONArray("moshaf");
        for(int i=0;i<moshafs.length();i++){
            org.json.JSONObject m=moshafs.getJSONObject(i);
            if(mujawwadOnly&&!isMujawwadMoshaf(m))continue;
            String list=m.optString("surah_list","");
            boolean includes=false;
            for(String id:list.split(",")){try{if(Integer.parseInt(id.trim())==surahId){includes=true;break;}}catch(Exception ignored){}}
            String server=m.optString("server","");
            if(includes&&!server.isEmpty()){if(!server.endsWith("/"))server+="/";return server+String.format(Locale.US,"%03d.mp3",surahId);}
        }
        return null;
    }





}