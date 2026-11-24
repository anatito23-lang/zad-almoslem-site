package com.zad.almoslem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
import android.os.IBinder;
import java.io.File;
import java.util.Locale;

public class ReminderAudioService extends Service {
    public static final String ACTION_PLAY = "com.zad.almoslem.PLAY_REMINDER_AUDIO";
    public static final String ACTION_STOP = "com.zad.almoslem.STOP_REMINDER_AUDIO";
    public static final String ACTION_TOGGLE = "com.zad.almoslem.TOGGLE_REMINDER_AUDIO";
    public static final String ACTION_SET_VOLUME = "com.zad.almoslem.SET_REMINDER_AUDIO_VOLUME";
    private static final String CHANNEL = "zad_reminder_audio";
    private static final int NOTIFICATION_ID = 7520;
    private MediaPlayer player;
    private String activeKind;
    private int postAdhanQueueIndex;
    private boolean userStopped;
    private boolean sharawyPlayed;
    private String currentTrackTitle="";
    private static final String[][] POST_ADHAN_DUAS = {
        {"alsayed_abdalzez_1","https://www.ourquraan.com/audio/prayers/Alsayed.Abdalzez.Alzadany%201.mp3"},
        {"alsayed_abdalzez_2","https://www.ourquraan.com/audio/prayers/Alsayed.Abdalzez.Alzadany%202.mp3"},
        {"muhammed_alasyouty","https://www.ourquraan.com/audio/prayers/Muhammed.Alasyouty.mp3"},
        {"salah_algamal","https://www.ourquraan.com/audio/prayers/Salah.Algamal%20-%20Doaa.Alanbiaa.mp3"},
        {"muhammad_jibril","https://www.ourquraan.com/audio/qdr/Muahmmed.Jebreel.mp3"},
        {"hany_alrefaey","https://www.ourquraan.com/audio/quraan-completion/Hany.Alrefaey.mp3"},
        {"edres_abkar","https://www.ourquraan.com/audio/prayers/Edres.Abkar.mp3"},
        {"khalid_aljalil","https://www.ourquraan.com/audio/prayers/Khalid.Aljalil.mp3"},
        {"maher_m3equly","https://www.ourquraan.com/audio/prayers/Maher.M3equly.mp3"},
        {"naser_alqutamy","https://www.ourquraan.com/audio/prayers/Naser.Alqutamy%201.mp3"},
        {"yaser_aldosy","https://www.ourquraan.com/audio/prayers/Yaser.Aldosy.mp3"},
        {"mashary_rashed","https://www.ourquraan.com/audio/prayers/Mashary.Rashed.mp3"}
    };

    public static void play(Context context, String kind) {
        play(context, kind, null);
    }

    public static void play(Context context, String kind, String message) {
        play(context, kind, message, null);
    }

    public static void play(Context context, String kind, String message, String audioFile) {
        Intent i = new Intent(context, ReminderAudioService.class);
        i.setAction(ACTION_PLAY);
        i.putExtra("kind", kind);
        if (message != null) i.putExtra("message", message);
        if (audioFile != null) i.putExtra("audio_file", audioFile);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i);
        else context.startService(i);
    }

    public static void stop(Context context) {
        Intent i = new Intent(context, ReminderAudioService.class);
        i.setAction(ACTION_STOP);
        try { context.startService(i); } catch (Exception ignored) {}
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SET_VOLUME.equals(intent.getAction())) {
            int volume=Math.max(0,Math.min(100,intent.getIntExtra("volume",80)));
            if(player!=null)player.setVolume(volume/100f,volume/100f);
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction())) {
            if(player!=null){try{if(player.isPlaying())player.pause();else player.start();}catch(Exception ignored){} updatePlaybackNotification();}
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            userStopped=true;
            finishPlayback();
            return START_NOT_STICKY;
        }
        String kind = intent == null ? "qiyam" : intent.getStringExtra("kind");
        if (!"morning".equals(kind) && !"evening".equals(kind) && !"dua".equals(kind)
                && !"approach".equals(kind) && !"gentle".equals(kind)) kind = "qiyam";
        activeKind = kind;
        if("dua".equals(kind)){postAdhanQueueIndex=0;userStopped=false;sharawyPlayed=false;currentTrackTitle="دعاء ما بعد الأذان";}
        if (!"dua".equals(kind) && !getSharedPreferences("zad_reminders", MODE_PRIVATE).getBoolean("reminder_audio_master_enabled", true)) {
            finishPlayback();
            return START_NOT_STICKY;
        }
        String reminderMessage = intent == null ? null : intent.getStringExtra("message");
        String requestedAudioFile = intent == null ? null : intent.getStringExtra("audio_file");
        createChannel();
        String title = "morning".equals(kind) ? "أذكار الصباح" :
                "evening".equals(kind) ? "أذكار المساء" :
                "dua".equals(kind) ? "دعاء ما بعد الأذان" :
                "approach".equals(kind) ? "تنبيه اقتراب الصلاة" :
                "gentle".equals(kind) ? "تذكير بالذكر" : "قيام الليل";
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText("يعمل في الخلفية")
                .setOngoing(true);
        startForeground(NOTIFICATION_ID, b.build());
        updatePlaybackNotification();
        boolean adhanActive = getSharedPreferences("zad_reminders", MODE_PRIVATE).getBoolean("adhan_active", false);
        if (adhanActive && !"dua".equals(kind)) { finishPlayback(); return START_NOT_STICKY; }
        if (!"dua".equals(kind)) {
            Intent stopRadio = new Intent(this, RadioPlaybackService.class);
            stopRadio.setAction(RadioPlaybackService.ACTION_STOP);
            try { startService(stopRadio); } catch (Exception ignored) {}
        }
        stopCurrentPlayers();
        if ("approach".equals(kind)) {
            playFile(requestedAudioFile == null ? "approach" : requestedAudioFile, null);
        } else if ("gentle".equals(kind)) {
            playFile(requestedAudioFile == null ? nextRecordedAyah() : requestedAudioFile, null);
        } else if ("morning".equals(kind) || "evening".equals(kind) || "qiyam".equals(kind)) {
            // All spoken reminders use the downloaded human Quran recitations only.
            playFile(nextRecordedAyah(), null);
        } else {
            playFile(kind, null);
        }
        return START_NOT_STICKY;
    }

    private void updatePlaybackNotification(){
        try{
            boolean playing=player!=null&&player.isPlaying();
            Intent toggle=new Intent(this,ReminderAudioService.class);toggle.setAction(ACTION_TOGGLE);
            PendingIntent toggleAction=PendingIntent.getService(this,7522,toggle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Intent stop=new Intent(this,ReminderAudioService.class);stop.setAction(ACTION_STOP);
            PendingIntent stopAction=PendingIntent.getService(this,7521,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            String title="dua".equals(activeKind)?"دعاء ما بعد الأذان":
                    "morning".equals(activeKind)?"أذكار الصباح":
                    "evening".equals(activeKind)?"أذكار المساء":
                    "approach".equals(activeKind)?"تنبيه الصلاة":
                    "gentle".equals(activeKind)?"تذكير بالذكر":"قيام الليل";
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title)
                    .setContentText(!currentTrackTitle.isEmpty()?currentTrackTitle:(playing?"يعمل في الخلفية":"متوقف مؤقتًا"))
                    .setOngoing(true).setOnlyAlertOnce(true)
                    .addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,
                            playing?"إيقاف مؤقت":"استكمال",toggleAction)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel,"إيقاف",stopAction);
            NotificationManager nm=getSystemService(NotificationManager.class);
            if(nm!=null)nm.notify(NOTIFICATION_ID,b.build());
        }catch(Exception error){android.util.Log.w("ZadAlmoslem","Could not update reminder playback notification",error);}
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel c = new NotificationChannel(CHANNEL, "التذكيرات الصوتية", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("تشغيل أذكار الصباح والمساء ودعاء ما بعد الأذان");
            nm.createNotificationChannel(c);
        }
    }

    private String nextRecordedAyah() {
        android.content.SharedPreferences prefs=getSharedPreferences("zad_reminders",MODE_PRIVATE);
        int index=prefs.getInt("recorded_ayah_rotation",0);
        if(index<0 || index>3) index=0;
        prefs.edit().putInt("recorded_ayah_rotation",(index+1)%4).apply();
        return "gentle_"+index;
    }

    private void playFile(String kind, String unusedMessage) {
        try {
            android.content.SharedPreferences reminderPrefs=getSharedPreferences("zad_reminders",MODE_PRIVATE);
            String reciter=reminderPrefs.getString("reminder_reciter","basit");
            if(!"husary".equals(reciter))reciter="basit";
            File reminderDir=new File(getFilesDir(),"reminders");
            File file=new File(reminderDir,reciter+"_"+kind+".mp3");
            if(!file.isFile()||file.length()<5000) file=new File(reminderDir,kind+".mp3");
            String remoteUrl = recordedRecitationUrl(kind);
            if ((!file.isFile() || file.length() < 10000) && remoteUrl == null) {
                android.util.Log.w("ZadAlmoslem", "Reminder audio is unavailable: " + kind);
                finishPlayback();
                return;
            }
            player = new MediaPlayer();
            player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            if ("dua".equals(kind)) {
                int volume=getSharedPreferences("zad_reminders",MODE_PRIVATE).getInt("dua_after_adhan_volume",80);
                player.setVolume(volume/100f,volume/100f);
            }
            player.setOnCompletionListener(mp -> finishPlayback());
            player.setOnErrorListener((mp, what, extra) -> {
                android.util.Log.e("ZadAlmoslem", "Reminder recitation playback failed: " + kind + " (" + what + "/" + extra + ")");
                finishPlayback();
                return true;
            });
            player.setOnPreparedListener(mp -> {
                int level = "dua".equals(kind)
                        ? reminderPrefs.getInt("dua_after_adhan_volume", 80)
                        : ("gentle".equals(kind) ? reminderPrefs.getInt("gentle_audio_volume", 75) : 75);
                if (reminderPrefs.getBoolean("power_saver_enabled", false)) level = Math.min(level, 20);
                mp.setVolume(level / 100f, level / 100f);
                mp.start();
                updatePlaybackNotification();
            });
            if (file.isFile() && file.length() >= 10000) player.setDataSource(file.getAbsolutePath());
            else player.setDataSource(remoteUrl);
            player.prepareAsync();
        } catch (Exception e) { finishPlayback(); }
    }

    private String recordedRecitationUrl(String kind) {
        String ayah;
        if ("approach".equals(kind)) ayah="002238";
        else if ("gentle_0".equals(kind)) ayah="013028";
        else if ("gentle_1".equals(kind)) ayah="002152";
        else if ("gentle_2".equals(kind)) ayah="033041";
        else if ("gentle_3".equals(kind)) ayah="003191";
        else return null;
        String reciter=getSharedPreferences("zad_reminders",MODE_PRIVATE).getString("reminder_reciter","basit");
        String folder="husary".equals(reciter)?"Husary_128kbps":"Abdul_Basit_Murattal_192kbps";
        return "https://everyayah.com/data/"+folder+"/"+ayah+".mp3";
    }

    private void playNextPostAdhanTrack(){
        if(userStopped||postAdhanQueueIndex>=POST_ADHAN_DUAS.length){finishPlayback();return;}
        if(!sharawyPlayed){
            sharawyPlayed=true;
            File sharawy=new File(new File(getFilesDir(),"reminders"),"dua.mp3");
            if(sharawy.isFile()&&sharawy.length()>10000){
                currentTrackTitle="دعاء الشيخ محمد متولي الشعراوي";
                playPostAdhanFile(sharawy);
                return;
            }
            android.util.Log.w("ZadAlmoslem","Sheikh Al-Shaarawy dua is not downloaded; continuing with the available recitations");
        }
        String[] item=POST_ADHAN_DUAS[postAdhanQueueIndex++];
        currentTrackTitle=postAdhanTitle(item[0]);
        updatePlaybackNotification();
        try{
            stopCurrentPlayers();
            File local=new File(new File(getFilesDir(),"duas"),item[0]+".mp3");
            player=new MediaPlayer();player.setWakeMode(getApplicationContext(),PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            int volume=getSharedPreferences("zad_reminders",MODE_PRIVATE).getInt("dua_after_adhan_volume",80);
            player.setVolume(volume/100f,volume/100f);
            player.setOnCompletionListener(mp->finishPlayback());
            player.setOnErrorListener((mp,what,extra)->{android.util.Log.w("ZadAlmoslem","Post-adhan dua skipped after playback error: "+item[0]);finishPlayback();return true;});
            player.setOnPreparedListener(mp->{mp.start();updatePlaybackNotification();});
            if(local.isFile()&&local.length()>10000)player.setDataSource(local.getAbsolutePath());else player.setDataSource(item[1]);
            player.prepareAsync();
        }catch(Exception error){android.util.Log.w("ZadAlmoslem","Could not start post-adhan dua "+item[0],error);finishPlayback();}
    }

    private void playPostAdhanFile(File file){
        try{
            stopCurrentPlayers();
            player=new MediaPlayer();
            player.setWakeMode(getApplicationContext(),PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            int volume=getSharedPreferences("zad_reminders",MODE_PRIVATE).getInt("dua_after_adhan_volume",80);
            player.setVolume(volume/100f,volume/100f);
            player.setOnCompletionListener(mp->finishPlayback());
            player.setOnErrorListener((mp,what,extra)->{finishPlayback();return true;});
            player.setOnPreparedListener(mp->{mp.start();updatePlaybackNotification();});
            player.setDataSource(file.getAbsolutePath());
            player.prepareAsync();
        }catch(Exception error){android.util.Log.w("ZadAlmoslem","Could not play downloaded Al-Shaarawy dua",error);finishPlayback();}
    }

    private String postAdhanTitle(String key){
        switch(key){
            case "alsayed_abdalzez_1": return "الشيخ السيد عبدالعزيز السعدني";
            case "alsayed_abdalzez_2": return "الشيخ السيد عبدالعزيز السعدني";
            case "muhammed_alasyouty": return "الشيخ محمد الأسيوطي";
            case "salah_algamal": return "الشيخ صلاح الجمل";
            case "muhammad_jibril": return "الشيخ محمد جبريل";
            case "hany_alrefaey": return "الشيخ هاني الرفاعي";
            case "edres_abkar": return "الشيخ إدريس أبكر";
            case "khalid_aljalil": return "الشيخ خالد الجليل";
            case "maher_m3equly": return "الشيخ ماهر المعيقلي";
            case "naser_alqutamy": return "الشيخ ناصر القطامي";
            case "yaser_aldosy": return "الشيخ ياسر الدوسري";
            case "mashary_rashed": return "الشيخ مشاري راشد";
            default: return "دعاء صوتي";
        }
    }

    private void stopCurrentPlayers() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    private void finishPlayback() {
        if("dua".equals(activeKind)&&!userStopped&&postAdhanQueueIndex<POST_ADHAN_DUAS.length){stopCurrentPlayers();playNextPostAdhanTrack();return;}
        boolean resumeRadio = "dua".equals(activeKind);
        stopCurrentPlayers();
        // Start/resume the radio while this playback service is still foreground.
        // Dropping foreground first can make Android reject the subsequent service start.
        if (resumeRadio) {
            Intent resumeAudio = new Intent(this, RadioPlaybackService.class);
            resumeAudio.setAction(RadioPlaybackService.ACTION_RESUME_AFTER_ADHAN);
            try { startService(resumeAudio); }
            catch (Exception error) {
                android.util.Log.w("ZadAlmoslem", "Could not resume background audio after post-adhan dua", error);
            }
        }
        if(resumeRadio)RamadanAudioService.resumeAfterAdhan(this);
        stopForeground(true);
        activeKind = null;
        stopSelf();
    }

    @Override public void onDestroy() { stopCurrentPlayers(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
