package com.zad.almoslem;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MushafPageActivity extends Activity {
    private static final String IMAGE_BASE = "https://raw.githubusercontent.com/Miftah-Fentaw/Quran_webp/main/png/";
    private static final String[] SURAHS = {
        "الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس",
        "هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه",
        "الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم",
        "لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر",
        "فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق",
        "الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة",
        "الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج",
        "نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس",
        "التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد",
        "الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات",
        "القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر",
        "المسد","الإخلاص","الفلق","الناس"
    };
    private static final int[] SURAH_PAGES = {
        1,2,50,77,106,128,151,177,187,208,221,235,249,255,262,267,282,293,305,312,
        322,332,342,350,359,367,377,385,396,404,411,415,418,428,434,440,446,453,458,467,
        477,483,489,496,499,502,507,511,515,518,520,523,526,528,531,534,537,542,545,549,
        551,553,554,556,558,560,562,564,566,568,570,572,574,575,577,578,580,582,583,585,
        586,587,589,590,591,591,592,593,594,595,595,596,596,597,597,598,598,599,599,600,
        600,601,601,601,602,602,602,603,603,603,604,604,604,604
    };

    private int page = 1;
    private boolean busy = false;
    private TextView pageLabel, status;
    private ImageView pageImage;
    private Button previous, next;
    private ProgressBar progress;
    private GestureDetector gesture;
    private final int green = Color.rgb(8,48,40);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(green);
        getWindow().setNavigationBarColor(green);
        page = Math.max(1, Math.min(604, getIntent().getIntExtra("page",
                getSharedPreferences("zad_mushaf", MODE_PRIVATE).getInt("last_page", 1))));
        build();
        showPage(page);
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(3), dp(3), dp(3), dp(4));
        root.setBackgroundColor(Color.rgb(242,240,230));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);

        Button index = new Button(this);
        index.setText("☰ فهرس السور");
        index.setTextSize(13);
        toolbar.addView(index, new LinearLayout.LayoutParams(0, dp(38), 1));
        index.setOnClickListener(v -> showSurahIndex());

        pageLabel = label("", 15, green, true);
        toolbar.addView(pageLabel, new LinearLayout.LayoutParams(0, dp(38), 1));
        root.addView(toolbar);

        LinearLayout pageFrame = new LinearLayout(this);
        pageFrame.setOrientation(LinearLayout.VERTICAL);
        pageFrame.setGravity(Gravity.CENTER);
        pageFrame.setPadding(dp(1), dp(2), dp(1), dp(2));
        pageFrame.setBackground(shape(Color.rgb(255,253,244), 5, Color.rgb(177,157,112)));
        pageImage = new ImageView(this);
        pageImage.setAdjustViewBounds(true);
        pageImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pageFrame.addView(pageImage, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(-1, 0, 1);
        imageLp.topMargin = dp(2);
        root.addView(pageFrame, imageLp);

        gesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null || busy) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) < dp(28) || Math.abs(dx) < Math.abs(dy) * 1.15f || Math.abs(vx) < dp(65)) return false;
                if (dx < 0 && page < 604) showPage(page + 1);
                else if (dx > 0 && page > 1) showPage(page - 1);
                return true;
            }
        });
        pageImage.setClickable(true);\n        pageImage.setOnTouchListener((v, event) -> gesture.onTouchEvent(event));\n        pageFrame.setOnTouchListener((v, event) -> gesture.onTouchEvent(event));

        status = label("صفحات المصحف تُحمّل وتُحفظ تلقائيًا على الهاتف", 12, Color.DKGRAY, false);
        status.setTextSize(10);\n        status.setPadding(0, dp(2), 0, dp(2));
        root.addView(status);
        TextView attribution = label("صور صفحات المصحف: Quran-Images — ترخيص MIT", 9, Color.GRAY, false);
        root.addView(attribution);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        previous = new Button(this);
        previous.setText("السابق");
        next = new Button(this);
        next.setText("التالي");
        nav.addView(previous, new LinearLayout.LayoutParams(0, dp(40), 1));
        nav.addView(next, new LinearLayout.LayoutParams(0, dp(40), 1));
        root.addView(nav);
        previous.setOnClickListener(v -> { if (page > 1) showPage(page - 1); });
        next.setOnClickListener(v -> { if (page < 604) showPage(page + 1); });

        setContentView(AppTabs.wrap(this, root, "mushaf"));
    }

    private File pageFile(int number) {
        File dir = new File(getFilesDir(), "mushaf_images");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, number + ".png");
    }

    private void showPage(int number) {
        if (number < 1 || number > 604 || busy) return;
        page = number;
        getSharedPreferences("zad_mushaf", MODE_PRIVATE).edit().putInt("last_page", page).apply();
        pageLabel.setText("صفحة " + page + " من 604");
        previous.setEnabled(page > 1);
        next.setEnabled(page < 604);
        loadImage(page);
    }

    private void loadImage(int number) {
        File cached = pageFile(number);
        if (cached.isFile() && cached.length() > 10000) {
            Bitmap bitmap = BitmapFactory.decodeFile(cached.getAbsolutePath());
            if (bitmap != null) {
                pageImage.setImageBitmap(bitmap);
                status.setText("صفحة محفوظة على الهاتف — يمكنك القراءة دون إنترنت");
                progress.setVisibility(View.GONE);
                return;
            }
        }
        busy = true;
        progress.setVisibility(View.VISIBLE);
        status.setText("جارٍ تحميل صفحة المصحف " + number + "...");
        previous.setEnabled(false);
        next.setEnabled(false);
        new Thread(() -> {
            HttpURLConnection connection = null;
            File temp = new File(cached.getParentFile(), number + ".part");
            try {
                URL url = new URL(IMAGE_BASE + number + ".png");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "ZadAlmoslem Android");
                connection.connect();
                if (connection.getResponseCode() != 200) throw new IOException("HTTP " + connection.getResponseCode());
                try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    out.getFD().sync();
                }
                if (temp.length() < 10000) throw new IOException("ملف الصفحة غير مكتمل");
                if (cached.exists()) cached.delete();
                if (!temp.renameTo(cached)) throw new IOException("تعذر حفظ الصفحة");
                Bitmap bitmap = BitmapFactory.decodeFile(cached.getAbsolutePath());
                if (bitmap == null) throw new IOException("تعذر قراءة صورة الصفحة");
                runOnUiThread(() -> {
                    if (page == number) {
                        pageImage.setImageBitmap(bitmap);
                        status.setText("تم تحميل الصفحة وحفظها على الهاتف");
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (page == number) status.setText("تعذر تحميل الصفحة. تحقق من الإنترنت ثم أعد المحاولة.");
                });
            } finally {
                if (connection != null) connection.disconnect();
                if (temp.exists()) temp.delete();
                runOnUiThread(() -> {
                    busy = false;
                    progress.setVisibility(View.GONE);
                    previous.setEnabled(page > 1);
                    next.setEnabled(page < 604);
                });
            }
        }).start();
    }

    private void showSurahIndex() {
        String[] entries = new String[SURAHS.length];
        for (int i = 0; i < SURAHS.length; i++)
            entries[i] = String.format(ArabicLocaleHolder.LOCALE, "%03d  سورة %s  —  صفحة %d", i + 1, SURAHS[i], SURAH_PAGES[i]);
        new AlertDialog.Builder(this)
                .setTitle("فهرس سور القرآن الكريم")
                .setItems(entries, (dialog, which) -> showPage(SURAH_PAGES[which]))
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private static final class ArabicLocaleHolder {
        static final Locale LOCALE = new Locale("ar");
    }
}
