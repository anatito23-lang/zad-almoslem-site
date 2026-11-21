package com.zad.almoslem;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AppTabs {
    private static final String PREFS = "zad_app_navigation";
    private static final String[] KEYS = {"more","ramadan","tasbeeh","calendar","radio","quran","mushaf","home"};
    private static final String[] LABELS = {"المزيد","رمضان","السبحة","التقويم","الإذاعة","استماع","المصحف","الرئيسية"};
    private static final String[] ICONS = {"☰","🌙","۞","▦","◉","♫","▤","⌂"};
    private static final Class<?>[] SCREENS = {MoreActivity.class,RamadanActivity.class,TasbeehActivity.class,CalendarActivity.class,RadioActivity.class,QuranListeningActivity.class,MushafPageActivity.class,HomeActivity.class};
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
        shell.addView(bar,new LinearLayout.LayoutParams(-1,dp(a,68)));
        shell.setOnApplyWindowInsetsListener((v,insets)->{
            int bottom=0,top=0;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets nav=insets.getInsets(WindowInsets.Type.navigationBars()|WindowInsets.Type.displayCutout());
                android.graphics.Insets status=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.displayCutout());
                bottom=nav.bottom;top=status.top;
            } else { bottom=insets.getSystemWindowInsetBottom();top=insets.getSystemWindowInsetTop(); }
            int safe=Math.max(0,bottom);shell.setPadding(0,Math.max(0,top),0,0);
            bar.setPadding(dp(a,7),dp(a,3),dp(a,7),safe);
            LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)bar.getLayoutParams();int h=dp(a,68)+safe;
            if(p.height!=h){p.height=h;bar.setLayoutParams(p);}return insets;
        });
        shell.setFitsSystemWindows(false);shell.requestApplyInsets();
        syncNavigation(a,active,bar);
        return shell;
    }

    private static LinearLayout createBar(Activity a,String active) {
        LinearLayout outer=new LinearLayout(a);outer.setOrientation(1);outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(AppThemeColors.nav(a));outer.setPadding(dp(a,5),dp(a,4),dp(a,5),0);
        LinearLayout dock=new LinearLayout(a);dock.setOrientation(LinearLayout.HORIZONTAL);dock.setGravity(Gravity.CENTER);
        dock.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        GradientDrawable bg=new GradientDrawable();bg.setColor(AppThemeColors.nav(a));dock.setBackground(bg);
        dock.setPadding(dp(a,1),dp(a,1),dp(a,1),dp(a,1));
        outer.addView(dock,new LinearLayout.LayoutParams(-1,dp(a,60)));
        render(a,active,outer,readCached(a));
        return outer;
    }

    private static JSONArray readCached(Activity a) {
        try { return new JSONArray(a.getSharedPreferences(PREFS,Activity.MODE_PRIVATE).getString("tabs","[]")); }
        catch(Exception ignored){ return new JSONArray(); }
    }

    private static void syncNavigation(Activity a,String active,LinearLayout bar) {
        try {
        FirebaseFirestore.getInstance().collection("waqafat").document("__app_navigation__").get()
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
        if (outer == null || outer.getChildCount() == 0 || !(outer.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout dock=(LinearLayout)outer.getChildAt(0);
        dock.removeAllViews();
        String lang=a.getSharedPreferences("zad_settings",Activity.MODE_PRIVATE).getString("app_language","ar");
        int li="en".equals(lang)?1:"fr".equals(lang)?2:"ur".equals(lang)?3:"tr".equals(lang)?4:0;
        if("prayer".equals(active)||"library".equals(active)||"settings".equals(active))active="more";
        java.util.ArrayList<JSONObject> rows=new java.util.ArrayList<>();
        if(config!=null&&config.length()>0){
            for(int i=0;i<config.length();i++){
                JSONObject row=config.optJSONObject(i);if(row==null||!row.optBoolean("visible",true))continue;
                String key=row.optString("key","");
                if(indexOf(key)>=0)rows.add(row);
            }
        }
        if(rows.isEmpty())for(int i=0;i<KEYS.length;i++){
            JSONObject row=new JSONObject();try{row.put("key",KEYS[i]);row.put("label",LABELS[i]);row.put("icon",ICONS[i]);row.put("visible",true);}catch(Exception ignored){}rows.add(row);
        } else {
            boolean hasRamadan=false;
            for(JSONObject row:rows) if("ramadan".equals(row.optString("key",""))) hasRamadan=true;
            if(!hasRamadan) {
                JSONObject ramadan=new JSONObject();
                try { ramadan.put("key","ramadan");ramadan.put("label","رمضان");ramadan.put("icon","🌙");ramadan.put("visible",true); }
                catch(Exception ignored) {}
                rows.add(Math.min(1,rows.size()),ramadan);
            }
        }
        for(JSONObject row:rows){
            String key=row.optString("key","");int index=indexOf(key);if(index<0)continue;
            final Class<?> target=SCREENS[index];boolean selected=key.equals(active);
            String labelText=row.optString("label",LABELS[index]);String iconText=row.optString("icon",ICONS[index]);
            if(li>0 && LABELS[index].equals(labelText))labelText=localizedLabel(index,li,labelText);
            LinearLayout item=new LinearLayout(a);item.setOrientation(1);item.setGravity(Gravity.CENTER);
            item.setClickable(!selected);item.setFocusable(!selected);item.setPadding(dp(a,1),dp(a,3),dp(a,1),dp(a,2));
            if(selected){GradientDrawable s=new GradientDrawable();s.setColor(AppThemeColors.primary(a));s.setCornerRadius(dp(a,13));s.setStroke(dp(a,1),Color.rgb(190,158,91));item.setBackground(s);}
            else item.setBackgroundResource(android.R.drawable.list_selector_background);
            TextView icon=new TextView(a);icon.setText(iconText);icon.setTextSize(18);icon.setGravity(Gravity.CENTER);
            icon.setTextColor(selected?Color.rgb(235,205,139):AppThemeColors.accent(a));icon.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            item.addView(icon,new LinearLayout.LayoutParams(-1,dp(a,27)));
            TextView label=new TextView(a);label.setText(labelText);label.setTextSize(8);label.setSingleLine(true);label.setGravity(Gravity.CENTER);
            label.setTextColor(selected?Color.WHITE:Color.rgb(195,204,199));label.setTypeface(Typeface.DEFAULT,selected?Typeface.BOLD:Typeface.NORMAL);
            item.addView(label,new LinearLayout.LayoutParams(-1,dp(a,17)));
            item.setOnClickListener(v->{Intent in=new Intent(a,target);in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);a.startActivity(in);});
            dock.addView(item,new LinearLayout.LayoutParams(0,-1,1f));
        }
    }

    private static String localizedLabel(int index,int lang,String fallback) {
        String[][] names={{"المزيد","More","Plus","مزید","Daha Fazla"},{"رمضان","Ramadan","Ramadan","رمضان","Ramazan"},{"السبحة","Tasbeeh","Tasbih","تسبیح","Tesbih"},{"النتيجة","Calendar","Calendrier","کیلنڈر","Takvim"},{"الإذاعة","Radio","Radio","ریڈیو","Radyo"},{"استماع","Listen","Écoute","تلاوت","Dinle"},{"المصحف","Mushaf","Coran","قرآن","Mushaf"},{"الرئيسية","Home","Accueil","مرکزی","Ana Sayfa"}};
        return index>=0&&index<names.length?names[index][lang]:fallback;
    }
    private static int indexOf(String key){for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return i;return -1;}
    private static int dp(Activity a,float n){return(int)(n*a.getResources().getDisplayMetrics().density+.5f);}
}