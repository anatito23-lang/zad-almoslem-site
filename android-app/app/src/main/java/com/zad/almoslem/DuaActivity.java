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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class DuaActivity extends Activity {
    private static final String[][] ITEMS = {
        {"دعاء ما بعد الأذان","الشيخ محمد متولي الشعراوي","https://archive.org/download/Islamic_Tape-258_uP_bY_mUSLEm/sharawy_doaa_24.mp3","sharawy_after_adhan"},
        {"الدعاء الأول","الشيخ السيد عبدالعزيز السعدني","https://www.ourquraan.com/audio/prayers/Alsayed.Abdalzez.Alzadany%201.mp3","alsayed_abdalzez_1"},
        {"الدعاء الثاني","الشيخ السيد عبدالعزيز السعدني","https://www.ourquraan.com/audio/prayers/Alsayed.Abdalzez.Alzadany%202.mp3","alsayed_abdalzez_2"},
        {"دعاء","الشيخ محمد الأسيوطي","https://www.ourquraan.com/audio/prayers/Muhammed.Alasyouty.mp3","muhammed_alasyouty"},
        {"دعاء الأنبياء","الشيخ صلاح الجمل","https://www.ourquraan.com/audio/prayers/Salah.Algamal%20-%20Doaa.Alanbiaa.mp3","salah_algamal"},
        {"دعاء ليلة القدر","الشيخ محمد جبريل","https://www.ourquraan.com/audio/qdr/Muahmmed.Jebreel.mp3","muhammad_jibril"},
        {"دعاء ختم القرآن","الشيخ هاني الرفاعي","https://www.ourquraan.com/audio/quraan-completion/Hany.Alrefaey.mp3","hany_alrefaey"},
        {"دعاء خاشع","الشيخ إدريس أبكر","https://www.ourquraan.com/audio/prayers/Edres.Abkar.mp3","edres_abkar"},
        {"دعاء","الشيخ خالد الجليل","https://www.ourquraan.com/audio/prayers/Khalid.Aljalil.mp3","khalid_aljalil"},
        {"دعاء","الشيخ ماهر المعيقلي","https://www.ourquraan.com/audio/prayers/Maher.M3equly.mp3","maher_m3equly"},
        {"دعاء آخر","الشيخ ماهر المعيقلي","https://www.ourquraan.com/audio/prayers/Maher.M3equly%202.mp3","maher_m3equly_2"},
        {"دعاء","الشيخ ناصر القطامي","https://www.ourquraan.com/audio/prayers/Naser.Alqutamy%201.mp3","naser_alqutamy"},
        {"دعاء","الشيخ ياسر الدوسري","https://www.ourquraan.com/audio/prayers/Yaser.Aldosy.mp3","yaser_aldosy"},
        {"الدعاء","القارئ أحمد البطل","https://www.ourquraan.com/audio/prayers/Ahmed.Albatal.mp3","ahmed_albatal"},
        {"دعاء","الشيخ مشاري راشد العفاسي","https://www.ourquraan.com/audio/prayers/Mashary.Rashed.mp3","mashary_rashed"}
    };
    private TextView status, selectedSheikh, nowPlaying;
    private LinearLayout root, tracksBox, playerCard;
    private Button pauseButton;
    private String selectedName = "";
    private int selectedIndex = -1;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void style(Button b,int color){b.setBackground(shape(color,13));b.setTextColor(Color.WHITE);b.setAllCaps(false);}
    @Override public void onCreate(Bundle b){super.onCreate(b);AppThemeColors.apply(this);build();}
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(15),dp(15),dp(26));
        root.setBackground(new IslamicWallpaperDrawable(this,"dua"));scroll.addView(root);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(12),dp(20),dp(12),dp(20));
        hero.setBackground(shape(0xEE173F34,22));
        TextView icon=label("🤲",38,Color.WHITE,true);icon.setGravity(Gravity.CENTER);hero.addView(icon);
        TextView title=label("مكتبة الدعاء",25,Color.WHITE,true);title.setGravity(Gravity.CENTER);hero.addView(title);
        TextView sub=label("اختر الشيخ أولًا، ثم اختر الدعاء الذي تريد سماعه",14,0xFFE8D6A9,false);sub.setGravity(Gravity.CENTER);hero.addView(sub);
        root.addView(hero,new LinearLayout.LayoutParams(-1,dp(160)));
        status=label("اختر أحد القراء لعرض الأدعية المتاحة بصوته.",13,Color.WHITE,false);status.setPadding(0,dp(14),0,dp(8));root.addView(status);
        root.addView(label("القراء",18,0xFFFFD98A,true),full());
        LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();
        for(String[] item:ITEMS)counts.put(item[1],counts.containsKey(item[1])?counts.get(item[1])+1:1);
        for(Map.Entry<String,Integer> entry:counts.entrySet()) addSheikh(entry.getKey(),entry.getValue());
        selectedSheikh=label("",17,0xFFFFD98A,true);selectedSheikh.setPadding(0,dp(16),0,dp(8));root.addView(selectedSheikh,full());
        tracksBox=new LinearLayout(this);tracksBox.setOrientation(LinearLayout.VERTICAL);root.addView(tracksBox,full());
        playerCard=new LinearLayout(this);playerCard.setOrientation(LinearLayout.VERTICAL);playerCard.setPadding(dp(14),dp(12),dp(14),dp(12));playerCard.setBackground(shape(0xF4FFFFFF,18));
        LinearLayout.LayoutParams pc=full();pc.topMargin=dp(16);root.addView(playerCard,pc);
        playerCard.addView(label("مشغل الدعاء",17,0xFF174436,true),full());
        nowPlaying=label("لم يتم اختيار دعاء بعد",13,0xFF53645B,false);nowPlaying.setPadding(0,dp(7),0,dp(8));playerCard.addView(nowPlaying,full());
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);
        pauseButton=new Button(this);pauseButton.setText("⏸ إيقاف مؤقت");style(pauseButton,0xFF245A45);
        Button stop=new Button(this);stop.setText("■ إيقاف");style(stop,0xFF8D453B);
        controls.addView(pauseButton,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(stop,new LinearLayout.LayoutParams(0,dp(48),1));playerCard.addView(controls,full());
        pauseButton.setOnClickListener(v->sendPlayback(RadioPlaybackService.ACTION_TOGGLE));
        stop.setOnClickListener(v->sendPlayback(RadioPlaybackService.ACTION_STOP));
        playerCard.setVisibility(View.GONE);
        setContentView(AppTabs.wrap(this,scroll,"dua","dua"));
    }
    private LinearLayout.LayoutParams full(){return new LinearLayout.LayoutParams(-1,-2);}
    private void addSheikh(String name,int count){
        Button b=new Button(this);b.setText("🎙  "+name+"   •   "+count+" "+(count==1?"دعاء":"أدعية"));
        style(b,0xFF245A45);LinearLayout.LayoutParams p=full();p.topMargin=dp(7);root.addView(b,p);
        b.setOnClickListener(v->{selectedName=name;selectedSheikh.setText("أدعية "+name);showTracks(name);});
    }
    private void showTracks(String sheikh){
        tracksBox.removeAllViews();int count=0;
        for(int i=0;i<ITEMS.length;i++)if(ITEMS[i][1].equals(sheikh)){addTrack(i);count++;}
        status.setText("اختر الدعاء المطلوب. يمكنك الاستماع أو تنزيله للاستماع دون إنترنت.");
        selectedSheikh.setVisibility(View.VISIBLE);tracksBox.setVisibility(View.VISIBLE);
    }
    private File fileFor(String key){
        if("sharawy_after_adhan".equals(key))return new File(new File(getFilesDir(),"reminders"),"dua.mp3");
        return new File(new File(getFilesDir(),"duas"),key+".mp3");
    }
    private void addTrack(int index){
        String[] item=ITEMS[index];File local=fileFor(item[3]);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(13),dp(11),dp(13),dp(11));card.setBackground(shape(0xF7FFFFFF,15));
        TextView name=label(item[0],16,0xFF173E32,true);card.addView(name);
        TextView reader=label(item[1],12,0xFF6A786E,false);reader.setPadding(0,dp(3),0,dp(7));card.addView(reader);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button play=new Button(this);play.setText("▶ استماع");style(play,0xFF245A45);
        Button download=new Button(this);download.setText(local.isFile()&&local.length()>10000?"✓ تم التنزيل":"⬇ تنزيل");style(download,0xFF9A773F);
        actions.addView(play,new LinearLayout.LayoutParams(0,dp(46),1));actions.addView(download,new LinearLayout.LayoutParams(0,dp(46),1));card.addView(actions);
        LinearLayout.LayoutParams p=full();p.topMargin=dp(7);tracksBox.addView(card,p);
        play.setOnClickListener(v->playOne(index));
        download.setOnClickListener(v->downloadItem(item,download));
    }
    private void playOne(int index){
        String[] item=ITEMS[index];File local=fileFor(item[3]);
        ArrayList<String> urls=new ArrayList<>(),titles=new ArrayList<>(),paths=new ArrayList<>();
        urls.add(item[2]);titles.add(item[0]+" — "+item[1]);paths.add(local.isFile()&&local.length()>10000?local.getAbsolutePath():"");
        Intent intent=new Intent(this,RadioPlaybackService.class);intent.setAction(RadioPlaybackService.ACTION_PLAY);
        intent.putExtra("url",item[2]);intent.putExtra("kind","dua");intent.putExtra("label",titles.get(0));
        intent.putStringArrayListExtra("queue_urls",urls);intent.putStringArrayListExtra("queue_titles",titles);intent.putStringArrayListExtra("queue_local_paths",paths);
        try{
            if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);
            selectedIndex=index;playerCard.setVisibility(View.VISIBLE);nowPlaying.setText(item[0]+"\n"+item[1]);
            pauseButton.setText("⏸ إيقاف مؤقت");status.setText("بدأ تشغيل الدعاء المختار. يمكنك الخروج وسيستمر الصوت في الخلفية.");
        }catch(Exception ex){status.setText("تعذر بدء التشغيل: "+ex.getMessage());}
    }
    private void sendPlayback(String action){
        Intent intent=new Intent(this,RadioPlaybackService.class);intent.setAction(action);
        try{if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);
            if(RadioPlaybackService.ACTION_STOP.equals(action)){playerCard.setVisibility(View.GONE);status.setText("تم إيقاف الدعاء.");}
            else {boolean paused=getSharedPreferences("zad_audio",MODE_PRIVATE).getBoolean("radio_paused",false);pauseButton.setText(paused?"▶ استئناف":"⏸ إيقاف مؤقت");}
        }catch(Exception e){status.setText("تعذر تنفيذ أمر المشغل: "+e.getMessage());}
    }
    @Override protected void onResume(){super.onResume();android.content.SharedPreferences p=getSharedPreferences("zad_audio",MODE_PRIVATE);
        if("dua".equals(p.getString("active_kind",""))&&p.getBoolean("radio_playing",false)){
            playerCard.setVisibility(View.VISIBLE);nowPlaying.setText(p.getString("radio_title","دعاء"));
            pauseButton.setText(p.getBoolean("radio_paused",false)?"▶ استئناف":"⏸ إيقاف مؤقت");
        }
    }
    private void downloadItem(String[] item,Button button){
        File target=fileFor(item[3]);if(target.isFile()&&target.length()>10000){button.setText("✓ تم التنزيل");return;}
        button.setEnabled(false);button.setText("جارٍ التنزيل...");
        new Thread(()->{
            HttpURLConnection connection=null;File temp=null;
            try{
                File dir=target.getParentFile();if(!dir.exists()&&!dir.mkdirs())throw new Exception("تعذر إنشاء مجلد التنزيلات");
                temp=new File(dir,target.getName()+".part");
                connection=(HttpURLConnection)new URL(item[2]).openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(90000);connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
                int code=connection.getResponseCode();if(code!=200)throw new Exception("مصدر الصوت غير متاح الآن (HTTP "+code+")");
                try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                    byte[] buffer=new byte[16384];int n;long got=0;while((n=in.read(buffer))!=-1){got+=n;if(got>100L*1024*1024)throw new Exception("حجم الملف أكبر من الحد المسموح");out.write(buffer,0,n);}out.getFD().sync();
                }
                if(temp.length()<10000)throw new Exception("الملف الصوتي غير مكتمل");
                try(FileInputStream in=new FileInputStream(temp)){byte[] h=new byte[3];int n=in.read(h);if(!(n>=3&&h[0]=='I'&&h[1]=='D'&&h[2]=='3')&&!(n>=2&&(h[0]&255)==255&&(h[1]&224)==224))throw new Exception("المصدر لم يُرجع ملف MP3 صالحًا");}
                if(target.exists()&&!target.delete())throw new Exception("تعذر استبدال الملف القديم");
                if(!temp.renameTo(target))throw new Exception("تعذر حفظ الملف على الهاتف");
                runOnUiThread(()->{button.setText("✓ تم التنزيل");status.setText("تم حفظ "+item[0]+" داخل التطبيق للاستماع دون إنترنت.");});
            }catch(Exception e){runOnUiThread(()->{button.setText("إعادة المحاولة");status.setText("فشل تنزيل "+item[0]+": "+(e.getMessage()==null?"خطأ غير معروف":e.getMessage()));});}
            finally{if(connection!=null)connection.disconnect();if(temp!=null&&temp.exists())temp.delete();runOnUiThread(()->button.setEnabled(true));}
        }).start();
    }
}
