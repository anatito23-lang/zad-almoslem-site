package com.zad.almoslem;

import android.app.*;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.*;
import java.io.File;

public class RamadanAudioService extends Service {
    private static final String CHANNEL="zad_ramadan_audio";
    private static final int ID=7630;
    private MediaPlayer player;
    private AudioTrack cannonTrack;
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
        if ("ramadan_iftar_cannon".equals(key)) {
            playCannonSound();
            return START_NOT_STICKY;
        }
        if ("ramadan_adhan_test".equals(key)) {
            playBundledAdhan();
            return START_NOT_STICKY;
        }
        String fileName="ramadan_iftar_voice".equals(key)?"iftar_voice.mp3":"mesaharaty.mp3";
        File file=new File(new File(getFilesDir(),"ramadan_audio"),fileName);
        if(file.isFile()&&file.length()>1000){playFile(file);return START_NOT_STICKY;}
        if ("ramadan_iftar_voice".equals(key)) playBundledAdhan();
        else showMissingRecording();
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
        }catch(Exception e){android.util.Log.e("ZadRamadan","Audio file playback failed",e);showMissingRecording();}
    }
    // A short, locally synthesized cannon boom: no user audio file or internet required.
    private void playCannonSound(){
        try {
            final int sampleRate=44100;
            final int count=(int)(sampleRate*2.1f);
            short[] pcm=new short[count];
            long seed=0x5DEECE66DL;
            for(int i=0;i<count;i++){
                float t=i/(float)sampleRate;
                float envelope=(float)Math.exp(-3.1f*t);
                float sweep=78f-34f*Math.min(1f,t/1.7f);
                float boom=(float)Math.sin(2.0*Math.PI*sweep*t);
                seed=(seed*25214903917L+11)&((1L<<48)-1);
                float noise=(((seed>>>16)&0x7fff)/16384f)-1f;
                float transientNoise=(float)Math.exp(-22f*t);
                float sample=(boom*0.72f+noise*transientNoise*0.30f)*envelope;
                pcm[i]=(short)(Math.max(-1f,Math.min(1f,sample))*24500);
            }
            AudioTrack track=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.length*2).build();
            cannonTrack=track;
            track.write(pcm,0,pcm.length);
            track.setNotificationMarkerPosition(pcm.length);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener(){
                public void onMarkerReached(AudioTrack t){finish();}
                public void onPeriodicNotification(AudioTrack t){}
            });
            track.play();
            new Handler(Looper.getMainLooper()).postDelayed(this::finish,2600);
        } catch(Exception error) {
            android.util.Log.e("ZadRamadan","Cannon sound playback failed",error);
            showMissingRecording();
        }
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
                    .setContentText("أضف تسجيلًا بشريًا من إعدادات أصوات رمضان لتشغيله هنا.")
                    .setOngoing(false).setAutoCancel(true);
            getSystemService(NotificationManager.class).notify(ID,b.build());
        }catch(Exception e){android.util.Log.e("ZadRamadan","Could not show missing recording notice",e);}
        new Handler(Looper.getMainLooper()).postDelayed(this::finish,2500);
    }
    private void stopCurrent(){
        if(player!=null){try{player.stop();}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}player=null;}
        if(cannonTrack!=null){try{cannonTrack.stop();}catch(Exception ignored){}try{cannonTrack.release();}catch(Exception ignored){}cannonTrack=null;}
        if(tts!=null){try{tts.stop();tts.shutdown();}catch(Exception ignored){}tts=null;}
    }
    private void finish(){stopCurrent();if(wakeLock!=null&&wakeLock.isHeld()){try{wakeLock.release();}catch(Exception ignored){}wakeLock=null;}stopForeground(true);stopSelf();}
    @Override public void onDestroy(){stopCurrent();if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
