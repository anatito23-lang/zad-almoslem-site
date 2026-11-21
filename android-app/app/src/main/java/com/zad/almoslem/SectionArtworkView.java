package com.zad.almoslem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.view.View;

/** Purpose-built illustrated artwork for each Islamic audio section. */
public final class SectionArtworkView extends View {
    private final String kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int gold = Color.rgb(238, 202, 132);
    private final int cream = Color.rgb(250, 239, 211);
    private final int mint = Color.rgb(126, 190, 160);
    public SectionArtworkView(Context c, String kind) { super(c); this.kind=kind; setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
    private float d(float n){ return n*getResources().getDisplayMetrics().density; }
    private void fill(Canvas c,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setShader(null);}
    private void stroke(Canvas c,int color,float width){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(width));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setShader(null);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas); float w=getWidth(),h=getHeight(); if(w<=0||h<=0)return;
        int a=Color.rgb(7,45,39), b=Color.rgb(17,91,69);
        if("radio".equals(kind)){a=Color.rgb(12,35,52);b=Color.rgb(35,75,96);}
        if("ruqyah".equals(kind)){a=Color.rgb(21,55,47);b=Color.rgb(49,101,76);}
        if("ramadan".equals(kind)){a=Color.rgb(25,38,65);b=Color.rgb(77,62,91);}
        p.setShader(new LinearGradient(0,0,w,h,a,b,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(0,0,w,h),d(22),d(22),p);p.setShader(null);
        fill(canvas,0x18FFFFFF);canvas.drawCircle(w*.12f,h*.2f,d(55),p);canvas.drawCircle(w*.88f,h*.85f,d(72),p);
        if("listening".equals(kind)) drawPlayer(canvas,w,h);
        else if("radio".equals(kind)) drawMicrophone(canvas,w,h);
        else if("ruqyah".equals(kind)) drawMushaf(canvas,w,h);
        else drawLanterns(canvas,w,h);
    }
    private void drawPlayer(Canvas c,float w,float h){
        float x=w*.5f,y=h*.48f,r=Math.min(h*.36f,w*.19f);
        fill(c,0xFF102B26);c.drawCircle(x,y,r+d(8),p);stroke(c,gold,1.5f);c.drawCircle(x,y,r+d(8),p);
        fill(c,0xFF17221F);c.drawCircle(x,y,r,p);
        for(int i=0;i<5;i++){stroke(c,0xFF49685A,1);c.drawCircle(x,y,r*(.28f+i*.14f),p);}
        fill(c,gold);c.drawCircle(x,y,d(9),p);
        fill(c,cream);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(d(25));c.drawText("♪",x,y+d(9),p);
        fill(c,0xFFECCB8D);p.setTextSize(d(11));c.drawText("QURAN • PLAYER",x,h-d(12),p);
        for(int i=0;i<7;i++){float bh=d(10+(i%3)*7);fill(c,0xBFEBCB91);c.drawRoundRect(w*.17f+i*d(12),y-bh/2,w*.17f+i*d(12)+d(4),y+bh/2,d(3),d(3),p);}
    }
    private void drawMicrophone(Canvas c,float w,float h){
        float x=w*.5f,y=h*.42f;
        fill(c,0xFF183346);c.drawRoundRect(new RectF(x-d(27),y-d(50),x+d(27),y+d(22)),d(25),d(25),p);
        stroke(c,gold,2);c.drawRoundRect(new RectF(x-d(27),y-d(50),x+d(27),y+d(22)),d(25),d(25),p);
        for(int i=-2;i<=2;i++){stroke(c,0xFFB9C9C8,1);c.drawLine(x+i*d(8),y-d(35),x+i*d(8),y+d(5),p);}
        stroke(c,cream,3);c.drawArc(new RectF(x-d(45),y-d(20),x+d(45),y+d(60)),0,180,false,p);c.drawLine(x,y+d(60),x,y+d(77),p);c.drawLine(x-d(24),y+d(77),x+d(24),y+d(77),p);
        fill(c,gold);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(d(12));c.drawText("LIVE RADIO",x,h-d(12),p);
        for(int i=0;i<3;i++){stroke(c,0x88EACB8B,2);c.drawArc(new RectF(x-d(56+i*12),y-d(60-i*7),x+d(56+i*12),y+d(65+i*7)),205,130,false,p);}
    }
    private void drawMushaf(Canvas c,float w,float h){
        float x=w*.5f,y=h*.52f;
        Path left=new Path();left.moveTo(x,y-d(4));left.quadTo(x-d(42),y-d(30),x-d(76),y-d(16));left.lineTo(x-d(76),y+d(38));left.quadTo(x-d(38),y+d(22),x,y+d(48));left.close();
        Path right=new Path();right.moveTo(x,y-d(4));right.quadTo(x+d(42),y-d(30),x+d(76),y-d(16));right.lineTo(x+d(76),y+d(38));right.quadTo(x+d(38),y+d(22),x,y+d(48));right.close();
        fill(c,cream);c.drawPath(left,p);c.drawPath(right,p);stroke(c,gold,2);c.drawPath(left,p);c.drawPath(right,p);
        stroke(c,0xFF78947D,1);for(int i=0;i<4;i++){float yy=y+d(i*10);c.drawLine(x-d(63),yy,x-d(12),yy,p);c.drawLine(x+d(12),yy,x+d(63),yy,p);}
        stroke(c,gold,2);c.drawLine(x,y-d(3),x,y+d(48),p);
        // Tasbeeh beads along the lower edge.
        for(int i=0;i<13;i++){double angle=Math.PI*(.12+i*.76/12);float bx=x+(float)Math.cos(angle)*d(92),by=y+d(20)+(float)Math.sin(angle)*d(35);fill(c,i%2==0?gold:cream);c.drawCircle(bx,by,d(3.2f),p);}
        fill(c,cream);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(d(11));c.drawText("سَكِينَةٌ وَشِفَاء",x,h-d(12),p);
    }
    private void drawLanterns(Canvas c,float w,float h){
        for(int i=0;i<3;i++){float x=w*(.25f+i*.25f),top=d(10+(i%2)*12),hh=h*.58f,ww=d(34);
            stroke(c,gold,1.8f);c.drawLine(x,0,x,top,p);
            Path q=new Path();q.moveTo(x-ww*.35f,top+hh*.18f);q.lineTo(x+ww*.35f,top+hh*.18f);q.lineTo(x+ww*.5f,top+hh*.42f);q.lineTo(x+ww*.28f,top+hh*.83f);q.lineTo(x,top+hh);q.lineTo(x-ww*.28f,top+hh*.83f);q.lineTo(x-ww*.5f,top+hh*.42f);q.close();
            fill(c,0x33FFD98B);c.drawPath(q,p);stroke(c,gold,2);c.drawPath(q,p);fill(c,0x66FFD98B);c.drawCircle(x,top+hh*.52f,d(6),p);
        }
        fill(c,gold);c.drawCircle(w*.79f,h*.28f,d(19),p);fill(c,bgColor());c.drawCircle(w*.84f,h*.23f,d(17),p);
        fill(c,cream);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(d(12));c.drawText("رَمَضَانُ كَرِيم",w*.5f,h-d(13),p);
    }
    private int bgColor(){return Color.rgb(42,47,76);}
}
