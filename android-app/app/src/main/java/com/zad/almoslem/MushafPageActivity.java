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

public class MushafPageActivity extends Activity {
    private int page=1;private boolean busy=false;private TextView pageTitle,status,content;private Button previous,next,save;
    private final int green=Color.rgb(8,48,40);
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);page=getIntent().getIntExtra("page",1);build();showPage(page);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));d.setStroke(dp(1),Color.rgb(190,170,120));return d;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setTextDirection(View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return t;}
    private void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(14),dp(14),dp(24));root.setBackgroundColor(Color.rgb(244,247,243));AppTabs.add(this,root,"mushaf");scroll.addView(root);setContentView(scroll);
        TextView title=label("المصحف الشريف — صفحات",24,Color.WHITE,true);title.setPadding(dp(12),dp(20),dp(12),dp(20));title.setBackground(bg(green,16));root.addView(title);
        pageTitle=label("",16,green,true);pageTitle.setPadding(0,dp(10),0,dp(4));root.addView(pageTitle);
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
        previous=new Button(this);previous.setText("الصفحة السابقة");next=new Button(this);next.setText("الصفحة التالية");
        nav.addView(previous,new LinearLayout.LayoutParams(0,dp(48),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(nav);
        previous.setOnClickListener(v->{if(page>1)showPage(page-1);});next.setOnClickListener(v->{if(page<604)showPage(page+1);});
        SeekBar seek=new SeekBar(this);seek.setMax(603);seek.setProgress(page-1);root.addView(seek);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){if(user)pageTitle.setText("الصفحة "+(p+1)+" من 604");}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){showPage(b.getProgress()+1);}});
        save=new Button(this);save.setText("حفظ الصفحة للقراءة دون إنترنت");root.addView(save);save.setOnClickListener(v->load(page,true));
        status=label("جارٍ تجهيز المصحف...",12,Color.DKGRAY,false);root.addView(status);
        LinearLayout pageFrame=new LinearLayout(this);pageFrame.setOrientation(LinearLayout.VERTICAL);pageFrame.setPadding(dp(18),dp(22),dp(18),dp(22));pageFrame.setBackground(bg(Color.rgb(255,253,242),8));
        content=label("",22,Color.rgb(45,45,38),false);content.setGravity(Gravity.CENTER);content.setLineSpacing(dp(13),1.12f);pageFrame.addView(content,new LinearLayout.LayoutParams(-1,-2));LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.topMargin=dp(10);root.addView(pageFrame,fp);
        Button back=new Button(this);back.setText("العودة للأقسام");root.addView(back);back.setOnClickListener(v->finish());
    }
    private File file(int n){File d=new File(getFilesDir(),"mushaf_pages");if(!d.exists())d.mkdirs();return new File(d,"page_"+n+".json");}
    private void showPage(int n){if(n<1||n>604)return;page=n;pageTitle.setText("الصفحة "+page+" من 604");previous.setEnabled(page>1);next.setEnabled(page<604);load(page,false);}
    private void load(int n,boolean saveToDisk){
        if(busy)return;File f=file(n);if(f.isFile()&&f.length()>100){read(f,n,saveToDisk?"الصفحة محفوظة على الهاتف وتعمل دون إنترنت.":"تم فتح الصفحة المحفوظة.");return;}
        busy=true;save.setEnabled(false);status.setText("جارٍ تحميل صفحة المصحف "+n+"...");
        new Thread(()->{HttpURLConnection c=null;File temp=new File(f.getParentFile(),"page_"+n+".part");try{
            c=(HttpURLConnection)new URL("https://api.alquran.cloud/v1/page/"+n+"/quran-uthmani").openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","ZadAlmoslem Android");
            if(c.getResponseCode()!=200)throw new Exception("تعذر الاتصال بمصدر المصحف");
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int k;while((k=in.read(b))!=-1)out.write(b,0,k);}
            String json=new String(out.toByteArray(),StandardCharsets.UTF_8);JSONObject root=new JSONObject(json);if(root.optInt("code")!=200)throw new Exception("بيانات الصفحة غير مكتملة");
            JSONObject data=root.getJSONObject("data");JSONArray ayahs=data.getJSONArray("ayahs");if(ayahs.length()==0)throw new Exception("الصفحة فارغة");
            if(saveToDisk){try(FileOutputStream fos=new FileOutputStream(temp)){fos.write(json.getBytes(StandardCharsets.UTF_8));fos.getFD().sync();}if(f.exists())f.delete();if(!temp.renameTo(f))throw new Exception("تعذر حفظ الصفحة");}
            runOnUiThread(()->{render(ayahs,n);status.setText(saveToDisk?"تم حفظ الصفحة دون إنترنت.":"يمكنك حفظ الصفحة لتعمل دون إنترنت.");});
        }catch(Exception e){runOnUiThread(()->status.setText("تعذر فتح الصفحة: "+(e.getMessage()==null?"تحقق من الإنترنت":e.getMessage())));}
        finally{if(c!=null)c.disconnect();if(temp.exists())temp.delete();runOnUiThread(()->{busy=false;save.setEnabled(true);});}}).start();
    }
    private void read(File f,int n,String message){try(FileInputStream in=new FileInputStream(f)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int k;while((k=in.read(b))!=-1)out.write(b,0,k);JSONObject root=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));render(root.getJSONObject("data").getJSONArray("ayahs"),n);status.setText(message);}catch(Exception e){status.setText("تعذر قراءة الصفحة المحفوظة.");}}
    private void render(JSONArray ayahs,int n){StringBuilder text=new StringBuilder();String lastSurah="";for(int i=0;i<ayahs.length();i++){JSONObject a=ayahs.optJSONObject(i);if(a==null)continue;JSONObject surah=a.optJSONObject("surah");String name=surah==null?"":surah.optString("name","");if(!name.equals(lastSurah)){text.append("\n۞ ").append(name).append(" ۞\n");lastSurah=name;}text.append(a.optString("text","")).append(" ﴿").append(a.optInt("numberInSurah",0)).append("﴾  ");}content.setText(text.toString());content.setTypeface(Typeface.create("serif",Typeface.NORMAL));pageTitle.setText("الصفحة "+n+" من 604");}
}