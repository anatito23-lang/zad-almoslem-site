package com.zad.almoslem;

import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.*;
import java.io.File;

public class RamadanAudioService extends Service {
    private static final String CHANNEL="zad_ramadan_audio";
    private static final int ID=7630;
    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String key=intent==null?"":intent.getStringExtra("key");
        if("stop".equals(key)){finish();return START_NOT_STICKY;}
        float volume=intent==null?1f:intent.getFloatExtra("volume",1f);
        volume=Math.max(0f,Math.min(1f,volume));
        createChannel();
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("سكينة — رمضان")
          .setContentText("تذكير رمضاني").setOngoing(true);
        startForeground(ID,b.build());
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
            File audio=new File(new File(getFilesDir(),"ramadan_audio"),fileName);
            if (audio.isFile() && audio.length()>1000) playFile(audio,volume);
            else {
                // Test buttons use *_test keys, while RemoteAudioAssets stores the
                // matching production URL keys. Resolve both to the same saved asset.
                String remoteKey=key;
                if("ramadan_cannon_test".equals(key)) remoteKey="ramadan_iftar_cannon";
                else if("ramadan_iftar_voice_test".equals(key)) remoteKey="ramadan_iftar_voice";
                else if("ramadan_suhoor_voice_test".equals(key)) remoteKey="ramadan_suhoor";
                String remoteUrl=prefs.getString("remote_"+remoteKey,"");
                if(remoteUrl.startsWith("https://")) playRemote(remoteUrl,volume);
                else showMissingRecording();
            }
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
            player.setDataSource(url);player.setOnPreparedListener(mp->{mp.setVolume(volume,volume);mp.start();});player.prepareAsync();
        }catch(Exception error){android.util.Log.e("ZadRamadan","Remote Ramadan audio playback failed",error);showMissingRecording();}
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
            player.setDataSource(file.getAbsolutePath());player.setOnPreparedListener(mp->{mp.setVolume(volume,volume);mp.start();});player.prepareAsync();
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
                    .setContentText("أضف التسجيل الصوتي المطلوب من قسم أصوات رمضان لتشغيله هنا.")
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
