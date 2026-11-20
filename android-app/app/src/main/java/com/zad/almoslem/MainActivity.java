package com.zad.almoslem;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREFS = "zad_reminders";
    private static final String[] PRAYERS = {"الفجر","الظهر","العصر","المغرب","العشاء"};
    private static final String[] KEYS = {"fajr","dhuhr","asr","maghrib","isha"};
    private static final String[] VOICES = {"doha","basit","mustafa","husary","banna","haram"};
    private static final String[] VOICE_LABELS = {"أذان الدوحة","عبد الباسط عبد الصمد","مصطفى إسماعيل","محمود خليل الحصري","محمود علي البنا","أذان الحرم المكي"};
    private final Map<String, Spinner> voiceSpinners = new LinkedHashMap<>();
    private final Map<String, SeekBar> volumeBars = new LinkedHashMap<>();
    private final Map<String, TextView> timeLabels = new LinkedHashMap<>();
    private TextView status;
    private ProgressBar progress;
    private TextView progressText;
    private Button downloadButton;
    private boolean downloading = false;
    private SharedPreferences prefs;
    private final TextView[] homePrayerTimeViews = new TextView[5];

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ReminderReceiver.ensureDefaultDailyReminders(this);
        getWindow().setStatusBarColor(Color.rgb(8,48,40));
        getWindow().setNavigationBarColor(Color.rgb(8,48,40));
        requestNotificationPermission();
        buildScreen();
        refreshStorageStatus();
        if (!hasAllSelectedAdhanFiles()) {
            downloadButton.postDelayed(() -> downloadSelectedVoices(), 1200);
        }
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.RIGHT);
        return v;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    private void pad(View v, int all) { v.setPadding(dp(all),dp(all),dp(all),dp(all)); }
    private void addHomePrayerSummary(LinearLayout root) {
        TextView title=text("مواقيت الصلاة",19,Color.rgb(20,55,43),true);
        title.setPadding(0,0,0,dp(7));
        root.addView(title);
        LinearLayout card=column();
        card.setBackground(shape(Color.WHITE,16));
        card.setPadding(dp(8),dp(10),dp(8),dp(10));
        LinearLayout times=row();
        String[] icons={"🌅","☀️","🌤️","🌇","🌙"};
        for(int i=0;i<PRAYERS.length;i++){
            LinearLayout cell=column();
            cell.setGravity(Gravity.CENTER);
            TextView icon=text(icons[i],17,Color.rgb(25,90,70),false);
            icon.setGravity(Gravity.CENTER);
            TextView name=text(PRAYERS[i],11,Color.rgb(70,85,75),true);
            name.setGravity(Gravity.CENTER);
            int h=prefs.getInt("native_time_hour_"+PRAYERS[i],-1);
            int minute=prefs.getInt("native_time_minute_"+PRAYERS[i],0);
            String time=h<0?"--:--":String.format(Locale.getDefault(),"%02d:%02d",h,minute);
            TextView value=text(time,13,Color.rgb(8,48,40),true);
            value.setGravity(Gravity.CENTER);
            homePrayerTimeViews[i]=value;
            cell.addView(icon);cell.addView(name);cell.addView(value);
            times.addView(cell,new LinearLayout.LayoutParams(0,-2,1f));
        }
        card.addView(times);
        TextView note=text("المواقيت المحفوظة على هاتفك",10,Color.rgb(115,125,115),false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0,dp(6),0,0);
        card.addView(note);
        LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(10);
        root.addView(card,lp);
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = column();
        root.setPadding(dp(12),dp(10),dp(12),dp(24));
        root.setBackgroundColor(Color.rgb(244,247,243));
        scroll.addView(root);
        addHomePrayerSummary(root);
        TextView heading = text("زاد المسلم",22,Color.WHITE,true);
        heading.setGravity(Gravity.CENTER);
        TextView ayah = text("﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",13,Color.rgb(232,211,158),true);
        ayah.setGravity(Gravity.CENTER);
        TextView ayahRef = text("سورة الرعد • الآية ٢٨",11,Color.rgb(222,239,229),false);
        ayahRef.setGravity(Gravity.CENTER);
        TextClock liveClock = new TextClock(this);
        liveClock.setFormat12Hour("hh:mm:ss a");
        liveClock.setFormat24Hour("HH:mm:ss");
        liveClock.setTextSize(22);
        liveClock.setTextColor(Color.WHITE);
        liveClock.setGravity(Gravity.CENTER);
        liveClock.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        liveClock.setPadding(0,dp(3),0,dp(2));
        LinearLayout hero = column(); hero.setGravity(Gravity.CENTER);
        hero.setBackground(shape(Color.rgb(8,48,40),18)); hero.setPadding(dp(12),dp(9),dp(12),dp(9));
        hero.addView(heading); hero.addView(liveClock); hero.addView(ayah); hero.addView(ayahRef);
        root.addView(hero, matchWrap());
        Button prayerDetails = new Button(this); prayerDetails.setText("عرض مواقيت الصلاة بالتفصيل");
        prayerDetails.setOnClickListener(v -> startActivity(new Intent(this, PrayerDashboardActivity.class)));
        root.addView(prayerDetails, matchWrap());
        Button libraryButton = new Button(this); libraryButton.setText("📚  مكتبة خطب الجمعة");
        libraryButton.setOnClickListener(v -> startActivity(new Intent(this, KhutbahLibraryActivity.class)));
        root.addView(libraryButton, matchWrap());
        Button qiblaButton = new Button(this); qiblaButton.setText("🧭  بوصلة القبلة");
        qiblaButton.setOnClickListener(v -> startActivity(new Intent(this, QiblaActivity.class)));
        root.addView(qiblaButton, matchWrap());
        Button mushafButton = new Button(this); mushafButton.setText("📖  المصحف الشريف");
        mushafButton.setOnClickListener(v -> startActivity(new Intent(this, MushafPageActivity.class)));
        root.addView(mushafButton, matchWrap());
        Button booksButton = new Button(this); booksButton.setText("📚  مكتبة الكتب الإسلامية");
        booksButton.setOnClickListener(v -> startActivity(new Intent(this, BookLibraryActivity.class)));
        root.addView(booksButton, matchWrap());
        Button calendarButton = new Button(this); calendarButton.setText("🗓️  التقويم الهجري والميلادي");
        calendarButton.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        root.addView(calendarButton, matchWrap());
        Button audioLibraryButton = new Button(this); audioLibraryButton.setText("🎧  الابتهالات والرقية الشرعية");
        audioLibraryButton.setOnClickListener(v -> startActivity(new Intent(this, AudioLibraryActivity.class)));
        root.addView(audioLibraryButton, matchWrap());
        Button kidsButton = new Button(this); kidsButton.setText("🧒  ركن الأطفال");
        kidsButton.setOnClickListener(v -> startActivity(new Intent(this, KidsCornerActivity.class)));
        root.addView(kidsButton, matchWrap());
        Button fridayButton = new Button(this); fridayButton.setText("🌙  ورد يوم الجمعة");
        fridayButton.setOnClickListener(v -> startActivity(new Intent(this, FridayWirdActivity.class)));
        root.addView(fridayButton, matchWrap());
        Button faithButton = new Button(this); faithButton.setText("📚  المكتبة الإيمانية والسنة والأدعية");
        faithButton.setOnClickListener(v -> startActivity(new Intent(this, FaithLibraryActivity.class)));
        root.addView(faithButton, matchWrap());
        Button dailyWirdButton = new Button(this); dailyWirdButton.setText("☀️  وردي اليومي");
        dailyWirdButton.setOnClickListener(v -> startActivity(new Intent(this, DailyWirdActivity.class)));
        root.addView(dailyWirdButton, matchWrap());
        Button remindersButton = new Button(this); remindersButton.setText("⏰  إعدادات التنبيهات اليومية");
        remindersButton.setOnClickListener(v -> startActivity(new Intent(this, DailyReminderSettingsActivity.class)));
        root.addView(remindersButton, matchWrap());
        TextView intro = text("إعدادات الأذان",20,Color.rgb(20,55,43),true);
        intro.setPadding(0,dp(20),0,dp(8)); root.addView(intro);
        Switch master = new Switch(this);
        master.setText("تفعيل الأذان عند مواقيت الصلاة");
        master.setTextColor(Color.rgb(30,55,45));
        master.setChecked(prefs.getBoolean("adhan_enabled",true));
        master.setOnCheckedChangeListener((b,checked)->prefs.edit().putBoolean("adhan_enabled",checked).apply());
        addCard(root,master);

        TextView silentInfo = text("يمكنك اختيار الصلوات التي تريد أن يتحول فيها الهاتف إلى الوضع الصامت وقت الأذان، ثم يعود إلى وضعه السابق تلقائيًا بعد 15 دقيقة. يتطلب ذلك منح إذن عدم الإزعاج من إعدادات Android.",13,Color.rgb(70,85,75),false);
        addCard(root,silentInfo);
        Button silentPermission = new Button(this);
        silentPermission.setText("إعداد إذن الوضع الصامت");
        silentPermission.setOnClickListener(v -> {
            android.app.NotificationManager nm = (android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 23 && nm != null && nm.isNotificationPolicyAccessGranted()) {
                Toast.makeText(this,"إذن الوضع الصامت مفعّل بالفعل",Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this,"تعذر فتح إعداد الإذن في الهاتف",Toast.LENGTH_LONG).show();
            }
        });
        addCard(root,silentPermission);

        addStandaloneTools(root);
        addNativeAzkar(root);

        for (int i=0;i<PRAYERS.length;i++) addPrayerCard(root,i);
        addPrayerTimesUpdater(root);

        TextView downloadTitle = text("حفظ التسجيلات على الهاتف",18,Color.rgb(20,55,43),true);
        downloadTitle.setPadding(0,dp(20),0,dp(8)); root.addView(downloadTitle);
        TextView note = text("بعد اكتمال التنزيل ستعمل التسجيلات من ذاكرة التطبيق دون الحاجة إلى الموقع أو الإنترنت.",13,Color.DKGRAY,false);
        addCard(root,note);
        downloadButton = new Button(this); downloadButton.setText("تنزيل الأصوات المختارة");
        downloadButton.setTextColor(Color.WHITE); downloadButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(17,91,68)));
        root.addView(downloadButton, matchWrap());
        downloadButton.setOnClickListener(v->downloadSelectedVoices());
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgress(0); root.addView(progress,matchWrap());
        progressText = text("لم يبدأ التنزيل",12,Color.DKGRAY,false); root.addView(progressText);
        status = text("",13,Color.rgb(30,70,50),false); status.setPadding(0,dp(12),0,0); root.addView(status);
        Button audioCheck = new Button(this); audioCheck.setText("فحص حالة صوت الأذان");
        root.addView(audioCheck, matchWrap());
        TextView audioDiagnostics = text("اضغط هنا بعد تجربة الأذان لمعرفة حالة الملف وسبب أي تعذر في التشغيل.",12,Color.rgb(70,85,75),false);
        root.addView(audioDiagnostics, matchWrap());
        audioCheck.setOnClickListener(v -> audioDiagnostics.setText(buildAudioDiagnostics()));
        setContentView(AppTabs.wrap(this,scroll,"more"));
    }

    private void addStandaloneTools(LinearLayout root) {
        TextView toolsTitle=text("خدمات التطبيق المستقلة",19,Color.rgb(20,55,43),true);
        toolsTitle.setPadding(0,dp(18),0,dp(8)); root.addView(toolsTitle);

        LinearLayout radioCard=column();
        radioCard.setBackground(shape(Color.WHITE,16)); radioCard.setPadding(dp(14),dp(12),dp(14),dp(12));
        TextView radioTitle=text("إذاعة القرآن الكريم",17,Color.rgb(20,55,43),true);
        TextView radioDesc=text("تشغيل البث الصوتي مباشرة من مشغل Android، دون فتح أي صفحة ويب.",13,Color.DKGRAY,false);
        radioCard.addView(radioTitle); radioCard.addView(radioDesc);
        Button radioButton=new Button(this);
        radioCard.addView(radioButton,matchWrap());
        Button cairoRadioButton=new Button(this);cairoRadioButton.setText("إذاعة القرآن الكريم من القاهرة");
        radioCard.addView(cairoRadioButton,matchWrap());
        cairoRadioButton.setOnClickListener(v->{
            String stream="https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8";
            Intent intent=new Intent(this,RadioPlaybackService.class);intent.setAction(RadioPlaybackService.ACTION_PLAY);
            intent.putExtra("url",stream);intent.putExtra("kind","radio");intent.putExtra("label","إذاعة القرآن الكريم من القاهرة");intent.putExtra("station","egypt");
            try{if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);}
            catch(Exception e){status.setText("تعذر تشغيل إذاعة القاهرة: "+e.getMessage());}
        });
        Button stopRadioButton=new Button(this);stopRadioButton.setText("إيقاف الإذاعة");
        radioCard.addView(stopRadioButton,matchWrap());
        stopRadioButton.setOnClickListener(v->{
            Intent stop=new Intent(this,RadioPlaybackService.class);stop.setAction(RadioPlaybackService.ACTION_STOP);
            try{startService(stop);}catch(Exception e){status.setText("تعذر إيقاف الإذاعة: "+e.getMessage());}
        });
        Runnable refreshRadio=()->{
            boolean playing=getSharedPreferences("zad_audio",MODE_PRIVATE).getBoolean("radio_playing",false)
                    && "radio".equals(getSharedPreferences("zad_audio",MODE_PRIVATE).getString("active_kind",""));
            radioButton.setText(playing?"إيقاف إذاعة القرآن":"تشغيل إذاعة القرآن");
        };
        refreshRadio.run();
        radioButton.setOnClickListener(v->{
            android.content.SharedPreferences audio=getSharedPreferences("zad_audio",MODE_PRIVATE);
            boolean playing=audio.getBoolean("radio_playing",false)&&"radio".equals(audio.getString("active_kind",""));
            Intent intent=new Intent(this,RadioPlaybackService.class);
            if(playing){intent.setAction(RadioPlaybackService.ACTION_STOP);audio.edit().putBoolean("radio_playing",false).putString("active_kind","").apply();}
            else{
                String stream="https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8";
                intent.setAction(RadioPlaybackService.ACTION_PLAY);
                intent.putExtra("url",stream);intent.putExtra("kind","radio");intent.putExtra("label","إذاعة القرآن الكريم من القاهرة");intent.putExtra("station","egypt");
                audio.edit().putString("last_quran_url",stream).putString("last_quran_title","إذاعة القرآن الكريم").putBoolean("radio_playing",true).putString("active_kind","radio").apply();
            }
            try{if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);radioButton.postDelayed(refreshRadio,500);}
            catch(Exception e){status.setText("تعذر تشغيل الإذاعة: "+e.getMessage());}
        });
        addCard(root,radioCard);
        addNativeQuranPlayer(root);

        LinearLayout tasbeeh=column();
        tasbeeh.setBackground(shape(Color.rgb(17,65,51),20));
        tasbeeh.setPadding(dp(12),dp(12),dp(12),dp(12));
        TextView tasTitle=text("السبحة الإلكترونية",18,Color.WHITE,true);
        tasTitle.setGravity(Gravity.CENTER);
        TextView tasDesc=text("وَاذْكُرُوا اللَّهَ كَثِيرًا",14,Color.rgb(224,199,133),true);
        tasDesc.setGravity(Gravity.CENTER);
        String[] tasbeehDhikr={"سبحان الله","الحمد لله","الله أكبر","لا إله إلا الله","أستغفر الله وأتوب إليه","اللهم صلِّ وسلم على نبينا محمد"};
        int[] tasbeehGoals={33,66,99};
        Spinner tasbeehGoal=new Spinner(this);
        tasbeehGoal.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"٣٣ مرة","٦٦ مرة","٩٩ مرة"}));
        int savedGoal=prefs.getInt("native_tasbeeh_goal",33);
        int goalIndex=savedGoal==66?1:(savedGoal==99?2:0);
        tasbeehGoal.setSelection(goalIndex);
        TextView currentDhikr=text("",17,Color.WHITE,true);currentDhikr.setGravity(Gravity.CENTER);
        currentDhikr.setPadding(dp(4),dp(20),dp(4),dp(10));
        TextView count=text("0",34,Color.rgb(232,203,137),true);count.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable countRing=new android.graphics.drawable.GradientDrawable();
        countRing.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        countRing.setColor(Color.rgb(10,49,40));countRing.setStroke(dp(3),Color.rgb(218,185,112));
        android.widget.FrameLayout countCircle=new android.widget.FrameLayout(this);
        countCircle.setBackground(countRing);
        countCircle.addView(count,new android.widget.FrameLayout.LayoutParams(dp(104),dp(104),Gravity.CENTER));
        LinearLayout circleHolder=row();circleHolder.setGravity(Gravity.CENTER);circleHolder.addView(countCircle,new LinearLayout.LayoutParams(dp(112),dp(112)));
        TextView target=text("",13,Color.rgb(225,225,215),false);target.setGravity(Gravity.CENTER);
        TextView tasProgress=text("",13,Color.rgb(225,225,215),false);tasProgress.setGravity(Gravity.CENTER);
        int[] dhikrIndex={prefs.getInt("native_tasbeeh_dhikr",0)};
        int[] currentCount={prefs.getInt("native_tasbeeh_count",0)};
        int[] goal={savedGoal};
        Runnable drawTasbeeh=()->{
            dhikrIndex[0]=Math.floorMod(dhikrIndex[0],tasbeehDhikr.length);
            currentCount[0]=Math.max(0,Math.min(currentCount[0],goal[0]));
            currentDhikr.setText(tasbeehDhikr[dhikrIndex[0]]);
            count.setText(String.valueOf(currentCount[0]));
            target.setText("الهدف: "+goal[0]+" مرة");
            tasProgress.setText("الذكر "+(dhikrIndex[0]+1)+" من "+tasbeehDhikr.length+"  •  "+currentCount[0]+" / "+goal[0]);
        };
        LinearLayout actions=row();
        Button add=new Button(this);add.setText("سَبِّح");add.setTextSize(17);
        Button reset=new Button(this);reset.setText("إعادة");
        actions.addView(add,new LinearLayout.LayoutParams(0,dp(54),2));
        actions.addView(reset,new LinearLayout.LayoutParams(0,dp(54),1));
        tasbeehGoal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                int selectedGoal=tasbeehGoals[position];
                if(goal[0]!=selectedGoal){
                    goal[0]=selectedGoal;
                    currentCount[0]=0;
                    prefs.edit().putInt("native_tasbeeh_goal",goal[0]).putInt("native_tasbeeh_count",0).apply();
                }
                drawTasbeeh.run();
            }
        });
        add.setOnClickListener(v->{
            if(currentCount[0]<goal[0]) currentCount[0]++;
            if(currentCount[0]>=goal[0]){
                prefs.edit().putInt("native_tasbeeh_count",0)
                    .putInt("native_tasbeeh_dhikr",(dhikrIndex[0]+1)%tasbeehDhikr.length).apply();
                dhikrIndex[0]=(dhikrIndex[0]+1)%tasbeehDhikr.length;
                currentCount[0]=0;
                drawTasbeeh.run();
                android.widget.Toast.makeText(this,"أحسنت، اكتمل الذكر. انتقلنا إلى الذكر التالي",android.widget.Toast.LENGTH_SHORT).show();
            }else{
                prefs.edit().putInt("native_tasbeeh_count",currentCount[0])
                    .putInt("native_tasbeeh_dhikr",dhikrIndex[0]).apply();
                drawTasbeeh.run();
            }
        });
        reset.setOnClickListener(v->{
            currentCount[0]=0;
            prefs.edit().putInt("native_tasbeeh_count",0).apply();
            drawTasbeeh.run();
        });
        tasbeeh.addView(tasTitle);tasbeeh.addView(tasDesc);
        tasbeeh.addView(text("اختر عدد التسبيحات",13,Color.WHITE,true));
        tasbeeh.addView(tasbeehGoal,matchWrap());
        tasbeeh.addView(currentDhikr);tasbeeh.addView(circleHolder);
        tasbeeh.addView(target);tasbeeh.addView(tasProgress);
        tasbeeh.addView(actions);
        drawTasbeeh.run();
        addCard(root,tasbeeh);

        LinearLayout dhikr=column();
        dhikr.setBackground(shape(Color.WHITE,16));dhikr.setPadding(dp(14),dp(12),dp(14),dp(12));
        dhikr.addView(text("ذكر اليوم",17,Color.rgb(20,55,43),true));
        dhikr.addView(text("قال رسول الله ﷺ: «من قال سبحان الله وبحمده في يوم مائة مرة، حُطَّت خطاياه وإن كانت مثل زبد البحر».",15,Color.rgb(55,75,65),false));
        dhikr.addView(text("رواه البخاري ومسلم.",12,Color.rgb(120,120,120),false));
        addCard(root,dhikr);
    }

    private void addNativeQuranPlayer(LinearLayout root) {
        TextView section=text("استماع القرآن الكريم",18,Color.rgb(20,55,43),true);
        section.setPadding(0,dp(18),0,dp(8));root.addView(section);
        LinearLayout card=column();card.setBackground(shape(Color.WHITE,16));card.setPadding(dp(14),dp(12),dp(14),dp(12));
        card.addView(text("اختر القارئ والسورة، وسيعمل الصوت في مشغل Android بالخلفية دون فتح الموقع.",13,Color.DKGRAY,false));
        Spinner reciterSpinner=new Spinner(this),surahSpinner=new Spinner(this);
        reciterSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"جارٍ تحميل القراء..."}));
        String[] surahs={"الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"};
        surahSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,surahs));
        card.addView(text("القارئ",12,Color.DKGRAY,true));card.addView(reciterSpinner,matchWrap());
        card.addView(text("السورة",12,Color.DKGRAY,true));card.addView(surahSpinner,matchWrap());
        Button play=new Button(this);play.setText("استمع الآن");card.addView(play,matchWrap());
        Button download=new Button(this);download.setText("تنزيل السورة للاستماع دون إنترنت");card.addView(download,matchWrap());
        ProgressBar quranProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);quranProgress.setMax(100);card.addView(quranProgress,matchWrap());
        TextView state=text("جارٍ تحميل قائمة القراء من مكتبة التسجيلات.",12,Color.rgb(80,100,90),false);card.addView(state);
        addCard(root,card);
        final ArrayList<org.json.JSONObject> reciters=new ArrayList<>();
        final ArrayList<String> reciterNames=new ArrayList<>();
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL("https://mp3quran.net/api/v3/reciters?language=ar").openConnection();
                connection.setConnectTimeout(15000);connection.setReadTimeout(20000);
                if(connection.getResponseCode()!=200)throw new Exception("تعذر تحميل قائمة القراء");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=connection.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
                org.json.JSONArray array=new org.json.JSONObject(out.toString("UTF-8")).getJSONArray("reciters");
                for(int i=0;i<array.length();i++){org.json.JSONObject item=array.getJSONObject(i);if(item.has("moshaf")&&item.getJSONArray("moshaf").length()>0){reciters.add(item);reciterNames.add(item.optString("name","قارئ"));}}
                runOnUiThread(()->{
                    if(reciterNames.isEmpty()){state.setText("لا توجد قائمة قراء متاحة حاليًا.");return;}
                    reciterSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,reciterNames));
                    state.setText("تم تحميل "+reciterNames.size()+" قارئًا. اختر القارئ والسورة.");
                });
            }catch(Exception e){runOnUiThread(()->state.setText("تعذر تحميل القراء. تحقق من الإنترنت ثم أعد فتح التطبيق."));}
            finally{if(connection!=null)connection.disconnect();}
        }).start();
        play.setOnClickListener(v->{
            int ri=reciterSpinner.getSelectedItemPosition(),si=surahSpinner.getSelectedItemPosition()+1;
            if(ri<0||ri>=reciters.size()){state.setText("انتظر حتى تكتمل قائمة القراء.");return;}
            try{
                org.json.JSONObject reciter=reciters.get(ri);
                String audioUrl=resolveQuranAudioUrl(reciter,si);
                if(audioUrl==null){state.setText("السورة غير متاحة لهذا القارئ. اختر قارئًا آخر.");return;}
                String label="سورة "+surahs[si-1]+" — "+reciter.optString("name","القارئ");
                getSharedPreferences("zad_audio",MODE_PRIVATE).edit().putString("last_quran_url",audioUrl).putString("last_quran_title",label).putBoolean("radio_playing",true).putString("active_kind","quran").apply();
                Intent intent=new Intent(this,RadioPlaybackService.class);intent.setAction(RadioPlaybackService.ACTION_PLAY);
                intent.putExtra("url",audioUrl);intent.putExtra("kind","quran");intent.putExtra("label",label);
                File offline=new File(new File(getFilesDir(),"quran"),"reciter_"+ri+"_surah_"+si+".mp3");
                if(offline.isFile()&&offline.length()>10000)intent.putExtra("local_path",offline.getAbsolutePath());
                if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);
                state.setText("يتم تشغيل "+label+" في الخلفية.");
            }catch(Exception e){state.setText("تعذر بدء التلاوة: "+e.getMessage());}
        });
        download.setOnClickListener(v->{
            int ri=reciterSpinner.getSelectedItemPosition(),si=surahSpinner.getSelectedItemPosition()+1;
            if(ri<0||ri>=reciters.size()){state.setText("انتظر حتى تكتمل قائمة القراء.");return;}
            try{
                String url=resolveQuranAudioUrl(reciters.get(ri),si);
                if(url==null){state.setText("السورة غير متاحة لهذا القارئ.");return;}
                String label="سورة "+surahs[si-1]+" — "+reciterNames.get(ri);
                download.setEnabled(false);quranProgress.setProgress(0);state.setText("جارٍ تنزيل "+label+"...");
                new Thread(()->{
                    HttpURLConnection connection=null;File temp=null;
                    try{
                        File dir=new File(getFilesDir(),"quran");if(!dir.exists()&&!dir.mkdirs())throw new Exception("تعذر إنشاء مجلد القرآن");
                        File target=new File(dir,"reciter_"+ri+"_surah_"+si+".mp3");temp=new File(dir,target.getName()+".part");
                        connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(20000);connection.setReadTimeout(90000);connection.setInstanceFollowRedirects(true);
                        if(connection.getResponseCode()!=200)throw new Exception("مصدر التلاوة غير متاح");
                        long total=connection.getContentLength(),got=0;
                        try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                            byte[] buffer=new byte[16384];int n;
                            while((n=in.read(buffer))!=-1){got+=n;if(got>100L*1024*1024)throw new Exception("حجم الملف تجاوز الحد المسموح");out.write(buffer,0,n);
                                final long current=got,expected=total;
                                runOnUiThread(()->quranProgress.setProgress(expected>0?(int)Math.min(99,current*100/expected):0));
                            }out.getFD().sync();
                        }
                        if(temp.length()<10000)throw new Exception("ملف التلاوة غير مكتمل");
                        try(FileInputStream in=new FileInputStream(temp)){byte[] h=new byte[3];int n=in.read(h);if(!(n>=3&&h[0]=='I'&&h[1]=='D'&&h[2]=='3')&&!(n>=2&&(h[0]&255)==255&&(h[1]&224)==224))throw new Exception("الملف الذي وصل ليس MP3 صالحًا");}
                        if(target.exists()&&!target.delete())throw new Exception("تعذر استبدال الملف السابق");
                        if(!temp.renameTo(target))throw new Exception("تعذر حفظ الملف داخل التطبيق");
                        runOnUiThread(()->{quranProgress.setProgress(100);state.setText("تم تنزيل "+label+" وحفظه داخل التطبيق. سيعمل دون إنترنت.");});
                    }catch(Exception e){runOnUiThread(()->state.setText("فشل التنزيل: "+(e.getMessage()==null?"خطأ غير معروف":e.getMessage())));}
                    finally{if(connection!=null)connection.disconnect();if(temp!=null&&temp.exists())temp.delete();runOnUiThread(()->download.setEnabled(true));}
                }).start();
            }catch(Exception e){state.setText("تعذر تجهيز التنزيل: "+e.getMessage());}
        });
    }

    private String resolveQuranAudioUrl(org.json.JSONObject reciter,int surahId) throws Exception {
        org.json.JSONArray moshafs=reciter.getJSONArray("moshaf");
        for(int i=0;i<moshafs.length();i++){
            org.json.JSONObject m=moshafs.getJSONObject(i);String list=m.optString("surah_list","");
            boolean includes=false;
            for(String id:list.split(",")){try{if(Integer.parseInt(id.trim())==surahId){includes=true;break;}}catch(Exception ignored){}}
            String server=m.optString("server","");
            if(includes&&!server.isEmpty()){if(!server.endsWith("/"))server+="/";return server+String.format(Locale.US,"%03d.mp3",surahId);}
        }
        return null;
    }

    private void addNativeAzkar(LinearLayout root) {
        TextView title=text("الأذكار اليومية",18,Color.rgb(20,55,43),true);
        title.setPadding(0,dp(18),0,dp(8));root.addView(title);
        LinearLayout card=column();card.setBackground(shape(Color.WHITE,16));card.setPadding(dp(14),dp(12),dp(14),dp(12));
        String[] categories={"أذكار الصباح","أذكار المساء","أذكار بعد الصلاة"};
        String[][][] items={
          {
            {"أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.", "1"},
            {"اللهم بك أصبحنا وبك أمسينا وبك نحيا وبك نموت وإليك النشور.", "1"},
            {"رضيت بالله ربًا، وبالإسلام دينًا، وبمحمد ﷺ نبيًا.", "3"},
            {"سبحان الله وبحمده.", "100"},
            {"أستغفر الله وأتوب إليه.", "100"}
          },
          {
            {"أمسينا وأمسى الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.", "1"},
            {"اللهم بك أمسينا وبك أصبحنا وبك نحيا وبك نموت وإليك المصير.", "1"},
            {"رضيت بالله ربًا، وبالإسلام دينًا، وبمحمد ﷺ نبيًا.", "3"},
            {"سبحان الله وبحمده.", "100"},
            {"أستغفر الله وأتوب إليه.", "100"}
          },
          {
            {"أستغفر الله.", "3"},
            {"اللهم أنت السلام ومنك السلام، تباركت يا ذا الجلال والإكرام.", "1"},
            {"سبحان الله.", "33"},
            {"الحمد لله.", "33"},
            {"الله أكبر.", "33"},
            {"لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.", "1"}
          }
        };
        Spinner category=new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,categories));
        card.addView(category,matchWrap());
        TextView dhikr=text("",19,Color.rgb(35,70,55),true);dhikr.setGravity(Gravity.CENTER);dhikr.setPadding(dp(4),dp(18),dp(4),dp(18));card.addView(dhikr);
        TextView progressText=text("",14,Color.rgb(100,110,100),false);progressText.setGravity(Gravity.CENTER);card.addView(progressText);
        Button countButton=new Button(this);countButton.setText("تسبيح");card.addView(countButton,matchWrap());
        Button resetButton=new Button(this);resetButton.setText("إعادة الذكر الحالي");card.addView(resetButton,matchWrap());
        final int[] current={0},count={0},active={0};
        Runnable render=()->{
            int cat=active[0];current[0]=Math.max(0,Math.min(current[0],items[cat].length-1));
            int goal=Integer.parseInt(items[cat][current[0]][1]);
            count[0]=prefs.getInt("native_azkar_"+cat+"_"+current[0],0);
            dhikr.setText(items[cat][current[0]][0]);
            progressText.setText("الذكر "+(current[0]+1)+" من "+items[cat].length+" — "+Math.min(count[0],goal)+" / "+goal);
            countButton.setText(count[0]>=goal?"اكتمل الذكر — التالي":"اضغط للتكرار");
        };
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){active[0]=position;current[0]=0;render.run();}
        });
        countButton.setOnClickListener(v->{
            int cat=active[0],goal=Integer.parseInt(items[cat][current[0]][1]);
            if(count[0]>=goal){current[0]=(current[0]+1)%items[cat].length;render.run();return;}
            count[0]++;prefs.edit().putInt("native_azkar_"+cat+"_"+current[0],count[0]).apply();
            if(count[0]>=goal){progressText.setText("أحسنت، اكتمل الذكر. اضغط للانتقال إلى الذكر التالي.");countButton.setText("التالي");}
            else progressText.setText("الذكر "+(current[0]+1)+" من "+items[cat].length+" — "+count[0]+" / "+goal);
        });
        resetButton.setOnClickListener(v->{prefs.edit().putInt("native_azkar_"+active[0]+"_"+current[0],0).apply();count[0]=0;render.run();});
        addCard(root,card);
    }

    private void addPrayerTimesUpdater(LinearLayout root) {
        TextView title=text("مواقيت الصلاة حسب مدينتك",18,Color.rgb(20,55,43),true);
        title.setPadding(0,dp(18),0,dp(8));root.addView(title);
        LinearLayout card=column();card.setBackground(shape(Color.WHITE,16));card.setPadding(dp(14),dp(12),dp(14),dp(12));
        TextView hint=text("اختر المدينة لتحديث المواقيت وحفظ تنبيهات الصلوات على الهاتف. آخر مواقيت محفوظة تظل متاحة دون إنترنت.",13,Color.DKGRAY,false);
        card.addView(hint);
        String[] cities={"القاهرة","الجيزة","الزقازيق","الإسكندرية","المنصورة","طنطا","بنها","الإسماعيلية","بورسعيد","السويس","أسيوط","الأقصر","أسوان"};
        String[] english={"Cairo","Giza","Zagazig","Alexandria","Mansoura","Tanta","Banha","Ismailia","Port Said","Suez","Asyut","Luxor","Aswan"};
        Spinner city=new Spinner(this);
        city.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cities));
        city.setSelection(Math.max(0,Arrays.asList(english).indexOf(prefs.getString("native_prayer_city","Cairo"))));
        card.addView(city,matchWrap());
        Button update=new Button(this);update.setText("تحديث مواقيت الصلاة");card.addView(update,matchWrap());
        TextView result=text("لم يتم تحديث المواقيت بعد.",12,Color.rgb(80,100,90),false);card.addView(result);
        update.setOnClickListener(v->{
            int selected=city.getSelectedItemPosition();String cityEn=english[selected],cityAr=cities[selected];
            prefs.edit().putString("native_prayer_city",cityEn).apply();
            update.setEnabled(false);result.setText("جارٍ جلب المواقيت...");
            new Thread(()->{
                HttpURLConnection connection=null;
                try{
                    String url="https://api.aladhan.com/v1/timingsByCity?city="+java.net.URLEncoder.encode(cityEn,"UTF-8")+"&country=Egypt&method=5";
                    connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(20000);
                    connection.setRequestMethod("GET");
                    if(connection.getResponseCode()!=200)throw new Exception("تعذر الاتصال بخدمة المواقيت");
                    ByteArrayOutputStream output=new ByteArrayOutputStream();
                    try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)output.write(buffer,0,n);}
                    org.json.JSONObject rootJson=new org.json.JSONObject(output.toString("UTF-8"));
                    org.json.JSONObject timings=rootJson.getJSONObject("data").getJSONObject("timings");
                    String[] apiKeys={"Fajr","Dhuhr","Asr","Maghrib","Isha"};
                    long now=System.currentTimeMillis();
                    for(int i=0;i<PRAYERS.length;i++){
                        String raw=timings.getString(apiKeys[i]).split(" ")[0];
                        String[] hm=raw.split(":");int hour=Integer.parseInt(hm[0]),minute=Integer.parseInt(hm[1]);
                        prefs.edit().putInt("native_time_hour_"+PRAYERS[i],hour).putInt("native_time_minute_"+PRAYERS[i],minute).apply();
                        Calendar next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,hour);next.set(Calendar.MINUTE,minute);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
                        if(next.getTimeInMillis()<=now)next.add(Calendar.DAY_OF_YEAR,1);
                        if(prefs.getBoolean("prayer_enabled_"+PRAYERS[i],true))ReminderReceiver.schedulePrayer(this,PRAYERS[i],next.getTimeInMillis());
                        final String label=String.format(Locale.getDefault(),"%02d:%02d",hour,minute);
                        final String prayer=PRAYERS[i];
                        runOnUiThread(()->{TextView tv=timeLabels.get(prayer);if(tv!=null)tv.setText(label);});
                    }
                    runOnUiThread(()->result.setText("تم تحديث مواقيت "+cityAr+" وحفظ التنبيهات بنجاح."));
                }catch(Exception e){runOnUiThread(()->result.setText("تعذر تحديث المواقيت: "+(e.getMessage()==null?"تحقق من الإنترنت":e.getMessage())));}
                finally{if(connection!=null)connection.disconnect();runOnUiThread(()->update.setEnabled(true));}
            }).start();
        });
        addCard(root,card);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
    }
    private void addCard(LinearLayout root, View content) {
        LinearLayout card=column(); card.setBackground(shape(Color.WHITE,16)); card.setPadding(dp(14),dp(12),dp(14),dp(12));
        card.addView(content,matchWrap());
        LinearLayout.LayoutParams lp=matchWrap(); lp.bottomMargin=dp(10); root.addView(card,lp);
    }
    private void addPrayerCard(LinearLayout root,int index) {
        String prayer=PRAYERS[index], key=KEYS[index];
        LinearLayout card=column(); card.setBackground(shape(Color.WHITE,16)); card.setPadding(dp(14),dp(12),dp(14),dp(12));
        LinearLayout.LayoutParams outer=matchWrap(); outer.bottomMargin=dp(12); root.addView(card,outer);
        LinearLayout top=row();
        TextView name=text(prayer,18,Color.rgb(20,55,43),true);
        top.addView(name,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView time=text(savedTimeLabel(prayer),14,Color.rgb(80,100,90),false);
        timeLabels.put(prayer,time); top.addView(time);
        // أوقات الصلاة تُحدّث آليًا من طريقة الحساب المختارة، ولا يوجد تعديل يدوي للوقت.
        card.addView(top);
        TextView voiceLabel=text("صوت المؤذن",13,Color.DKGRAY,true); voiceLabel.setPadding(0,dp(8),0,0); card.addView(voiceLabel);
        Spinner spinner=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,VOICE_LABELS);
        spinner.setAdapter(adapter);
        String saved=prefs.getString("adhan_voice_"+prayer,"doha");
        spinner.setSelection(Math.max(0,Arrays.asList(VOICES).indexOf(saved)));
        card.addView(spinner,matchWrap()); voiceSpinners.put(prayer,spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int position,long id){
                prefs.edit().putString("adhan_voice_"+prayer,VOICES[position]).apply();
            }
        });
        TextView volLabel=text("مستوى الصوت: "+prefs.getInt("adhan_volume_"+key,100)+"%",12,Color.DKGRAY,false);
        card.addView(volLabel);
        SeekBar seek=new SeekBar(this); seek.setMax(100); seek.setProgress(prefs.getInt("adhan_volume_"+key,100));
        card.addView(seek,matchWrap()); volumeBars.put(prayer,seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int value,boolean user){volLabel.setText("مستوى الصوت: "+value+"%"); if(user)prefs.edit().putInt("adhan_volume_"+key,value).apply();}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        LinearLayout actions=row();
        Switch enabled=new Switch(this); enabled.setText("تفعيل الصلاة"); enabled.setTextSize(12);
        enabled.setChecked(prefs.getBoolean("prayer_enabled_"+prayer,true));
        enabled.setOnCheckedChangeListener((b,checked)->prefs.edit().putBoolean("prayer_enabled_"+prayer,checked).apply());
        actions.addView(enabled,new LinearLayout.LayoutParams(0,dp(48),1));
        Button test=new Button(this); test.setText("تجربة الأذان"); test.setTextSize(11); actions.addView(test);
        test.setOnClickListener(v->playPrayer(prayer));
        card.addView(actions);

        Switch silent = new Switch(this);
        silent.setText("اجعل الهاتف صامتًا عند أذان " + prayer);
        silent.setTextSize(12);
        silent.setTextColor(Color.rgb(35,65,50));
        silent.setChecked(prefs.getBoolean("prayer_silent_" + prayer,false));
        silent.setOnCheckedChangeListener((button,checked) -> {
            prefs.edit().putBoolean("prayer_silent_" + prayer,checked).apply();
            if (checked) {
                android.app.NotificationManager nm = (android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= 23 && (nm == null || !nm.isNotificationPolicyAccessGranted())) {
                    Toast.makeText(this,"فعّل إذن عدم الإزعاج من زر إعداد إذن الوضع الصامت أعلى الصفحة",Toast.LENGTH_LONG).show();
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                    } catch (Exception ignored) {}
                }
            }
        });
        card.addView(silent);
    }

    private String savedTimeLabel(String prayer) {
        int h=prefs.getInt("native_time_hour_"+prayer,-1), m=prefs.getInt("native_time_minute_"+prayer,0);
        return h<0 ? "لم يحدد" : String.format(Locale.getDefault(),"%02d:%02d",h,m);
    }
    private void choosePrayerTime(String prayer,TextView label) {
        int h=prefs.getInt("native_time_hour_"+prayer,Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        int m=prefs.getInt("native_time_minute_"+prayer,0);
        new TimePickerDialog(this,(view,hour,minute)->{
            prefs.edit().putInt("native_time_hour_"+prayer,hour).putInt("native_time_minute_"+prayer,minute).apply();
            label.setText(String.format(Locale.getDefault(),"%02d:%02d",hour,minute));
            Calendar next=Calendar.getInstance(); next.set(Calendar.HOUR_OF_DAY,hour); next.set(Calendar.MINUTE,minute); next.set(Calendar.SECOND,0); next.set(Calendar.MILLISECOND,0);
            if(next.getTimeInMillis()<=System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR,1);
            ReminderReceiver.schedulePrayer(this,prayer,next.getTimeInMillis());
            status.setText("تم حفظ تنبيه "+prayer+" على "+label.getText());
        },h,m,true).show();
    }

    private void playPrayer(String prayer) {
        String voice=prefs.getString("adhan_voice_"+prayer,"doha");
        Intent intent=new Intent(this,AdhanPlaybackService.class);
        intent.setAction(AdhanPlaybackService.ACTION_PLAY);
        intent.putExtra("prayer",prayer); intent.putExtra("voice",voice);
        intent.putExtra("play_dua_after",false);
        try {
            if(Build.VERSION.SDK_INT>=26) startForegroundService(intent); else startService(intent);
        } catch(Exception e) { status.setText("تعذر بدء التشغيل: "+e.getMessage()); }
    }

    private boolean hasAllSelectedAdhanFiles() {
        File dir = new File(getFilesDir(), "adhan");
        for (int i = 0; i < PRAYERS.length; i++) {
            String voice = prefs.getString("adhan_voice_" + PRAYERS[i], "doha");
            String fileVoice = "doha".equals(voice) ? "doha_" + KEYS[i] : voice;
            File mp3 = new File(dir, fileVoice + ".mp3");
            File ogg = new File(dir, fileVoice + ".ogg");
            if (!isAudioFileValid(mp3) && !isAudioFileValid(ogg)) return false;
        }
        return true;
    }

    private void downloadSelectedVoices() {
        if(downloading)return;
        LinkedHashSet<String> selected=new LinkedHashSet<>();
        for(String prayer:PRAYERS) selected.add(prefs.getString("adhan_voice_"+prayer,"doha"));
        downloading=true; downloadButton.setEnabled(false); progress.setProgress(0);
        progressText.setText("جارٍ تجهيز التنزيل...");
        new Thread(()->{
            List<String> failed=new ArrayList<>(); int done=0;
            for(String voice:selected) {
                try {
                    if("doha".equals(voice)) downloadDoha();
                    else downloadVoice(voice);
                } catch(Exception e) { failed.add(voice+": "+(e.getMessage()==null?"فشل التنزيل":e.getMessage())); }
                done++;
                final int p=(int)(done*100f/selected.size());
                final int finished=done;
                runOnUiThread(()->{progress.setProgress(p);progressText.setText("اكتمل تنزيل "+finished+" من "+selected.size()+" ("+p+"%)");});
            }
            final boolean ok=failed.isEmpty();
            runOnUiThread(()->{
                downloading=false; downloadButton.setEnabled(true); refreshStorageStatus();
                status.setText(ok?"اكتمل حفظ كل الأصوات المختارة داخل التطبيق.": "لم يكتمل تنزيل بعض الأصوات: "+android.text.TextUtils.join("، ",failed));
            });
        }).start();
    }

    private void downloadDoha() throws Exception {
        String[] keys={"fajr","dhuhr","asr","maghrib","isha"};
        String[] urls={
          "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_01_Fajr_Adhan.mp3",
          "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_02_Dhuhr_Adhan.mp3",
          "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_03_Asr_Adhan.mp3",
          "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_04_Maghrib_Adhan.mp3",
          "https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_05_Isha_Adhan.mp3"
        };
        for(int i=0;i<keys.length;i++) downloadFile(urls[i],"doha_"+keys[i]+".mp3");
    }
    private void downloadVoice(String voice) throws Exception {
        String file;
        switch(voice) {
            case "basit": file="Abdulbasit_Abdusamad_1_-_Egypt_(عبد_الباسط_عبد_الصمد_-_مصر).mp3"; break;
            case "mustafa": file="Mustafa_Ismail_-_Egypt_(مصطفى_إسماعيل_-_مصر).mp3"; break;
            case "husary": file="Mahmoud_Khalil_Al_Hussary_-_Cairo_(محمود_خليل_الحصري_-_القاهرة).mp3"; break;
            case "banna": file="Mahmoud_Ali_Al_Banna_-_Cairo_(محمود_علي_البنا_-_القاهرة).mp3"; break;
            case "haram": file="Ali_Ibn_Ahmad_Mala_1_-_Al_Haram_Al_Maki_(علي_بن_أحمد_ملا_-_الحرم_المكي).mp3"; break;
            default: throw new Exception("هذا الصوت غير متاح");
        }
        String encoded=java.net.URLEncoder.encode(file,"UTF-8").replace("+","%20").replace("%28","(").replace("%29",")");
        String url="https://cdn.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/"+encoded;
        downloadFile(url,voice+".mp3");
    }
    private void downloadFile(String address,String filename) throws Exception {
        File dir=new File(getFilesDir(),"adhan"); if(!dir.exists()&&!dir.mkdirs())throw new Exception("تعذر إنشاء مجلد الصوت");
        File temp=new File(dir,filename+".part"), target=new File(dir,filename);
        HttpURLConnection c=null;
        try {
            c=(HttpURLConnection)new URL(address).openConnection(); c.setConnectTimeout(25000); c.setReadTimeout(90000);
            c.setInstanceFollowRedirects(true); c.setRequestProperty("User-Agent","ZadAlmoslem Android Native");
            int code=c.getResponseCode(); if(code!=200)throw new Exception("مصدر الصوت أعاد "+code);
            long expected=c.getContentLength(),received=0;
            try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){received+=n;if(received>20L*1024*1024)throw new Exception("حجم الملف أكبر من المسموح");out.write(buffer,0,n);
                    final long got=received,total=expected;
                    runOnUiThread(()->{if(total>0){int pct=(int)Math.min(99,got*100/total);progress.setProgress(pct);progressText.setText("تنزيل "+filename+" — "+pct+"%");}});
                } out.getFD().sync();
            }
            if(received<10000||!validAudio(temp))throw new Exception("الملف الصوتي غير مكتمل أو غير صالح");
            if(target.exists()&&!target.delete())throw new Exception("تعذر استبدال الملف القديم");
            if(!temp.renameTo(target))throw new Exception("تعذر حفظ الملف");
        } finally {if(c!=null)c.disconnect();if(temp.exists())temp.delete();}
    }
    private boolean validAudio(File f) {
        if(!f.isFile()||f.length()<10000)return false;
        try(FileInputStream in=new FileInputStream(f)){byte[] h=new byte[4];int n=in.read(h);
            return (n>=3&&h[0]=='I'&&h[1]=='D'&&h[2]=='3')||(n>=2&&(h[0]&255)==255&&(h[1]&224)==224)||(n>=4&&h[0]=='O'&&h[1]=='g'&&h[2]=='g'&&h[3]=='S');
        }catch(Exception e){return false;}
    }
    private void refreshStorageStatus() {
        File dir=new File(getFilesDir(),"adhan");File[] files=dir.listFiles();int count=0;long bytes=0;
        if(files!=null)for(File f:files)if(f.isFile()&&(f.getName().endsWith(".mp3")||f.getName().endsWith(".ogg"))){count++;bytes+=f.length();}
        if(status!=null)status.setText("التسجيلات المحفوظة داخل التطبيق: "+count+" ملف — "+android.text.format.Formatter.formatShortFileSize(this,bytes));
    }
    private String buildAudioDiagnostics() {
        StringBuilder report = new StringBuilder();
        android.content.SharedPreferences audioPrefs = getSharedPreferences("zad_reminders", MODE_PRIVATE);
        report.append("نتيجة آخر تجربة: ").append(audioPrefs.getBoolean("adhan_playback_started", false) ? "وصل المشغل إلى حالة الجاهزية" : "لم يؤكد المشغل بدء الصوت").append("\\n");
        String lastError = audioPrefs.getString("adhan_last_playback_error", "");
        if (!lastError.isEmpty()) report.append("سبب التعذر: ").append(lastError).append("\\n");
        String startError = audioPrefs.getString("adhan_last_start_error", "");
        if (!startError.isEmpty()) report.append("خطأ بدء الخدمة: ").append(startError).append("\\n");
        File dir = new File(getFilesDir(), "adhan");
        int valid = 0;
        for (int i = 0; i < PRAYERS.length; i++) {
            String prayer = PRAYERS[i];
            String voice = prefs.getString("adhan_voice_" + prayer, "doha");
            String fileVoice = voice;
            if ("doha".equals(voice)) fileVoice = "doha_" + KEYS[i];
            File mp3 = new File(dir, fileVoice + ".mp3");
            File ogg = new File(dir, fileVoice + ".ogg");
            File chosen = isAudioFileValid(mp3) ? mp3 : (isAudioFileValid(ogg) ? ogg : null);
            if (chosen != null) valid++;
            report.append(prayer).append(": ").append(chosen == null ? "ملف غير موجود/غير صالح" : "ملف سليم — " + android.text.format.Formatter.formatShortFileSize(this, chosen.length())).append("\\n");
        }
        report.append("ملفات الأذان الصالحة للصلاة المختارة: ").append(valid).append(" من 5");
        return report.toString();
    }

    private boolean isAudioFileValid(File file) {
        if (file == null || !file.isFile() || file.length() < 10000) return false;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] h = new byte[4]; int n = in.read(h);
            return (n >= 3 && h[0] == 'I' && h[1] == 'D' && h[2] == '3')
                    || (n >= 2 && (h[0] & 255) == 255 && (h[1] & 224) == 224)
                    || (n >= 4 && h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S');
        } catch (Exception e) { return false; }
    }

    private void requestNotificationPermission() {
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2401);
    }
    @Override protected void onResume() {
        super.onResume();
        for(int i=0;i<PRAYERS.length;i++){
            int h=prefs.getInt("native_time_hour_"+PRAYERS[i],-1);
            int minute=prefs.getInt("native_time_minute_"+PRAYERS[i],0);
            if(homePrayerTimeViews[i]!=null)homePrayerTimeViews[i].setText(h<0?"--:--":String.format(Locale.getDefault(),"%02d:%02d",h,minute));
        }
        {
            android.app.AlarmManager alarms=(android.app.AlarmManager)getSystemService(ALARM_SERVICE);
            if(alarms!=null){
                for(String prayer:PRAYERS){
                    int h=prefs.getInt("native_time_hour_"+prayer,-1);
                    if(h>=0&&prefs.getBoolean("prayer_enabled_"+prayer,true)){
                        Calendar next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,h);next.set(Calendar.MINUTE,prefs.getInt("native_time_minute_"+prayer,0));next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
                        if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);
                        ReminderReceiver.schedulePrayer(this,prayer,next.getTimeInMillis());
                    }
                }
            }
        }
    }
}
