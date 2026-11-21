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
        createChannel();
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("زاد وطمأنينة — رمضان")
          .setContentText("تذكير رمضاني").setOngoing(true);
        startForeground(ID,b.build());
        stopCurrent();
        SharedPreferences prefs=getSharedPreferences("zad_ramadan",MODE_PRIVATE);
        boolean iftarVoice="voice".equals(prefs.getString("iftar_mode","music"));
        boolean suhoorVoice="voice".equals(prefs.getString("suhoor_mode","music"));
        String fileName=null;

        if ("ramadan_music_test".equals(key)) {
            playFile(generateRamadanMusicFile());
            return START_NOT_STICKY;
        }
        if ("ramadan_iftar_cannon".equals(key) || "ramadan_cannon_test".equals(key)) {
            fileName="iftar_cannon.mp3";
        } else if ("ramadan_iftar_voice_test".equals(key) ||
                ("ramadan_iftar_voice".equals(key) && iftarVoice)) {
            fileName="iftar_announcement.mp3";
        } else if ("ramadan_suhoor_voice_test".equals(key) ||
                ("ramadan_suhoor".equals(key) && suhoorVoice)) {
            fileName="suhoor_voice.mp3";
        } else if ("ramadan_iftar_voice".equals(key) || "ramadan_suhoor".equals(key)) {
            playFile(generateRamadanMusicFile());
            return START_NOT_STICKY;
        }

        if (fileName != null) {
            File audio=new File(new File(getFilesDir(),"ramadan_audio"),fileName);
            if (audio.isFile() && audio.length()>1000) playFile(audio);
            else showMissingRecording();
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
    // Original, locally synthesized gentle bell-like Ramadan melody. No download or external track.
    private File generateRamadanMusicFile(){
        final int rate=22050; final double[] notes={392.00,440.00,523.25,493.88,440.00,392.00,329.63,392.00,440.00,523.25,587.33,523.25,440.00,392.00};
        try{
            File out=new File(getCacheDir(),"zad_ramadan_melody.wav");
            int samples=(int)(rate*0.72*notes.length);
            try(java.io.DataOutputStream d=new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(out)))){
                writeWavHeader(d,samples,rate);
                for(int i=0;i<samples;i++){
                    double t=i/(double)rate, beat=t/0.72;int n=Math.min(notes.length-1,(int)beat);
                    double local=(beat-n)*0.72, env=Math.min(1,local/0.035)*Math.min(1,(0.72-local)/0.18);
                    double f=notes[n];double bell=Math.sin(2*Math.PI*f*t)+0.22*Math.sin(2*Math.PI*f*2.01*t)+0.08*Math.sin(2*Math.PI*f*3.98*t);
                    double pad=0.12*Math.sin(2*Math.PI*130.81*t)+0.08*Math.sin(2*Math.PI*196*t);
                    short v=(short)(Math.max(-1,Math.min(1,(bell*0.19*env+pad*0.22))*0.72)*32767);
                    d.writeShort(Short.reverseBytes(v));
                }
            }
            return out;
        }catch(Exception e){throw new RuntimeException("Could not create Ramadan melody",e);}
    }
    // A short synthesized low boom for the iftar-cannon cue; generated on-device.
    private File generateCannonFile(){
        final int rate=22050,samples=rate*3;
        try{
            File out=new File(getCacheDir(),"zad_iftar_cannon.wav");
            try(java.io.DataOutputStream d=new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(out)))){
                writeWavHeader(d,samples,rate);
                long seed=246813579L;
                for(int i=0;i<samples;i++){
                    double t=i/(double)rate;
                    double decay=Math.exp(-t*3.1);
                    seed=(seed*1103515245L+12345L)&0x7fffffffL;
                    double noise=((seed/1073741824.0)-1.0);
                    double boom=Math.sin(2*Math.PI*(82-18*t)*t)*0.9+noise*0.18;
                    double attack=Math.min(1,t/0.008);
                    short v=(short)(Math.max(-1,Math.min(1,boom*decay*attack*0.9))*32767);
                    d.writeShort(Short.reverseBytes(v));
                }
            }
            return out;
        }catch(Exception e){throw new RuntimeException("Could not create cannon cue",e);}
    }
    private void writeWavHeader(java.io.DataOutputStream d,int samples,int rate)throws java.io.IOException{
        int dataBytes=samples*2;
        d.writeBytes("RIFF");writeIntLE(d,36+dataBytes);d.writeBytes("WAVEfmt ");writeIntLE(d,16);
        writeShortLE(d,1);writeShortLE(d,1);writeIntLE(d,rate);writeIntLE(d,rate*2);writeShortLE(d,2);writeShortLE(d,16);
        d.writeBytes("data");writeIntLE(d,dataBytes);
    }
    private void writeIntLE(java.io.DataOutputStream d,int v)throws java.io.IOException{d.writeInt(Integer.reverseBytes(v));}
    private void writeShortLE(java.io.DataOutputStream d,int v)throws java.io.IOException{d.writeShort(Short.reverseBytes((short)v));}
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
