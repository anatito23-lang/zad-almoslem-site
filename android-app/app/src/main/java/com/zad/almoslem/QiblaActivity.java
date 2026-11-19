package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.widget.*;
import java.util.Locale;

public class QiblaActivity extends Activity implements SensorEventListener {
    private final String[] cities={"القاهرة","الزقازيق","الإسكندرية","الجيزة","المنصورة","طنطا","الإسماعيلية","أسيوط","الأقصر","أسوان"};
    private final double[][] coords={{30.0444,31.2357},{30.5877,31.5020},{31.2001,29.9187},{30.0131,31.2089},{31.0409,31.3785},{30.7865,31.0004},{30.5965,32.2715},{27.1809,31.1837},{25.6872,32.6396},{24.0889,32.8998}};
    private SensorManager manager;
    private Sensor rotation;
    private TextView arrow, degree, status, cityLabel;
    private double qiblaBearing=0;
    private float heading=0;
    private int green=Color.rgb(8,48,40);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(green); getWindow().setNavigationBarColor(green);
        manager=(SensorManager)getSystemService(SENSOR_SERVICE);
        rotation=manager==null?null:manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        build();
        updateBearing(1);
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(18),dp(22),dp(18),dp(28));root.setBackgroundColor(Color.rgb(244,247,243));
        TextView title=text("بوصلة القبلة",25,Color.WHITE,true);
        TextView sub=text("اتجاه الكعبة من المدينة المختارة",13,Color.rgb(222,239,229),false);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setGravity(Gravity.CENTER);head.setPadding(dp(16),dp(18),dp(16),dp(18));head.setBackground(shape(green,18));head.addView(title);head.addView(sub);root.addView(head,new LinearLayout.LayoutParams(-1,-2));
        TextView choose=text("اختر مدينتك",15,green,true);choose.setPadding(0,dp(18),0,dp(5));root.addView(choose);
        Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cities));spinner.setSelection(1);root.addView(spinner,new LinearLayout.LayoutParams(-1,-2));
        cityLabel=text("الزقازيق",13,Color.DKGRAY,false);root.addView(cityLabel);
        LinearLayout compass=new LinearLayout(this);compass.setGravity(Gravity.CENTER);compass.setBackground(shape(Color.WHITE,120));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(250),dp(250));cp.topMargin=dp(20);cp.bottomMargin=dp(16);root.addView(compass,cp);
        arrow=text("▲",100,Color.rgb(177,135,54),true);compass.addView(arrow);
        degree=text("جارٍ حساب اتجاه القبلة",20,green,true);root.addView(degree);
        status=text(rotation==null?"مستشعر اتجاه الهاتف غير متاح؛ الزاوية محسوبة من الشمال فقط.":"حرّك الهاتف ببطء حتى يستقر المؤشر على اتجاه القبلة.",13,Color.DKGRAY,false);status.setPadding(dp(8),dp(12),dp(8),dp(12));root.addView(status);
        TextView north=text("الشمال ↑     الشرق →     الجنوب ↓     الغرب ←",12,Color.GRAY,false);root.addView(north);
        Button back=new Button(this);back.setText("رجوع");root.addView(back);back.setOnClickListener(v->finish());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){cityLabel.setText(cities[pos]);updateBearing(pos);}});
        setContentView(root);
    }
    private void updateBearing(int i){
        double lat1=Math.toRadians(coords[i][0]),lon1=Math.toRadians(coords[i][1]),lat2=Math.toRadians(21.4225),lon2=Math.toRadians(39.8262);
        double dl=lon2-lon1;
        qiblaBearing=(Math.toDegrees(Math.atan2(Math.sin(dl)*Math.cos(lat2),Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(dl)))+360)%360;
        degree.setText(String.format(Locale.getDefault(),"اتجاه القبلة %.0f° من الشمال",qiblaBearing));
        rotateArrow();
    }
    private void rotateArrow(){if(arrow!=null)arrow.setRotation((float)(qiblaBearing-heading));}
    @Override protected void onResume(){super.onResume();if(rotation!=null)manager.registerListener(this,rotation,SensorManager.SENSOR_DELAY_UI);}
    @Override protected void onPause(){super.onPause();if(manager!=null)manager.unregisterListener(this);}
    @Override public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()!=Sensor.TYPE_ROTATION_VECTOR)return;
        float[] matrix=new float[9],orientation=new float[3];
        SensorManager.getRotationMatrixFromVector(matrix,e.values);
        int axisX=SensorManager.AXIS_X,axisY=SensorManager.AXIS_Y;
        int rot=getWindowManager().getDefaultDisplay().getRotation();
        if(rot==Surface.ROTATION_90){axisX=SensorManager.AXIS_Y;axisY=SensorManager.AXIS_MINUS_X;}
        else if(rot==Surface.ROTATION_180){axisX=SensorManager.AXIS_MINUS_X;axisY=SensorManager.AXIS_MINUS_Y;}
        else if(rot==Surface.ROTATION_270){axisX=SensorManager.AXIS_MINUS_Y;axisY=SensorManager.AXIS_X;}
        float[] adjusted=new float[9];
        SensorManager.remapCoordinateSystem(matrix,axisX,axisY,adjusted);
        SensorManager.getOrientation(adjusted,orientation);
        heading=(float)((Math.toDegrees(orientation[0])+360)%360);
        rotateArrow();
        status.setText("البوصلة تعمل — حرّك الهاتف ببطء للحصول على قراءة مستقرة.");
    }
    @Override public void onAccuracyChanged(Sensor s,int accuracy){}
}
