package com.zad.almoslem;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Full-bleed photographic Islamic wallpapers with a restrained readable overlay. */
public final class IslamicWallpaperDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final String style;
    private final Bitmap wallpaper;

    public IslamicWallpaperDrawable(Activity activity, String style) {
        this.style = style == null ? "home" : style;
        String asset = "quran_wallpaper";
        if ("radio".equals(this.style)) asset = "madinah_wallpaper";
        else if ("ruqyah".equals(this.style)) asset = "quran_wallpaper";
        else if ("recordings".equals(this.style)) asset = "azhar_mosque_bg";
        else if ("ramadan".equals(this.style)) asset = "kaaba_wallpaper";
        else if ("quran".equals(this.style) || "mushaf".equals(this.style)) asset = "quran_wallpaper";
        else if ("home".equals(this.style)) asset = "aqsa_wallpaper";

        int id = activity.getResources().getIdentifier(asset, "drawable", activity.getPackageName());
        Bitmap decoded = null;
        if (id != 0) {
            try { decoded = BitmapFactory.decodeResource(activity.getResources(), id); }
            catch (Exception ignored) { decoded = null; }
        }
        wallpaper = decoded;
    }

    private int fallbackBase() {
        if ("radio".equals(style)) return Color.rgb(8, 37, 48);
        if ("ruqyah".equals(style)) return Color.rgb(7, 43, 35);
        if ("recordings".equals(style)) return Color.rgb(29, 31, 49);
        if ("ramadan".equals(style)) return Color.rgb(22, 35, 57);
        return Color.rgb(8, 43, 38);
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float w = bounds.width(), h = bounds.height();
        if (w <= 0 || h <= 0) return;

        if (wallpaper != null && !wallpaper.isRecycled()) {
            float scale = Math.max(w / wallpaper.getWidth(), h / wallpaper.getHeight());
            float sw = wallpaper.getWidth() * scale, sh = wallpaper.getHeight() * scale;
            float left = (w - sw) / 2f, top = (h - sh) / 2f;
            Rect src = new Rect(0, 0, wallpaper.getWidth(), wallpaper.getHeight());
            Rect dst = new Rect((int)left, (int)top, (int)(left + sw), (int)(top + sh));
            paint.setAlpha(255);
            canvas.drawBitmap(wallpaper, src, dst, paint);
        } else {
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{fallbackBase(), Color.rgb(3, 18, 24), fallbackBase()},
                    null, Shader.TileMode.CLAMP));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(bounds, paint);
            paint.setShader(null);
        }

        // Deep translucent veil: keeps text readable without hiding the photograph.
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xB9001118, 0x78001218, 0xA9001118},
                new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds, paint);
        paint.setShader(null);

        // Fine warm-gold framing and a quiet mihrab motif.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, w / 420f));
        paint.setColor(0x36E8D3A0);
        float inset = Math.max(13f, w * .045f);
        canvas.drawRoundRect(inset, inset, w - inset, h - inset,
                Math.max(14f, w * .035f), Math.max(14f, w * .035f), paint);

        paint.setStrokeWidth(Math.max(1.5f, w / 230f));
        paint.setColor(0x25F0D9A2);
        float mid = w / 2f;
        Path arch = new Path();
        arch.moveTo(w * .13f, h * .48f);
        arch.lineTo(w * .13f, h * .25f);
        arch.quadTo(w * .13f, h * .075f, mid, h * .075f);
        arch.quadTo(w * .87f, h * .075f, w * .87f, h * .25f);
        arch.lineTo(w * .87f, h * .48f);
        canvas.drawPath(arch, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x2DEBD5A0);
        float radius = Math.max(2f, w * .004f);
        canvas.drawCircle(mid, h * .075f, radius, paint);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
