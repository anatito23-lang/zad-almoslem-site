package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public final class AdminTabContent {
    private AdminTabContent() {}
    private static int dp(Activity a,float n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    public static void attach(Activity a, LinearLayout root, String tabId) {
        FirebaseFirestore.getInstance().collection("waqafat").whereEqualTo("tabId",tabId).whereEqualTo("published",true).get()
            .addOnSuccessListener(snapshot -> {
                if(a.isFinishing() || (android.os.Build.VERSION.SDK_INT>=17 && a.isDestroyed())) return;
                if(snapshot.isEmpty()) return;
                LinearLayout section=new LinearLayout(a);section.setOrientation(LinearLayout.VERTICAL);
                section.setPadding(dp(a,14),dp(a,12),dp(a,14),dp(a,8));
                TextView heading=new TextView(a);heading.setText("إضافات من إدارة زاد وطمأنينة");heading.setTextSize(17);
                heading.setTextColor(AppThemeColors.text(a));heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                heading.setGravity(Gravity.RIGHT);section.addView(heading,new LinearLayout.LayoutParams(-1,-2));
                java.util.ArrayList<QueryDocumentSnapshot> rows=new java.util.ArrayList<>();for(QueryDocumentSnapshot d:snapshot)rows.add(d);
                rows.sort((x,y)->Long.compare(x.getLong("order")==null?0:x.getLong("order"),y.getLong("order")==null?0:y.getLong("order")));
                for(QueryDocumentSnapshot d:rows){
                    LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(a,13),dp(a,11),dp(a,13),dp(a,11));
                    GradientDrawable bg=new GradientDrawable();bg.setColor(AppThemeColors.surface(a));bg.setCornerRadius(dp(a,14));card.setBackground(bg);
                    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(a,9);section.addView(card,cp);
                    TextView title=new TextView(a);title.setText(d.getString("title")==null?"":d.getString("title"));title.setTextSize(16);
                    title.setTextColor(AppThemeColors.text(a));title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.RIGHT);card.addView(title);
                    TextView body=new TextView(a);body.setText(d.getString("body")==null?"":d.getString("body"));body.setTextSize(14);
                    body.setTextColor(AppThemeColors.muted(a));body.setGravity(Gravity.RIGHT);body.setLineSpacing(dp(a,4),1.05f);
                    LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(a,6);card.addView(body,bp);
                    String source=d.getString("source");if(source!=null&&!source.trim().isEmpty()){
                        TextView ref=new TextView(a);ref.setText("المصدر: "+source);ref.setTextSize(11);ref.setTextColor(AppThemeColors.accent(a));ref.setGravity(Gravity.RIGHT);
                        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(a,6);card.addView(ref,rp);
                    }
                }
                LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(a,10);root.addView(section,sp);
            })
            .addOnFailureListener(e->android.util.Log.w("ZadAdminContent","Could not load published section content: "+tabId,e));
    }
}
