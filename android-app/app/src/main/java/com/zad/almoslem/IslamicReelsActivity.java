package com.zad.almoslem;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.BitmapOverlay;
import androidx.media3.effect.OverlayEffect;
import androidx.media3.effect.StaticOverlaySettings;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public class IslamicReelsActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ArrayList<JSONObject> reels = new ArrayList<>();
    private int index = 0;
    private float downY;
    private MediaPlayer player;
    private boolean playerPrepared = false;
    private ImageView image;
    private TextView title, description, counter, status;
    private Button playButton, saveAudioButton, saveReelButton, shareButton;
    private Transformer transformer;
    private boolean exporting = false;

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
        RemoteContentRepository.sync(this, "reels", this::loadReels);
        loadReels();
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
        title = label("جارٍ تحميل الريلز…", 20, 0xffffe5b0, true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(44)));
        description = label("", 14, Color.WHITE, false);
        root.addView(description, new LinearLayout.LayoutParams(-1, -2));
        status = label("اختر ريلز للاستماع", 12, 0xffd7e4dc, false);
        status.setPadding(0, dp(7), 0, dp(7)); root.addView(status);
        playButton = button("▶ تشغيل"); root.addView(playButton, new LinearLayout.LayoutParams(-1, dp(48)));
        playButton.setOnClickListener(v -> togglePlayback());
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER);
        saveAudioButton = button("حفظ الصوت MP3");
        saveReelButton = button("حفظ الريلز فيديو");
        actions.addView(saveAudioButton, new LinearLayout.LayoutParams(0, dp(52), 1));
        actions.addView(saveReelButton, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(actions);
        shareButton = button("مشاركة الريلز"); root.addView(shareButton, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER);
        Button prev = button("السابق"), next = button("التالي");
        nav.addView(prev, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(next, new LinearLayout.LayoutParams(0, dp(48), 1)); root.addView(nav);
        prev.setOnClickListener(v -> move(-1)); next.setOnClickListener(v -> move(1));
        saveAudioButton.setOnClickListener(v -> saveAudio());
        saveReelButton.setOnClickListener(v -> exportReel(false));
        shareButton.setOnClickListener(v -> exportReel(true));
        frame.setOnTouchListener((v, e) -> {
            if (e.getAction() == 0) { downY = e.getY(); return true; }
            if (e.getAction() == 1) { float d = downY - e.getY(); if (Math.abs(d) > dp(45)) move(d > 0 ? 1 : -1); return true; }
            return true;
        });
        setContentView(AppTabs.wrap(this, root, "more"));
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private void loadReels() {
        JSONArray rows = RemoteContentRepository.cached(this, "reels");
        ArrayList<JSONObject> fresh = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && row.optBoolean("published", true)
                    && row.optString("imageUrl").startsWith("https://")
                    && row.optString("audioUrl").startsWith("https://")) fresh.add(row);
        }
        Collections.sort(fresh, Comparator.comparingInt(o -> o.optInt("order", 0)));
        reels.clear(); reels.addAll(fresh);
        if (reels.isEmpty()) {
            stopPlayer(); title.setText("لا توجد ريلز منشورة حاليًا");
            description.setText("ستظهر هنا الريلز التي تنشرها من لوحة الإدارة.");
            counter.setText("0 / 0"); image.setImageDrawable(null);
            setActionsEnabled(false); return;
        }
        if (index >= reels.size()) index = 0;
        showReel();
    }

    private void setActionsEnabled(boolean enabled) {
        playButton.setEnabled(enabled); saveAudioButton.setEnabled(enabled);
        saveReelButton.setEnabled(enabled); shareButton.setEnabled(enabled);
    }

    private JSONObject current() { return reels.isEmpty() ? null : reels.get(index); }

    private void showReel() {
        JSONObject r = current(); if (r == null) return;
        stopPlayer();
        title.setText(r.optString("title", "تذكرة إيمانية"));
        description.setText(r.optString("description", ""));
        counter.setText((index + 1) + " / " + reels.size());
        status.setText("جارٍ تحميل الصورة…"); playButton.setText("▶ تشغيل");
        setActionsEnabled(true);
        io.execute(() -> {
            Bitmap bitmap = null;
            try { bitmap = downloadBitmap(r.optString("imageUrl")); } catch (Exception ignored) {}
            Bitmap result = bitmap;
            runOnUiThread(() -> {
                if (current() != r) return;
                if (result != null) { image.setImageBitmap(result); status.setText("جاهز للاستماع"); }
                else status.setText("تعذر تحميل الصورة. تحقق من الاتصال.");
            });
        });
    }

    private Bitmap downloadBitmap(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(20000); c.setInstanceFollowRedirects(true);
        try (InputStream in = c.getInputStream()) { return BitmapFactory.decodeStream(in); }
        finally { c.disconnect(); }
    }

    private File downloadToCache(String url, String suffix) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setInstanceFollowRedirects(true);
        File out = File.createTempFile("zad_reel_", suffix, getCacheDir());
        try (InputStream in = c.getInputStream(); FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[16384]; int n; while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
        } finally { c.disconnect(); }
        return out;
    }

    private void togglePlayback() {
        JSONObject r = current(); if (r == null) return;
        if (player != null && player.isPlaying()) { player.pause(); playButton.setText("▶ استكمال"); return; }
        try {
            if (player == null) {
                player = new MediaPlayer();
                player.setOnPreparedListener(mp -> { playerPrepared = true; mp.start(); playButton.setText("Ⅱ إيقاف مؤقت"); status.setText("يعمل الصوت الآن"); });
                player.setOnCompletionListener(mp -> move(1));
                player.setOnErrorListener((mp, what, extra) -> { status.setText("تعذر تشغيل التسجيل."); return true; });
                player.setDataSource(r.optString("audioUrl")); player.prepareAsync();
                status.setText("جارٍ تجهيز الصوت…");
            } else if (!playerPrepared) { status.setText("جارٍ تجهيز الصوت، انتظر لحظة…"); } else { player.start(); playButton.setText("Ⅱ إيقاف مؤقت"); }
        } catch (Exception e) { status.setText("تعذر تشغيل الصوت: " + e.getMessage()); }
    }

    private void stopPlayer() {
        if (player != null) { try { player.stop(); } catch (Exception ignored) {} player.release(); player = null; }
        playerPrepared = false;
    }

    private void move(int direction) {
        if (reels.isEmpty()) return;
        index = (index + direction + reels.size()) % reels.size(); showReel();
    }

    private void saveAudio() {
        JSONObject r = current(); if (r == null) return;
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(r.optString("audioUrl")));
            String audioUrl = r.optString("audioUrl");
            String path = Uri.parse(audioUrl).getLastPathSegment();
            String ext = path != null && path.matches("(?i).+\\.(mp3|m4a|wav|ogg)") ? path.substring(path.lastIndexOf('.')) : ".mp3";
            String name = safeName(r.optString("title", "zad-reel")) + ext;
            req.setTitle("زاد المسلم - " + r.optString("title"));
            req.setDescription("تحميل صوت الريلز");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "زاد المسلم/" + name);
            req.setAllowedOverMetered(true); req.setAllowedOverRoaming(false);
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
            status.setText("بدأ تنزيل الصوت إلى مجلد التنزيلات.");
        } catch (Exception e) { status.setText("تعذر بدء تنزيل الصوت: " + e.getMessage()); }
    }

    private String safeName(String value) {
        String s = value == null ? "zad-reel" : value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return s.isEmpty() ? "zad-reel" : s;
    }

    private void exportReel(boolean shareAfter) {
        JSONObject r = current(); if (r == null || exporting) return;
        exporting = true; setActionsEnabled(false); status.setText("جارٍ تجهيز الريلز المرئي…");
        final String titleText = r.optString("title", "zad-reel");
        io.execute(() -> {
            File img = null, audio = null, output = null;
            try {
                img = downloadToCache(r.optString("imageUrl"), ".jpg");
                audio = downloadToCache(r.optString("audioUrl"), ".mp3");
                android.media.MediaMetadataRetriever meta = new android.media.MediaMetadataRetriever();
                meta.setDataSource(audio.getAbsolutePath());
                String durationText = meta.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                meta.release();
                long durationMs = Long.parseLong(durationText);
                if (durationMs <= 0 || durationMs >= 60000) throw new Exception("مدة الصوت يجب أن تكون أقل من دقيقة.");
                output = new File(getCacheDir(), "zad-reel-" + System.currentTimeMillis() + ".mp4");
                File finalImg = img, finalAudio = audio, finalOutput = output;
                runOnUiThread(() -> startExport(finalImg, finalAudio, finalOutput, titleText, durationMs, shareAfter));
            } catch (Exception e) {
                exporting = false; runOnUiThread(() -> { setActionsEnabled(true); status.setText("تعذر تجهيز الريلز: " + e.getMessage()); });
            }
        });
    }

    private void startExport(File imageFile, File audioFile, File output, String reelTitle, long durationMs, boolean shareAfter) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bounds);
            int markWidth = Math.max(640, Math.min(1920, bounds.outWidth));
            int markHeight = Math.max(640, Math.round(markWidth * (bounds.outHeight / (float)Math.max(1, bounds.outWidth))));
            Bitmap mark = Bitmap.createBitmap(markWidth, markHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mark);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            float margin = markWidth * 0.025f, bandHeight = Math.max(72f, markHeight * 0.075f);
            canvas.drawRoundRect(margin, markHeight - bandHeight - margin, markWidth - margin, markHeight - margin,
                    bandHeight * 0.18f, bandHeight * 0.18f, p);
            p.setColor(0xbb000000);
            canvas.drawRoundRect(margin, markHeight - bandHeight - margin, markWidth - margin, markHeight - margin,
                    bandHeight * 0.18f, bandHeight * 0.18f, p);
            p.setColor(Color.WHITE); p.setTextSize(Math.max(28f, bandHeight * 0.42f));
            p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("زاد المسلم  |  zad-almoslem.github.io", markWidth / 2f,
                    markHeight - margin - bandHeight * 0.34f, p);
            BitmapOverlay watermark = BitmapOverlay.createStaticBitmapOverlay(mark, new StaticOverlaySettings.Builder().build());
            OverlayEffect overlay = new OverlayEffect(Collections.singletonList(watermark));
            MediaItem imageItem = new MediaItem.Builder().setUri(Uri.fromFile(imageFile)).setImageDurationMs(durationMs).build();
            EditedMediaItem video = new EditedMediaItem.Builder(imageItem).setFrameRate(30)
                    .setRemoveAudio(true).setEffects(new Effects(Collections.emptyList(), Collections.singletonList(overlay))).build();
            EditedMediaItem audio = new EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(audioFile))).setRemoveVideo(true).build();
            EditedMediaItemSequence videoSequence = EditedMediaItemSequence.withVideoFrom(Collections.singletonList(video));
            EditedMediaItemSequence audioSequence = EditedMediaItemSequence.withAudioFrom(Collections.singletonList(audio));
            Composition composition = new Composition.Builder(videoSequence, audioSequence).build();
            transformer = new Transformer.Builder(this).addListener(new Transformer.Listener() {
                @Override public void onCompleted(Composition c, ExportResult result) {
                    exporting = false; transformer = null; setActionsEnabled(true);
                    saveExportedVideo(output, reelTitle, shareAfter);
                }
                @Override public void onError(Composition c, ExportResult result, ExportException error) {
                    exporting = false; transformer = null; setActionsEnabled(true);
                    status.setText("تعذر إنشاء ملف الريلز: " + error.getMessage());
                }
            }).build();
            transformer.start(composition, output.getAbsolutePath());
        } catch (Exception e) {
            exporting = false; transformer = null; setActionsEnabled(true);
            status.setText("تعذر بدء تصدير الريلز: " + e.getMessage());
        }
    }

    private void saveExportedVideo(File file, String reelTitle, boolean shareAfter) {
        io.execute(() -> {
            Uri uri = null;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, safeName(reelTitle) + "-زاد-المسلم.mp4");
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/زاد المسلم");
                uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("تعذر إنشاء ملف الفيديو في المعرض.");
                try (InputStream in = new FileInputStream(file); OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[16384]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                }
                Uri finalUri = uri;
                runOnUiThread(() -> {
                    status.setText("تم حفظ الريلز المرئي في مجلد Movies/زاد المسلم.");
                    if (shareAfter) {
                        Intent send = new Intent(Intent.ACTION_SEND); send.setType("video/mp4");
                        send.putExtra(Intent.EXTRA_STREAM, finalUri); send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(send, "مشاركة ريلز زاد المسلم"));
                    }
                });
            } catch (Exception e) {
                if (uri != null) getContentResolver().delete(uri, null, null);
                runOnUiThread(() -> status.setText("تعذر حفظ ملف الفيديو: " + e.getMessage()));
            } finally { file.delete(); }
        });
    }

    @Override protected void onPause() { super.onPause(); if (player != null && player.isPlaying()) player.pause(); }
    @Override protected void onDestroy() { stopPlayer(); io.shutdownNow(); super.onDestroy(); }
}
