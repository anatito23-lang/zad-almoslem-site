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
import java.util.HashSet;
import java.util.Set;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public class RadioPlaybackService extends Service {
    public static final String ACTION_PLAY="com.zad.almoslem.PLAY_RADIO";
    public static final String ACTION_STOP="com.zad.almoslem.STOP_RADIO";
    private static final String CHANNEL="zad_radio_playback";
    private static final int NOTIFICATION=7410;
    private ExoPlayer player;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private Runnable startupTimeout;

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && ACTION_STOP.equals(intent.getAction())) { stopRadio(); return START_NOT_STICKY; }
        createChannel();
        Intent open=new Intent(this,HomeActivity.class);
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
        boolean allowedRadio="https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8".equals(url) ||
             "https://qurango.net/radio/tarateel".equals(url) ||
             "https://stream.radiojar.com/8s5u5tpdtwzuv".equals(url) ||
             "https://qurango.net/radio/mishary_alafasi".equals(url) ||
             "https://live.kwikmotion.com/sbrksaquranradiolive/srpksaquranradio/playlist.m3u8".equals(url) ||
             "http://www.quran-radio.org:8002/".equals(url);
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
                    (host.equalsIgnoreCase("archive.org") || host.toLowerCase().endsWith(".archive.org") || host.toLowerCase().endsWith(".us.archive.org") ||
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
        getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",true).putBoolean("radio_ready",false).remove("radio_last_error").putString("active_kind",kind==null?"radio":kind).putString("radio_station",intent==null?"":intent.getStringExtra("station")).apply();
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
            DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36 ZadAlmoslem")
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(15000)
                    .setReadTimeoutMs(30000);
            player = new ExoPlayer.Builder(this)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(httpFactory))
                    .build();
            player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true);
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_BUFFERING) {
                        updateRadioNotification("إذاعة القرآن الكريم", "جارٍ الاتصال بالبث...");
                    } else if (state == Player.STATE_READY) {
                        String active = getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", "");
                        if ("radio".equals(active)) {
                            if (startupTimeout != null) playbackHandler.removeCallbacks(startupTimeout);
                            getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                                    .putBoolean("radio_ready", true).putBoolean("radio_playing", true)
                                    .remove("radio_last_error").apply();
                            updateRadioNotification("إذاعة القرآن الكريم", "البث يعمل الآن");
                        } else {
                            String title = "quran".equals(active) ? "استماع القرآن الكريم" :
                                    ("nasheed".equals(active) ? "الإنشاد والابتهالات" : "الرقية الشرعية");
                            updateRadioNotification(title, "يعمل الآن");
                        }
                    }
                }
                @Override public void onPlayerError(PlaybackException error) {
                    String detail = error.getErrorCodeName() + ": " +
                            (error.getMessage() == null ? error.toString() : error.getMessage());
                    getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                            .putBoolean("radio_ready", false).putBoolean("radio_playing", false)
                            .putString("radio_last_error", detail).apply();
                    android.util.Log.e("ZadAlmoslem", "Audio playback failed: " + url, error);
                    showRadioFailure(detail);
                    try { if (player != null) player.stop(); } catch (Exception ignored) {}
                }
            });
            Uri source = localFile != null ? Uri.fromFile(localFile) : Uri.parse(url);
            boolean radioStream = "radio".equals(getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", ""));
            boolean hls = url != null && url.toLowerCase().contains(".m3u8");
            MediaItem item = hls
                    ? new MediaItem.Builder().setUri(source).setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8).build()
                    : ((localFile != null || !radioStream) ? MediaItem.fromUri(source)
                    : new MediaItem.Builder().setUri(source).setMimeType(androidx.media3.common.MimeTypes.AUDIO_MPEG).build());
            player.setMediaItem(item);
            player.prepare();
            player.play();
            if ("radio".equals(getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", ""))) {
                startupTimeout = () -> {
                    if (player != null && player.getPlaybackState() != Player.STATE_READY) {
                        getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                                .putBoolean("radio_ready", false).putBoolean("radio_playing", false)
                                .putString("radio_last_error", "انتهت مهلة الاتصال بالمحطة المختارة").apply();
                        showRadioFailure("انتهت مهلة الاتصال بالمحطة المختارة");
                    }
                };
                playbackHandler.postDelayed(startupTimeout, 20000);
            }
        } catch(Exception e) {
            getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                    .putBoolean("radio_ready", false)
                    .putString("radio_last_error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()).apply();
            stopRadio();
        }
    }
    private void updateRadioNotification(String title, String message) {
        try {
            Intent open = new Intent(this, MainActivity.class);
            PendingIntent content = PendingIntent.getActivity(this, 7411, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(title).setContentText(message)
                    .setContentIntent(content).setOngoing(true);
            Notification n = b.build();
            if (Build.VERSION.SDK_INT >= 29)
                startForeground(NOTIFICATION, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(NOTIFICATION, n);
        } catch (Exception e) {
            android.util.Log.e("ZadAlmoslem", "Could not update radio notification", e);
        }
    }

    private void showRadioFailure(String detail) {
        try {
            Intent open = new Intent(this, MainActivity.class);
            PendingIntent content = PendingIntent.getActivity(this, 7411, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Intent stop = new Intent(this, RadioPlaybackService.class);
            stop.setAction(ACTION_STOP);
            PendingIntent stopAction = PendingIntent.getService(this, 7412, stop,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
            String message = detail == null ? "تعذر استقبال البث" : detail;
            if (message.length() > 110) message = message.substring(0, 110);
            b.setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("تعذر تشغيل الصوت")
                    .setContentText(message)
                    .setStyle(new Notification.BigTextStyle().bigText(message))
                    .setContentIntent(content).setOngoing(true)
                    .addAction(android.R.drawable.ic_media_pause, "إغلاق", stopAction);
            getSystemService(NotificationManager.class).notify(NOTIFICATION, b.build());
        } catch (Exception e) {
            android.util.Log.e("ZadAlmoslem", "Could not show radio error notification", e);
        }
    }
    private void releasePlayer() {
        if (startupTimeout != null) {
            playbackHandler.removeCallbacks(startupTimeout);
            startupTimeout = null;
        }
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }
    private void stopRadio() { getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",false).putBoolean("radio_ready",false).putString("active_kind","").apply(); releasePlayer(); stopForeground(true); stopSelf(); }
    @Override public void onDestroy(){releasePlayer();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}