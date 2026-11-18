package com.zad.almoslem;

import android.content.ComponentName;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.Build;

public class RadioTileService extends TileService {
    @Override public void onStartListening() { super.onStartListening(); refresh(); }
    private void refresh() {
        boolean playing=getSharedPreferences("zad_audio",MODE_PRIVATE).getBoolean("radio_playing",false)
            && !"quran".equals(getSharedPreferences("zad_audio",MODE_PRIVATE).getString("active_kind",""));
        getQsTile().setState(playing?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);
        getQsTile().setLabel(playing?"إيقاف الراديو":"تشغيل الراديو");
        getQsTile().updateTile();
    }
    @Override public void onClick() {
        super.onClick();
        android.content.SharedPreferences p=getSharedPreferences("zad_audio",MODE_PRIVATE);
        boolean playing=p.getBoolean("radio_playing",false)&&!"quran".equals(p.getString("active_kind",""));
        Intent i=new Intent(this,RadioPlaybackService.class);
        if(playing) i.setAction(RadioPlaybackService.ACTION_STOP);
        else {
            String stream=p.getString("radio_stream","main");
            String url="backup".equals(stream)?"https://qurango.net/radio/tarateel":"https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8";
            i.setAction(RadioPlaybackService.ACTION_PLAY); i.putExtra("url",url); i.putExtra("label",p.getString("radio_label","إذاعة القرآن الكريم")); i.putExtra("kind","radio");
        }
        if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
        refresh();
    }
}
