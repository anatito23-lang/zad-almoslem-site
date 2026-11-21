package com.zad.almoslem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Section headers use genuine photographs, cached locally after first successful load. */
public final class PhotoHeaderView extends FrameLayout {
    private static String photoUrl(String kind) {
        if ("radio".equals(kind))
            return "https://www.gub.uy/intendencia-florida/sites/intendencia-florida/files/imagenes/noticias/estudio%20de%20radio.opt_.png";
        if ("ruqyah".equals(kind))
            return "https://images.unsplash.com/photo-1721058677807-34a4fcde1952?auto=format&fit=crop&w=1400&q=82";
        return "https://images.unsplash.com/photo-1650083731680-9658df904125?auto=format&fit=crop&w=1400&q=82";
    }

    public PhotoHeaderView(Context context, String kind, String title, String subtitle) {
        super(context);
        setClipToOutline(true);
        GradientDrawable fallback = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{"radio".equals(kind) ? 0xFF172B3C : 0xFF173C32,
                        "ruqyah".equals(kind) ? 0xFF506B5C : 0xFF80613F});
        fallback.setCornerRadius(dp(22));
        setBackground(fallback);

        ImageView photo = new ImageView(context);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(photo, new FrameLayout.LayoutParams(-1, -1));

        View shade = new View(context);
        GradientDrawable scrim = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x11000000, 0x33000000, 0xDD000000});
        scrim.setCornerRadius(dp(22));
        shade.setBackground(scrim);
        addView(shade, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        labels.setPadding(dp(18), dp(18), dp(18), dp(18));
        FrameLayout.LayoutParams labelsLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        addView(labels, labelsLp);

        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(23);
        heading.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        labels.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        TextView detail = new TextView(context);
        detail.setText(subtitle);
        detail.setTextColor(0xFFF4EBDD);
        detail.setTextSize(12);
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
        detailLp.topMargin = dp(7);
        labels.addView(detail, detailLp);

        loadPhoto(kind, photo);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void loadPhoto(String kind, ImageView target) {
        File directory = new File(getContext().getFilesDir(), "section_photos");
        File cached = new File(directory, kind + ".img");
        new Thread(() -> {
            Bitmap bitmap = cached.isFile() ? BitmapFactory.decodeFile(cached.getAbsolutePath()) : null;
            if (bitmap == null) {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(photoUrl(kind)).openConnection();
                    connection.setConnectTimeout(12000);
                    connection.setReadTimeout(18000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", "ZadAlmoslem-Android");
                    connection.connect();
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        try (InputStream in = connection.getInputStream()) {
                            if (!directory.exists()) directory.mkdirs();
                            try (FileOutputStream out = new FileOutputStream(cached)) {
                                byte[] buffer = new byte[8192];
                                int count;
                                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                            }
                        }
                        bitmap = BitmapFactory.decodeFile(cached.getAbsolutePath());
                    }
                } catch (Exception e) {
                    android.util.Log.w("ZadPhoto", "Could not load section photo: " + kind, e);
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
            final Bitmap result = bitmap;
            if (result != null) post(() -> target.setImageBitmap(result));
        }).start();
    }
}
