package com.zad.almoslem;

import android.app.*;
import android.content.*;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import android.net.Uri;
import android.os.*;
import java.io.IOException;
import java.io.File;

public class RadioPlaybackService extends Service {
    public static final String ACTION_PLAY="com.zad.almoslem.PLAY_RADIO";
    public static final String ACTION_STOP="com.zad.almoslem.STOP_RADIO";
    private static final String CHANNEL="zad_radio_playback";
    private static final int NOTIFICATION=7410;
    private ExoPlayer player;

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && ACTION_STOP.equals(intent.getAction())) { stopRadio(); return START_NOT_STICKY; }
        createChannel();
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent content=PendingIntent.getActivity(this,7411,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,RadioPlaybackService.class); stop.setAction(ACTION_STOP);
        PendingIntent stopAction=PendingIntent.getService(this,7412,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("إذاعة القرآن الكريم")
          .setContentText("البث مستمر في الخلفية").setContentIntent(content).setOngoing(true)
          .addAction(android.R.drawable.ic_media_pause,"إيقاف الإذاعة",stopAction);
        Notification n=b.build();
        if(Build.VERSION.SDK_INT>=29) startForeground(NOTIFICATION,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NOTIFICATION,n);
        String url=intent==null?null:intent.getStringExtra("url");
        String kind=intent==null?"radio":intent.getStringExtra("kind");
        boolean allowedRadio="https://qurango.net/radio/tarateel".equals(url) ||
             "https://stream.radiojar.com/8s5u5tpdtwzuv".equals(url);
        boolean allowedQuran=false,allowedLocal=false,allowedAudio=false;
        File localFile=null;
        String localPath=intent==null?null:intent.getStringExtra("local_path");
        try {
            if("quran".equals(kind)&&localPath!=null){
                File quranDir=new File(getFilesDir(),"quran").getCanonicalFile();
                localFile=new File(localPath).getCanonicalFile();
                allowedLocal=localFile.isFile()&&localFile.length()>10000&&localFile.getPath().startsWith(quranDir.getPath()+File.separator);
            }
            if(!allowedLocal){
                android.net.Uri parsed=android.net.Uri.parse(url);
                String host=parsed.getHost();
                allowedQuran="quran".equals(kind) && "https".equalsIgnoreCase(parsed.getScheme()) && host!=null &&
                    (host.equalsIgnoreCase("mp3quran.net") || host.toLowerCase().endsWith(".mp3quran.net") || host.equalsIgnoreCase("mp3quran.com") || host.toLowerCase().endsWith(".mp3quran.com"));
                allowedAudio=("nasheed".equals(kind)||"ruqyah".equals(kind)) && "https".equalsIgnoreCase(parsed.getScheme()) && host!=null &&
                    (host.equalsIgnoreCase("archive.org") || host.equalsIgnoreCase("www.archive.org") ||
                     host.equalsIgnoreCase("nourelquran.com") || host.equalsIgnoreCase("www.nourelquran.com"));
            }
        } catch(Exception ignored) {}
        if(!allowedRadio && !allowedQuran && !allowedLocal && !allowedAudio) { stopRadio(); return START_NOT_STICKY; }
        String label=intent==null?null:intent.getStringExtra("label");
        if(label!=null) {
            Notification.Builder updated=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
            updated.setSmallIcon(android.R.drawable.ic_media_play)
              .setContentTitle("quran".equals(kind)?"استماع القرآن الكريم":("nasheed".equals(kind)?"الإنشاد والابتهالات":("ruqyah".equals(kind)?"الرقية الشرعية":"إذاعة القرآن الكريم")))
              .setContentText(label+" — يعمل في الخلفية").setContentIntent(content).setOngoing(true)
              .addAction(android.R.drawable.ic_media_pause,"إيقاف الصوت",stopAction);
            Notification n2=updated.build();
            if(Build.VERSION.SDK_INT>=29) startForeground(NOTIFICATION,n2,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(NOTIFICATION,n2);
        }
        getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",true).putString("active_kind",kind==null?"radio":kind).apply();
        play(url,allowedLocal?localFile:null);
        return START_NOT_STICKY;
    }
    private void createChannel() {
        if(Build.VERSION.SDK_INT>=26) {
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel c=new NotificationChannel(CHANNEL,"إذاعة القرآن الكريم",NotificationManager.IMPORTANCE_LOW);
            c.setDescription("يظهر أثناء استمرار تشغيل الإذاعة في الخلفية"); nm.createNotificationChannel(c);
        }
    }
    private void play(String url,File localFile) {
        releasePlayer();
        try {
            player = new ExoPlayer.Builder(this).build();
            player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true);
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                                .putBoolean("radio_ready", true).remove("radio_last_error").apply();
                    }
                }
                @Override public void onPlayerError(PlaybackException error) {
                    String detail = error.getMessage() == null ? error.getErrorCodeName() : error.getMessage();
                    getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                            .putBoolean("radio_ready", false).putBoolean("radio_playing", false)
                            .putString("radio_last_error", detail).apply();
                    android.util.Log.e("ZadAlmoslem", "Radio stream playback failed: " + url, error);
                    stopRadio();
                }
            });
            Uri source = localFile != null ? Uri.fromFile(localFile) : Uri.parse(url);
            player.setMediaItem(MediaItem.fromUri(source));
            player.prepare();
            player.play();
        } catch(Exception e) {
            getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                    .putBoolean("radio_ready", false)
                    .putString("radio_last_error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()).apply();
            stopRadio();
        }
    }
    private void releasePlayer() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }
    private void stopRadio() { getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",false).putString("active_kind","").apply(); releasePlayer(); stopForeground(true); stopSelf(); }
    @Override public void onDestroy(){releasePlayer();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}