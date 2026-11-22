package com.zad.almoslem.admin;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AdminHomeActivity extends Activity {
    private final int ink = Color.rgb(28, 54, 48);
    private final int green = Color.rgb(25, 105, 82);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(18, 66, 54));
        getWindow().setNavigationBarColor(Color.rgb(18, 66, 54));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(22), dp(30), dp(22), dp(24));
        root.setBackgroundColor(Color.rgb(247, 249, 246));

        TextView title = new TextView(this);
        title.setText("إدارة زاد المسلم");
        title.setTextColor(ink);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("لوحة واحدة لإدارة التطبيق والموقع");
        subtitle.setTextColor(Color.rgb(102, 119, 111));
        subtitle.setTextSize(15);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(8);
        subParams.bottomMargin = dp(28);
        root.addView(subtitle, subParams);

        root.addView(createCard("إدارة التطبيق", "الخطب والأحاديث والوقفات وتبويبات التطبيق",
                "فتح لوحة إدارة التطبيق", "https://zad-almoslem.github.io/content-admin.html"));
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(16)));
        root.addView(createCard("إدارة الموقع", "أقسام الموقع ومحتواه والتبويبات المخصصة",
                "فتح لوحة إدارة الموقع", "https://zad-almoslem.github.io/website-admin.html"));

        TextView note = new TextView(this);
        note.setText("الدخول محمي بحساب المسؤول المعتمد. لا تشارك بيانات تسجيل الدخول.");
        note.setTextColor(Color.rgb(112, 122, 116));
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(28);
        root.addView(note, noteParams);
        setContentView(root);
    }

    private View createCard(String heading, String description, String action, String url) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(background(Color.WHITE, dp(18), Color.rgb(225, 233, 227)));
        card.setElevation(dp(2));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView h = new TextView(this);
        h.setText(heading);
        h.setTextColor(ink);
        h.setTextSize(20);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(h);

        TextView d = new TextView(this);
        d.setText(description);
        d.setTextColor(Color.rgb(94, 111, 102));
        d.setTextSize(14);
        LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(-1, -2);
        dParams.topMargin = dp(9);
        card.addView(d, dParams);

        TextView button = new TextView(this);
        button.setText(action + "  ←");
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(13), dp(14), dp(13));
        button.setBackground(background(green, dp(12), green));
        LinearLayout.LayoutParams bParams = new LinearLayout.LayoutParams(-1, -2);
        bParams.topMargin = dp(18);
        card.addView(button, bParams);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminWebActivity.class);
            intent.putExtra(AdminWebActivity.EXTRA_URL, url);
            intent.putExtra(AdminWebActivity.EXTRA_TITLE, heading);
            startActivity(intent);
        });
        return card;
    }

    private GradientDrawable background(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), stroke);
        return d;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
