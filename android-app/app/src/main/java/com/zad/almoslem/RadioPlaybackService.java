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
import java.util.HashMap;
import java.util.Map;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public class RadioPlaybackService extends Service {
    public static final String ACTION_PLAY="com.zad.almoslem.PLAY_RADIO";
    public static final String ACTION_STOP="com.zad.almoslem.STOP_RADIO";
    public static final String ACTION_TOGGLE="com.zad.almoslem.TOGGLE_RADIO";
    public static final String ACTION_PAUSE="com.zad.almoslem.PAUSE_RADIO";
    public static final String ACTION_RESUME="com.zad.almoslem.RESUME_RADIO";
    public static final String ACTION_PAUSE_FOR_ADHAN="com.zad.almoslem.PAUSE_FOR_ADHAN";
    public static final String ACTION_RESUME_AFTER_ADHAN="com.zad.almoslem.RESUME_AFTER_ADHAN";
    private boolean pausedForAdhan;
    private boolean adhanPauseRequested;
    private boolean resumeAfterAdhan;
    private long positionBeforeAdhan;
    private static final String CHANNEL="zad_radio_playback";
    private static final int NOTIFICATION=7410;
    private ExoPlayer player;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private Runnable startupTimeout;

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        // A newly selected stream must also remain paused if playback is started during adhan.
        if (getSharedPreferences("zad_reminders", MODE_PRIVATE).getBoolean("adhan_active", false))
            adhanPauseRequested = true;
        if(intent!=null && ACTION_STOP.equals(intent.getAction())) { stopRadio(); return START_NOT_STICKY; }
        if(intent!=null && (ACTION_TOGGLE.equals(intent.getAction()) || ACTION_PAUSE.equals(intent.getAction()) || ACTION_RESUME.equals(intent.getAction()))) {
            if (player != null) {
                boolean shouldPlay = ACTION_TOGGLE.equals(intent.getAction()) ? !player.isPlaying() : ACTION_RESUME.equals(intent.getAction());
                if (adhanPauseRequested) {
                    // A deliberate pause during adhan cancels the automatic return.
                    // A deliberate resume keeps the user's request queued until dua ends.
                    if (shouldPlay) resumeAfterAdhan = true;
                    else resumeAfterAdhan = false;
                } else if (shouldPlay) {
                    player.play();
                } else {
                    player.pause();
                }
                getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("radio_paused", !shouldPlay).apply();
                updateRadioNotification(playbackTitle(), shouldPlay ? "يعمل الآن" : "متوقف مؤقتًا");
            }
            return START_NOT_STICKY;
        }
        if(intent!=null && ACTION_PAUSE_FOR_ADHAN.equals(intent.getAction())) {
            pauseForAdhan();
            // No active stream to preserve: do not leave an idle background service running.
            if (player == null) stopSelf();
            return START_NOT_STICKY;
        }
        if(intent!=null && ACTION_RESUME_AFTER_ADHAN.equals(intent.getAction())) {
            resumeAfterAdhan();
            return START_NOT_STICKY;
        }
        createChannel();
        Notification n=buildPlaybackNotification("زاد وطمأنينة","جارٍ تجهيز الصوت");
        if(Build.VERSION.SDK_INT>=29) startForeground(NOTIFICATION,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NOTIFICATION,n);
        String url=intent==null?null:intent.getStringExtra("url");
        String kind=intent==null?"radio":intent.getStringExtra("kind");
        boolean allowedRadio="https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8".equals(url) ||
             "https://qurango.net/radio/tarateel".equals(url) ||
             "https://stream.radiojar.com/8s5u5tpdtwzuv".equals(url) ||
             "https://qurango.net/radio/mishary_alafasi".equals(url) ||
             "https://live.kwikmotion.com/sbrksaquranradiolive/srpksaquranradio/playlist.m3u8".equals(url) ||
             false;
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
        getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",true).putBoolean("radio_ready",false).remove("radio_last_error").putString("active_kind",kind==null?"radio":kind).putString("radio_station",intent==null?"":intent.getStringExtra("station"))\n          .putString("radio_title",label==null?defaultTitle(kind):label).putBoolean("radio_paused",false).apply();
        QuranHomeWidget.refreshAll(this);
        play(url,allowedLocal?localFile:null);
        return START_NOT_STICKY;
    }
    private void pauseForAdhan() {
        adhanPauseRequested = true;
        if (player == null || pausedForAdhan) return;
        resumeAfterAdhan = player.getPlayWhenReady() &&
                player.getPlaybackState() != Player.STATE_IDLE &&
                player.getPlaybackState() != Player.STATE_ENDED;
        if (!resumeAfterAdhan) return;
        positionBeforeAdhan = Math.max(0L, player.getCurrentPosition());
        pausedForAdhan = true;
        player.pause();
        android.util.Log.i("ZadAlmoslem", "Audio paused for adhan at " + positionBeforeAdhan);
    }

    private void resumeAfterAdhan() {
        adhanPauseRequested = false;
        boolean shouldResume = resumeAfterAdhan;
        long savedPosition = positionBeforeAdhan;
        pausedForAdhan = false;
        resumeAfterAdhan = false;
        if (player != null && shouldResume) {
            player.seekTo(savedPosition);
            player.play();
            android.util.Log.i("ZadAlmoslem", "Audio resumed after adhan at " + savedPosition);
        }
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
            String activeKind = getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", "");
            String station = getSharedPreferences("zad_audio", MODE_PRIVATE).getString("radio_station", "");
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Accept", "*/*");
            if ("radio".equals(activeKind) && "saudi".equals(station)) {
                requestHeaders.put("Referer", "https://radioplus.sba.sa/");
                requestHeaders.put("Origin", "https://radioplus.sba.sa");
            } else if ("radio".equals(activeKind) && "palestine".equals(station)) {
                requestHeaders.put("Icy-MetaData", "1");
                requestHeaders.put("Referer", "https://quran-radio.com/");
            } else if ("ruqyah".equals(activeKind)) {
                requestHeaders.put("Referer", "https://archive.org/");
            }
            DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36 ZadAlmoslem")
                    .setDefaultRequestProperties(requestHeaders)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(20000)
                    .setReadTimeoutMs(45000);
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
                        updateRadioNotification(playbackTitle(), "جارٍ الاتصال بالبث...");
                    } else if (state == Player.STATE_READY) {
                        String active = getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", "");
                        if ("radio".equals(active)) {
                            if (startupTimeout != null) playbackHandler.removeCallbacks(startupTimeout);
                            getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                                    .putBoolean("radio_ready", true).putBoolean("radio_playing", true)
                                    .remove("radio_last_error").apply();
                            QuranHomeWidget.refreshAll(RadioPlaybackService.this);
                            updateRadioNotification("إذاعة القرآن الكريم", "البث يعمل الآن");
                        } else {
                            String title = "quran".equals(active) ? "استماع القرآن الكريم" :
                                    ("nasheed".equals(active) ? "الإنشاد والابتهالات" : "الرقية الشرعية");
                            updateRadioNotification(title, "يعمل الآن");
                        }
                    }
                }
                @Override public void onIsPlayingChanged(boolean isPlaying) {\n                    getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("radio_paused", !isPlaying).apply();\n                    updateRadioNotification(playbackTitle(), isPlaying ? "يعمل الآن" : "متوقف مؤقتًا");\n                }\n                @Override public void onPlayerError(PlaybackException error) {
                    // Cancel the startup timeout so it cannot overwrite the real playback error.
                    if (startupTimeout != null) {
                        playbackHandler.removeCallbacks(startupTimeout);
                        startupTimeout = null;
                    }
                    String detail = error.getErrorCodeName() + ": " +
                            (error.getMessage() == null ? error.toString() : error.getMessage());
                    getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                            .putBoolean("radio_ready", false).putBoolean("radio_playing", false)
                            .putString("radio_last_error", detail).apply();
                    QuranHomeWidget.refreshAll(RadioPlaybackService.this);
                    android.util.Log.e("ZadAlmoslem", "Audio playback failed: " + url, error);
                    showRadioFailure(detail);
                    try { if (player != null) player.stop(); } catch (Exception ignored) {}
                }
            });
            Uri source = localFile != null ? Uri.fromFile(localFile) : Uri.parse(url);
            boolean hls = localFile == null && url != null && url.toLowerCase().contains(".m3u8");
            // Keep explicit MIME selection for HLS playlists only. Progressive radio
            // streams may be MP3, AAC, or ICY-wrapped; let Media3 sniff the response
            // instead of forcing every non-HLS station to be parsed as audio/mpeg.
            MediaItem item = hls
                    ? new MediaItem.Builder().setUri(source).setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8).build()
                    : MediaItem.fromUri(source);
            player.setMediaItem(item);
            player.prepare();
            if (adhanPauseRequested) {
                positionBeforeAdhan = 0L;
                resumeAfterAdhan = true;
                pausedForAdhan = true;
                player.pause();
            } else {
                player.play();
            }
            if ("radio".equals(getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", ""))) {
                startupTimeout = () -> {
                    startupTimeout = null;
                    if (player != null && player.getPlaybackState() != Player.STATE_READY) {
                        String timeoutMessage = "انتهت مهلة الاتصال بالمحطة المختارة";
                        getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                                .putBoolean("radio_ready", false).putBoolean("radio_playing", false)
                                .putString("radio_last_error", timeoutMessage).apply();
                        try { player.stop(); } catch (Exception ignored) {}
                        QuranHomeWidget.refreshAll(RadioPlaybackService.this);
                        showRadioFailure(timeoutMessage);
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
    private String defaultTitle(String kind) {
        if ("quran".equals(kind)) return "استماع القرآن الكريم";
        if ("ruqyah".equals(kind)) return "الرقية الشرعية";
        if ("nasheed".equals(kind)) return "الإنشاد والابتهالات";
        return "إذاعة القرآن الكريم";
    }

    private String playbackTitle() {
        android.content.SharedPreferences p=getSharedPreferences("zad_audio",MODE_PRIVATE);
        String title=p.getString("radio_title","");
        return title.isEmpty()?defaultTitle(p.getString("active_kind","radio")):title;
    }

    private Notification buildPlaybackNotification(String title, String message) {
        Intent open = new Intent(this, NowPlayingActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 7411, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean playing = player != null && player.isPlaying();
        Intent toggle = new Intent(this, RadioPlaybackService.class);
        toggle.setAction(ACTION_TOGGLE);
        PendingIntent toggleAction = PendingIntent.getService(this, 7413, toggle,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, RadioPlaybackService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 7412, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title == null ? "زاد وطمأنينة" : title)
                .setContentText(message == null ? "تشغيل صوتي في الخلفية" : message)
                .setContentIntent(content).setOngoing(true).setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        playing ? "إيقاف مؤقت" : "استئناف", toggleAction)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopAction);
        return b.build();
    }

    private void updateRadioNotification(String title, String message) {
        try {
            Notification n = buildPlaybackNotification(title, message);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.notify(NOTIFICATION, n);
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
    private void stopRadio() { adhanPauseRequested=false; pausedForAdhan=false; resumeAfterAdhan=false; getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",false).putBoolean("radio_ready",false).putString("active_kind","").putString("radio_title","").putBoolean("radio_paused",false).apply(); QuranHomeWidget.refreshAll(this); releasePlayer(); stopForeground(true); stopSelf(); }
    @Override public void onDestroy(){releasePlayer();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}