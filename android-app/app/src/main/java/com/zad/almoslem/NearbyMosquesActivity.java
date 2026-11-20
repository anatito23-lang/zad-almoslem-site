package com.zad.almoslem;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NearbyMosquesActivity extends Activity {
    private static final int LOCATION_REQ=41;
    private LinearLayout list;
    private TextView message;
    private LocationManager lm;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(AppThemeColors.text(this));t.setGravity(Gravity.RIGHT);return t;}
    @Override public void onCreate(Bundle b){super.onCreate(b);AppThemeColors.apply(this);lm=(LocationManager)getSystemService(LOCATION_SERVICE);build();}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(18),dp(16),dp(20));root.setBackgroundColor(AppThemeColors.bg(this));
        TextView title=label("المساجد القريبة منك",23);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView intro=label("اعثر على المساجد حول موقعك واعرف المسافة إليها.",14);intro.setPadding(0,dp(8),0,dp(12));root.addView(intro);
        Button search=new Button(this);search.setText("ابحث عن المساجد القريبة");root.addView(search);search.setOnClickListener(v->startSearch());
        message=label("اضغط على البحث واسمح للتطبيق باستخدام الموقع أثناء هذه الميزة.",13);message.setPadding(0,dp(12),0,dp(10));root.addView(message);
        ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        Button back=new Button(this);back.setText("رجوع");back.setOnClickListener(v->finish());root.addView(back);
        setContentView(AppTabs.wrap(this,root,"more"));
    }
    private void startSearch(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_REQ);return;
        }
        findLocation();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==LOCATION_REQ){boolean ok=false;for(int x:results)if(x==PackageManager.PERMISSION_GRANTED)ok=true;if(ok)findLocation();else message.setText("لم يتم السماح بالموقع. يمكنك تفعيل الإذن من إعدادات التطبيق أو البحث يدويًا في الخرائط.");}}
    private void findLocation(){
        try{
            Location best=null;
            for(String provider:lm.getProviders(true)){
                if((provider.equals(LocationManager.GPS_PROVIDER)||provider.equals(LocationManager.NETWORK_PROVIDER)) && (Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)){
                    Location x=lm.getLastKnownLocation(provider);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;
                }
            }
            if(best!=null){searchMosques(best);return;}
            message.setText("جارٍ تحديد موقعك… تأكد من تشغيل خدمة الموقع.");
            String provider=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)?LocationManager.NETWORK_PROVIDER:LocationManager.GPS_PROVIDER;
            lm.requestSingleUpdate(provider,new LocationListener(){public void onLocationChanged(Location l){searchMosques(l);}public void onProviderDisabled(String p){}public void onProviderEnabled(String p){}public void onStatusChanged(String p,int s,android.os.Bundle b){}},null);
        }catch(SecurityException e){message.setText("تعذر الوصول للموقع. راجع أذونات التطبيق.");}
        catch(Exception e){message.setText("تعذر تحديد الموقع. شغّل خدمة الموقع وحاول مرة أخرى.");}
    }
    private void searchMosques(Location loc){
        runOnUiThread(()->message.setText("جارٍ البحث عن المساجد حولك…"));
        worker.execute(()->{
            HttpURLConnection conn=null;
            try{
                String q="[out:json][timeout:20];(node[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:5000,"+loc.getLatitude()+","+loc.getLongitude()+");way[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:5000,"+loc.getLatitude()+","+loc.getLongitude()+"););out center;";
                URL url=new URL("https://overpass-api.de/api/interpreter?data="+URLEncoder.encode(q,"UTF-8"));
                conn=(HttpURLConnection)url.openConnection();conn.setConnectTimeout(15000);conn.setReadTimeout(25000);conn.setRequestMethod("GET");
                java.io.InputStream in=conn.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
                JSONObject data=new JSONObject(out.toString("UTF-8"));JSONArray els=data.optJSONArray("elements");java.util.ArrayList<JSONObject> found=new java.util.ArrayList<>();
                if(els!=null)for(int i=0;i<els.length();i++){JSONObject e=els.getJSONObject(i);JSONObject tags=e.optJSONObject("tags");if(tags==null)continue;String name=tags.optString("name","مسجد");double lat=e.has("lat")?e.optDouble("lat"):e.optJSONObject("center")!=null?e.getJSONObject("center").optDouble("lat"):Double.NaN;double lon=e.has("lon")?e.optDouble("lon"):e.optJSONObject("center")!=null?e.getJSONObject("center").optDouble("lon"):Double.NaN;if(!Double.isNaN(lat)&&!Double.isNaN(lon)){e.put("_displayName",name);e.put("_distance",distance(loc.getLatitude(),loc.getLongitude(),lat,lon));e.put("_lat",lat);e.put("_lon",lon);found.add(e);}}
                java.util.Collections.sort(found,(a,b)->Double.compare(a.optDouble("_distance"),b.optDouble("_distance")));
                runOnUiThread(()->showResults(found));
            }catch(Exception ex){runOnUiThread(()->message.setText("لم نتمكن من تحميل النتائج الآن. تحقق من الإنترنت وحاول مرة أخرى."));}
            finally{if(conn!=null)conn.disconnect();}
        });
    }
    private double distance(double a,double b,double c,double d){float[] r=new float[1];Location.distanceBetween(a,b,c,d,r);return r[0];}
    private void showResults(java.util.ArrayList<JSONObject> items){
        list.removeAllViews();
        if(items.isEmpty()){message.setText("لم نعثر على مساجد مسجلة ضمن 5 كم. جرّب تحديث البحث أو افتح الخرائط.");return;}
        message.setText("أقرب المساجد المسجلة ضمن نطاق 5 كم — "+items.size()+" نتيجة");
        for(JSONObject e:items){
            String name=e.optString("_displayName","مسجد");double lat=e.optDouble("_lat"),lon=e.optDouble("_lon"),meters=e.optDouble("_distance");
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(11),dp(14),dp(11));card.setBackgroundColor(AppThemeColors.surface(this));
            TextView nm=label(name,17);nm.setTypeface(null,android.graphics.Typeface.BOLD);card.addView(nm);
            TextView ds=label(meters<1000?String.format(Locale.getDefault(),"يبعد %.0f متر",meters):String.format(Locale.getDefault(),"يبعد %.1f كم",meters/1000),13);card.addView(ds);
            Button nav=new Button(this);nav.setText("الاتجاهات إلى المسجد");card.addView(nav);nav.setOnClickListener(v->{Uri uri=Uri.parse("google.navigation:q="+lat+","+lon);Intent i=new Intent(Intent.ACTION_VIEW,uri);i.setPackage("com.google.android.apps.maps");if(i.resolveActivity(getPackageManager())==null)i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+lat+","+lon));startActivity(i);});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(9);list.addView(card,lp);
        }
    }
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}