package com.zad.almoslem;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RemoteAudioAssets {
    private static final String TAG="ZadRemoteAudio";
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static volatile boolean callbackRegistered=false;
    private RemoteAudioAssets(){}
    public static void watchConnectivity(Context context){
        Context app=context.getApplicationContext();
        refresh(app);
        if(callbackRegistered)return;
        synchronized(RemoteAudioAssets.class){
            if(callbackRegistered)return;
            ConnectivityManager manager=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(manager==null)return;
            try{
                NetworkRequest request=new NetworkRequest.Builder().addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
                manager.registerNetworkCallback(request,new ConnectivityManager.NetworkCallback(){
                    @Override public void onAvailable(Network network){refresh(app);}
                });
                callbackRegistered=true;
            }catch(Exception error){Log.w(TAG,"Could not register network callback",error);}
        }
    }
    public static void refresh(Context context){
        refresh(context,null);
    }
    public static void refresh(Context context,Runnable onComplete){
        Context app=context.getApplicationContext();
        FirebaseFirestore.getInstance().collection("app_audio_assets").document("ramadan").get()
          .addOnSuccessListener(snapshot->{
            if(snapshot.exists()&&snapshot.getData()!=null){
                SharedPreferences prefs=app.getSharedPreferences("zad_ramadan",Context.MODE_PRIVATE);
                Map<String,Object> values=snapshot.getData();
                syncOne(app,prefs,values,"cannonUrl","ramadan_iftar_cannon","iftar_cannon.mp3");
                syncOne(app,prefs,values,"iftarVoiceUrl","ramadan_iftar_voice","iftar_announcement.mp3");
                syncOne(app,prefs,values,"suhoorVoiceUrl","ramadan_suhoor","suhoor_voice.mp3");
            }
            if(onComplete!=null)onComplete.run();
          }).addOnFailureListener(error->{
            Log.w(TAG,"Could not read remote audio configuration",error);
            if(onComplete!=null)onComplete.run();
          });
    }
    private static void syncOne(Context context,SharedPreferences prefs,Map<String,Object> data,String field,String key,String fileName){
        Object raw=data.get(field);if(!(raw instanceof String))return;
        String url=((String)raw).trim();if(!url.startsWith("https://"))return;
        String prefKey="remote_"+key;String previous=prefs.getString(prefKey,"");
        prefs.edit().putString(prefKey,url).apply();
        File dir=new File(context.getFilesDir(),"ramadan_audio");File target=new File(dir,fileName);
        if(url.equals(previous)&&target.isFile()&&target.length()>1000)return;
        WORKER.execute(()->download(url,dir,target));
    }
    private static void download(String url,File dir,File target){
        HttpURLConnection connection=null;File temp=new File(dir,target.getName()+".part");
        try{
            if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Cannot create audio cache directory");
            connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(30000);connection.setInstanceFollowRedirects(true);connection.connect();
            int status=connection.getResponseCode();if(status<200||status>=300)throw new IllegalStateException("HTTP "+status);
            long length=connection.getContentLengthLong();if(length>25L*1024L*1024L)throw new IllegalStateException("Audio file exceeds 25 MB");
            try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                byte[] buffer=new byte[16384];int count;long total=0;
                while((count=in.read(buffer))!=-1){total+=count;if(total>25L*1024L*1024L)throw new IllegalStateException("Audio file exceeds 25 MB");out.write(buffer,0,count);}out.getFD().sync();
            }
            if(temp.length()<1000)throw new IllegalStateException("Audio file is empty or invalid");
            if(target.exists()&&!target.delete())throw new IllegalStateException("Cannot replace cached audio");
            if(!temp.renameTo(target))throw new IllegalStateException("Cannot finalize cached audio");
            Log.i(TAG,"Downloaded "+target.getName());
        }catch(Exception error){Log.w(TAG,"Audio download failed for "+target.getName(),error);if(temp.exists())temp.delete();}
        finally{if(connection!=null)connection.disconnect();}
    }
}
