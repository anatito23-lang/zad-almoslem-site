package com.zad.almoslem;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AppTabs {
    private static final String PREFS = "zad_app_navigation";
    private static final String[] KEYS = {"more","ramadan","dua","tasbeeh","calendar","radio","quran","mushaf","home"};
    private static final String[] LABELS = {"المزيد","رمضان","دعاء","السبحة","التقويم","الإذاعة","استماع","المصحف","الرئيسية"};
    private static final String[] ICONS = {"⋯","☾","🤲","۞","▦","◉","♫","📖","⌂"};
    private static final Class<?>[] SCREENS = {MoreActivity.class,RamadanActivity.class,DuaActivity.class,TasbeehActivity.class,CalendarActivity.class,RadioActivity.class,QuranListeningActivity.class,MushafPageActivity.class,HomeActivity.class};
    private AppTabs() {}

    public static View wrap(Activity a, View content, String active) {
        return wrap(a, content, active, active);
    }

    public static View wrap(Activity a, View content, String active, String wallpaperStyle) {
        ScrollView scroll;
        if (content instanceof ScrollView) scroll=(ScrollView)content;
        else { scroll=new ScrollView(a); scroll.setFillViewport(true); scroll.addView(content); }
        scroll.setFillViewport(true); scroll.setClipToPadding(false);
        View page = scroll.getChildAt(0);
        LinearLayout shell=new LinearLayout(a); shell.setOrientation(1); shell.setBackgroundColor(AppThemeColors.bg(a));
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout bar=createBar(a,active);
        shell.addView(bar,new LinearLayout.LayoutParams(-1,dp(a,62)));
        shell.setOnApplyWindowInsetsListener((v,insets)->{
            int bottom=0,top=0;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets nav=insets.getInsets(WindowInsets.Type.navigationBars()|WindowInsets.Type.displayCutout());
                android.graphics.Insets status=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.displayCutout());
                bottom=nav.bottom;top=status.top;
            } else { bottom=insets.getSystemWindowInsetBottom();top=insets.getSystemWindowInsetTop(); }
            int safe=Math.max(0,bottom);shell.setPadding(0,Math.max(0,top),0,0);
            bar.setPadding(dp(a,6),dp(a,2),dp(a,6),safe);
            LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)bar.getLayoutParams();int h=dp(a,62)+safe;
            if(p.height!=h){p.height=h;bar.setLayoutParams(p);}return insets;
        });
        shell.setFitsSystemWindows(false);shell.requestApplyInsets();
        syncNavigation(a,active,bar);
        return shell;
    }

    private static LinearLayout createBar(Activity a,String active) {
        LinearLayout outer=new LinearLayout(a);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setGravity(Gravity.CENTER_VERTICAL);
        outer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        GradientDrawable barBg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.rgb(239,245,237),Color.rgb(225,236,225)});
        barBg.setCornerRadii(new float[]{dp(a,18),dp(a,18),dp(a,18),dp(a,18),0,0,0,0});
        outer.setBackground(barBg);
        outer.setElevation(dp(a,8));
        outer.setPadding(dp(a,6),dp(a,5),dp(a,6),0);
        LinearLayout homeSlot=new LinearLayout(a);
        homeSlot.setGravity(Gravity.CENTER);

        HorizontalScrollView dockScroll=new HorizontalScrollView(a);
        dockScroll.setHorizontalScrollBarEnabled(false);
        dockScroll.setHorizontalFadingEdgeEnabled(true);
        dockScroll.setFadingEdgeLength(dp(a,18));
        dockScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        if(Build.VERSION.SDK_INT>=21)dockScroll.setEdgeEffectColor(Color.rgb(207,174,112));
        dockScroll.setFillViewport(false);
        dockScroll.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout dock=new LinearLayout(a);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(247,249,244));bg.setCornerRadius(dp(a,16));bg.setStroke(dp(a,1),Color.rgb(211,224,210));dock.setBackground(bg);
        dock.setPadding(dp(a,1),dp(a,1),dp(a,1),dp(a,1));
        dockScroll.addView(dock,new HorizontalScrollView.LayoutParams(-2,-1));
        LinearLayout moreSlot=new LinearLayout(a);
        moreSlot.setGravity(Gravity.CENTER);
        // LTR order: More on the left, scrollable tabs in the middle, Home on the right.
        outer.addView(moreSlot,new LinearLayout.LayoutParams(dp(a,58),dp(a,50)));
        outer.addView(dockScroll,new LinearLayout.LayoutParams(0,dp(a,50),1f));
        outer.addView(homeSlot,new LinearLayout.LayoutParams(dp(a,50),dp(a,50)));
        render(a,active,outer,readCached(a));
        return outer;
    }

    private static JSONArray readCached(Activity a) {
        try { return new JSONArray(a.getSharedPreferences(PREFS,Activity.MODE_PRIVATE).getString("tabs","[]")); }
        catch(Exception ignored){ return new JSONArray(); }
    }

    private static void syncNavigation(Activity a,String active,LinearLayout bar) {
        try {
        FirebaseFirestore.getInstance().collection("waqafat").document("app_navigation_config").get()
            .addOnSuccessListener(snapshot->{
                if(!snapshot.exists()||!snapshot.contains("tabs"))return;
                Object value=snapshot.get("tabs");if(!(value instanceof java.util.List))return;
                JSONArray tabs=new JSONArray();
                for(Object item:(java.util.List<?>)value){
                    if(item instanceof java.util.Map){
                        JSONObject row=new JSONObject((java.util.Map<?,?>)item);
                        tabs.put(row);
                    }
                }
                if(tabs.length()==0)return;
                a.getSharedPreferences(PREFS,Activity.MODE_PRIVATE).edit().putString("tabs",tabs.toString()).apply();
                render(a,active,bar,tabs);
            });
        } catch (RuntimeException e) {
            android.util.Log.w("ZadNavigation","Cloud navigation sync unavailable; using local tabs",e);
        }
    }

    private static void render(Activity a,String active,LinearLayout outer,JSONArray config) {
        if(outer==null||outer.getChildCount()<3)return;
        LinearLayout moreSlot=(LinearLayout)outer.getChildAt(0);
        HorizontalScrollView dockScroll=(HorizontalScrollView)outer.getChildAt(1);
        LinearLayout homeSlot=(LinearLayout)outer.getChildAt(2);
        if(dockScroll.getChildCount()==0||!(dockScroll.getChildAt(0) instanceof LinearLayout))return;
        LinearLayout dock=(LinearLayout)dockScroll.getChildAt(0);
        homeSlot.removeAllViews();dock.removeAllViews();moreSlot.removeAllViews();
        String lang=a.getSharedPreferences("zad_settings",Activity.MODE_PRIVATE).getString("app_language","ar");
        int li="en".equals(lang)?1:"fr".equals(lang)?2:"ur".equals(lang)?3:"tr".equals(lang)?4:0;
        if("prayer".equals(active)||"library".equals(active)||"settings".equals(active))active="more";
        java.util.ArrayList<JSONObject> rows=new java.util.ArrayList<>();
        if(config!=null&&config.length()>0){
            for(int i=0;i<config.length();i++){
                JSONObject row=config.optJSONObject(i);
                if(row==null||!row.optBoolean("visible",true))continue;
                String key=row.optString("key","");
                String label=row.optString("label","").trim().replace("ـ","");
                String normalizedKey=key.toLowerCase(java.util.Locale.ROOT);
                String normalizedLabel=label.replaceAll("\\s+","");
                boolean companionStory=normalizedKey.matches(".*(sahab|sahabi|sahaba|companion).*")
                    || normalizedLabel.contains("صحاب") || normalizedLabel.contains("الصحابه")
                    || normalizedLabel.contains("قصصصح") || normalizedLabel.contains("قصهصح");
                boolean contentSection="hadith".equals(normalizedKey)||"hadiths".equals(normalizedKey)||"khutbah".equals(normalizedKey)||"khutbas".equals(normalizedKey)||"sermons".equals(normalizedKey)||"sahaba".equals(normalizedKey)||"companions".equals(normalizedKey)||"stories_sahaba".equals(normalizedKey)||"home".equals(normalizedKey)||"more".equals(normalizedKey)||normalizedLabel.contains("حديث")||normalizedLabel.contains("خطب")||companionStory;
                if(!contentSection&&key.matches("[A-Za-z][A-Za-z0-9_-]{0,59}"))rows.add(row);
            }
        }
        boolean hasDua=false;for(JSONObject row:rows)if("dua".equals(row.optString("key",""))){hasDua=true;break;}
        if(!hasDua){JSONObject dua=new JSONObject();try{dua.put("key","dua");dua.put("label","دعاء");dua.put("icon","🤲");dua.put("visible",true);}catch(Exception ignored){}rows.add(dua);}
        JSONObject home=new JSONObject();
        try{home.put("key","home");home.put("label","الرئيسية");home.put("icon","⌂");home.put("visible",true);}catch(Exception ignored){}
        homeSlot.addView(makeTabItem(a,home,active,li),new LinearLayout.LayoutParams(-1,-1));
        JSONObject more=new JSONObject();
        try{more.put("key","more");more.put("label","المزيد");more.put("icon","⋯");more.put("visible",true);}catch(Exception ignored){}
        moreSlot.addView(makeTabItem(a,more,active,li),new LinearLayout.LayoutParams(-1,-1));
        for(JSONObject row:rows){
            dock.addView(makeTabItem(a,row,active,li),new LinearLayout.LayoutParams(dp(a,62),-1));
        }
        final String activeKey=active;
        dockScroll.post(()->{
            for(int i=0;i<rows.size();i++){
                if(activeKey.equals(rows.get(i).optString("key",""))){
                    View selected=dock.getChildAt(i);
                    if(selected!=null)dockScroll.smoothScrollTo(Math.max(0,selected.getLeft()-dp(a,70)),0);
                    break;
                }
            }
        });
    }

    private static View makeTabItem(Activity a,JSONObject row,String active,int li) {
        String key=row.optString("key","");
        int index=indexOf(key);
        final Class<?> target=index>=0?SCREENS[index]:CustomTabActivity.class;
        boolean selected=key.equals(active);
        String labelText=row.optString("label",index>=0?LABELS[index]:"قسم جديد");
        String iconText=row.optString("icon",index>=0?ICONS[index]:"✨");
        if(li>0&&index>=0&&LABELS[index].equals(labelText))labelText=localizedLabel(index,li,labelText);
        final String clickLabel=labelText,clickIcon=iconText;
        LinearLayout item=new LinearLayout(a);item.setOrientation(1);item.setGravity(Gravity.CENTER);
        item.setClickable(!selected);item.setFocusable(!selected);
        item.setPadding(dp(a,2),dp(a,1),dp(a,2),dp(a,1));
        if(selected){GradientDrawable selectedBg=new GradientDrawable();selectedBg.setColor(Color.rgb(218,235,218));selectedBg.setCornerRadius(dp(a,12));selectedBg.setStroke(dp(a,1),Color.rgb(177,199,165));item.setBackground(selectedBg);item.setElevation(dp(a,2));}
        else {GradientDrawable idleBg=new GradientDrawable();idleBg.setColor(Color.TRANSPARENT);idleBg.setCornerRadius(dp(a,12));item.setBackground(idleBg);}
        TextView emoji = new TextView(a);
        emoji.setText(emojiForTab(key, iconText));
        emoji.setTextSize(25);
        emoji.setGravity(Gravity.CENTER);
        emoji.setIncludeFontPadding(false);
        item.addView(emoji, new LinearLayout.LayoutParams(-1, dp(a,38)));
        TextView label=new TextView(a);label.setText(labelText);label.setTextSize(9);label.setSingleLine(true);label.setGravity(Gravity.CENTER);
        label.setTextColor(selected?Color.rgb(28,83,59):Color.rgb(91,111,96));label.setTypeface(Typeface.DEFAULT,selected?Typeface.BOLD:Typeface.NORMAL);
        item.addView(label,new LinearLayout.LayoutParams(-1,dp(a,15)));
        item.setOnClickListener(v->{
            if("website".equals(key)){
                try{
                    Intent browser=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://zad-almoslem.github.io/"));
                    a.startActivity(browser);
                }catch(Exception e){
                    android.widget.Toast.makeText(a,"تعذر فتح الموقع في المتصفح",android.widget.Toast.LENGTH_SHORT).show();
                }
                return;
            }
            Intent in=new Intent(a,target);
            if(index<0){in.putExtra("tab_id",key);in.putExtra("tab_title",clickLabel);in.putExtra("tab_icon",clickIcon);}
            in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);a.startActivity(in);
        });
        return item;
    }

    private static final class IllustratedTabIcon extends View {
        private final String kind;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int gold = Color.rgb(211, 185, 137);
        private final int page = Color.rgb(241, 235, 219);
        private final int ink = Color.rgb(27, 76, 61);
        IllustratedTabIcon(Activity activity, String kind, boolean selected) {
            super(activity);
            this.kind = kind;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setAlpha(selected ? 1f : .98f);
        }
        private float d(float value) { return value * getResources().getDisplayMetrics().density; }
        private void fill(Canvas c, int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
        private void line(int color, float width) { paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(d(width)); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx=getWidth()/2f, cy=getHeight()/2f;
            canvas.save();
            canvas.translate(cx,cy);
            canvas.scale(1.18f,1.18f);
            switch (kind) {
                case "mushaf": drawMushaf3D(canvas); break;
                case "radio": drawMicrophone3D(canvas); break;
                case "quran": case "listening": case "listen": drawMusicNote3D(canvas); break;
                case "tasbeeh": case "sebha": drawTasbeeh3D(canvas); break;
                case "ramadan": drawRamadan3D(canvas); break;
                case "calendar": drawCalendar3D(canvas); break;
                case "home": drawHome3D(canvas); break;
                case "more": drawMore3D(canvas); break;
                default: drawDefault3D(canvas); break;
            }
            canvas.restore();
        }
        private void drawMushaf3D(Canvas c) {
            // A small carved wooden rehal remains visible beneath the open mushaf.
            canvasShadow(c,0xAA000000,1.8f,.7f,1.4f);
            fill(c,0xFF70401F);
            Path standLeft=new Path();standLeft.moveTo(-8,5);standLeft.lineTo(-1,9);standLeft.lineTo(-4,14);standLeft.lineTo(-10,10);standLeft.close();
            Path standRight=new Path();standRight.moveTo(8,5);standRight.lineTo(1,9);standRight.lineTo(4,14);standRight.lineTo(10,10);standRight.close();
            c.drawPath(standLeft,paint);c.drawPath(standRight,paint);
            paint.clearShadowLayer();
            fill(c,0xFFC18445);
            Path standLight=new Path();standLight.moveTo(-7,6);standLight.lineTo(-2,9);standLight.lineTo(-4,11);standLight.close();
            c.drawPath(standLight,paint);
            Path standLight2=new Path();standLight2.moveTo(7,6);standLight2.lineTo(2,9);standLight2.lineTo(4,11);standLight2.close();
            c.drawPath(standLight2,paint);
            line(0xFF5A321B,.7f);c.drawLine(-8,7,-4,10,paint);c.drawLine(8,7,4,10,paint);
            canvasShadow(c, 0x99000000, 2.2f, 1.2f, 1.8f);
            fill(c, 0xFF075A3D); c.drawRoundRect(new RectF(-11,-10,11,10),3,3,paint);
            paint.clearShadowLayer();
            fill(c, 0xFF13A66A); c.drawRoundRect(new RectF(-9,-9,9,8),2,2,paint);
            line(gold,1.2f); c.drawRoundRect(new RectF(-10,-10,10,10),3,3,paint);
            fill(c, page);
            Path l=new Path(); l.moveTo(0,-6);l.quadTo(-5,-9,-8,-6);l.lineTo(-8,6);l.quadTo(-4,4,0,7);l.close();
            Path r=new Path(); r.moveTo(0,-6);r.quadTo(5,-9,8,-6);r.lineTo(8,6);r.quadTo(4,4,0,7);r.close();
            c.drawPath(l,paint);c.drawPath(r,paint);
            line(ink,.75f);c.drawPath(l,paint);c.drawPath(r,paint);c.drawLine(0,-6,0,7,paint);
            line(gold,.8f);c.drawLine(-5,-2,-1,-2,paint);c.drawLine(1,-2,5,-2,paint);c.drawLine(-5,1,-1,1,paint);c.drawLine(1,1,5,1,paint);
            fill(c,0xFFFFE7A8);c.drawCircle(0,-11,1.5f,paint);
        }
        private void drawMicrophone3D(Canvas c) {
            canvasShadow(c,0xAA000000,2f,1f,2f);
            fill(c,0xFF075BC8);c.drawRoundRect(new RectF(-5,-12,5,5),5,5,paint);
            paint.clearShadowLayer();
            fill(c,0xFF12A8E8);c.drawRoundRect(new RectF(-3.7f,-10.5f,2,-1),3,3,paint);
            line(gold,1.1f);c.drawRoundRect(new RectF(-5,-12,5,5),5,5,paint);
            line(page,.7f);for(int x=-2;x<=2;x+=2)c.drawLine(x,-8,x,-2,paint);
            line(gold,1.5f);c.drawArc(new RectF(-9,-7,9,10),0,180,false,paint);c.drawLine(0,10,0,13,paint);c.drawLine(-5,13,5,13,paint);
            line(0xFFFFE7A8,.8f);c.drawArc(new RectF(-12,-13,12,14),205,130,false,paint);
        }
        private void drawMusicNote3D(Canvas c) {
            Path note=new Path();note.moveTo(2,-10);note.lineTo(9,-12);note.lineTo(9,4);note.cubicTo(9,8,3,9,2,6);note.cubicTo(1,3,5,1,7,3);note.lineTo(7,-7);note.lineTo(2,-6);note.close();
            canvasShadow(c,0xAA000000,2f,1f,2f);fill(c,0xFFFF9D00);c.drawPath(note,paint);paint.clearShadowLayer();
            fill(c,0xFFFFD21C);Path shine=new Path();shine.moveTo(3,-9);shine.lineTo(8,-10);shine.lineTo(8,-8);shine.lineTo(3,-7);shine.close();c.drawPath(shine,paint);
            fill(c,0xFFFFF3A6);c.drawOval(new RectF(-7,3,-1,8),paint);
            fill(c,0xFFD6AD68);c.drawOval(new RectF(-8,4,-2,9),paint);
            line(0xFFFFE7B0,.8f);c.drawLine(2,-5,2,1,paint);
        }
        private void drawTasbeeh3D(Canvas c) {
            line(0xFF8C6638,1.8f);c.drawArc(new RectF(-10,-10,10,10),-205,230,false,paint);
            for(int i=0;i<13;i++){double a=Math.toRadians(-205+i*(230.0/12));float x=(float)Math.cos(a)*9.5f,y=(float)Math.sin(a)*9.5f;
                canvasShadow(c,0x99000000,1.4f,.6f,1f);fill(c,(i%2==0)?0xFF13BFA0:0xFF00A878);c.drawCircle(x,y,2.05f,paint);paint.clearShadowLayer();
                fill(c,0xFFFFF0C9);c.drawCircle(x-.55f,y-.65f,.65f,paint);
            }
            fill(c,0xFF9D713E);c.drawCircle(0,-12,2.2f,paint);fill(c,0xFFFFE2A0);c.drawCircle(-.6f,-12.6f,.7f,paint);line(gold,1);c.drawLine(0,-10,0,-8,paint);
        }
        private void drawRamadan3D(Canvas c) {
            // Crescent with a softly raised gold edge.
            canvasShadow(c,0xAA000000,2f,1f,2f);
            fill(c,0xFFB98643);c.drawCircle(-5,-3,8.5f,paint);
            fill(c,0xFFFFE3A0);c.drawCircle(-6,-4,7.6f,paint);
            fill(c,0xFF0C5AC8);c.drawCircle(-2,-6,7.2f,paint);
            paint.clearShadowLayer();
            fill(c,0xFFFFE8B0);c.drawCircle(7,-8,1.2f,paint);c.drawCircle(10,-3,.8f,paint);
            // Hanging Ramadan lantern.
            line(0xFFD7B477,1f);c.drawLine(0,2,0,4,paint);
            Path cap=new Path();cap.moveTo(-4,4);cap.lineTo(4,4);cap.lineTo(3,6);cap.lineTo(-3,6);cap.close();
            fill(c,0xFF9D6B35);c.drawPath(cap,paint);
            canvasShadow(c,0x99000000,1.8f,.6f,1.4f);
            fill(c,0xFF00A6E8);c.drawRoundRect(new RectF(-3.2f,6,3.2f,13),1,1,paint);
            paint.clearShadowLayer();
            fill(c,0xFFFFE64D);c.drawRoundRect(new RectF(-1.7f,7,1.4f,12),.7f,.7f,paint);
            line(0xFF8B6339,.9f);c.drawLine(-4,7,-3,6,paint);c.drawLine(4,7,3,6,paint);c.drawLine(-3,13,3,13,paint);
            fill(c,0xFFFFE3A0);c.drawCircle(0,14,1,paint);
        }
        private void drawCalendar3D(Canvas c) {
            canvasShadow(c,0xAA000000,2f,1f,2f);fill(c,0xFFFFF5E5);c.drawRoundRect(new RectF(-10,-10,10,11),3,3,paint);paint.clearShadowLayer();
            fill(c,0xFFE53935);c.drawRoundRect(new RectF(-9,-9,9,-3),2,2,paint);
            line(gold,1f);c.drawRoundRect(new RectF(-10,-10,10,11),3,3,paint);c.drawLine(-5,-12,-5,-7,paint);c.drawLine(5,-12,5,-7,paint);
            fill(c,0xFF1689E8);for(int row=0;row<2;row++)for(int col=0;col<3;col++)c.drawCircle(-5+col*5,1+row*5,1.2f,paint);
            fill(c,0xFFD9A85F);c.drawCircle(5,6,1.3f,paint);
            fill(c,0xFFFFFFFF);c.drawCircle(-5.4f,-6.5f,.7f,paint);
        }
        private void drawHome3D(Canvas c) {
            Path roof=new Path();roof.moveTo(-12,-1);roof.lineTo(0,-11);roof.lineTo(12,-1);roof.close();
            canvasShadow(c,0xAA000000,2f,1f,2f);fill(c,0xFFB84A16);c.drawPath(roof,paint);paint.clearShadowLayer();
            fill(c,0xFFFFC928);Path roofLight=new Path();roofLight.moveTo(-10,-2);roofLight.lineTo(0,-10);roofLight.lineTo(0,-7);roofLight.lineTo(-7,-1);roofLight.close();c.drawPath(roofLight,paint);
            fill(c,0xFFEEE4CC);c.drawRoundRect(new RectF(-9,-1,9,11),2,2,paint);
            fill(c,0xFF00A6A6);c.drawRoundRect(new RectF(-2,3,2,11),1,1,paint);
            line(gold,.9f);c.drawRoundRect(new RectF(-9,-1,9,11),2,2,paint);
        }
        private void drawMore3D(Canvas c) {
            for(int i=0;i<3;i++){float x=(i-1)*7;
                canvasShadow(c,0xAA000000,1.5f,.5f,1f);fill(c,0xFF9B713B);c.drawCircle(x,0,2.8f,paint);paint.clearShadowLayer();
                fill(c,0xFFFFE4A5);c.drawCircle(x-.7f,-.8f,.9f,paint);
            }
        }
        private void drawDefault3D(Canvas c) { drawMusicNote3D(c); }
        private void canvasShadow(Canvas c,int color,float radius,float dx,float dy) { paint.setShadowLayer(d(radius),d(dx),d(dy),color); }
        private void drawRamadan(Canvas c) {
            line(gold,1.7f); c.drawArc(new RectF(-9,-10,9,10),-55,290,false,paint);
            fill(c,gold); c.drawCircle(5,-6,1.2f,paint); c.drawCircle(9,-10,1f,paint);
            fill(c,page); c.drawCircle(-2,0,7,paint);
            fill(c,ink); c.drawCircle(1,-3,7,paint);
            fill(c,gold); c.drawCircle(-8,9,1.2f,paint); c.drawCircle(0,12,1f,paint); c.drawCircle(8,9,1.2f,paint);
        }
        private void drawTasbeeh(Canvas c) {
            line(gold,1.2f); c.drawArc(new RectF(-10,-10,10,10),-205,230,false,paint);
            for(int i=0;i<11;i++){ double a=Math.toRadians(-205+i*23); float x=(float)Math.cos(a)*9.5f,y=(float)Math.sin(a)*9.5f; fill(c,(i%2==0)?page:gold); c.drawCircle(x,y,1.65f,paint); }
            fill(c,gold); c.drawCircle(0,-12,2,paint); c.drawLine(0,-10,0,-7,paint);
        }
        private void drawListening(Canvas c) {
            fill(c,0xFF173F34); c.drawRoundRect(new RectF(-10,-10,10,10),3,3,paint);
            line(gold,1f); c.drawRoundRect(new RectF(-10,-10,10,10),3,3,paint);
            line(page,1.1f); c.drawLine(-5,-5,-5,5,paint); c.drawLine(-5,-5,1,-5,paint); c.drawLine(-5,0,0,0,paint);
            fill(c,gold); Path p=new Path(); p.moveTo(2,-4);p.lineTo(8,0);p.lineTo(2,4);p.close();c.drawPath(p,paint);
        }
        private void drawHome(Canvas c) {
            Path roof=new Path(); roof.moveTo(-12,-1);roof.lineTo(0,-11);roof.lineTo(12,-1);roof.close();
            fill(c,gold);c.drawPath(roof,paint);
            fill(c,page);c.drawRoundRect(new RectF(-9,-1,9,11),2,2,paint);
            fill(c,ink);c.drawRoundRect(new RectF(-2,3,2,11),1,1,paint);
            fill(c,gold);c.drawRect(-11,-1,-9,1,paint);c.drawRect(9,-1,11,1,paint);
        }
        private void drawMore(Canvas c) {
            fill(c,gold); c.drawCircle(-7,0,2.2f,paint);c.drawCircle(0,0,2.2f,paint);c.drawCircle(7,0,2.2f,paint);
        }
        private void drawMushaf(Canvas c) {
            RectF cover=new RectF(-10,-10,10,10);
            fill(c,0xFF173F34);c.drawRoundRect(cover,3,3,paint);
            line(gold,1.1f);c.drawRoundRect(cover,3,3,paint);
            fill(c,page);c.drawRoundRect(new RectF(-8,-8,8,8),2,2,paint);
            Path left=new Path();left.moveTo(0,-6);left.quadTo(-4,-8,-7,-6);left.lineTo(-7,6);left.quadTo(-3,4,0,7);left.close();
            Path right=new Path();right.moveTo(0,-6);right.quadTo(4,-8,7,-6);right.lineTo(7,6);right.quadTo(3,4,0,7);right.close();
            fill(c,page);c.drawPath(left,paint);c.drawPath(right,paint);
            line(ink,.7f);c.drawPath(left,paint);c.drawPath(right,paint);c.drawLine(0,-6,0,7,paint);
            line(gold,.7f);c.drawLine(-5,-2,-1,-2,paint);c.drawLine(1,-2,5,-2,paint);c.drawLine(-5,1,-1,1,paint);c.drawLine(1,1,5,1,paint);
            fill(c,gold);c.drawCircle(0,-11.5f,1.4f,paint);
        }
        private void drawMicrophone(Canvas c) {
            fill(c,0xFF173D50);c.drawRoundRect(new RectF(-5,-11,5,5),5,5,paint);
            line(gold,1);c.drawRoundRect(new RectF(-5,-11,5,5),5,5,paint);
            line(page,.65f);for(int x=-2;x<=2;x+=2)c.drawLine(x,-7,x,-1,paint);
            line(gold,1.3f);c.drawArc(new RectF(-9,-7,9,10),0,180,false,paint);
            c.drawLine(0,10,0,13,paint);c.drawLine(-5,13,5,13,paint);
            line(0x99EBC77E,.8f);c.drawArc(new RectF(-12,-13,12,14),205,130,false,paint);
        }
        private void drawCalendar(Canvas c) {
            fill(c,page);c.drawRoundRect(new RectF(-10,-10,10,10),2,2,paint);
            line(gold,1.1f);c.drawRoundRect(new RectF(-10,-10,10,10),2,2,paint);
            fill(c,0xFF1B5948);c.drawRoundRect(new RectF(-9,-9,9,-3),1.5f,1.5f,paint);
            line(gold,1.2f);c.drawLine(-5,-12,-5,-7,paint);c.drawLine(5,-12,5,-7,paint);
            fill(c,ink);
            for(int row=0;row<2;row++)for(int col=0;col<3;col++)c.drawCircle(-5+col*5,1+row*5,1.05f,paint);
            fill(c,gold);c.drawCircle(5,6,1.2f,paint);
        }
    }

    private static String emojiForTab(String key, String fallback) {
        switch (key) {
            case "mushaf": return "📖";
            case "ramadan": return "🏮";
            case "dua": return "🤲";
            case "radio": return "🎙️";
            case "quran": case "listening": case "listen": return "🎧";
            case "calendar": return "📆";
            case "tasbeeh": case "sebha": return "📿";
            case "home": return "🏠";
            case "website": return "🌐";
            case "more": return "⋯";
            default: return (fallback == null || fallback.trim().isEmpty()) ? "✨" : fallback;
        }
    }

    private static String localizedLabel(int index,int lang,String fallback) {
        String[][] names={{"المزيد","More","Plus","مزید","Daha Fazla"},{"رمضان","Ramadan","Ramadan","رمضان","Ramazan"},{"دعاء","Dua","Prière","دعا","Dua"},{"السبحة","Tasbeeh","Tasbih","تسبیح","Tesbih"},{"النتيجة","Calendar","Calendrier","کیلنڈر","Takvim"},{"الإذاعة","Radio","Radio","ریڈیو","Radyo"},{"استماع","Listen","Écoute","تلاوت","Dinle"},{"المصحف","Mushaf","Coran","قرآن","Mushaf"},{"الرئيسية","Home","Accueil","مرکزی","Ana Sayfa"}};
        return index>=0&&index<names.length?names[index][lang]:fallback;
    }
    private static int indexOf(String key){for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return i;return -1;}
    private static int dp(Activity a,float n){return(int)(n*a.getResources().getDisplayMetrics().density+.5f);}
}