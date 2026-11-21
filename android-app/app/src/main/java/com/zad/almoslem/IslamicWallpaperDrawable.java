package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Locally drawn, scalable Islamic geometric wallpaper for every app tab. */
public final class IslamicWallpaperDrawable extends Drawable {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String style;
    public IslamicWallpaperDrawable(Activity activity, String style) { this.style = style == null ? "home" : style; }

    private int base() {
        if ("radio".equals(style)) return Color.rgb(10,45,58);
        if ("ruqyah".equals(style)) return Color.rgb(13,53,43);
        if ("recordings".equals(style)) return Color.rgb(35,39,61);
        if ("ramadan".equals(style)) return Color.rgb(22,39,65);
        if ("quran".equals(style) || "mushaf".equals(style)) return Color.rgb(8,55,43);
        if ("tasbeeh".equals(style)) return Color.rgb(22,57,52);
        if ("calendar".equals(style)) return Color.rgb(30,53,57);
        return Color.rgb(12,48,43);
    }

    @Override public void draw(Canvas c) {
        float w=getBounds().width(), h=getBounds().height();
        if(w<=0 || h<=0) return;
        int b=base();
        p.setShader(new LinearGradient(0,0,w,h,new int[]{mix(b,Color.rgb(4,18,30),.28f),b,mix(b,Color.rgb(3,23,20),.2f)},null,Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL); c.drawRect(getBounds(),p); p.setShader(null);

        // Delicate eight-point stars and interlaced circles.
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(1f,w/430f)); p.setColor(0x22E9D39B);
        float step=Math.max(64f,w/4.2f);
        for(float y=20;y<h+step;y+=step) for(float x=15;x<w+step;x+=step) {
            float xx=x+((((int)(y/step))&1)==0?0:step/2);
            star(c,xx,y,step*.18f); c.drawCircle(x,y,step*.31f,p);
        }

        // Large mihrab contour.
        p.setStrokeWidth(Math.max(2f,w/125f)); p.setColor(0x24E9D39B);
        Path arch=new Path(); float mid=w/2;
        arch.moveTo(w*.10f,h*.62f); arch.lineTo(w*.10f,h*.25f);
        arch.quadTo(w*.10f,h*.07f,mid,h*.07f); arch.quadTo(w*.90f,h*.07f,w*.90f,h*.25f);
        arch.lineTo(w*.90f,h*.62f); c.drawPath(arch,p);

        // Low-contrast mosque silhouette along the bottom.
        p.setStyle(Paint.Style.FILL); p.setColor(0x26020D12);
        Path mosque=new Path(); mosque.moveTo(0,h*.89f); mosque.lineTo(w*.13f,h*.89f);
        mosque.lineTo(w*.13f,h*.76f); mosque.quadTo(w*.20f,h*.62f,w*.27f,h*.76f);
        mosque.lineTo(w*.27f,h*.89f); mosque.lineTo(w*.39f,h*.89f);
        mosque.lineTo(w*.39f,h*.80f); mosque.quadTo(mid,h*.57f,w*.61f,h*.80f);
        mosque.lineTo(w*.61f,h*.89f); mosque.lineTo(w*.73f,h*.89f);
        mosque.lineTo(w*.73f,h*.76f); mosque.quadTo(w*.80f,h*.62f,w*.87f,h*.76f);
        mosque.lineTo(w*.87f,h*.89f); mosque.lineTo(w,h*.89f); mosque.lineTo(w,h); mosque.lineTo(0,h); mosque.close();
        c.drawPath(mosque,p);

        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(1f,w/280f)); p.setColor(0x35E9D39B);
        c.drawLine(0,h*.89f,w,h*.89f,p);
        minaret(c,w*.075f,h*.89f,h*.27f); minaret(c,w*.925f,h*.89f,h*.27f);
        p.setStyle(Paint.Style.FILL);
    }

    private void star(Canvas c,float x,float y,float r) {
        Path path=new Path();
        for(int i=0;i<16;i++){double a=Math.PI*i/8-Math.PI/2;float rr=(i%2==0)?r:r*.48f;
            float xx=x+(float)Math.cos(a)*rr, yy=y+(float)Math.sin(a)*rr;
            if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}
        path.close(); c.drawPath(path,p);
    }
    private void minaret(Canvas c,float x,float base,float h) {
        p.setColor(0x28E9D39B); p.setStrokeWidth(Math.max(1f,h/55f));
        c.drawLine(x,base,x,base-h*.72f,p); c.drawLine(x-h*.05f,base-h*.42f,x+h*.05f,base-h*.42f,p);
        c.drawLine(x-h*.04f,base-h*.72f,x+h*.04f,base-h*.72f,p); c.drawLine(x,base-h*.72f,x,base-h,p);
        c.drawCircle(x,base-h,h*.018f,p);
    }
    private int mix(int a,int b,float t){return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),(int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));}
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}