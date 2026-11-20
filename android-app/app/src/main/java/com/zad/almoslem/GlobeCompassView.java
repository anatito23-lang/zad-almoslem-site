package com.zad.almoslem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class GlobeCompassView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float heading = 0, bearing = 0;
    public GlobeCompassView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    public void setDirections(float h, float b) { heading=h; bearing=b; invalidate(); }
    private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float w) {
        p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(w); c.drawLine(x1,y1,x2,y2,p);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx=getWidth()/2f, cy=getHeight()/2f, r=Math.min(getWidth(),getHeight())*.39f;
        p.setStyle(Paint.Style.FILL); p.setColor(0xffdceee8); c.drawCircle(cx,cy,r,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.2f); p.setColor(0xff176b5b); c.drawCircle(cx,cy,r,p);
        p.setColor(0xff83b9a9); p.setStrokeWidth(1.5f);
        c.drawOval(new RectF(cx-r*.38f,cy-r,cx+r*.38f,cy+r),p);
        c.drawOval(new RectF(cx-r*.72f,cy-r,cx+r*.72f,cy+r),p);
        c.drawOval(new RectF(cx-r,cy-r*.42f,cx+r,cy+r*.42f),p);
        c.drawOval(new RectF(cx-r,cy-r*.72f,cx+r,cy+r*.72f),p);
        p.setColor(0xff176b5b); p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(r*.19f);
        c.drawText("N",cx,cy-r- dp(9),p);
        c.drawText("S",cx,cy+r+dp(22),p);
        c.drawText("E",cx+r+dp(15),cy+dp(6),p);
        c.drawText("W",cx-r-dp(15),cy+dp(6),p);
        float a=(float)Math.toRadians(bearing-heading-90);
        float len=r*.76f, ex=cx+(float)Math.cos(a)*len, ey=cy+(float)Math.sin(a)*len;
        p.setColor(0xffbd8733); p.setStrokeWidth(dp(5)); p.setStrokeCap(Paint.Cap.ROUND); c.drawLine(cx,cy,ex,ey,p);
        Path tip=new Path(); tip.moveTo(ex,ey); tip.lineTo(ex-(float)Math.cos(a-.48)*dp(17),ey-(float)Math.sin(a-.48)*dp(17)); tip.lineTo(ex-(float)Math.cos(a+.48)*dp(17),ey-(float)Math.sin(a+.48)*dp(17)); tip.close();
        p.setStyle(Paint.Style.FILL); c.drawPath(tip,p);
        p.setColor(0xff176b5b); c.drawCircle(cx,cy,dp(7),p);
        p.setColor(0xffbd8733); c.drawCircle(cx,cy,dp(3),p);
    }
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
}