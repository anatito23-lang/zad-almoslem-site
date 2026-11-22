package com.zad.almoslem.admin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.SafeBrowsingResponse;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.view.Gravity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AdminWebActivity extends Activity {
    static final String EXTRA_URL = "admin_url";
    static final String EXTRA_TITLE = "admin_title";
    private static final Set<String> ALLOWED_HOSTS = new HashSet<>(Arrays.asList(
            "zad-almoslem.github.io",
            "www.gstatic.com",
            "zad-almoslem.firebaseapp.com",
            "identitytoolkit.googleapis.com",
            "securetoken.googleapis.com",
            "firestore.googleapis.com",
            "firebaseinstallations.googleapis.com",
            "www.googleapis.com",
            "accounts.google.com"
    ));
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (!isAllowed(url)) { finish(); return; }
        getWindow().setStatusBarColor(Color.rgb(18, 66, 54));
        getWindow().setNavigationBarColor(Color.rgb(18, 66, 54));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        TextView bar = new TextView(this);
        bar.setText("‹   " + (title == null ? "لوحة الإدارة" : title));
        bar.setTextSize(17);
        bar.setTextColor(Color.WHITE);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), dp(12), dp(18), dp(12));
        bar.setBackgroundColor(Color.rgb(18, 66, 54));
        bar.setOnClickListener(v -> finish());
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(54)));

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);
        WebView.setWebContentsDebuggingEnabled(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equalsIgnoreCase(uri.getScheme()) && isAllowed(uri.toString())) return false;
                if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
                return true;
            }
            @Override public void onSafeBrowsingHit(WebView view, WebResourceRequest request,
                    int threatType, SafeBrowsingResponse callback) {
                callback.backToSafety(true);
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        webView.loadUrl(url);
    }

    private boolean isAllowed(String value) {
        if (value == null) return false;
        Uri uri = Uri.parse(value);
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && host != null && ALLOWED_HOSTS.contains(host);
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (webView != null) { webView.stopLoading(); webView.destroy(); webView = null; }
        super.onDestroy();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
