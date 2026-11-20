package com.zad.almoslem;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.Build;
import java.io.File;

public class QuranTileService extends TileService {
    @Override public void onStartListening() { super.onStartListening(); refresh(); }
    private void refresh() {
        android.content.SharedPreferences p=getSharedPreferences("zad_audio",MODE_PRIVATE);
        boolean playing=p.getBoolean("radio_playing",false)&&"quran".equals(p.getString("active_kind",""));
        getQsTile().setState(playing?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);
        getQsTile().setLabel(playing?"إيقاف التلاوة":"تشغيل آخر تلاوة");
        getQsTile().updateTile();
    }
    @Override public void onClick() {
        super.onClick();
        android.content.SharedPreferences p=getSharedPreferences("zad_audio",MODE_PRIVATE);
        boolean playing=p.getBoolean("radio_playing",false)&&"quran".equals(p.getString("active_kind",""));
        Intent i=new Intent(this,RadioPlaybackService.class);
        if(playing) { p.edit().putBoolean("radio_playing",false).putString("active_kind","").apply(); i.setAction(RadioPlaybackService.ACTION_STOP); }
        else {
            String url=p.getString("last_quran_url","");
            if(url.isEmpty()) { startActivityAndCollapse(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return; }
            p.edit().putBoolean("radio_playing",true).putString("active_kind","quran").apply();
            i.setAction(RadioPlaybackService.ACTION_PLAY); i.putExtra("url",url); i.putExtra("label",p.getString("last_quran_title","تلاوة القرآن الكريم")); i.putExtra("kind","quran");
            String storageKey=p.getString("last_quran_storage_key","");
            int surahId=p.getInt("last_quran_surah_id",0);
            if(!storageKey.isEmpty()&&surahId>=1&&surahId<=114){
                File offline=new File(new File(getFilesDir(),"quran"),"reciter_"+storageKey+"_surah_"+surahId+".mp3");
                if(offline.isFile()&&offline.length()>10000)i.putExtra("local_path",offline.getAbsolutePath());
            }
        }
        if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        refresh();
    }
}
