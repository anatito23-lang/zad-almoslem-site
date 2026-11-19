package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public class CalendarActivity extends Activity {
    private final int green=Color.rgb(8,48,40);
    private final GregorianCalendar shown=new GregorianCalendar();
    private LinearLayout grid;
    private TextView title;
    private final String[] weekdays={"السبت","الأحد","الإثنين","الثلاثاء","الأربعاء","الخميس","الجمعة"};
    private final String[] hijriMonths={"محرم","صفر","ربيع الأول","ربيع الآخر","جمادى الأولى","جمادى الآخرة","رجب","شعبان","رمضان","شوال","ذو القعدة","ذو الحجة"};
    private final String[] gregMonths={"يناير","فبراير","مارس","أبريل","مايو","يونيو","يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);shown.set(Calendar.DAY_OF_MONTH,1);
        getWindow().setStatusBarColor(green);getWindow().setNavigationBarColor(green);build();draw();
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER);t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable bg(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(13),dp(15),dp(13),dp(25));
        root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);setContentView(scroll);
        TextView heading=label("التقويم الهجري والميلادي",23,Color.WHITE,true);
        TextView note=label("تقويم أم القرى على الجهاز",13,Color.rgb(222,239,229),false);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(12),dp(17),dp(12),dp(17));hero.setBackground(bg(green,18));hero.addView(heading);hero.addView(note);
        root.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setPadding(0,dp(12),0,dp(12));
        Button prev=new Button(this);prev.setText("السابق");Button next=new Button(this);next.setText("التالي");
        title=label("",16,green,true);
        nav.addView(prev,new LinearLayout.LayoutParams(dp(86),dp(48)));
        nav.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        nav.addView(next,new LinearLayout.LayoutParams(dp(86),dp(48)));root.addView(nav);
        prev.setOnClickListener(v->{shown.add(Calendar.MONTH,-1);draw();});
        next.setOnClickListener(v->{shown.add(Calendar.MONTH,1);draw();});
        grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);root.addView(grid);
        TextView foot=label("يعتمد التاريخ الهجري على تقويم أم القرى المتاح في نظام Android، وقد يختلف يومًا عن إعلان الرؤية الشرعية في بعض البلدان.",12,Color.rgb(100,110,102),false);
        foot.setPadding(dp(6),dp(14),dp(6),dp(4));root.addView(foot);
        if(Build.VERSION.SDK_INT<24)foot.setText("عرض التاريخ الهجري يحتاج إلى خدمة التقويم الإسلامي في Android 7 أو أحدث. التاريخ الميلادي متاح.");
    }
    private void draw(){
        if(grid==null)return;
        grid.removeAllViews();int year=shown.get(Calendar.YEAR),month=shown.get(Calendar.MONTH);
        title.setText(gregMonths[month]+" "+year);
        LinearLayout header=new LinearLayout(this);grid.addView(header);
        for(String day:weekdays){TextView cell=label(day,10,Color.DKGRAY,true);header.addView(cell,new LinearLayout.LayoutParams(0,dp(36),1));}
        GregorianCalendar first=new GregorianCalendar(year,month,1);
        int offset=first.get(Calendar.DAY_OF_WEEK)%7;
        int days=first.getActualMaximum(Calendar.DAY_OF_MONTH);
        GregorianCalendar today=new GregorianCalendar();
        int total=((offset+days+6)/7)*7;
        for(int row=0;row<total/7;row++){
            LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);grid.addView(line);
            for(int col=0;col<7;col++){
                int day=row*7+col-offset;TextView cell;
                if(day<0||day>=days)cell=label("",12,Color.DKGRAY,false);
                else{
                    GregorianCalendar date=new GregorianCalendar(year,month,day+1);
                    boolean isToday=date.get(Calendar.YEAR)==today.get(Calendar.YEAR)&&date.get(Calendar.DAY_OF_YEAR)==today.get(Calendar.DAY_OF_YEAR);
                    String h=toHijri(date.getTime());
                    cell=label((day+1)+"\n"+h,11,green,isToday);
                    cell.setPadding(dp(1),dp(5),dp(1),dp(5));cell.setMinHeight(dp(66));
                    cell.setBackground(bg(isToday?Color.rgb(226,239,229):Color.WHITE,9));
                }
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(72),1);lp.setMargins(dp(2),dp(2),dp(2),dp(2));line.addView(cell,lp);
            }
        }
    }
    private String toHijri(Date date){
        if(Build.VERSION.SDK_INT<24)return "—";
        try{
            Class<?> type=Class.forName("android.icu.util.IslamicCalendar");
            Object calendar=type.getConstructor().newInstance();
            try{
                Class<?> calculation=Class.forName("android.icu.util.IslamicCalendar$CalculationType");
                @SuppressWarnings({"unchecked","rawtypes"}) Object umm=Enum.valueOf((Class<? extends Enum>)calculation.asSubclass(Enum.class),"ISLAMIC_UMALQURA");
                type.getMethod("setCalculationType",calculation).invoke(calendar,umm);
            }catch(Exception ignored){}
            type.getMethod("setTime",Date.class).invoke(calendar,date);
            int day=(Integer)type.getMethod("get",int.class).invoke(calendar,5);
            int month=(Integer)type.getMethod("get",int.class).invoke(calendar,2)+1;
            return day+" "+hijriMonths[Math.max(0,Math.min(11,month-1))];
        }catch(Exception e){return "—";}
    }
}