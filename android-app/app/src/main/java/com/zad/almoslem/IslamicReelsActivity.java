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
    private int index = 0;
    private float downY;
    private ImageView image;
    private TextView title, description, counter, status;

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
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(18)); root.setBackgroundColor(Color.BLACK);
        TextView brand = label("زَادُ الْمُسْلِم  •  ريلز إيمانية", 16, 0xffffdf9b, true);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(42)));
        counter = label("", 12, Color.WHITE, false);
        root.addView(counter, new LinearLayout.LayoutParams(-1, dp(28)));
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(round(0xff172a28, 20));
        image = new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        TextView watermark = label("زاد المسلم  |  zad-almoslem.github.io", 12, Color.WHITE, true);
        watermark.setBackground(round(0x99000000, 9)); watermark.setPadding(dp(9), dp(5), dp(9), dp(5));
        FrameLayout.LayoutParams wm = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        wm.bottomMargin = dp(12); frame.addView(watermark, wm);
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        frameLp.bottomMargin = dp(10); root.addView(frame, frameLp);
        title = label("جارٍ تحميل الصور…", 20, 0xffffe5b0, true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(44)));
        description = label("", 14, Color.WHITE, false);
        root.addView(description, new LinearLayout.LayoutParams(-1, -2));
        status = label("اسحب للأعلى للتنقل بين الريلز", 12, 0xffd7e4dc, false);
        status.setPadding(0, dp(7), 0, dp(7)); root.addView(status);
        // The image fills the frame and receives the touch first, so attach the
        // same vertical gesture handler to the image itself (and the frame).
        View.OnTouchListener swipe = (v, e) -> {
            int action = e.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                downY = e.getRawY();
                // Prevent the parent tab/scroll container from stealing a vertical
                // swipe before the reels feed can process it.
                if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_UP) {
                float distance = downY - e.getRawY();
                if (Math.abs(distance) > dp(55)) move(distance > 0 ? 1 : -1);
                if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_CANCEL) {
                if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
            return true;
        };
        frame.setOnTouchListener(swipe);
        image.setOnTouchListener(swipe);
        image.setClickable(true);
        image.setFocusable(true);
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
        posts.clear(); posts.addAll(fresh);
        // Keep the built-in reminders alongside published reels so the feed is never a single card.
        addBuiltInPosts();
        if (index >= posts.size()) index = 0;
        showPost();
    }

    private JSONObject current() { return posts.isEmpty() ? null : posts.get(index); }

    private void showPost() {
        JSONObject post = current(); if (post == null) return;
        title.setText(post.optString("title", "تذكرة إيمانية"));
        description.setText(post.optString("description", ""));
        counter.setText((index + 1) + " / " + posts.size());
        image.setImageDrawable(null);
        if (post.optBoolean("builtin", false)) {
            image.setImageBitmap(makeBuiltInCard(post.optString("verse"), post.optString("reference")));
            status.setText("ريلز إيمانية جاهزة — صور فقط");
            return;
        }
        status.setText("جارٍ تحميل الصورة…");
        io.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection)new URL(post.optString("imageUrl")).openConnection();
                connection.setConnectTimeout(12000); connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);
                try (InputStream in = connection.getInputStream()) { bitmap = BitmapFactory.decodeStream(in); }
            } catch (Exception ignored) {
            } finally { if (connection != null) connection.disconnect(); }
            Bitmap result = bitmap;
            runOnUiThread(() -> {
                if (current() != post) return;
                if (result != null) { image.setImageBitmap(result); status.setText("صورة منشورة"); }
                else status.setText("تعذر تحميل الصورة. تحقق من الاتصال.");
            });
        });
    }

    private void addBuiltInPosts() {
        String[][] cards = {
            {"ألا بذكر الله تطمئن القلوب", "سورة الرعد • الآية ٢٨", "طمئن قلبك بذكر الله، ولا تجعل ضجيج الدنيا يسرق منك سكينة القرب منه."},
            {"فإن مع العسر يسرا", "سورة الشرح • الآية ٥", "لا تيأس إذا ضاقت بك الأسباب؛ ففرج الله قريب، ورحمته أوسع من همك."},
            {"ومن يتق الله يجعل له مخرجا", "سورة الطلاق • الآية ٢", "اجعل تقوى الله طريقك، وثق أن من صدق مع الله لم يضيعه الله."},
            {"فاذكروني أذكركم", "سورة البقرة • الآية ١٥٢", "تذكر ربك فيذكرك، وأقبل عليه بقلبك."},
            {"ولا تيأسوا من روح الله", "سورة يوسف • الآية ٨٧", "مهما طال البلاء، لا تغلق باب الرجاء في رحمة الله."},
            {"إن الله مع الصابرين", "سورة البقرة • الآية ١٥٣", "اصبر واحتسب، فمعية الله أعظم عون في الشدائد."},
            {"وقل رب زدني علما", "سورة طه • الآية ١١٤", "اجعل يومك خطوة في طريق العلم النافع والعمل الصالح."},
            {"إن ربي قريب مجيب", "سورة هود • الآية ٦١", "ارفع يديك بالدعاء، فربك قريب يسمع ويرى."},
            {"والله خير الرازقين", "سورة الجمعة • الآية ١١", "لا يحملنك القلق على الرزق أن تنسى الرزاق."},
            {"إن مع العسر يسرا", "سورة الشرح • الآية ٦", "لا يدوم عسر، ووعد الله حق."},
            {"واستعينوا بالصبر والصلاة", "سورة البقرة • الآية ٤٥", "إذا أثقلتك الدنيا، فافزع إلى الصلاة والصبر."},
            {"ومن يتوكل على الله فهو حسبه", "سورة الطلاق • الآية ٣", "خذ بالأسباب، ثم فوض أمرك إلى الله."},
            {"فاصفح الصفح الجميل", "سورة الحجر • الآية ٨٥", "سامح ابتغاء وجه الله، ولا تجعل قلبك أسير الخصومة."},
            {"إن الله يحب التوابين", "سورة البقرة • الآية ٢٢٢", "عد إلى الله مهما كثرت عثراتك، فباب التوبة مفتوح."},
            {"رب اشرح لي صدري", "سورة طه • الآية ٢٥", "اجعل هذا الدعاء رفيقك عند كل أمر عسير."},
            {"وهو معكم أينما كنتم", "سورة الحديد • الآية ٤", "راقب الله في السر والعلن، واستحضر قربه وعلمه."},
            {"فمن يعمل مثقال ذرة خيرا يره", "سورة الزلزلة • الآية ٧", "لا تستصغر معروفا، فكل خير محفوظ عند الله."},
            {"إن الله يحب المحسنين", "سورة البقرة • الآية ١٩٥", "أحسن في قولك وعملك، فالإحسان طريق محبة الله."},
            {"ففروا إلى الله", "سورة الذاريات • الآية ٥٠", "إذا ضاقت بك السبل، فارجع إلى الله بقلب صادق."},
            {"إن رحمت الله قريب من المحسنين", "سورة الأعراف • الآية ٥٦", "أحسن الظن بربك، وأكثر من العمل الصالح."},
            {"فإنك بأعيننا", "سورة الطور • الآية ٤٨", "اطمئن إلى حفظ الله ورعايته، واصبر لحكم ربك."},
            {"إن الله لا يغير ما بقوم حتى يغيروا ما بأنفسهم", "سورة الرعد • الآية ١١", "ابدأ بإصلاح قلبك وعملك، واجعل التغيير الصادق من داخلك."},
            {"ولا تمش في الأرض مرحا", "سورة الإسراء • الآية ٣٧", "تواضع لله، فالعبد لا يعلو على عباد الله."},
            {"وقولوا للناس حسنا", "سورة البقرة • الآية ٨٣", "كلمة طيبة قد تجبر قلبا لا تعلم ما يحمل."},
            {"والكاظمين الغيظ والعافين عن الناس", "سورة آل عمران • الآية ١٣٤", "القوة أن تملك نفسك حين تغضب، وأن تعفو حين تقدر."},
            {"إن الله لا يضيع أجر المحسنين", "سورة التوبة • الآية ١٢٠", "لا يضيع عند الله تعب صادق ولا خير خفي."},
            {"أليس الله بكاف عبده", "سورة الزمر • الآية ٣٦", "يكفيك الله إذا صدقت في التوكل عليه."},
            {"فاذكروا الله قياما وقعودا", "سورة النساء • الآية ١٠٣", "اجعل لسانك رطبا بذكر الله في كل أحوالك."},
            {"ولا تنسوا الفضل بينكم", "سورة البقرة • الآية ٢٣٧", "لا تنس المعروف بسبب خلاف عابر."},
            {"إن الله مع الذين اتقوا", "سورة النحل • الآية ١٢٨", "التقوى زادك في الخلوة والجلوة، وفي الشدة والرخاء."},
            {"ألا إن نصر الله قريب", "سورة البقرة • الآية ٢١٤", "إذا طال الطريق، فلا تستبطئ نصر الله؛ فالوعد حق والفرج بيده سبحانه."},
            {"لا يكلف الله نفسا إلا وسعها", "سورة البقرة • الآية ٢٨٦", "ما نزل بك إلا والله يعلم قدرتك عليه؛ فاستعن به ولا تستسلم."},
            {"ولا تقنطوا من رحمة الله", "سورة الزمر • الآية ٥٣", "مهما كثرت الذنوب، لا تغلق باب التوبة؛ ارجع إلى ربك صادقا."},
            {"وعسى أن تكرهوا شيئا وهو خير لكم", "سورة البقرة • الآية ٢١٦", "قد تخفى عليك الحكمة اليوم، ورب الخير لا يختار لعبده إلا بعلم ورحمة."},
            {"كل نفس ذائقة الموت", "سورة آل عمران • الآية ١٨٥", "تذكر أن الرحيل حق، فاستعد للقاء الله بعمل صالح وقلب سليم."},
            {"وما توفيقي إلا بالله", "سورة هود • الآية ٨٨", "خذ بالأسباب، ولا تركن إلى قوتك؛ فالتوفيق من الله وحده."},
            {"فاستقم كما أمرت", "سورة هود • الآية ١١٢", "الثبات ليس لحظة حماس؛ بل طاعة مستمرة ولو قل العمل."},
            {"إن الله يدافع عن الذين آمنوا", "سورة الحج • الآية ٣٨", "اطمئن إلى تدبير الله، ولا تظن أن مظلوما غاب عن علمه."},
            {"إن أكرمكم عند الله أتقاكم", "سورة الحجرات • الآية ١٣", "ميزان الكرامة عند الله ليس مالا ولا نسبا؛ إنما التقوى والعمل."},
            {"إن الله يأمر بالعدل والإحسان", "سورة النحل • الآية ٩٠", "اجعل العدل خلقك، والإحسان طريقك، ولا تظلم أحدا."},
            {"وقل لعبادي يقولوا التي هي أحسن", "سورة الإسراء • الآية ٥٣", "انتق كلماتك؛ فالكلمة الطيبة قد تطفئ خصومة وتجبر خاطرا."},
            {"واصبر وما صبرك إلا بالله", "سورة النحل • الآية ١٢٧", "لا تواجه الابتلاء بقوتك وحدها؛ اطلب العون من الله واصبر."},
            {"قل لن يصيبنا إلا ما كتب الله لنا", "سورة التوبة • الآية ٥١", "ما قدره الله لك لن يخطئك، فاسكن إلى قضائه وخذ بالأسباب."},
            {"ومن أحسن قولا ممن دعا إلى الله وعمل صالحا", "سورة فصلت • الآية ٣٣", "اجمع بين الدعوة والعمل؛ فحسن القول يصدقه صالح الفعل."},
            {"ادفع بالتي هي أحسن", "سورة فصلت • الآية ٣٤", "لا تجعل إساءة الناس تنزع منك أجمل أخلاقك؛ قابلها بالحكمة والإحسان."},
            {"فاصبر إن وعد الله حق", "سورة غافر • الآية ٥٥", "لا تتعجل الثمرة؛ اصبر على الطاعة، فالله لا يخلف وعده."},
            {"وما عند الله خير وأبقى", "سورة القصص • الآية ٦٠", "لا تبع الباقي بالفاني؛ فما عند الله أعظم وأدوم."},
            {"إن الله يحب التوابين ويحب المتطهرين", "سورة البقرة • الآية ٢٢٢", "كلما أخطأت فبادر بالتوبة؛ أحب الأعمال أن ترجع إلى الله."},
            {"إن الله لا يظلم مثقال ذرة", "سورة النساء • الآية ٤٠", "لا تخف ضياع حقك؛ ربك عدل لا يظلم مثقال ذرة."},
            {"ولا تنابزوا بالألقاب", "سورة الحجرات • الآية ١١", "صن ألسنتكم عن السخرية والألقاب الجارحة؛ فكرامة الناس ليست لعبة."}
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

    private void move(int direction) {
        if (posts.isEmpty()) return;
        index = (index + direction + posts.size()) % posts.size(); showPost();
    }

    @Override protected void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
