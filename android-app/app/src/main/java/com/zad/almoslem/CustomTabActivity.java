package com.zad.almoslem;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;

public class CustomTabActivity extends Activity {
    private String tabId, tabTitle;
    private LinearLayout list;
    private int green = Color.rgb(8, 48, 40);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        tabId=getIntent().getStringExtra("tab_id"); tabTitle=getIntent().getStringExtra("tab_title");
        if(tabId==null||!tabId.matches("[A-Za-z][A-Za-z0-9_-]{0,59}")){finish();return;}
        if(tabTitle==null||tabTitle.trim().isEmpty())tabTitle="قسم إيماني";
        build(); render(readCache()); load();
    }
    private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);t.setLineSpacing(dp(5),1.05f);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void build(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);
        root.setPadding(dp(16),dp(16),dp(16),dp(26));root.setBackgroundColor(Color.rgb(244,247,243));scroll.addView(root);
        TextView title=label(tabTitle,23,Color.WHITE,true);title.setGravity(Gravity.CENTER);
        LinearLayout hero=new LinearLayout(this);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(16),dp(18),dp(16),dp(18));
        hero.setBackground(bg(green,18));hero.addView(title);root.addView(hero);
        list=new LinearLayout(this);list.setOrientation(1);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(14);root.addView(list,lp);setContentView(AppTabs.wrap(this,scroll,tabId));
    }
    private JSONArray readCache(){try{return new JSONArray(getSharedPreferences("zad_custom_tabs",MODE_PRIVATE).getString(tabId,"[]"));}catch(Exception e){return new JSONArray();}}
    private void saveCache(JSONArray rows){getSharedPreferences("zad_custom_tabs",MODE_PRIVATE).edit().putString(tabId,rows.toString()).apply();}
    private void load(){
        FirebaseFirestore.getInstance().collection("waqafat").whereEqualTo("tabId",tabId).whereEqualTo("published",true).get()
        .addOnSuccessListener(snapshot->{JSONArray rows=new JSONArray();for(QueryDocumentSnapshot d:snapshot){
            JSONObject item=new JSONObject();try{item.put("title",d.getString("title")==null?tabTitle:d.getString("title"));
            item.put("body",d.getString("body")==null?"":d.getString("body"));item.put("source",d.getString("source")==null?"":d.getString("source"));}catch(Exception ignored){}rows.put(item);}
            saveCache(rows);render(rows);})
        .addOnFailureListener(e->android.util.Log.w("ZadCustomTab","Using cached custom tab content",e));
    }
    private void render(JSONArray rows){
        if(list==null)return;list.removeAllViews();if(rows.length()==0){TextView empty=label("لا يوجد محتوى منشور في هذا التبويب حاليًا.",15,Color.DKGRAY,false);
            empty.setPadding(dp(8),dp(18),dp(8),dp(18));list.addView(empty);return;}
        for(int i=0;i<rows.length();i++){JSONObject item=rows.optJSONObject(i);if(item==null)continue;
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(15),dp(14),dp(15),dp(14));card.setBackground(bg(Color.WHITE,15));
            card.addView(label(item.optString("title",tabTitle),18,green,true));TextView body=label(item.optString("body",""),16,Color.rgb(45,62,50),false);
            body.setPadding(0,dp(8),0,0);card.addView(body);String source=item.optString("source","");
            if(!source.isEmpty())card.addView(label("المصدر: "+source,12,Color.GRAY,false));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(11);list.addView(card,cp);}
    }
}
