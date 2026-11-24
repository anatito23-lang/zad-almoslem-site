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
    private TextView status;
    private ProgressBar progress;
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
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(15),dp(15),dp(15),dp(26));
        root.setBackground(new IslamicWallpaperDrawable(this,"dua"));scroll.addView(root);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(12),dp(20),dp(12),dp(20));
        hero.setBackground(new IslamicWallpaperDrawable(this,"dua"));
        TextView icon=label("🤲",38,Color.WHITE,true);icon.setGravity(Gravity.CENTER);hero.addView(icon);
        TextView title=label("مكتبة الدعاء",25,Color.WHITE,true);title.setGravity(Gravity.CENTER);hero.addView(title);
        TextView sub=label("أدعية خاشعة بأصوات نخبة من القراء",14,0xFFE8D6A9,false);sub.setGravity(Gravity.CENTER);hero.addView(sub);
        root.addView(hero,new LinearLayout.LayoutParams(-1,dp(170)));
        TextView intro=label("اختر الدعاء والقارئ، واستمع داخل التطبيق. يمكنك تنزيل أي تسجيل وحفظه على هاتفك للاستماع إليه دون اتصال بالإنترنت.",13,Color.WHITE,false);
        intro.setPadding(dp(5),dp(14),dp(5),dp(14));root.addView(intro);
        for(int i=0;i<ITEMS.length;i++)addTrack(root,i);
        status=label("جاهز للاستماع.",12,Color.WHITE,false);status.setPadding(dp(4),dp(8),dp(4),dp(8));root.addView(status);
        setContentView(AppTabs.wrap(this,scroll,"dua","dua"));
    }
    private void addTrack(LinearLayout root,int index){
        String[] item=ITEMS[index];File local=fileFor(item[3]);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(13),dp(12),dp(13),dp(12));
        card.setBackground(shape(0xF7FFFFFF,16));
        TextView name=label(item[0],16,0xFF173E32,true);card.addView(name);
        TextView reader=label(item[1],12,0xFF6A786E,false);reader.setPadding(0,dp(3),0,dp(8));card.addView(reader);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setGravity(Gravity.CENTER_VERTICAL);
        Button play=new Button(this);play.setText(local.isFile()&&local.length()>10000?"▶ تشغيل دون إنترنت":"▶ استماع");style(play,0xFF245A45);
        Button download=new Button(this);download.setText(local.isFile()&&local.length()>10000?"✓ تم التنزيل":"⬇ تنزيل");style(download,0xFF9A773F);
        actions.addView(play,new LinearLayout.LayoutParams(0,dp(46),1f));actions.addView(download,new LinearLayout.LayoutParams(0,dp(46),1f));
        card.addView(actions);
        play.setOnClickListener(v->playQueue(index));
        download.setOnClickListener(v->downloadItem(item,download));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);root.addView(card,lp);
    }
    private File fileFor(String key){
        if("sharawy_after_adhan".equals(key))return new File(new File(getFilesDir(),"reminders"),"dua.mp3");
        File dir=new File(getFilesDir(),"duas");return new File(dir,key+".mp3");
    }
    private void playQueue(int startIndex){
        java.util.ArrayList<String> urls=new java.util.ArrayList<>();
        java.util.ArrayList<String> titles=new java.util.ArrayList<>();
        java.util.ArrayList<String> localPaths=new java.util.ArrayList<>();
        for(int i=Math.max(0,startIndex);i<ITEMS.length;i++){
            String[] item=ITEMS[i];File local=fileFor(item[3]);
            urls.add(item[2]);titles.add(item[0]+" — "+item[1]);
            localPaths.add(local.isFile()&&local.length()>10000?local.getAbsolutePath():"");
        }
        if(urls.isEmpty())return;
        Intent intent=new Intent(this,RadioPlaybackService.class);intent.setAction(RadioPlaybackService.ACTION_PLAY);
        intent.putExtra("url",urls.get(0));intent.putExtra("kind","dua");intent.putExtra("label",titles.get(0));
        intent.putStringArrayListExtra("queue_urls",urls);intent.putStringArrayListExtra("queue_titles",titles);intent.putStringArrayListExtra("queue_local_paths",localPaths);
        try{if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);if(status!=null)status.setText("بدأت قائمة الأدعية. سينتقل التشغيل تلقائيًا إلى الدعاء التالي.");}
        catch(Exception ex){if(status!=null)status.setText("تعذر بدء التشغيل: "+ex.getMessage());}
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
                long total=connection.getContentLengthLong(),got=0;
                try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                    byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1){got+=n;if(got>100L*1024*1024)throw new Exception("حجم الملف أكبر من الحد المسموح");out.write(buffer,0,n);}out.getFD().sync();
                }
                if(temp.length()<10000)throw new Exception("الملف الصوتي غير مكتمل");
                try(FileInputStream in=new FileInputStream(temp)){byte[] h=new byte[3];int n=in.read(h);if(!(n>=3&&h[0]=='I'&&h[1]=='D'&&h[2]=='3')&&!(n>=2&&(h[0]&255)==255&&(h[1]&224)==224))throw new Exception("المصدر لم يُرجع ملف MP3 صالحًا");}
                if(target.exists()&&!target.delete())throw new Exception("تعذر استبدال الملف القديم");
                if(!temp.renameTo(target))throw new Exception("تعذر حفظ الملف على الهاتف");
                runOnUiThread(()->{button.setText("✓ تم التنزيل");if(status!=null)status.setText("تم حفظ "+item[0]+" داخل التطبيق، ويمكن تشغيله دون إنترنت.");});
            }catch(Exception e){runOnUiThread(()->{button.setText("إعادة المحاولة");if(status!=null)status.setText("فشل تنزيل "+item[0]+": "+(e.getMessage()==null?"خطأ غير معروف":e.getMessage()));});}
            finally{if(connection!=null)connection.disconnect();if(temp!=null&&temp.exists())temp.delete();runOnUiThread(()->button.setEnabled(true));}
        }).start();
    }
}
