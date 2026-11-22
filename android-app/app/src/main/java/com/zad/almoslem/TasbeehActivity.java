package com.zad.almoslem;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class TasbeehActivity extends Activity {
    private final String[] dhikr = {
            "سبحان الله", "الحمد لله", "الله أكبر", "لا إله إلا الله",
            "أستغفر الله وأتوب إليه", "اللهم صلِّ وسلم على نبينا محمد",
            "سبحان الله وبحمده، سبحان الله العظيم", "لا حول ولا قوة إلا بالله"
    };
    private final int[] goals = {33, 66, 99};
    private SharedPreferences prefs;
    private int count, goal, index;
    private TextView number, phrase, progress, target, completion;
    private ProgressBar bar;
    private Button addButton, undoButton;
    private Spinner dhikrPicker, goalPicker;
    private boolean firstDhikrSelection = true, firstGoalSelection = true;

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView section(String value) {
        TextView t = label(value, 14, AppThemeColors.text(this), true);
        t.setGravity(Gravity.RIGHT);
        t.setPadding(dp(3), dp(16), dp(3), dp(7));
        return t;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setBackground(bg(AppThemeColors.surface(this), 12));
        s.setPadding(dp(10), dp(4), dp(10), dp(4));
        return s;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("zad_reminders", MODE_PRIVATE);
        goal = prefs.getInt("native_tasbeeh_goal", 33);
        if (goal != 33 && goal != 66 && goal != 99) goal = 33;
        count = Math.max(0, prefs.getInt("native_tasbeeh_count", 0));
        count = Math.min(count, goal);
        index = Math.floorMod(prefs.getInt("native_tasbeeh_dhikr", 0), dhikr.length);
        AppThemeColors.apply(this);
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(24));
        root.setBackgroundColor(AppThemeColors.bg(this));
        scroll.addView(root);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(18), dp(20), dp(18), dp(20));
        GradientDrawable heroBg = bg(AppThemeColors.primary(this), 26);
        heroBg.setStroke(dp(1), Color.rgb(194, 163, 101));
        hero.setBackground(heroBg);

        TextView title = label("سُبْحَتِي", 25, Color.WHITE, true);
        hero.addView(title);
        TextView subtitle = label("لحظات ذكر… وطمأنينة للقلب", 13,
                Color.rgb(231, 215, 178), false);
        subtitle.setPadding(0, dp(5), 0, dp(13));
        hero.addView(subtitle);

        phrase = label("", 19, Color.WHITE, true);
        phrase.setPadding(dp(4), dp(5), dp(4), dp(13));
        hero.addView(phrase);

        FrameLayout ring = new FrameLayout(this);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.rgb(10, 39, 34));
        circle.setStroke(dp(3), Color.rgb(211, 177, 105));
        ring.setBackground(circle);
        number = label("", 43, Color.rgb(239, 211, 151), true);
        ring.addView(number, new FrameLayout.LayoutParams(dp(142), dp(142), Gravity.CENTER));
        LinearLayout ringHolder = new LinearLayout(this);
        ringHolder.setGravity(Gravity.CENTER);
        ringHolder.addView(ring, new LinearLayout.LayoutParams(dp(158), dp(158)));
        hero.addView(ringHolder);

        target = label("", 13, Color.rgb(235, 235, 221), false);
        target.setPadding(0, dp(13), 0, dp(3));
        hero.addView(target);
        progress = label("", 12, Color.rgb(220, 228, 218), false);
        hero.addView(progress);
        completion = label("", 13, Color.rgb(245, 221, 164), true);
        completion.setPadding(0, dp(7), 0, 0);
        hero.addView(completion);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(218, 185, 112)));
        }
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(6));
        bp.setMargins(dp(12), dp(12), dp(12), 0);
        hero.addView(bar, bp);
        root.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        root.addView(section("اختر الذكر"));
        dhikrPicker = spinner(dhikr);
        root.addView(dhikrPicker, new LinearLayout.LayoutParams(-1, dp(52)));
        dhikrPicker.setSelection(index);
        dhikrPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstDhikrSelection) { firstDhikrSelection = false; return; }
                if (index != position) {
                    index = position;
                    count = 0;
                    save();
                    render();
                }
            }
        });

        root.addView(section("اختر عدد التسبيحات"));
        goalPicker = spinner(new String[]{"٣٣ تسبيحة", "٦٦ تسبيحة", "٩٩ تسبيحة"});
        root.addView(goalPicker, new LinearLayout.LayoutParams(-1, dp(52)));
        goalPicker.setSelection(goal == 66 ? 1 : goal == 99 ? 2 : 0);
        goalPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstGoalSelection) { firstGoalSelection = false; return; }
                int selectedGoal = goals[position];
                if (goal != selectedGoal) {
                    goal = selectedGoal;
                    count = Math.min(count, goal);
                    save();
                    render();
                }
            }
        });

        addButton = new Button(this);
        addButton.setTextSize(17);
        addButton.setTextColor(Color.WHITE);
        addButton.setAllCaps(false);
        addButton.setBackground(bg(Color.rgb(20, 105, 76), 18));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(-1, dp(60));
        addLp.topMargin = dp(18);
        root.addView(addButton, addLp);
        addButton.setOnClickListener(v -> {
            if (count < goal) {
                count++;
                save();
                render();
                if (count == goal) {
                    Toast.makeText(this, "اكتمل وردك، تقبّل الله ذكرك", Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, dp(50));
        actionsLp.topMargin = dp(9);
        root.addView(actions, actionsLp);
        Button addHomeWidget = new Button(this);
        addHomeWidget.setText("إضافة السبحة إلى الشاشة الرئيسية");
        addHomeWidget.setAllCaps(false);
        LinearLayout.LayoutParams widgetLp = new LinearLayout.LayoutParams(-1, dp(48));
        widgetLp.topMargin = dp(10);
        root.addView(addHomeWidget, widgetLp);
        addHomeWidget.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                android.appwidget.AppWidgetManager manager = android.appwidget.AppWidgetManager.getInstance(this);
                android.content.ComponentName provider = new android.content.ComponentName(this, TasbeehWidget.class);
                if (manager.isRequestPinAppWidgetSupported()) {
                    manager.requestPinAppWidget(provider, null, null);
                    Toast.makeText(this, "اتبع رسالة الهاتف لإضافة السبحة إلى الشاشة الرئيسية.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "افتح قائمة الأدوات في الشاشة الرئيسية واختر «سبحة زاد وطمأنينة».", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "افتح قائمة الأدوات في الشاشة الرئيسية واختر «سبحة زاد وطمأنينة».", Toast.LENGTH_LONG).show();
            }
        });

        undoButton = new Button(this);
        undoButton.setText("تراجع");
        undoButton.setTextSize(14);
        undoButton.setAllCaps(false);
        undoButton.setTextColor(AppThemeColors.primary(this));
        undoButton.setBackground(bg(AppThemeColors.surface(this), 14));
        LinearLayout.LayoutParams undoLp = new LinearLayout.LayoutParams(0, -1, 1);
        undoLp.setMargins(0, 0, dp(5), 0);
        actions.addView(undoButton, undoLp);
        undoButton.setOnClickListener(v -> {
            if (count > 0) {
                count--;
                save();
                render();
            }
        });

        Button reset = new Button(this);
        reset.setText("تصفير العداد");
        reset.setTextSize(14);
        reset.setAllCaps(false);
        reset.setTextColor(AppThemeColors.primary(this));
        reset.setBackground(bg(AppThemeColors.surface(this), 14));
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, -1, 1);
        resetLp.setMargins(dp(5), 0, 0, 0);
        actions.addView(reset, resetLp);
        reset.setOnClickListener(v -> {
            count = 0;
            save();
            render();
        });

        TextView hint = label("كل ضغطة تُحسب تسبيحة واحدة. يُحفظ الذكر والعدد تلقائيًا حتى بعد إغلاق التطبيق.",
                12, AppThemeColors.muted(this), false);
        hint.setPadding(dp(8), dp(14), dp(8), 0);
        root.addView(hint);

        AdminTabContent.attach(this,root,"tasbeeh");
        setContentView(AppTabs.wrap(this, scroll, "tasbeeh"));
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null && number != null) {
            count = Math.max(0, prefs.getInt("native_tasbeeh_count", 0));
            goal = prefs.getInt("native_tasbeeh_goal", 33);
            index = Math.floorMod(prefs.getInt("native_tasbeeh_dhikr", 0), dhikr.length);
            render();
        }
    }

    private void save() {
        prefs.edit()
                .putInt("native_tasbeeh_goal", goal)
                .putInt("native_tasbeeh_count", count)
                .putInt("native_tasbeeh_dhikr", index)
                .apply();
        TasbeehWidget.refreshAll(this);
    }

    private void render() {
        phrase.setText(dhikr[index]);
        number.setText(String.valueOf(count));
        target.setText("الهدف: " + goal + " مرة");
        progress.setText("أنجزت " + count + " من " + goal);
        boolean complete = count >= goal;
        completion.setText(complete ? "اكتمل الورد • تقبّل الله" : " ");
        bar.setProgress((int) (100f * count / Math.max(1, goal)));
        addButton.setText(complete ? "اكتمل الورد" : "سَبِّح الآن  •  اضغط للعد");
        addButton.setEnabled(!complete);
        addButton.setAlpha(complete ? 0.72f : 1f);
        undoButton.setEnabled(count > 0);
        undoButton.setAlpha(count > 0 ? 1f : 0.55f);
    }
}
