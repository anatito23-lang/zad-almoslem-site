package com.zad.almoslem;

import android.content.Context;
import android.widget.ImageView;

/**
 * Loads photographs packaged inside the APK.
 * The build workflow downloads and validates the licensed Wikimedia Commons
 * photographs before Gradle runs, so no network request is needed on-device.
 */
public final class AdhanWallpaperImages {
    private AdhanWallpaperImages() {}

    public static void load(Context context, ImageView target, String id) {
        if (target == null || id == null) return;
        final int resourceId;
        switch (id) {
            case "azhar_mosque_bg":
                resourceId = R.drawable.azhar_mosque_bg;
                break;
            case "kaaba_wallpaper":
                resourceId = R.drawable.kaaba_wallpaper;
                break;
            case "madinah_wallpaper":
                resourceId = R.drawable.madinah_wallpaper;
                break;
            case "aqsa_wallpaper":
                resourceId = R.drawable.aqsa_wallpaper;
                break;
            case "quran_wallpaper":
                resourceId = R.drawable.quran_wallpaper;
                break;
            default:
                return;
        }
        target.setImageResource(resourceId);
    }
}