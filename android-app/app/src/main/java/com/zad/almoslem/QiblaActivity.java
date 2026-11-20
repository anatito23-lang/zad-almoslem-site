package com.zad.almoslem;

import android.app.Activity;
import android.graphics.*;
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
    private Sensor rotation, accelerometer, magnetometer;
    private float[] gravityValues, magneticValues;
    private TextView degree,status,cityLabel;
    private GlobeCompass globe;
    private double qiblaBearing=0;
    private float heading=0;
    private boolean hasHeading=false;
    private int green=Color.rgb(8,48,40);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(green); getWindow().setNavigationBarColor(green);
        manager=(SensorManager)getSystemService(SENSOR_SERVICE);
        rotation=manager==null?null:manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer=manager==null?null:manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer=manager==null?null:manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        build();
        updateBearing(1);
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(18),dp(18),dp(18),dp(22));root.setBackgroundColor(Color.rgb(244,247,243));
        TextView title=text("بوصلة القبلة",25,Color.WHITE,true);
        TextView sub=text("كرة أرضية تفاعلية توضح اتجاه الكعبة",13,Color.rgb(222,239,229),false);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setGravity(Gravity.CENTER);head.setPadding(dp(16),dp(16),dp(16),dp(16));head.setBackground(shape(green,18));head.addView(title);head.addView(sub);root.addView(head,new LinearLayout.LayoutParams(-1,-2));
        TextView choose=text("اختر مدينتك",15,green,true);choose.setPadding(0,dp(14),0,dp(4));root.addView(choose);
        Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cities));spinner.setSelection(1);root.addView(spinner,new LinearLayout.LayoutParams(-1,-2));
        cityLabel=text("الزقازيق",13,Color.DKGRAY,false);root.addView(cityLabel);
        globe=new GlobeCompass(this);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(270),dp(270));gp.topMargin=dp(10);gp.bottomMargin=dp(8);root.addView(globe,gp);
        degree=text("جارٍ حساب اتجاه القبلة",20,green,true);root.addView(degree);
        status=text("جارٍ تشغيل مستشعرات الاتجاه...",13,Color.DKGRAY,false);status.setPadding(dp(8),dp(10),dp(8),dp(10));root.addView(status);
        TextView hint=text("أمسك الهاتف أفقيًا وحرّكه ببطء بعيدًا عن المعادن ومصادر التشويش.",12,Color.GRAY,false);root.addView(hint);
        Button back=new Button(this);back.setText("رجوع");root.addView(back);back.setOnClickListener(v->finish());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){cityLabel.setText(cities[pos]);updateBearing(pos);}});
        setContentView(root);
    }
    private void updateBearing(int i){
        double lat1=Math.toRadians(coords[i][0]),lon1=Math.toRadians(coords[i][1]),lat2=Math.toRadians(21.4225),lon2=Math.toRadians(39.8262);
        double dl=lon2-lon1;
        qiblaBearing=(Math.toDegrees(Math.atan2(Math.sin(dl)*Math.cos(lat2),Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(dl)))+360)%360;
        degree.setText(String.format(Locale.getDefault(),"اتجاه القبلة %.0f° من الشمال",qiblaBearing));
        updateGlobe();
    }
    private void updateGlobe(){if(globe!=null)globe.setAngles(heading,(float)qiblaBearing,hasHeading);}
    @Override protected void onResume(){
        super.onResume();
        if(manager==null)return;
        if(rotation!=null){manager.registerListener(this,rotation,SensorManager.SENSOR_DELAY_GAME);status.setText("مستشعر اتجاه الهاتف نشط.");}
        else if(accelerometer!=null&&magnetometer!=null){manager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_GAME);manager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_GAME);status.setText("يعمل بوضع المستشعرات البديل؛ أبعد الهاتف عن المعادن.");}
        else status.setText("مستشعر البوصلة غير متاح على هذا الجهاز؛ اتجاه القبلة محسوب من المدينة فقط.");
    }
    @Override protected void onPause(){super.onPause();if(manager!=null)manager.unregisterListener(this);}
    @Override public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){
            float[] matrix=new float[9];SensorManager.getRotationMatrixFromVector(matrix,e.values);applyMatrix(matrix);return;
        }
        if(e.sensor.getType()==Sensor.TYPE_ACCELEROMETER)gravityValues=e.values.clone();
        if(e.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)magneticValues=e.values.clone();
        if(gravityValues!=null&&magneticValues!=null){
            float[] matrix=new float[9];
            if(SensorManager.getRotationMatrix(matrix,null,gravityValues,magneticValues))applyMatrix(matrix);
        }
    }
    private void applyMatrix(float[] matrix){
        int axisX=SensorManager.AXIS_X,axisY=SensorManager.AXIS_Y;
        int rot=getWindowManager().getDefaultDisplay().getRotation();
        if(rot==Surface.ROTATION_90){axisX=SensorManager.AXIS_Y;axisY=SensorManager.AXIS_MINUS_X;}
        else if(rot==Surface.ROTATION_180){axisX=SensorManager.AXIS_MINUS_X;axisY=SensorManager.AXIS_MINUS_Y;}
        else if(rot==Surface.ROTATION_270){axisX=SensorManager.AXIS_MINUS_Y;axisY=SensorManager.AXIS_X;}
        float[] adjusted=new float[9],orientation=new float[3];
        if(!SensorManager.remapCoordinateSystem(matrix,axisX,axisY,adjusted))return;
        SensorManager.getOrientation(adjusted,orientation);
        float raw=(float)((Math.toDegrees(orientation[0])+360)%360);
        // Smooth small sensor fluctuations without introducing a 0/360 degree jump.
        if(hasHeading){float delta=((raw-heading+540)%360)-180;heading=(heading+delta*.22f+360)%360;}else{heading=raw;hasHeading=true;}
        updateGlobe();
        status.setText("البوصلة تعمل الآن — حرّك الهاتف ببطء للحصول على قراءة أدق.");
    }
    @Override public void onAccuracyChanged(Sensor s,int accuracy){
        if(accuracy==SensorManager.SENSOR_STATUS_UNRELIABLE)status.setText("قراءة البوصلة غير مستقرة. عاير الهاتف بحركة رقم 8 وأبعده عن المعادن.");
        else if(accuracy==SensorManager.SENSOR_STATUS_ACCURACY_LOW)status.setText("دقة المستشعر منخفضة؛ حرّك الهاتف على شكل رقم 8 لمعايرته.");
    }

    private class GlobeCompass extends View {
        private final Paint p=new Paint(3);
        private float phoneHeading=0,qibla=0;
        private boolean active=false;
        GlobeCompass(Activity a){super(a);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setAngles(float h,float q,boolean ready){phoneHeading=h;qibla=q;active=ready;invalidate();}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.43f;
            p.setShader(new RadialGradient(cx-r*.28f,cy-r*.35f,r*1.55f,new int[]{Color.rgb(88,170,139),Color.rgb(26,103,83),Color.rgb(7,48,47)},null,Shader.TileMode.CLAMP));
            p.setStyle(Paint.Style.FILL);canvas.drawCircle(cx,cy,r,p);p.setShader(null);
            canvas.save();Path clip=new Path();clip.addCircle(cx,cy,r,Path.Direction.CW);canvas.clipPath(clip);
            p.setColor(Color.argb(65,220,245,231));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.3f));
            canvas.drawOval(cx-r*.98f,cy-r*.42f,cx+r*.98f,cy+r*.42f,p);
            canvas.drawOval(cx-r*.72f,cy-r*.72f,cx+r*.72f,cy+r*.72f,p);
            canvas.drawLine(cx-r,cy,cx+r,cy,p);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(40,4,35,34));
            Path land=new Path();land.moveTo(cx-r*.68f,cy-r*.24f);land.cubicTo(cx-r*.5f,cy-r*.62f,cx-r*.18f,cy-r*.55f,cx-r*.05f,cy-r*.3f);land.cubicTo(cx+r*.12f,cy-r*.16f,cx+r*.02f,cy+r*.04f,cx-r*.18f,cy+r*.12f);land.cubicTo(cx-r*.35f,cy+r*.36f,cx-r*.62f,cy+r*.22f,cx-r*.68f,cy-r*.24f);canvas.drawPath(land,p);
            Path land2=new Path();land2.moveTo(cx+r*.12f,cy-r*.55f);land2.cubicTo(cx+r*.52f,cy-r*.52f,cx+r*.68f,cy-r*.22f,cx+r*.48f,cy-r*.04f);land2.cubicTo(cx+r*.65f,cy+r*.22f,cx+r*.35f,cy+r*.48f,cx+r*.18f,cy+r*.26f);land2.cubicTo(cx+r*.05f,cy+r*.06f,cx+r*.25f,cy-r*.1f,cx+r*.12f,cy-r*.55f);canvas.drawPath(land2,p);
            canvas.restore();
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(205,174,105));canvas.drawCircle(cx,cy,r,p);
            p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(13));p.setColor(Color.WHITE);canvas.drawText("N",cx,cy-r-dp(8),p);
            canvas.save();canvas.rotate(active?qibla-phoneHeading:qibla,cx,cy);
            p.setColor(Color.rgb(242,207,126));p.setStyle(Paint.Style.FILL);
            Path needle=new Path();needle.moveTo(cx,cy-r*.68f);needle.lineTo(cx-dp(12),cy+dp(13));needle.lineTo(cx,cy+dp(5));needle.lineTo(cx+dp(12),cy+dp(13));needle.close();canvas.drawPath(needle,p);
            p.setColor(Color.WHITE);canvas.drawCircle(cx,cy,dp(5),p);canvas.restore();
            p.setColor(Color.WHITE);p.setTextSize(dp(11));p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT);canvas.drawText(active?"اتجاه الهاتف مباشر":"الاتجاه محسوب من المدينة",cx,cy+r+dp(22),p);
        }
    }
}
