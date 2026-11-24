package com.zad.almoslem;

import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.*;
import java.io.File;

public class RamadanAudioService extends Service {
    public static final String ACTION_PAUSE_FOR_ADHAN="com.zad.almoslem.RAMADAN_PAUSE_FOR_ADHAN";
    public static final String ACTION_RESUME_AFTER_ADHAN="com.zad.almoslem.RAMADAN_RESUME_AFTER_ADHAN";
    public static final String ACTION_TOGGLE="com.zad.almoslem.RAMADAN_TOGGLE_AUDIO";
    public static final String ACTION_STOP="com.zad.almoslem.RAMADAN_STOP_AUDIO";
    private boolean userPaused;
    private boolean pausedForAdhan;
    private boolean wasPlayingBeforeAdhan;
    private int positionBeforeAdhan;
    private static final String CHANNEL="zad_ramadan_audio";
    private static final int ID=7630;
    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;
    public static void pauseForAdhan(android.content.Context context){
        Intent i=new Intent(context,RamadanAudioService.class);i.setAction(ACTION_PAUSE_FOR_ADHAN);
        try{context.startService(i);}catch(Exception ignored){}
    }
    public static void resumeAfterAdhan(android.content.Context context){
        Intent i=new Intent(context,RamadanAudioService.class);i.setAction(ACTION_RESUME_AFTER_ADHAN);
        try{context.startService(i);}catch(Exception ignored){}
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_TOGGLE.equals(intent.getAction())){
            if(player!=null){
                if(player.isPlaying()){positionBeforeAdhan=player.getCurrentPosition();player.pause();userPaused=true;wasPlayingBeforeAdhan=false;}
                else {userPaused=false;if(pausedForAdhan)wasPlayingBeforeAdhan=true;else try{player.start();}catch(Exception ignored){}}
                updatePlaybackNotification();
            }
            return START_NOT_STICKY;
        }
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){finish();return START_NOT_STICKY;}
        if(intent!=null&&ACTION_PAUSE_FOR_ADHAN.equals(intent.getAction())){
            if(player!=null&&player.isPlaying()){positionBeforeAdhan=player.getCurrentPosition();wasPlayingBeforeAdhan=true;pausedForAdhan=true;player.pause();updatePlaybackNotification();}
            else if(player==null)stopSelf(startId);
            return START_NOT_STICKY;
        }
        if(intent!=null&&ACTION_RESUME_AFTER_ADHAN.equals(intent.getAction())){
            if(player!=null&&pausedForAdhan&&wasPlayingBeforeAdhan&&!userPaused){
                try{player.seekTo(positionBeforeAdhan);player.start();}
                catch(Exception error){android.util.Log.w("ZadRamadan","Could not resume Ramadan audio after adhan",error);}
                pausedForAdhan=false;wasPlayingBeforeAdhan=false;updatePlaybackNotification();
            }else if(player!=null){pausedForAdhan=false;wasPlayingBeforeAdhan=false;updatePlaybackNotification();}
            else stopSelf(startId);
            return START_NOT_STICKY;
        }
        String key=intent==null?"":intent.getStringExtra("key");
        if("stop".equals(key)){finish();return START_NOT_STICKY;}
        float volume=intent==null?1f:intent.getFloatExtra("volume",1f);
        volume=Math.max(0f,Math.min(1f,volume));
        createChannel();
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("سكينة — رمضان")
          .setContentText("تشغيل صوت رمضاني").setOngoing(true);
        startForeground(ID,b.build());
        updatePlaybackNotification();
        stopCurrent();
        SharedPreferences prefs=getSharedPreferences("zad_ramadan",MODE_PRIVATE);
        String fileName=null;

        if ("ramadan_iftar_cannon".equals(key) || "ramadan_cannon_test".equals(key)) {
            fileName="iftar_cannon.mp3";
        } else if ("ramadan_iftar_voice_test".equals(key) || "ramadan_iftar_voice".equals(key)) {
            fileName="iftar_announcement.mp3";
        } else if ("ramadan_suhoor_voice_test".equals(key) || "ramadan_suhoor".equals(key)) {
            fileName="suhoor_voice.mp3";
        }

        if (fileName != null) {
            // The administration panel is the sole publishing source. A cached file
            // is valid only when its matching admin URL has been synchronized.
            String remoteKey=key;
            if("ramadan_cannon_test".equals(key)) remoteKey="ramadan_iftar_cannon";
            else if("ramadan_iftar_voice_test".equals(key)) remoteKey="ramadan_iftar_voice";
            else if("ramadan_suhoor_voice_test".equals(key)) remoteKey="ramadan_suhoor";
            String remoteUrl=prefs.getString("remote_"+remoteKey,"");
            if(!remoteUrl.startsWith("https://")) {
                // Use the bundled, human-recorded CC0 adhan as the default suhoor alert.
                // An administrator-published suhoor recording may still override it.
                if("ramadan_suhoor_voice_test".equals(key)||"ramadan_suhoor".equals(key)){
                    playBundledAdhan();
                    return START_NOT_STICKY;
                }
                showMissingRecording();
                return START_NOT_STICKY;
            }
            File audio=new File(new File(getFilesDir(),"ramadan_audio"),fileName);
            if(audio.isFile() && audio.length()>1000) playFile(audio,volume);
            else playRemote(remoteUrl,volume);
            return START_NOT_STICKY;
        }
        if ("ramadan_adhan_test".equals(key)) {
            playBundledAdhan();
            return START_NOT_STICKY;
        }
        String fallbackFileName="ramadan_iftar_voice".equals(key)?"iftar_announcement.mp3":"suhoor_voice.mp3";
        File fallbackFile=new File(new File(getFilesDir(),"ramadan_audio"),fallbackFileName);
        if(fallbackFile.isFile()&&fallbackFile.length()>1000){playFile(fallbackFile,1f);return START_NOT_STICKY;}
        showMissingRecording();
        return START_NOT_STICKY;
    }
    private void playRemote(String url,float volume){
        try{
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ZadAlmoslem:RamadanRemoteAudio");wakeLock.acquire(4*60*1000L);
            player=new MediaPlayer();player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setOnCompletionListener(mp->finish());player.setOnErrorListener((mp,w,e)->{finish();return true;});
            player.setDataSource(url);player.setOnPreparedListener(mp->{mp.setVolume(volume,volume);mp.start();updatePlaybackNotification();});player.prepareAsync();
        }catch(Exception error){android.util.Log.e("ZadRamadan","Remote Ramadan audio playback failed",error);showMissingRecording();}
    }
    private void updatePlaybackNotification(){
        try{
            boolean playing=player!=null&&player.isPlaying();
            Intent toggle=new Intent(this,RamadanAudioService.class);toggle.setAction(ACTION_TOGGLE);
            PendingIntent toggleAction=PendingIntent.getService(this,7631,toggle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Intent stop=new Intent(this,RamadanAudioService.class);stop.setAction(ACTION_STOP);
            PendingIntent stopAction=PendingIntent.getService(this,7632,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("سكينة — رمضان")
                    .setContentText(playing?"يعمل في الخلفية":"متوقف مؤقتًا")
                    .setOngoing(true).setOnlyAlertOnce(true)
                    .addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,
                            playing?"إيقاف مؤقت":"استكمال",toggleAction)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel,"إيقاف",stopAction);
            NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.notify(ID,b.build());
        }catch(Exception error){android.util.Log.w("ZadRamadan","Could not update Ramadan audio notification",error);}
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel c=new NotificationChannel(CHANNEL,"تنبيهات رمضان الصوتية",NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("الموسيقى الرمضانية والأذان");nm.createNotificationChannel(c);}
    }
    private void playFile(File file,float volume){
        try{
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ZadAlmoslem:RamadanAudio");wakeLock.acquire(3*60*1000L);
            player=new MediaPlayer();player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setOnCompletionListener(mp->finish());player.setOnErrorListener((mp,w,e)->{finish();return true;});
            player.setDataSource(file.getAbsolutePath());player.setOnPreparedListener(mp->{mp.setVolume(volume,volume);mp.start();updatePlaybackNotification();});player.prepareAsync();
        }catch(Exception e){android.util.Log.e("ZadRamadan","Audio file playback failed",e);showMissingRecording();}
    }
    private void playBundledAdhan(){
        try{
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ZadAlmoslem:RamadanAdhan");
            wakeLock.acquire(4*60*1000L);
            player=new MediaPlayer();
            player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            android.content.res.AssetFileDescriptor audio=getResources().openRawResourceFd(R.raw.beautiful_adhan);
            try{player.setDataSource(audio.getFileDescriptor(),audio.getStartOffset(),audio.getLength());}
            finally{audio.close();}
            player.setOnCompletionListener(mp->finish());
            player.setOnErrorListener((mp,w,e)->{finish();return true;});
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
        }catch(Exception e){
            android.util.Log.e("ZadRamadan","Bundled human adhan playback failed",e);
            showMissingRecording();
        }
    }
    private void showMissingRecording(){
        try{
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("سكينة — تسجيل صوتي")
                    .setContentText("التسجيل غير متاح حاليًا. حاول مرة أخرى لاحقًا.")
                    .setOngoing(false).setAutoCancel(true);
            getSystemService(NotificationManager.class).notify(ID,b.build());
        }catch(Exception e){android.util.Log.e("ZadRamadan","Could not show missing recording notice",e);}
        new Handler(Looper.getMainLooper()).postDelayed(this::finish,2500);
    }
    private void stopCurrent(){
        if(player!=null){try{player.stop();}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}player=null;}
    }
    private void finish(){stopCurrent();if(wakeLock!=null&&wakeLock.isHeld()){try{wakeLock.release();}catch(Exception ignored){}wakeLock=null;}stopForeground(true);stopSelf();}
    @Override public void onDestroy(){stopCurrent();if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
