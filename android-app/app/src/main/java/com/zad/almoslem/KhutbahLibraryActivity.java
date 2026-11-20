package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class KhutbahLibraryActivity extends Activity {
    private JSONArray sermons = new JSONArray();
    private LinearLayout root, list;
    private EditText search;
    private Spinner categorySpinner;
    private String selectedCategory="الكل";
    private int green = Color.rgb(8,48,40), ink = Color.rgb(30,55,45);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(green);
        getWindow().setNavigationBarColor(green);
        loadSermons();
        showList("");
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.RIGHT); t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void loadSermons() {
        try (InputStream in = getAssets().open("khutbahs.json"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n; while ((n=in.read(b))!=-1) out.write(b,0,n);
            sermons = new JSONArray(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception ignored) { sermons = new JSONArray(); }
    }
    private void base(String title, String subtitle) {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(16),dp(16),dp(28)); root.setBackgroundColor(Color.rgb(244,247,243));
        scroll.addView(root); setContentView(scroll);
        TextView heading = label(title,24,Color.WHITE,true); heading.setGravity(Gravity.CENTER);
        TextView sub = label(subtitle,13,Color.rgb(222,239,229),false); sub.setGravity(Gravity.CENTER);
        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(16),dp(18),dp(16),dp(18)); hero.setBackground(bg(green,18));
        hero.addView(heading); hero.addView(sub); root.addView(hero);
    }
    private void showList(String query) {
        base("مكتبة خطب الجمعة","خطب محفوظة داخل التطبيق — تعمل دون إنترنت");
        TextView count = label("عدد الخطب: "+sermons.length(),14,ink,true);
        count.setPadding(0,dp(14),0,dp(8)); root.addView(count);
        search = new EditText(this); search.setSingleLine(true); search.setText(query);
        search.setHint("ابحث بعنوان الخطبة"); search.setTextSize(15);
        search.setPadding(dp(12),dp(10),dp(12),dp(10)); search.setBackground(bg(Color.WHITE,12));
        root.addView(search);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(12); root.addView(list,lp);
        fillList(query);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){fillList(s.toString());}
            public void afterTextChanged(Editable e){}
        });
    }
    private void fillList(String query) {
        if(list==null)return; list.removeAllViews();
        int shown=0;
        for(int i=0;i<sermons.length();i++) {
            JSONObject item=sermons.optJSONObject(i); if(item==null)continue;
            String title=item.optString("title","خطبة بدون عنوان");
            String date=item.optString("date",""); String cat=item.optString("category","خطبة الجمعة");
            boolean matchesText=title.toLowerCase().contains(query.toLowerCase())||cat.toLowerCase().contains(query.toLowerCase());
            if(!matchesText)continue;
            if(!"الكل".equals(selectedCategory)&&!selectedCategory.equals(cat))continue;
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14),dp(12),dp(14),dp(12)); card.setBackground(bg(Color.WHITE,15));
            TextView t=label(title,18,ink,true); card.addView(t);
            TextView meta=label(cat+(date.isEmpty()?"":"  •  "+date),12,Color.rgb(105,120,110),false);
            meta.setPadding(0,dp(5),0,0); card.addView(meta);
            TextView action=label("اقرأ الخطبة ←",13,green,true); action.setPadding(0,dp(8),0,0); card.addView(action);
            final JSONObject selected=item;
            card.setOnClickListener(v->showDetail(selected));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(10); list.addView(card,cp);
            shown++;
        }
        if(shown==0) { TextView empty=label("لا توجد خطبة مطابقة للبحث.",14,Color.DKGRAY,false); list.addView(empty); }
    }
    private void showDetail(JSONObject item) {
        base(item.optString("title","خطبة الجمعة"),item.optString("date",""));
        Button back=new Button(this); back.setText("العودة إلى قائمة الخطب");
        root.addView(back); back.setOnClickListener(v->showList(""));
        TextView body=label(item.optString("body","لا يوجد نص لهذه الخطبة."),17,ink,false);
        body.setLineSpacing(dp(5),1f); body.setPadding(dp(3),dp(18),dp(3),dp(24));
        root.addView(body);
    }
}
