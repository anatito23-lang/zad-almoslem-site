package com.zad.almoslem;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import androidx.media3.common.MediaItem;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import android.net.Uri;
import android.os.*;
import java.io.IOException;
import java.io.File;
import java.util.HashSet;\nimport java.util.ArrayList;\nimport org.json.JSONArray;
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
    private PowerManager.WakeLock playbackWakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean resumeOnAudioFocusGain;
    private final AudioManager.OnAudioFocusChangeListener audioFocusListener = this::handleAudioFocusChange;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private Runnable startupTimeout;\n    private ArrayList<String> duaQueueUrls = new ArrayList<>();\n    private ArrayList<String> duaQueueTitles = new ArrayList<>();\n    private ArrayList<String> duaQueueLocalPaths = new ArrayList<>();

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        // A newly selected stream must also remain paused if playback is started during adhan.
        if (getSharedPreferences("zad_reminders", MODE_PRIVATE).getBoolean("adhan_active", false))
            adhanPauseRequested = true;
        if(intent!=null && ACTION_STOP.equals(intent.getAction())) { stopRadio(); return START_NOT_STICKY; }
        if (intent == null) {
            android.content.SharedPreferences saved = getSharedPreferences("zad_audio", MODE_PRIVATE);
            if (!saved.getBoolean("radio_playing", false) || saved.getBoolean("radio_paused", false)) return START_NOT_STICKY;
            intent = new Intent(this, RadioPlaybackService.class);
            intent.putExtra("url", saved.getString("stream_url", null));
            intent.putExtra("kind", saved.getString("active_kind", "radio"));
            intent.putExtra("label", saved.getString("radio_title", "زاد وطمأنينة"));
            intent.putExtra("station", saved.getString("radio_station", ""));
            intent.putExtra("local_path", saved.getString("stream_local_path", null));\n            restoreDuaQueueExtras(intent, saved);
        }
        if(intent!=null && (ACTION_TOGGLE.equals(intent.getAction()) || ACTION_PAUSE.equals(intent.getAction()) || ACTION_RESUME.equals(intent.getAction()))) {
            if (player != null) {
                boolean shouldPlay = ACTION_TOGGLE.equals(intent.getAction()) ? !(player.getPlayWhenReady() || resumeOnAudioFocusGain || (adhanPauseRequested && resumeAfterAdhan)) : ACTION_RESUME.equals(intent.getAction());
                if (adhanPauseRequested) {
                    // A deliberate pause during adhan cancels the automatic return.
                    // A deliberate resume keeps the user's request queued until dua ends.
                    if (shouldPlay) resumeAfterAdhan = true;
                    else resumeAfterAdhan = false;
                } else if (shouldPlay) {
                    resumeOnAudioFocusGain = true;
                    if (requestAudioFocus()) {
                        resumeOnAudioFocusGain = false;
                        player.play();
                    } else {
                        player.pause();
                    }
                } else {
                    resumeOnAudioFocusGain = false;
                    player.pause();
                }
                getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("radio_paused", !shouldPlay).putBoolean("waiting_for_audio_focus", shouldPlay && !player.getPlayWhenReady() && !adhanPauseRequested).apply();
                updateRadioNotification(playbackTitle(), shouldPlay ? "يعمل الآن" : "متوقف مؤقتًا");
            } else {
                android.content.SharedPreferences saved = getSharedPreferences("zad_audio", MODE_PRIVATE);
                if (saved.getBoolean("radio_playing", false)) {
                    Intent restore = new Intent(this, RadioPlaybackService.class);
                    restore.putExtra("url", saved.getString("stream_url", null));
                    restore.putExtra("kind", saved.getString("active_kind", "radio"));
                    restore.putExtra("label", saved.getString("radio_title", "زاد وطمأنينة"));
                    restore.putExtra("station", saved.getString("radio_station", ""));
                    restore.putExtra("local_path", saved.getString("stream_local_path", null));\n                    restoreDuaQueueExtras(restore, saved);
                    intent = restore;
                } else return START_NOT_STICKY;
            }
            // If Android recreated the service after process eviction, the persisted stream
            // is restored below and remains in the foreground independently of the Activity.
        }
        if(intent!=null && ACTION_PAUSE_FOR_ADHAN.equals(intent.getAction())) {
            pauseForAdhan();
            // No active stream to preserve: do not leave an idle background service running.
            if (player == null) { stopSelf(); return START_NOT_STICKY; }
            return START_STICKY;
        }
        if(intent!=null && ACTION_RESUME_AFTER_ADHAN.equals(intent.getAction())) {
            resumeAfterAdhan();
            return player == null ? START_NOT_STICKY : START_STICKY;
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
        if("dua".equals(kind)){
            ArrayList<String> incomingUrls=intent==null?null:intent.getStringArrayListExtra("queue_urls");
            ArrayList<String> incomingTitles=intent==null?null:intent.getStringArrayListExtra("queue_titles");
            ArrayList<String> incomingPaths=intent==null?null:intent.getStringArrayListExtra("queue_local_paths");
            duaQueueUrls=new ArrayList<>();duaQueueTitles=new ArrayList<>();duaQueueLocalPaths=new ArrayList<>();
            if(incomingUrls!=null&&!incomingUrls.isEmpty()){
                duaQueueUrls.addAll(incomingUrls);
                for(int i=0;i<duaQueueUrls.size();i++){
                    duaQueueTitles.add(incomingTitles!=null&&i<incomingTitles.size()?incomingTitles.get(i):"دعاء");
                    duaQueueLocalPaths.add(incomingPaths!=null&&i<incomingPaths.size()?incomingPaths.get(i):"");
                }
            }else if(url!=null){duaQueueUrls.add(url);duaQueueTitles.add(intent==null?"الأدعية الصوتية":intent.getStringExtra("label"));duaQueueLocalPaths.add(localPath==null?"":localPath);}
            allowedAudio=!duaQueueUrls.isEmpty();
            for(int i=0;i<duaQueueUrls.size();i++){
                String path=i<duaQueueLocalPaths.size()?duaQueueLocalPaths.get(i):"";
                if(path!=null&&!path.isEmpty()&&isAllowedDuaLocalFile(path)){
                    if(i==0){try{localFile=new File(path).getCanonicalFile();allowedLocal=true;}catch(Exception ignored){}}
                    continue;
                }
                if(!isAllowedDuaUrl(duaQueueUrls.get(i)))allowedAudio=false;
            }
            if(allowedAudio){
                android.content.SharedPreferences.Editor qe=getSharedPreferences("zad_audio",MODE_PRIVATE).edit();
                qe.putString("dua_queue_urls",new JSONArray(duaQueueUrls).toString())
                  .putString("dua_queue_titles",new JSONArray(duaQueueTitles).toString())
                  .putString("dua_queue_local_paths",new JSONArray(duaQueueLocalPaths).toString()).apply();
            }
        }else{
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
        }
        if(!allowedRadio && !allowedQuran && !allowedLocal && !allowedAudio) { stopRadio(); return START_NOT_STICKY; }
        String label=intent==null?null:intent.getStringExtra("label");
        getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",true).putBoolean("radio_ready",false).remove("radio_last_error").putString("active_kind",kind==null?"radio":kind).putString("radio_station",intent==null?"":intent.getStringExtra("station"))
          .putString("radio_title",label==null?defaultTitle(kind):label).putString("stream_url",url)
          .putString("stream_local_path",allowedLocal?localFile.getAbsolutePath():"")
          .putBoolean("radio_paused",false).putBoolean("waiting_for_audio_focus",false).apply();
        QuranHomeWidget.refreshAll(this);
        play(url,allowedLocal?localFile:null);
        return START_STICKY;
    }
    private boolean isAllowedDuaUrl(String address){
        try{android.net.Uri parsed=android.net.Uri.parse(address);String host=parsed.getHost();
            if(!"https".equalsIgnoreCase(parsed.getScheme())||host==null)return false;
            return host.equalsIgnoreCase("ourquraan.com")||host.equalsIgnoreCase("www.ourquraan.com")||
                   host.equalsIgnoreCase("archive.org")||host.toLowerCase().endsWith(".archive.org");
        }catch(Exception e){return false;}
    }
    private boolean isAllowedDuaLocalFile(String path){
        try{File candidate=new File(path).getCanonicalFile();File duaDir=new File(getFilesDir(),"duas").getCanonicalFile();
            File sharawy=new File(new File(getFilesDir(),"reminders"),"dua.mp3").getCanonicalFile();
            return candidate.isFile()&&candidate.length()>10000&&(candidate.getPath().startsWith(duaDir.getPath()+File.separator)||candidate.equals(sharawy));
        }catch(Exception e){return false;}
    }
    private void restoreDuaQueueExtras(Intent target,android.content.SharedPreferences saved){
        if(!"dua".equals(saved.getString("active_kind","")))return;
        target.putStringArrayListExtra("queue_urls",readStringArray(saved.getString("dua_queue_urls","[]")));
        target.putStringArrayListExtra("queue_titles",readStringArray(saved.getString("dua_queue_titles","[]")));
        target.putStringArrayListExtra("queue_local_paths",readStringArray(saved.getString("dua_queue_local_paths","[]")));
    }
    private ArrayList<String> readStringArray(String raw){
        ArrayList<String> result=new ArrayList<>();try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++)result.add(a.optString(i,""));}catch(Exception ignored){}return result;
    }
    // Continue sequential Quran playback when the current surah reaches its end.
    private void playNextQuranSurah() {
        android.content.SharedPreferences prefs = getSharedPreferences("zad_audio", MODE_PRIVATE);
        int current = prefs.getInt("last_quran_surah_id", 0);
        String server = prefs.getString("last_quran_server", "");
        String available = prefs.getString("last_quran_surah_list", "");
        String reciter = prefs.getString("last_quran_reciter", "القارئ");
        String storageKey = prefs.getString("last_quran_storage_key", "unknown");
        if (current < 1 || current >= 114 || server == null || server.trim().isEmpty()) {
            stopRadio();
            return;
        }
        Set<Integer> surahs = new HashSet<>();
        if (available != null) {
            for (String part : available.split(",")) {
                try { surahs.add(Integer.parseInt(part.trim())); } catch (Exception ignored) {}
            }
        }
        int next = current + 1;
        while (next <= 114 && !surahs.contains(next)) next++;
        if (next > 114) {
            stopRadio();
            return;
        }
        try {
            String base = server.endsWith("/") ? server : server + "/";
            String nextUrl = base + String.format(java.util.Locale.US, "%03d.mp3", next);
            File quranDir = new File(getFilesDir(), "quran");
            File offline = new File(quranDir, "reciter_" + storageKey + "_surah_" + next + ".mp3");
            File legacyOffline = new File(quranDir, "reciter_" + storageKey + "_surah_" + next + ".mp3");
            File local = offline.isFile() && offline.length() > 10000 ? offline :
                    (legacyOffline.isFile() && legacyOffline.length() > 10000 ? legacyOffline : null);
            String title = "سورة رقم " + next + " — " + reciter;
            prefs.edit().putInt("last_quran_surah_id", next)
                    .putString("last_quran_url", nextUrl)
                    .putString("last_quran_title", title)
                    .putString("radio_title", title)
                    .putString("stream_url", nextUrl)
                    .putString("stream_local_path", local == null ? "" : local.getAbsolutePath())
                    .putBoolean("radio_playing", true).putBoolean("radio_paused", false).apply();
            QuranHomeWidget.refreshAll(this);
            Uri source = local == null ? Uri.parse(nextUrl) : Uri.fromFile(local);
            player.setMediaItem(MediaItem.fromUri(source));
            player.prepare();
            player.play();
            updateRadioNotification(title, "انتقل التشغيل تلقائيًا إلى السورة التالية");
        } catch (Exception e) {
            android.util.Log.e("ZadAlmoslem", "Could not continue to next Quran surah", e);
            stopRadio();
        }
    }

    private void pauseForAdhan() {
        adhanPauseRequested = true;
        resumeOnAudioFocusGain = false;
        if (player == null || pausedForAdhan) return;
        resumeAfterAdhan = (player.getPlayWhenReady() || resumeOnAudioFocusGain) &&
                player.getPlaybackState() != Player.STATE_IDLE &&
                player.getPlaybackState() != Player.STATE_ENDED;
        resumeOnAudioFocusGain = false;
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
            resumeOnAudioFocusGain = true;
            if (requestAudioFocus()) {
                resumeOnAudioFocusGain = false;
                getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("waiting_for_audio_focus", false).apply();
                player.play();
                android.util.Log.i("ZadAlmoslem", "Audio resumed after adhan at " + savedPosition);
            } else {
                getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("waiting_for_audio_focus", true).apply();
                player.pause();
                android.util.Log.i("ZadAlmoslem", "Audio waiting for focus after adhan");
            }
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
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .build();
            player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), false);
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED &&
                            "quran".equals(getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", ""))) {
                        playNextQuranSurah();
                        return;
                    }
                    if(state==Player.STATE_ENDED && "dua".equals(getSharedPreferences("zad_audio",MODE_PRIVATE).getString("active_kind",""))){\n                        getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",false).putBoolean("radio_paused",true).apply();\n                        updateRadioNotification(playbackTitle(),"اكتملت قائمة الأدعية");return;\n                    }\n                    if (state == Player.STATE_BUFFERING) {
                        getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("radio_ready", false).apply();
                        updateRadioNotification(playbackTitle(), "جارٍ الاتصال بالبث...");
                    } else if (state == Player.STATE_READY) {
                        String active = getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", "");
                        if ("radio".equals(active)) {
                            if (startupTimeout != null) playbackHandler.removeCallbacks(startupTimeout);
                            getSharedPreferences("zad_audio", MODE_PRIVATE).edit()
                                    .putBoolean("radio_ready", true).putBoolean("radio_playing", true)
                                    .remove("radio_last_error").apply();
                            QuranHomeWidget.refreshAll(RadioPlaybackService.this);
                            updateRadioNotification(playbackTitle(), "البث يعمل الآن");
                        } else {
                            getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("radio_ready", true).apply();
                            String title = "quran".equals(active) ? "استماع القرآن الكريم" :
                                    ("nasheed".equals(active) ? "الإنشاد والابتهالات" : "الرقية الشرعية");
                            updateRadioNotification(title, "يعمل الآن");
                        }
                    }
                }
                @Override public void onMediaItemTransition(MediaItem mediaItem,int reason){\n                    if(mediaItem!=null && "dua".equals(getSharedPreferences("zad_audio",MODE_PRIVATE).getString("active_kind",""))){\n                        String title=mediaItem.mediaId==null?"الأدعية الصوتية":mediaItem.mediaId;\n                        getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putString("radio_title",title).apply();\n                        updateRadioNotification(title,"انتقل التشغيل تلقائيًا إلى الدعاء التالي");\n                    }\n                }\n                @Override public void onIsPlayingChanged(boolean isPlaying) {
                    setPlaybackWakeLock(isPlaying);
                    getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("radio_paused", player == null || !player.getPlayWhenReady()).apply();
                    updateRadioNotification(playbackTitle(), isPlaying ? "يعمل الآن" : "متوقف مؤقتًا");
                }
                @Override public void onPlayerError(PlaybackException error) {
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
            if("dua".equals(activeKind)&&!duaQueueUrls.isEmpty()){
                ArrayList<MediaItem> playlist=new ArrayList<>();
                for(int i=0;i<duaQueueUrls.size();i++){
                    String itemPath=i<duaQueueLocalPaths.size()?duaQueueLocalPaths.get(i):"";
                    File itemLocal=itemPath==null||itemPath.isEmpty()?null:(isAllowedDuaLocalFile(itemPath)?new File(itemPath):null);
                    Uri itemUri=itemLocal!=null?Uri.fromFile(itemLocal):Uri.parse(duaQueueUrls.get(i));
                    String itemTitle=i<duaQueueTitles.size()?duaQueueTitles.get(i):"دعاء";
                    playlist.add(new MediaItem.Builder().setUri(itemUri).setMediaId(itemTitle).build());
                }
                player.setMediaItems(playlist,0,0L);
            }else{
                player.setMediaItem(item);
            }
            player.prepare();
            if (adhanPauseRequested) {
                positionBeforeAdhan = 0L;
                resumeAfterAdhan = true;
                pausedForAdhan = true;
                player.pause();
            } else {
                resumeOnAudioFocusGain = true;
                if (requestAudioFocus()) {
                    resumeOnAudioFocusGain = false;
                    getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("waiting_for_audio_focus", false).apply();
                    player.play();
                } else {
                    getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("waiting_for_audio_focus", true).apply();
                    player.pause();
                }
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
    private void handleAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (player != null && resumeOnAudioFocusGain && !adhanPauseRequested) {
                resumeOnAudioFocusGain = false;
                getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("waiting_for_audio_focus", false).apply();
                player.play();
                android.util.Log.i("ZadAlmoslem", "Audio resumed after external audio focus returned");
            }
            return;
        }
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            if (player != null && player.getPlayWhenReady()) {
                if (!adhanPauseRequested) resumeOnAudioFocusGain = true;
                getSharedPreferences("zad_audio", MODE_PRIVATE).edit().putBoolean("waiting_for_audio_focus", !adhanPauseRequested && resumeOnAudioFocusGain).apply();
                player.pause();
                android.util.Log.i("ZadAlmoslem", "Audio paused after external audio focus loss: " + focusChange);
            }
        }
    }

    private boolean requestAudioFocus() {
        if (audioManager == null) audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager == null) return false;
        if (Build.VERSION.SDK_INT >= 26) {
            if (audioFocusRequest == null) {
                android.media.AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attributes)
                        .setOnAudioFocusChangeListener(audioFocusListener, playbackHandler)
                        .setAcceptsDelayedFocusGain(true)
                        .setWillPauseWhenDucked(true)
                        .build();
            }
            int result = audioManager.requestAudioFocus(audioFocusRequest);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        int result = audioManager.requestAudioFocus(audioFocusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26 && audioFocusRequest != null)
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            else
                audioManager.abandonAudioFocus(audioFocusListener);
        } catch (Exception ignored) {}
        resumeOnAudioFocusGain = false;
    }

    private String defaultTitle(String kind) {
        if ("quran".equals(kind)) return "استماع القرآن الكريم";
        if ("ruqyah".equals(kind)) return "الرقية الشرعية";
        if ("nasheed".equals(kind)) return "الإنشاد والابتهالات";\n        if ("dua".equals(kind)) return "الأدعية الصوتية";
        return "إذاعة القرآن الكريم";
    }

    private String playbackTitle() {
        android.content.SharedPreferences p=getSharedPreferences("zad_audio",MODE_PRIVATE);
        String title=p.getString("radio_title","");
        return title.isEmpty()?defaultTitle(p.getString("active_kind","radio")):title;
    }

    private Notification buildPlaybackNotification(String title, String message) {
        String kind = getSharedPreferences("zad_audio", MODE_PRIVATE).getString("active_kind", "radio");
        Class<?> destination = "quran".equals(kind) ? QuranListeningActivity.class :
                "ruqyah".equals(kind) ? RuqyahActivity.class : RadioActivity.class;
        Intent open = new Intent(this, destination);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 7411, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean playing = player != null && (player.getPlayWhenReady() || resumeOnAudioFocusGain || (adhanPauseRequested && resumeAfterAdhan));
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
                .setContentTitle(title == null ? "سكينة – أذان وصلاة وقرآن" : title)
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
    private void setPlaybackWakeLock(boolean playing) {
        try {
            if (playing) {
                if (playbackWakeLock == null) {
                    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                    if (powerManager != null) {
                        playbackWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                "ZadAlmoslem:AudioPlayback");
                        playbackWakeLock.setReferenceCounted(false);
                    }
                }
                if (playbackWakeLock != null && !playbackWakeLock.isHeld()) playbackWakeLock.acquire();
            } else if (playbackWakeLock != null && playbackWakeLock.isHeld()) {
                playbackWakeLock.release();
            }
        } catch (Exception e) {
            android.util.Log.w("ZadAlmoslem", "Could not update playback wake lock", e);
        }
    }
    private void releasePlayer() {
        setPlaybackWakeLock(false);
        abandonAudioFocus();
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
    private void stopRadio() { adhanPauseRequested=false; pausedForAdhan=false; resumeAfterAdhan=false; getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putBoolean("radio_playing",false).putBoolean("radio_ready",false).putString("active_kind","").putString("radio_title","").remove("stream_url").remove("stream_local_path").remove("dua_queue_urls").remove("dua_queue_titles").remove("dua_queue_local_paths")
        .putBoolean("radio_paused",false).putBoolean("waiting_for_audio_focus",false).apply(); QuranHomeWidget.refreshAll(this); releasePlayer(); stopForeground(true); stopSelf(); }
    @Override public void onTaskRemoved(Intent rootIntent) {
        // Keep the user-started media session visible when the app is swiped from Recents.
        android.content.SharedPreferences saved = getSharedPreferences("zad_audio", MODE_PRIVATE);
        if (player != null && saved.getBoolean("radio_playing", false)) {
            try {
                Notification notification = buildPlaybackNotification(
                        playbackTitle(), player.isPlaying() ? "يعمل في الخلفية" : "متوقف مؤقتًا");
                if (Build.VERSION.SDK_INT >= 29) {
                    startForeground(NOTIFICATION, notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                } else {
                    startForeground(NOTIFICATION, notification);
                }
                android.util.Log.i("ZadAlmoslem", "Foreground media playback retained after task removal");
            } catch (Exception e) {
                android.util.Log.e("ZadAlmoslem", "Could not reaffirm foreground playback", e);
            }
        }
        // Do not stop the service here. A user force-stop from Android Settings is
        // intentionally respected by the operating system and cannot be bypassed.
    }
    @Override public void onDestroy(){releasePlayer();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}