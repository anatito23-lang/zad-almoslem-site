package com.zad.almoslem;
import android.app.Activity;import android.graphics.Color;
public final class AppThemeColors{
private AppThemeColors(){}
public static String get(Activity a){return a.getSharedPreferences("zad_settings",Activity.MODE_PRIVATE).getString("app_theme","green");}
public static int bg(Activity a){String t=get(a);return "dark".equals(t)?Color.rgb(18,27,34):"light".equals(t)?Color.rgb(247,248,250):Color.rgb(244,247,243);}
public static int surface(Activity a){return "dark".equals(get(a))?Color.rgb(32,44,52):Color.WHITE;}
public static int text(Activity a){return "dark".equals(get(a))?Color.rgb(239,243,245):Color.rgb(25,55,44);}
public static int muted(Activity a){return "dark".equals(get(a))?Color.rgb(185,197,204):Color.rgb(85,99,91);}
public static int primary(Activity a){return "light".equals(get(a))?Color.rgb(21,100,76):Color.rgb(8,48,40);}
public static int nav(Activity a){return "dark".equals(get(a))?Color.rgb(10,17,23):Color.rgb(16,34,54);}
public static int accent(Activity a){return Color.rgb(50,199,181);}
public static void apply(Activity a){int p=primary(a);a.getWindow().setStatusBarColor(p);a.getWindow().setNavigationBarColor(nav(a));}
}