package com.zad.almoslem;

import android.app.*;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.*;
import android.speech.tts.TextToSpeech;
import java.io.File;
import java.util.Locale;

public class RamadanAudioService extends Service {
    private static final String CHANNEL="zad_ramadan_audio";
    private static final int ID=7630;
    private MediaPlayer player;
    private TextToSpeech tts;
    private PowerManager.WakeLock wakeLock;
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String key=intent==null?"":intent.getStringExtra("key");
        createChannel();
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("زاد وطمأنينة — رمضان")
          .setContentText("تذكير رمضاني").setOngoing(true);
        startForeground(ID,b.build());
        stopCurrent();
        String fileName="ramadan_iftar_voice".equals(key)?"iftar_voice.mp3":
                "ramadan_iftar_cannon".equals(key)?"iftar_cannon.mp3":"mesaharaty.mp3";
        File file=new File(new File(getFilesDir(),"ramadan_audio"),fileName);
        if(file.isFile()&&file.length()>1000){playFile(file);return START_NOT_STICKY;}
        speakFallback(key);
        return START_NOT_STICKY;
    }
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel c=new NotificationChannel(CHANNEL,"تنبيهات رمضان الصوتية",NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("مدفع الإفطار وتنبيه السحور");nm.createNotificationChannel(c);}
    }
    private void playFile(File file){
        try{
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ZadAlmoslem:RamadanAudio");wakeLock.acquire(3*60*1000L);
            player=new MediaPlayer();player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setOnCompletionListener(mp->finish());player.setOnErrorListener((mp,w,e)->{finish();return true;});
            player.setDataSource(file.getAbsolutePath());player.setOnPreparedListener(MediaPlayer::start);player.prepareAsync();
        }catch(Exception e){android.util.Log.e("ZadRamadan","Audio file playback failed",e);speakFallback("");}
    }
    private void speakFallback(String key){
        String phrase="ramadan_iftar_voice".equals(key)?"اقترب موعد الإفطار، اللهم تقبل صيامكم.":
                "ramadan_suhoor".equals(key)?"اصحى يا نايم، وحد الدايم، قوم اتسحر واستعد لصلاة الفجر، رمضان كريم.":
                "حان وقت الإفطار، تقبل الله صيامكم.";
        tts=new TextToSpeech(this,status->{
            if(status==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("ar","EG"));tts.setSpeechRate(0.88f);
                if(Build.VERSION.SDK_INT>=21)tts.speak(phrase,TextToSpeech.QUEUE_FLUSH,null,"ramadan-alert");
                else tts.speak(phrase,TextToSpeech.QUEUE_FLUSH,null);
                new Handler(Looper.getMainLooper()).postDelayed(this::finish,12000);
            }else finish();
        });
    }
    private void stopCurrent(){
        if(player!=null){try{player.stop();}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}player=null;}
        if(tts!=null){try{tts.stop();tts.shutdown();}catch(Exception ignored){}tts=null;}
    }
    private void finish(){stopCurrent();if(wakeLock!=null&&wakeLock.isHeld()){try{wakeLock.release();}catch(Exception ignored){}wakeLock=null;}stopForeground(true);stopSelf();}
    @Override public void onDestroy(){stopCurrent();if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
