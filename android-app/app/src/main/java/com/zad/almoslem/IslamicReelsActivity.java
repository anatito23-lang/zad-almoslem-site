package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IslamicReelsActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ArrayList<JSONObject> posts = new ArrayList<>();
    private ScrollView reelScroll;
    private LinearLayout reelFeed;

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); return b;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        AppThemeColors.apply(this);
        build();
        RemoteContentRepository.sync(this, "reels", this::loadPosts);
        loadPosts();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(6), dp(10), dp(8));
        root.setBackgroundColor(Color.BLACK);

        TextView brand = label("زَادُ الْمُسْلِم  •  ريلز إيمانية", 17, 0xffffdf9b, true);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(42)));

        reelScroll = new ScrollView(this);
        reelScroll.setFillViewport(true);
        reelScroll.setVerticalScrollBarEnabled(false);
        reelScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        reelFeed = new LinearLayout(this);
        reelFeed.setOrientation(LinearLayout.VERTICAL);
        reelScroll.addView(reelFeed, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams feedLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        feedLp.topMargin = dp(4);
        root.addView(reelScroll, feedLp);

        TextView hint = label("اسحب لأعلى أو لأسفل لمشاهدة المزيد", 12, 0xffd7e4dc, false);
        root.addView(hint, new LinearLayout.LayoutParams(-1, dp(30)));
        setContentView(AppTabs.wrap(this, root, "more"));
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private void loadPosts() {
        JSONArray rows = RemoteContentRepository.cached(this, "reels");
        ArrayList<JSONObject> fresh = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && row.optBoolean("published", true)
                    && row.optString("imageUrl").startsWith("https://")) fresh.add(row);
        }
        Collections.sort(fresh, Comparator.comparingInt(o -> o.optInt("order", 0)));
        posts.clear();
        posts.addAll(fresh);
        if (posts.isEmpty()) addBuiltInPosts();
        renderFeed();
    }

    private void renderFeed() {
        reelFeed.removeAllViews();
        int pageHeight = Math.max(dp(420), getResources().getDisplayMetrics().heightPixels - dp(230));
        for (int i = 0; i < posts.size(); i++) {
            final JSONObject post = posts.get(i);
            FrameLayout page = new FrameLayout(this);
            page.setBackground(round(0xff10231f, 22));
            page.setClipToOutline(true);

            ImageView photo = new ImageView(this);
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.setBackgroundColor(0xff10231f);
            page.addView(photo, new FrameLayout.LayoutParams(-1, -1));

            android.graphics.drawable.GradientDrawable shade = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{0xee061713, 0x99061713, 0x00000000});
            View overlay = new View(this);
            overlay.setBackground(shade);
            page.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setPadding(dp(20), dp(18), dp(20), dp(24));
            FrameLayout.LayoutParams detailsLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            page.addView(details, detailsLp);

            TextView badge = label("زَادُ الْمُسْلِم  •  " + (i + 1) + " / " + posts.size(),
                    12, 0xffffdf9b, true);
            badge.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            details.addView(badge, new LinearLayout.LayoutParams(-1, dp(30)));

            TextView heading = label(post.optString("title", "وقفة إيمانية"), 21, Color.WHITE, true);
            heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            heading.setPadding(0, dp(5), 0, dp(7));
            details.addView(heading, new LinearLayout.LayoutParams(-1, -2));

            String body = post.optString("description", "");
            if (post.optBoolean("builtin", false)) {
                body = post.optString("verse", "") + "\n" + post.optString("reference", "") +
                        (body.isEmpty() ? "" : "\n" + body);
            }
            TextView caption = label(body, 15, 0xfff5f5f0, false);
            caption.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            details.addView(caption, new LinearLayout.LayoutParams(-1, -2));

            TextView watermark = label("زاد المسلم", 12, Color.WHITE, true);
            watermark.setBackground(round(0x88000000, 30));
            watermark.setPadding(dp(13), dp(6), dp(13), dp(6));
            FrameLayout.LayoutParams wm = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
            wm.setMargins(0, dp(14), dp(14), 0);
            page.addView(watermark, wm);

            LinearLayout.LayoutParams pageLp = new LinearLayout.LayoutParams(-1, pageHeight);
            pageLp.bottomMargin = dp(12);
            reelFeed.addView(page, pageLp);

            if (post.optBoolean("builtin", false)) {
                photo.setImageBitmap(makeBuiltInCard(post.optString("verse"), post.optString("reference")));
            } else {
                io.execute(() -> {
                    Bitmap bitmap = null;
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection)new URL(post.optString("imageUrl")).openConnection();
                        connection.setConnectTimeout(12000);
                        connection.setReadTimeout(20000);
                        connection.setInstanceFollowRedirects(true);
                        try (InputStream in = connection.getInputStream()) {
                            bitmap = BitmapFactory.decodeStream(in);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (connection != null) connection.disconnect();
                    }
                    Bitmap result = bitmap;
                    runOnUiThread(() -> {
                        if (result != null && page.getParent() == reelFeed) {
                            photo.setImageBitmap(result);
                        }
                    });
                });
            }
        }
    }

    private void addBuiltInPosts() {
        String[][] cards = {
            {"ألا بذكر الله تطمئن القلوب", "سورة الرعد • الآية ٢٨", "طمئن قلبك بذكر الله، ولا تجعل ضجيج الدنيا يسرق منك سكينة القرب منه."},
            {"فإن مع العسر يسرا", "سورة الشرح • الآية ٥", "لا تيأس إذا ضاقت بك الأسباب؛ ففرج الله قريب، ورحمته أوسع من همك."},
            {"ومن يتق الله يجعل له مخرجا", "سورة الطلاق • الآية ٢", "اجعل تقوى الله طريقك، وثق أن من صدق مع الله لم يضيعه الله."},
            {"فاذكروني أذكركم", "سورة البقرة • الآية ١٥٢", "يا لها من كرامة! تذكر ربك فيذكرك، وتقبل عليه فيفتح لك أبواب رحمته."}
        };
        for (String[] item : cards) {
            JSONObject row = new JSONObject();
            try {
                row.put("builtin", true);
                row.put("title", "وقفة إيمانية");
                row.put("verse", item[0]);
                row.put("reference", item[1]);
                row.put("description", item[2]);
            } catch (Exception ignored) {}
            posts.add(row);
        }
    }

    private Bitmap makeBuiltInCard(String verse, String reference) {
        int w = 1080, h = 1600;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{0xff082d27, 0xff155443, 0xff0b302a});
        bg.setBounds(0, 0, w, h);
        bg.draw(canvas);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(90, 110, 90, 110);
        TextView brand = label("زَادُ الْمُسْلِم", 34, 0xffffdf9b, true);
        TextView main = label(verse, 58, Color.WHITE, true);
        main.setPadding(0, 55, 0, 55);
        TextView ref = label(reference, 32, 0xffffdf9b, true);
        TextView footer = label("تذكرة إيمانية", 26, 0xffd7e4dc, false);
        card.addView(brand, new LinearLayout.LayoutParams(-1, -2));
        card.addView(main, new LinearLayout.LayoutParams(-1, -2));
        card.addView(ref, new LinearLayout.LayoutParams(-1, -2));
        card.addView(footer, new LinearLayout.LayoutParams(-1, -2));
        card.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        card.layout(0, 0, w, h);
        card.draw(canvas);
        return bitmap;
    }

    @Override protected void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
