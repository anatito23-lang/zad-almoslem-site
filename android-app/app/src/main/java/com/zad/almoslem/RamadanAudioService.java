package com.zad.almoslem;

import android.app.*;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;
import android.os.*;
import java.io.File;

public class RamadanAudioService extends Service {
    private static final String CHANNEL="zad_ramadan_audio";
    private static final int ID=7630;
    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String key=intent==null?"":intent.getStringExtra("key");
        createChannel();
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("زاد وطمأنينة — رمضان")
          .setContentText("تذكير رمضاني").setOngoing(true);
        startForeground(ID,b.build());
        stopCurrent();
        String mode=getSharedPreferences("zad_ramadan",MODE_PRIVATE).getString(
                "ramadan_iftar_cannon".equals(key)||"ramadan_iftar_voice".equals(key)?"iftar_mode":"suhoor_mode","music");
        String fileName=null;
        if("ramadan_music_test".equals(key))fileName="ramadan_music.mp3";
        else if("ramadan_iftar_voice_test".equals(key))fileName="iftar_announcement.mp3";
        else if("ramadan_cannon_test".equals(key))fileName="iftar_cannon.mp3";
        else if("ramadan_suhoor_voice_test".equals(key))fileName="suhoor_voice.mp3";
        else if("ramadan_iftar_voice".equals(key))fileName="voice".equals(mode)?"iftar_announcement.mp3":"ramadan_music.mp3";
        else if("ramadan_iftar_cannon".equals(key))fileName="voice".equals(mode)?"iftar_cannon.mp3":"ramadan_music.mp3";
        else if("ramadan_suhoor".equals(key))fileName="voice".equals(mode)?"suhoor_voice.mp3":"ramadan_music.mp3";
        // Built-in Arabic speech: no user-provided recording or network download is required.
        // The actual voice is supplied by the device's installed TTS engine; pitch is lowered
        // for a deeper delivery, but Android does not expose reliable voice-gender metadata.
        if ("ramadan_iftar_voice".equals(key) || "ramadan_iftar_voice_test".equals(key)) {
            speakArabic("اقترب موعد أذان المغرب");
            return START_NOT_STICKY;
        }
        if ("ramadan_suhoor".equals(key) || "ramadan_suhoor_voice_test".equals(key)) {
            speakArabic("اصحى يا نايم، وحد الله");
            return START_NOT_STICKY;
        }
        if(fileName!=null){
            File audio=new File(new File(getFilesDir(),"ramadan_audio"),fileName);
            if(audio.isFile()&&audio.length()>1000)playFile(audio);else showMissingRecording();
            return START_NOT_STICKY;
        }
        if ("ramadan_adhan_test".equals(key)) {
            playBundledAdhan();
            return START_NOT_STICKY;
        }
        String fallbackFileName="ramadan_iftar_voice".equals(key)?"iftar_announcement.mp3":"suhoor_voice.mp3";
        File fallbackFile=new File(new File(getFilesDir(),"ramadan_audio"),fallbackFileName);
        if(fallbackFile.isFile()&&fallbackFile.length()>1000){playFile(fallbackFile);return START_NOT_STICKY;}
        showMissingRecording();
        return START_NOT_STICKY;
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel c=new NotificationChannel(CHANNEL,"تنبيهات رمضان الصوتية",NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("الموسيقى الرمضانية والأذان");nm.createNotificationChannel(c);}
    }
    private void speakArabic(String phrase){
        try{
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ZadAlmoslem:RamadanSpeech");
            wakeLock.acquire(90*1000L);
            final TextToSpeech[] holder=new TextToSpeech[1];
            holder[0]=new TextToSpeech(this,status->{
                TextToSpeech tts=holder[0];
                if(tts==null)return;
                if(status!=TextToSpeech.SUCCESS){
                    tts.shutdown();
                    showSpeechUnavailable();
                    return;
                }
                int language=tts.setLanguage(new Locale("ar"));
                if(language==TextToSpeech.LANG_MISSING_DATA||language==TextToSpeech.LANG_NOT_SUPPORTED){
                    tts.shutdown();
                    showSpeechUnavailable();
                    return;
                }
                tts.setSpeechRate(0.88f);
                tts.setPitch(0.82f);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    @Override public void onStart(String id){}
                    @Override public void onDone(String id){tts.shutdown();new Handler(Looper.getMainLooper()).post(RamadanAudioService.this::finish);}
                    @Override public void onError(String id){tts.shutdown();new Handler(Looper.getMainLooper()).post(RamadanAudioService.this::finish);}
                });
                if(Build.VERSION.SDK_INT>=21)tts.speak(phrase,TextToSpeech.QUEUE_FLUSH,null,"zad_ramadan_speech");
                else tts.speak(phrase,TextToSpeech.QUEUE_FLUSH,null);
            });
        }catch(Exception e){
            android.util.Log.e("ZadRamadan","Arabic TTS playback failed",e);
            showSpeechUnavailable();
        }
    }
    private void showSpeechUnavailable(){
        try{
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("زاد وطمأنينة — الصوت الرجالي")
                    .setContentText("يلزم توفر صوت عربي في محرك تحويل النص إلى كلام على الجهاز.")
                    .setOngoing(false).setAutoCancel(true);
            getSystemService(NotificationManager.class).notify(ID,b.build());
        }catch(Exception e){android.util.Log.e("ZadRamadan","Could not show TTS notice",e);}
        new Handler(Looper.getMainLooper()).postDelayed(this::finish,2500);
    }
    private void playFile(File file){
        try{
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ZadAlmoslem:RamadanAudio");wakeLock.acquire(3*60*1000L);
            player=new MediaPlayer();player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setOnCompletionListener(mp->finish());player.setOnErrorListener((mp,w,e)->{finish();return true;});
            player.setDataSource(file.getAbsolutePath());player.setOnPreparedListener(MediaPlayer::start);player.prepareAsync();
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
                    .setContentTitle("زاد وطمأنينة — تسجيل صوتي")
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
