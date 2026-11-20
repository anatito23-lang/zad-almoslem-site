package com.zad.almoslem;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.Locale;

public class QuranHomeWidget extends AppWidgetProvider {
    public static final String ACTION_TOGGLE="com.zad.almoslem.WIDGET_QURAN_TOGGLE";
    public static final String ACTION_NEXT="com.zad.almoslem.WIDGET_QURAN_NEXT";
    public static final String ACTION_PREVIOUS="com.zad.almoslem.WIDGET_QURAN_PREVIOUS";
    private static final String[] SURAHS={"","الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"};

    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
        for(int id:ids)updateOne(context,manager,id);
    }
    public static void refreshAll(Context context){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        ComponentName component=new ComponentName(context,QuranHomeWidget.class);
        int[] ids=manager.getAppWidgetIds(component);
        for(int id:ids)updateOne(context,manager,id);
    }
    private static void updateOne(Context context,AppWidgetManager manager,int id){
        SharedPreferences audio=context.getSharedPreferences("zad_audio",Context.MODE_PRIVATE);
        boolean playing=audio.getBoolean("radio_playing",false)&&"quran".equals(audio.getString("active_kind",""));
        String title=audio.getString("last_quran_title","اختر تلاوة من مكتبة القرآن");
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.quran_widget);
        views.setTextViewText(R.id.quran_widget_title,title);
        views.setTextViewText(R.id.quran_widget_status,playing?"التلاوة تعمل الآن":"مشغل القرآن");
        views.setTextViewText(R.id.quran_widget_toggle,playing?"إيقاف":"تشغيل");
        views.setOnClickPendingIntent(R.id.quran_widget_toggle,pending(context,ACTION_TOGGLE,8101));
        views.setOnClickPendingIntent(R.id.quran_widget_previous,pending(context,ACTION_PREVIOUS,8102));
        views.setOnClickPendingIntent(R.id.quran_widget_next,pending(context,ACTION_NEXT,8103));
        Intent open=new Intent(context,QuranListeningActivity.class);
        PendingIntent openPending=PendingIntent.getActivity(context,8104,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.quran_widget_open,openPending);
        manager.updateAppWidget(id,views);
    }
    private static PendingIntent pending(Context c,String action,int code){
        Intent i=new Intent(c,QuranHomeWidget.class);i.setAction(action);
        return PendingIntent.getBroadcast(c,code,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    @Override public void onReceive(Context context,Intent intent){
        super.onReceive(context,intent);
        if(intent==null)return;
        if(ACTION_TOGGLE.equals(intent.getAction())){
            SharedPreferences p=context.getSharedPreferences("zad_audio",Context.MODE_PRIVATE);
            boolean playing=p.getBoolean("radio_playing",false)&&"quran".equals(p.getString("active_kind",""));
            if(playing){sendStop(context);}
            else playSaved(context,p);
        }else if(ACTION_NEXT.equals(intent.getAction()))changeSurah(context,1);
        else if(ACTION_PREVIOUS.equals(intent.getAction()))changeSurah(context,-1);
        refreshAll(context);
    }
    private void sendStop(Context context){
        Intent stop=new Intent(context,RadioPlaybackService.class);stop.setAction(RadioPlaybackService.ACTION_STOP);
        try{context.startService(stop);}catch(Exception ignored){}
    }
    private void playSaved(Context context,SharedPreferences p){
        String url=p.getString("last_quran_url","");
        if(url.isEmpty()){Intent open=new Intent(context,QuranListeningActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(open);return;}
        String title=p.getString("last_quran_title","تلاوة القرآن الكريم");
        startTrack(context,url,title,offlineTrackPath(context,p,p.getInt("last_quran_surah_id",0)));
    }
    private void changeSurah(Context context,int direction){
        SharedPreferences p=context.getSharedPreferences("zad_audio",Context.MODE_PRIVATE);
        String server=p.getString("last_quran_server",""),list=p.getString("last_quran_surah_list","");
        if(server.isEmpty()||list.isEmpty()){playSaved(context,p);return;}
        ArrayList<Integer> ids=new ArrayList<>();
        for(String part:list.split(",")){try{int id=Integer.parseInt(part.trim());if(id>=1&&id<=114&&!ids.contains(id))ids.add(id);}catch(Exception ignored){}}
        java.util.Collections.sort(ids);
        if(ids.isEmpty()){playSaved(context,p);return;}
        int current=p.getInt("last_quran_surah_id",ids.get(0)),at=ids.indexOf(current);
        if(at<0)at=0;
        int next=Math.max(0,Math.min(ids.size()-1,at+direction));
        int surah=ids.get(next);
        String url=server+(server.endsWith("/")?"":"/")+String.format(Locale.US,"%03d.mp3",surah);
        String reciter=p.getString("last_quran_reciter","");
        String title="سورة "+SURAHS[surah]+" — "+reciter;
        p.edit().putInt("last_quran_surah_id",surah).putString("last_quran_url",url).putString("last_quran_title",title).putBoolean("radio_playing",true).putString("active_kind","quran").apply();
        startTrack(context,url,title,offlineTrackPath(context,p,surah));
    }
    private String offlineTrackPath(Context context,SharedPreferences p,int surahId){
        String key=p.getString("last_quran_storage_key","");
        if(key.isEmpty()||surahId<1||surahId>114)return null;
        try{
            java.io.File dir=new java.io.File(context.getFilesDir(),"quran");
            java.io.File file=new java.io.File(dir,"reciter_"+key+"_surah_"+surahId+".mp3").getCanonicalFile();
            java.io.File canonicalDir=dir.getCanonicalFile();
            if(file.isFile()&&file.length()>10000&&file.getPath().startsWith(canonicalDir.getPath()+java.io.File.separator))
                return file.getAbsolutePath();
        }catch(Exception ignored){}
        return null;
    }
    private void startTrack(Context context,String url,String title){
        SharedPreferences p=context.getSharedPreferences("zad_audio",Context.MODE_PRIVATE);
        startTrack(context,url,title,offlineTrackPath(context,p,p.getInt("last_quran_surah_id",0)));
    }
    private void startTrack(Context context,String url,String title,String localPath){
        Intent play=new Intent(context,RadioPlaybackService.class);
        play.setAction(RadioPlaybackService.ACTION_PLAY);play.putExtra("url",url);play.putExtra("kind","quran");play.putExtra("label",title);
        if(localPath!=null)play.putExtra("local_path",localPath);
        try{if(Build.VERSION.SDK_INT>=26)context.startForegroundService(play);else context.startService(play);}catch(Exception ignored){}
    }
}
