package com.zad.almoslem;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.Build;

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
        if(playing) i.setAction(RadioPlaybackService.ACTION_STOP);
        else {
            String url=p.getString("last_quran_url","");
            if(url.isEmpty()) { startActivityAndCollapse(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return; }
            i.setAction(RadioPlaybackService.ACTION_PLAY); i.putExtra("url",url); i.putExtra("label",p.getString("last_quran_title","تلاوة القرآن الكريم")); i.putExtra("kind","quran");
        }
        if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        refresh();
    }
}
