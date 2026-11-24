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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DuaActivity extends Activity {
    private static final String SECTION_ID = "qalbi-itmaann";
    private static final String CACHE_NAME = "qalbi_itmaann_content";
    private static final String VERSE = "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾";
    private static final String VERSE_SOURCE = "سورة الرعد — الآية ٢٨";
    private LinearLayout list;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        AppThemeColors.apply(this);
        build();
        render(readCache());
    }

    @Override protected void onResume() {
        super.onResume();
        // AppTabs reuses this Activity when the user returns to the tab. Refresh
        // published content each time so new admin posts do not stay hidden
        // behind an old offline cache.
        loadContent();
    }

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), 0xFFD8C49A);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLineSpacing(dp(5), 1.08f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15), dp(15), dp(15), dp(26));
        root.setBackground(new IslamicWallpaperDrawable(this, "dua"));
        scroll.addView(root);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(16), dp(22), dp(16), dp(22));
        GradientDrawable heroBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF0B3028, 0xFF1D5945, 0xFF0B3028}
        );
        heroBg.setCornerRadius(dp(22));
        heroBg.setStroke(dp(1), 0xFFD9B76E);
        hero.setBackground(heroBg);

        TextView emblem = text("♡", 42, 0xFFFFD98A, true);
        emblem.setGravity(Gravity.CENTER);
        hero.addView(emblem, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView title = text("قلبي اطمئن", 27, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title);

        TextView subtitle = text("مع القرآن والسنة… كلمات تذكّر القلب بربه", 14, 0xFFF1DDAE, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(7), 0, 0);
        hero.addView(subtitle);
        root.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout verseCard = new LinearLayout(this);
        verseCard.setOrientation(LinearLayout.VERTICAL);
        verseCard.setGravity(Gravity.CENTER);
        verseCard.setPadding(dp(16), dp(18), dp(16), dp(18));
        verseCard.setBackground(background(0xFFFFF8E9, 18));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
        vp.topMargin = dp(14);
        root.addView(verseCard, vp);

        TextView verseLabel = text("آية تطمئن قلبك", 15, 0xFF986F2F, true);
        verseLabel.setGravity(Gravity.CENTER);
        verseCard.addView(verseLabel);
        TextView verse = text(VERSE, 22, 0xFF173E32, true);
        verse.setGravity(Gravity.CENTER);
        verse.setPadding(0, dp(12), 0, dp(7));
        verseCard.addView(verse);
        TextView verseSource = text(VERSE_SOURCE, 13, 0xFF6B6B5C, false);
        verseSource.setGravity(Gravity.CENTER);
        verseCard.addView(verseSource);

        status = text("جارٍ تحميل النفحات…", 13, Color.WHITE, false);
        status.setPadding(dp(4), dp(15), dp(4), dp(8));
        root.addView(status);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, new LinearLayout.LayoutParams(-1, -2));

        setContentView(AppTabs.wrap(this, scroll, "qalbi-itmaann", "dua"));
    }

    private JSONArray readCache() {
        try {
            return new JSONArray(getSharedPreferences(CACHE_NAME, MODE_PRIVATE).getString("items", "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private void saveCache(JSONArray rows) {
        getSharedPreferences(CACHE_NAME, MODE_PRIVATE).edit().putString("items", rows.toString()).apply();
    }

    private void loadContent() {
        FirebaseFirestore.getInstance().collection("site_content")
            .whereEqualTo("published", true)
            .get()
            .addOnSuccessListener(snapshot -> {
                ArrayList<JSONObject> items = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    if (!SECTION_ID.equals(doc.getString("sectionId"))) continue;
                    if (!Boolean.TRUE.equals(doc.getBoolean("published"))) continue;
                    JSONObject row = new JSONObject();
                    try {
                        row.put("title", doc.getString("title") == null ? "نفحة إيمانية" : doc.getString("title"));
                        row.put("body", doc.getString("body") == null ? "" : doc.getString("body"));
                        row.put("source", doc.getString("source") == null ? "" : doc.getString("source"));
                        Long orderValue = doc.getLong("order");
                        row.put("order", orderValue == null ? 0 : orderValue);
                    } catch (Exception ignored) {}
                    items.add(row);
                }
                Collections.sort(items, (a, b) ->
                    Integer.compare(a.optInt("order", 0), b.optInt("order", 0))
                );
                JSONArray rows = new JSONArray();
                for (JSONObject item : items) rows.put(item);
                saveCache(rows);
                render(rows);
                status.setText(rows.length() == 0
                    ? "هذه مساحة تتجدد بالنفحات المنشورة من لوحة الإدارة."
                    : "تتجدد هذه النفحات كلما نُشر محتوى جديد.");
            })
            .addOnFailureListener(error -> {
                status.setText("تعذر تحديث المحتوى الآن؛ نعرض آخر ما تم تحميله.");
                android.util.Log.w("QalbiItmaann", "Content refresh failed", error);
            });
    }

    private void render(JSONArray rows) {
        if (list == null) return;
        list.removeAllViews();
        if (rows.length() == 0) {
            TextView empty = text("قريبًا… ستجد هنا آيات نتدبرها، وأحاديث صحيحة، ومواقف وعبرًا ومواعظ نافعة.", 15, 0xFF173E32, false);
            empty.setPadding(dp(15), dp(18), dp(15), dp(18));
            empty.setBackground(background(0xFFFFF8E9, 16));
            list.addView(empty);
            return;
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item == null) continue;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackground(background(0xFFFFFDF7, 16));

            TextView heading = text(item.optString("title", "نفحة إيمانية"), 18, 0xFF174536, true);
            card.addView(heading);

            TextView body = text(item.optString("body", ""), 16, 0xFF34463C, false);
            body.setPadding(0, dp(9), 0, dp(9));
            card.addView(body);

            String source = item.optString("source", "").trim();
            if (!source.isEmpty()) {
                TextView citation = text("المصدر: " + source, 12, 0xFF8A713E, false);
                card.addView(citation);
            }

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(11);
            list.addView(card, cp);
        }
    }
}
