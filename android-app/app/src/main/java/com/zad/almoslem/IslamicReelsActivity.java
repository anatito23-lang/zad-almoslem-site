package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;

public class IslamicReelsActivity extends Activity {
    private final String[] headings={"طمأنينة القلب","صلّوا على النبي ﷺ","لا تترك الذكر","دعاء يعينك","اطلب الزيادة من الله","ذكر خفيف وأجر عظيم","دعاء جامع","اذكروني أذكركم"};
    private final String[] texts={
        "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",
        "«مَن صلّى عليَّ واحدةً صلّى الله عليه عشرًا»",
        "«أحبُّ الأعمال إلى الله أدومُها وإن قلَّ»",
        "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ",
        "﴿ وَقُل رَّبِّ زِدْنِي عِلْمًا ﴾",
        "«كلمتان خفيفتان على اللسان، ثقيلتان في الميزان، حبيبتان إلى الرحمن: سبحان الله وبحمده، سبحان الله العظيم»",
        "﴿ رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ ﴾",
        "﴿ فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ ﴾"
    };
    private final String[] sources={
        "سورة الرعد • الآية ٢٨",
        "صحيح مسلم",
        "متفق عليه • صحيح البخاري وصحيح مسلم",
        "سنن أبي داود والنسائي • حديث صحيح",
        "سورة طه • الآية ١١٤",
        "متفق عليه • صحيح البخاري وصحيح مسلم",
        "سورة البقرة • الآية ٢٠١",
        "سورة البقرة • الآية ١٥٢"
    };
    private final int[][] palettes={
        {0xff092f2a,0xff176b58},{0xff172c4c,0xff416b83},{0xff38264f,0xff79516f},{0xff183d43,0xff327d78},
        {0xff3a2d1d,0xff967344},{0xff142e38,0xff3c7778},{0xff293b27,0xff6b8150},{0xff1c2947,0xff4a6488}
    };
    private int index=0;
    private float downY;
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setTextDirection(View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    @Override public void onCreate(Bundle b){super.onCreate(b);AppThemeColors.apply(this);showCard();}
    private void showCard(){
        FrameLayout page=new FrameLayout(this);
        int minHeight=Math.max(dp(520),getResources().getDisplayMetrics().heightPixels-dp(150));
        page.setMinimumHeight(minHeight);
        page.setBackgroundColor(Color.BLACK);
        page.addView(new IslamicBackdrop(palettes[index][0],palettes[index][1]),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout overlay=new LinearLayout(this);overlay.setOrientation(LinearLayout.VERTICAL);overlay.setPadding(dp(22),dp(22),dp(22),dp(20));
        FrameLayout.LayoutParams overlayLp=new FrameLayout.LayoutParams(-1,-1);page.addView(overlay,overlayLp);
        TextView brand=label("زَادُ الْمُسْلِم  •  تَذْكِرَةٌ تُحْيِي الْقَلْب",13,0xffffdf9b,true);
        overlay.addView(brand,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView count=label((index+1)+" / "+headings.length,12,0xfff5f0df,false);
        overlay.addView(count,new LinearLayout.LayoutParams(-1,dp(28)));
        View spacer=new View(this);overlay.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        TextView heading=label(headings[index],21,0xffffe5b0,true);heading.setPadding(dp(8),dp(10),dp(8),dp(14));overlay.addView(heading);
        TextView quote=label(texts[index],index==5?23:25,Color.WHITE,true);quote.setLineSpacing(dp(12),1f);quote.setPadding(dp(8),dp(12),dp(8),dp(18));
        GradientDrawable panel=new GradientDrawable();panel.setColor(0x44202a25);panel.setCornerRadius(dp(22));panel.setStroke(dp(1),0x66f4d99c);quote.setBackground(panel);
        overlay.addView(quote,new LinearLayout.LayoutParams(-1,-2));
        TextView source=label(sources[index],13,0xffffdf9b,true);source.setPadding(0,dp(16),0,dp(8));overlay.addView(source);
        View spacer2=new View(this);overlay.addView(spacer2,new LinearLayout.LayoutParams(1,0,1));
        TextView hint=label("اسحب لأعلى للتذكرة التالية",12,0xfff4eee1,false);overlay.addView(hint,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setOrientation(LinearLayout.HORIZONTAL);
        Button previous=new Button(this);previous.setText("السابق");previous.setOnClickListener(v->move(-1));
        Button next=new Button(this);next.setText("التالي");next.setOnClickListener(v->move(1));
        controls.addView(previous,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(next,new LinearLayout.LayoutParams(0,dp(48),1));overlay.addView(controls);
        page.setOnTouchListener((v,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN){downY=event.getY();return true;}
            if(event.getAction()==MotionEvent.ACTION_UP){float delta=downY-event.getY();if(Math.abs(delta)>dp(45))move(delta>0?1:-1);return true;}
            return true;
        });
        setContentView(AppTabs.wrap(this,page,"more"));
    }
    private void move(int direction){index=(index+direction+headings.length)%headings.length;showCard();}

    private static class IslamicBackdrop extends View {
        private final Paint p=new Paint(3);private final int top,bottom;
        IslamicBackdrop(Activity a,int t,int b){super(a);top=t;bottom=b;}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,top,bottom,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setColor(0x33ffffff);
            for(int i=0;i<28;i++){float x=(i*73%997)/997f*w;float y=(i*131%887)/887f*h;c.drawCircle(x,y,(i%3==0?dp(2):dp(1)),p);}
            p.setColor(0x55f8d88e);c.drawCircle(w*.78f,h*.19f,dp(43),p);p.setColor(top);c.drawCircle(w*.81f,h*.16f,dp(39),p);
            p.setColor(0x553c2118);float base=h*.88f;
            c.drawRect(0,base,w,h,p);
            drawDome(c,w*.5f,base,dp(75),dp(100));
            drawDome(c,w*.18f,base,dp(42),dp(62));
            drawDome(c,w*.82f,base,dp(48),dp(68));
            p.setColor(0x66402018);c.drawRect(w*.08f,base-dp(95),w*.16f,base,p);c.drawRect(w*.84f,base-dp(110),w*.92f,base,p);
            p.setColor(0x66f5dba0);c.drawCircle(w*.12f,base-dp(95),dp(4),p);c.drawCircle(w*.88f,base-dp(110),dp(4),p);
        }
        private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
        private void drawDome(Canvas c,float cx,float base,float width,float height){
            p.setColor(0x55351f19);c.drawRect(cx-width*.52f,base-height*.48f,cx+width*.52f,base,p);
            p.setColor(0x664a2b20);c.drawOval(cx-width*.5f,base-height,cx+width*.5f,base-height*.05f,p);
            p.setColor(0x665d3928);c.drawRect(cx-width*.035f,base-height-dp(20),cx+width*.035f,base-height,p);
            p.setColor(0x77f4d99c);c.drawCircle(cx,base-height-dp(22),dp(3),p);
        }
    }
}
