package com.zad.almoslem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
import android.view.ViewGroup;
import android.text.InputType;
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
    private TextView pageLabel, status, attribution;
    private LinearLayout root, toolbar, offlineRow, pageFrame, nav;
    private boolean fullScreen = false;
    private ImageView pageImage;
    private Button previous, next;
    private ProgressBar progress;
    private GestureDetector gesture;
    private int green;
    private String pendingAyahMessage = "";
    private int pendingSurahNumber = -1;
    private int pendingVerseNumber = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        green=AppThemeColors.primary(this);
        AppThemeColors.apply(this);
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
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(3), dp(3), dp(3), dp(4));
        root.setBackgroundColor(AppThemeColors.bg(this));

        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);

        Button index = new Button(this);
        index.setText("☰ فهرس السور");
        index.setTextSize(13);
        toolbar.addView(index, new LinearLayout.LayoutParams(0, dp(34), 1));
        index.setOnClickListener(v -> showSurahIndex());

        pageLabel = label("", 15, AppThemeColors.text(this), true);
        toolbar.addView(pageLabel, new LinearLayout.LayoutParams(0, dp(38), 1));
        Button correction = new Button(this);
        correction.setText("بحث سورة/آية");
        correction.setTextSize(11);
        toolbar.addView(correction, new LinearLayout.LayoutParams(0, dp(38), 1));
        correction.setOnClickListener(v -> showAyahSearch());
        // Put the controls above the page and let the page consume all remaining
        // available height. A fixed pixel-derived height left a large blank area
        // on phones with different density and navigation-bar sizes.
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(34)));

        offlineRow = new LinearLayout(this);
        offlineRow.setOrientation(LinearLayout.VERTICAL);
        offlineRow.setPadding(dp(4), dp(3), dp(4), dp(3));
        Button downloadAll = new Button(this);
        downloadAll.setText("تحميل المصحف كاملًا للعمل دون إنترنت");
        downloadAll.setAllCaps(false);
        TextView offlineStatus = label("يمكنك تنزيل الصفحات الـ 604 مرة واحدة والقراءة دون اتصال.", 11, AppThemeColors.muted(this), false);
        offlineRow.addView(downloadAll, new LinearLayout.LayoutParams(-1, dp(38)));
        offlineRow.addView(offlineStatus, new LinearLayout.LayoutParams(-1, dp(18)));
        root.addView(offlineRow, new LinearLayout.LayoutParams(-1, -2));
        downloadAll.setOnClickListener(v -> downloadAllPages(downloadAll, offlineStatus));

        pageFrame = new LinearLayout(this);
        pageFrame.setOrientation(LinearLayout.VERTICAL);
        pageFrame.setGravity(Gravity.CENTER);
        pageFrame.setPadding(dp(1), dp(1), dp(1), dp(1));
        pageFrame.setBackground(shape(Color.rgb(255,253,244), 5, Color.rgb(177,157,112)));
        pageImage = new ImageView(this);
        pageImage.setAdjustViewBounds(false);
        pageImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pageFrame.addView(pageImage, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        imageLp.topMargin = dp(2);
        imageLp.bottomMargin = dp(2);
        root.addView(pageFrame, imageLp);

        gesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapUp(MotionEvent e) {
                toggleFullScreen();
                return true;
            }
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
        pageImage.setClickable(true);
        pageImage.setOnTouchListener((v, event) -> gesture.onTouchEvent(event));
        pageFrame.setOnTouchListener((v, event) -> gesture.onTouchEvent(event));

        status = label("صفحات المصحف تُحمّل وتُحفظ تلقائيًا على الهاتف", 12, AppThemeColors.muted(this), false);
        status.setTextSize(10);
        status.setPadding(0, dp(2), 0, dp(2));
        root.addView(status);
        attribution = label("صور صفحات المصحف: Quran-Images — ترخيص MIT", 9, AppThemeColors.muted(this), false);
        root.addView(attribution);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        nav = new LinearLayout(this);
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

    private void toggleFullScreen() {
        fullScreen = !fullScreen;
        if (fullScreen) {
            if (root.getParent() instanceof ViewGroup) ((ViewGroup) root.getParent()).removeView(root);
            root.setPadding(0, 0, 0, 0);
            toolbar.setVisibility(View.GONE);
            offlineRow.setVisibility(View.GONE);
            status.setVisibility(View.GONE);
            attribution.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
            nav.setVisibility(View.GONE);
            pageFrame.setPadding(0, 0, 0, 0);
            pageFrame.setBackgroundColor(Color.BLACK);
            pageFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
            setContentView(root);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(0);
            AppThemeColors.apply(this);
            root.setPadding(dp(3), dp(3), dp(3), dp(4));
            toolbar.setVisibility(View.VISIBLE);
            offlineRow.setVisibility(View.VISIBLE);
            status.setVisibility(View.VISIBLE);
            attribution.setVisibility(View.VISIBLE);
            nav.setVisibility(View.VISIBLE);
            pageFrame.setPadding(dp(1), dp(1), dp(1), dp(1));
            pageFrame.setBackground(shape(Color.rgb(255,253,244), 5, Color.rgb(177,157,112)));
            pageFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
            if (root.getParent() instanceof ViewGroup) ((ViewGroup) root.getParent()).removeView(root);
            setContentView(AppTabs.wrap(this, root, "mushaf"));
        }
    }

    @Override public void onBackPressed() {
        if (fullScreen) {
            toggleFullScreen();
            return;
        }
        super.onBackPressed();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fullScreen) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    private void showAyahSearch() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(12), dp(18), dp(6));
        TextView title = label("اختر السورة واكتب رقم الآية إن أردت", 14, AppThemeColors.text(this), true);
        title.setGravity(Gravity.RIGHT);
        box.addView(title, new LinearLayout.LayoutParams(-1, -2));
        Spinner surahPicker = new Spinner(this);
        surahPicker.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SURAHS));
        box.addView(surahPicker, new LinearLayout.LayoutParams(-1, dp(48)));
        EditText ayahNumber = new EditText(this);
        ayahNumber.setSingleLine(true);
        ayahNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        ayahNumber.setHint("رقم الآية (اختياري)");
        box.addView(ayahNumber, new LinearLayout.LayoutParams(-1, dp(50)));
        new AlertDialog.Builder(this)
            .setTitle("البحث في القرآن الكريم")
            .setView(box)
            .setPositiveButton("فتح", (dialog, which) -> {
                int surah = surahPicker.getSelectedItemPosition() + 1;
                String value = ayahNumber.getText().toString().trim();
                if (value.isEmpty()) {
                    pendingAyahMessage = "";
                    status.setClickable(false);
                    status.setOnClickListener(null);
                    showPage(SURAH_PAGES[surah - 1]);
                    return;
                }
                try {
                    int verse = Integer.parseInt(value);
                    if (verse < 1) throw new NumberFormatException();
                    findAyahPage(surah, verse);
                } catch (NumberFormatException ignored) {
                    Toast.makeText(this, "اكتب رقم آية صحيحًا", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void findAyahPage(int surah, int verse) {
        if (busy) return;
        busy = true;
        status.setText("جارٍ البحث عن الآية وموضعها في صفحات المصحف...");
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://api.alquran.cloud/v1/ayah/" + surah + ":" + verse + "/quran-uthmani");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "ZadAlmoslem-Android");
                if (connection.getResponseCode() != 200) throw new IOException("تعذر العثور على الآية");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream in = connection.getInputStream()) {
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                }
                org.json.JSONObject response = new org.json.JSONObject(new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
                if (response.optInt("code") != 200) throw new IOException("رقم الآية غير صحيح في هذه السورة");
                org.json.JSONObject data = response.getJSONObject("data");
                int targetPage = data.optInt("page", 0);
                if (targetPage < 1 || targetPage > 604) throw new IOException("تعذر تحديد صفحة الآية");
                String verseText = data.optString("text", "");
                String surahName = SURAHS[surah - 1];
                String message = "سورة " + surahName + " — الآية " + verse + " — صفحة " + targetPage;
                runOnUiThread(() -> {
                    busy = false;
                    pendingAyahMessage = message + "\n" + verseText;
                    pendingSurahNumber = surah;
                    pendingVerseNumber = verse;
                    showPage(targetPage);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    busy = false;
                    progress.setVisibility(View.GONE);
                    status.setText(error.getMessage() == null ? "تعذر البحث عن الآية. تحقق من الإنترنت." : error.getMessage());
                    Toast.makeText(this, "لم يتم العثور على الآية. راجع السورة ورقم الآية.", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void openInteractivePage() {
        Intent open = new Intent(this, QuranTextActivity.class);
        open.putExtra("page_number", page);
        startActivity(open);
    }

    private void showSearchVerseResult(String message) {
        status.setText(message);
        status.setTextSize(17);
        status.setTextColor(AppThemeColors.text(this));
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(12), dp(12), dp(12), dp(12));
        status.setBackground(shape(Color.rgb(255, 253, 244), 12, Color.rgb(177, 157, 112)));
        status.setText(message + "\n\nاضغط هنا لفتح الآية في عرض النص وتكبيرها");
        status.setClickable(true);
        status.setOnClickListener(v -> {
            if (pendingSurahNumber < 1 || pendingVerseNumber < 1) return;
            Intent open = new Intent(this, QuranTextActivity.class);
            open.putExtra("page_number", page);
            open.putExtra("surah_number", pendingSurahNumber);
            open.putExtra("target_ayah", pendingVerseNumber);
            startActivity(open);
        });
    }

    private File pageFile(int number) {
        File dir = new File(getFilesDir(), "mushaf_images");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, number + ".png");
    }

    private void showPage(int number) {
        if (number < 1 || number > 604 || busy) return;
        if (pendingAyahMessage.isEmpty()) {
            status.setClickable(false);
            status.setOnClickListener(null);
            pendingSurahNumber = -1;
            pendingVerseNumber = -1;
        }
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
                if (pendingAyahMessage.isEmpty()) status.setText("صفحة محفوظة على الهاتف — يمكنك القراءة دون إنترنت");
                else showSearchVerseResult(pendingAyahMessage);
                pendingAyahMessage = "";
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
                        if (pendingAyahMessage.isEmpty()) status.setText("تم تحميل الصفحة وحفظها على الهاتف");
                        else showSearchVerseResult(pendingAyahMessage);
                        pendingAyahMessage = "";
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
            entries[i] = String.format(ArabicLocaleHolder.LOCALE, "%03d  سورة %s  —  الآية ١  —  صفحة %d", i + 1, SURAHS[i], SURAH_PAGES[i]);
        new AlertDialog.Builder(this)
                .setTitle("فهرس سور القرآن الكريم")
                .setItems(entries, (dialog, which) -> showPage(SURAH_PAGES[which]))
                .setNegativeButton("إغلاق", null)
                .show();
    }



    private boolean isValidMushafPage(File file) {
        if (file == null || !file.isFile() || file.length() < 10000) return false;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return options.outWidth > 0 && options.outHeight > 0;
    }

    private void downloadAllPages(Button button, TextView message) {
        if (busy) return;
        File dir = new File(getFilesDir(), "mushaf_images");
        if (!dir.exists() && !dir.mkdirs()) {
            message.setText("تعذر إنشاء مساحة حفظ المصحف على الهاتف.");
            return;
        }
        busy = true;
        button.setEnabled(false);
        message.setText("جارٍ تجهيز تنزيل المصحف كاملًا...");
        new Thread(() -> {
            int completed = 0;
            int failedPage = -1;
            for (int number = 1; number <= 604; number++) {
                File target = new File(dir, number + ".png");
                if (isValidMushafPage(target)) {
                    completed++;
                } else {
                    HttpURLConnection connection = null;
                    File temp = new File(dir, number + ".part");
                    try {
                        connection = (HttpURLConnection) new URL(IMAGE_BASE + number + ".png").openConnection();
                        connection.setConnectTimeout(20000);
                        connection.setReadTimeout(45000);
                        connection.setRequestProperty("User-Agent", "ZadAlmoslem Android");
                        if (connection.getResponseCode() != 200) throw new IOException("HTTP " + connection.getResponseCode());
                        try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                            byte[] buffer = new byte[16384];
                            int count;
                            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                            out.getFD().sync();
                        }
                        if (temp.length() < 10000) throw new IOException("Incomplete page");
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(temp.getAbsolutePath(), options);
                        if (options.outWidth <= 0 || options.outHeight <= 0) throw new IOException("Invalid page image");
                        if (target.exists()) target.delete();
                        if (!temp.renameTo(target)) throw new IOException("Could not save page");
                        completed++;
                    } catch (Exception error) {
                        failedPage = number;
                        break;
                    } finally {
                        if (connection != null) connection.disconnect();
                        if (temp.exists()) temp.delete();
                    }
                }
                final int done = completed;
                if (done % 5 == 0 || done == 604) runOnUiThread(() ->
                        message.setText("تم حفظ " + done + " من 604 صفحة على الهاتف."));
            }
            final int done = completed;
            final int failed = failedPage;
            runOnUiThread(() -> {
                busy = false;
                button.setEnabled(true);
                if (failed < 0 && done == 604) {
                    message.setText("اكتمل تنزيل المصحف كاملًا. يمكنك الآن تقليب الصفحات دون إنترنت.");
                    getSharedPreferences("zad_mushaf", MODE_PRIVATE).edit().putBoolean("all_pages_downloaded", true).apply();
                } else {
                    message.setText("تم حفظ " + done + " صفحة. تعذر تنزيل الصفحة رقم " + failed + "؛ أعد المحاولة لاستكمال الباقي.");
                    getSharedPreferences("zad_mushaf", MODE_PRIVATE).edit().putBoolean("all_pages_downloaded", false).apply();
                }
            });
        }).start();
    }

    private static final class ArabicLocaleHolder {
        static final Locale LOCALE = new Locale("ar");
    }
}
