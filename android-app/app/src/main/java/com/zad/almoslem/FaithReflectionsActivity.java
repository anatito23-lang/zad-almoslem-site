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

import org.json.JSONArray;
import org.json.JSONObject;

public class FaithReflectionsActivity extends Activity {
    private final int green = Color.rgb(8, 48, 40);
    private LinearLayout list;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(green);
        getWindow().setNavigationBarColor(green);
        build();
        render();
        RemoteContentRepository.sync(this, "waqafat", this::render);
    }

    @Override protected void onResume() {
        super.onResume();
        if (list != null) RemoteContentRepository.sync(this, "waqafat", this::render);
    }

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLineSpacing(dp(5), 1.05f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        root.setBackgroundColor(Color.rgb(244, 247, 243));
        scroll.addView(root);

        TextView title = label("وقفات إيمانية", 24, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        TextView subtitle = label("كلمات تذكّر القلب بالله وتعينه على الخير", 13,
                Color.rgb(222, 239, 229), false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(16), dp(18), dp(16), dp(18));
        hero.setBackground(background(green, 18));
        hero.addView(title);
        hero.addView(subtitle);
        root.addView(hero);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        root.addView(list, lp);
        setContentView(AppTabs.wrap(this, scroll, "library"));
    }

    private void render() {
        if (list == null) return;
        list.removeAllViews();
        JSONArray rows = RemoteContentRepository.cached(this, "waqafat");
        if (rows.length() == 0) {
            TextView empty = label("لا توجد وقفات منشورة حاليًا. اتصل بالإنترنت واسحب لتحديث القسم لاحقًا.",
                    15, Color.DKGRAY, false);
            empty.setPadding(dp(8), dp(18), dp(8), dp(18));
            list.addView(empty);
            return;
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item == null) continue;
            String title = item.optString("title", "وقفة إيمانية");
            String body = item.optString("body", item.optString("text", ""));
            String date = item.optString("date", "");
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(15), dp(14), dp(15), dp(14));
            card.setBackground(background(Color.WHITE, 15));
            TextView heading = label(title, 18, green, true);
            card.addView(heading);
            if (!date.isEmpty()) {
                TextView meta = label(date, 12, Color.GRAY, false);
                meta.setPadding(0, dp(4), 0, dp(7));
                card.addView(meta);
            }
            TextView content = label(body, 16, Color.rgb(45, 62, 50), false);
            content.setPadding(0, dp(8), 0, 0);
            card.addView(content);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(11);
            list.addView(card, cp);
        }
    }
}
