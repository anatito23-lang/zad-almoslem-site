package com.zad.almoslem;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Calendar;
import java.util.TimeZone;

public class RamadanActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView fastCount, fastCompleted, fastRemaining, fastPercent, khatmaProgress, audioStatus;
    private KhatmaRing khatmaRing;
    private Button khatmaToggle;

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private LinearLayout.LayoutParams full(){return new LinearLayout.LayoutParams(-1,-2);}
    private void gap(LinearLayout parent,int h){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout card(String title){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackground(shape(AppThemeColors.surface(this),18));
        LinearLayout.LayoutParams p=full();p.topMargin=dp(8);root.addView(c,p);
        c.addView(text(title,18,AppThemeColors.text(this),true),full());return c;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);AppThemeColors.apply(this);prefs=getSharedPreferences("zad_ramadan",MODE_PRIVATE);RemoteAudioAssets.refresh(this);build();
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15),dp(14),dp(15),dp(24));root.setBackgroundColor(AppThemeColors.bg(this));scroll.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(12),dp(12),dp(12),dp(12));hero.setBackground(shape(Color.rgb(31,43,70),24));
        ImageView ramadanPhoto=new ImageView(this);ramadanPhoto.setImageResource(R.drawable.azhar_mosque_bg);ramadanPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);ramadanPhoto.setBackground(shape(Color.rgb(31,43,70),18));hero.addView(ramadanPhoto,new LinearLayout.LayoutParams(-1,dp(118)));
        TextView title=text("يا شهر رمضان",24,0xFFFFE2A3,true);title.setGravity(Gravity.CENTER);title.setPadding(0,dp(9),0,dp(2));hero.addView(title,full());
        TextView subtitle=text("أهلًا بشهر الصيام والقرآن • جدّد نيتك واملأ أيامك بالطاعة",12,0xFFFFE9BE,false);subtitle.setGravity(Gravity.CENTER);hero.addView(subtitle,full());
        root.addView(hero,full());

        LinearLayout fast=new LinearLayout(this);
        fast.setOrientation(LinearLayout.VERTICAL);
        fast.setPadding(dp(14),dp(14),dp(14),dp(14));
        fast.setBackground(shape(Color.rgb(25,36,61),22));
        LinearLayout.LayoutParams fastParams=full();fastParams.topMargin=dp(10);root.addView(fast,fastParams);
        TextView fastTitle=text("🌙 رحلتي مع الصيام",21,0xFFFFE2A3,true);
        fastTitle.setGravity(Gravity.CENTER);fast.addView(fastTitle,full());
        TextView fastSubtitle=text("ثلاثون يومًا من الطاعة والقرب من الله",12,0xFFFFE9BE,false);
        fastSubtitle.setGravity(Gravity.CENTER);fastSubtitle.setPadding(0,dp(4),0,dp(12));fast.addView(fastSubtitle,full());
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);
        fast.addView(stats,full());
        fastCompleted=text("",14,0xFFFFE2A3,true);
        fastRemaining=text("",14,0xFFFFE2A3,true);
        fastPercent=text("",14,0xFFFFE2A3,true);
        for(TextView stat:new TextView[]{fastCompleted,fastRemaining,fastPercent}){
            stat.setGravity(Gravity.CENTER);stat.setPadding(dp(3),dp(10),dp(3),dp(10));
            stat.setBackground(shape(Color.rgb(42,55,81),12));
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1f);sp.setMargins(dp(3),0,dp(3),dp(8));stats.addView(stat,sp);
        }
        fastCount=text("أيام الصيام المسجلة",12,0xFFFFE9BE,false);
        fastCount.setGravity(Gravity.CENTER);fast.addView(fastCount,full());
        ProgressBar fastProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        fastProgress.setMax(30);fastProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFE1B96D));
        fastProgress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF46516A));
        fastProgress.setProgress(0);
        LinearLayout.LayoutParams progressParams=full();progressParams.setMargins(0,dp(5),0,dp(12));fast.addView(fastProgress,progressParams);
        TextView daysTitle=text("اضغط على اليوم لتسجيل صيامك أو إلغاء التسجيل",12,0xFFFFE9BE,false);
        daysTitle.setGravity(Gravity.CENTER);daysTitle.setPadding(0,dp(3),0,dp(8));fast.addView(daysTitle,full());
        GridLayout days=new GridLayout(this);days.setColumnCount(5);days.setUseDefaultMargins(true);
        for(int i=1;i<=30;i++){
            final int day=i;
            TextView cell=text(String.format(java.util.Locale.getDefault(),"%02d",day),14,0xFFFFE9BE,true);
            cell.setGravity(Gravity.CENTER);cell.setPadding(0,dp(11),0,dp(11));
            boolean done=prefs.getBoolean("fast_"+day,false);
            cell.setBackground(shape(done?0xFFE1B96D:Color.rgb(48,60,84),12));
            if(done)cell.setTextColor(Color.rgb(31,39,57));
            cell.setOnClickListener(v->{
                boolean next=!prefs.getBoolean("fast_"+day,false);
                prefs.edit().putBoolean("fast_"+day,next).apply();
                cell.setBackground(shape(next?0xFFE1B96D:Color.rgb(48,60,84),12));
                cell.setTextColor(next?Color.rgb(31,39,57):0xFFFFE9BE);
                cell.setText(next?"✓ "+String.format(java.util.Locale.getDefault(),"%02d",day):String.format(java.util.Locale.getDefault(),"%02d",day));
                updateFastCount();
            });
            GridLayout.LayoutParams dp=new GridLayout.LayoutParams();
            dp.width=0;dp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            dp.setMargins(dp(3),dp(3),dp(3),dp(3));days.addView(cell,dp);
        }
        fast.addView(days,full());
        updateFastCount();
        fastProgress.setTag("fastTrackerProgress");

        LinearLayout khatma=card("ختمة القرآن الكريم");
        TextView invitation=text("اجعل لك وردًا يوميًا من القرآن 🌙\\nافتح المصحف، وسجّل تقدمك تلقائيًا أثناء تشغيل الختمة.",13,AppThemeColors.text(this),false);
        invitation.setPadding(0,dp(8),0,dp(10));khatma.addView(invitation,full());
        khatmaRing=new KhatmaRing();LinearLayout ringHolder=new LinearLayout(this);ringHolder.setGravity(Gravity.CENTER);ringHolder.addView(khatmaRing,new LinearLayout.LayoutParams(dp(116),dp(116)));khatma.addView(ringHolder,full());
        khatmaProgress=text("",15,AppThemeColors.accent(this),true);khatmaProgress.setGravity(Gravity.CENTER);khatma.addView(khatmaProgress,full());
        TextView note=text("يُسجّل الصفحات التي تفتحها أثناء تشغيل الختمة، ويُحفظ تقدمك عند العودة.",11,AppThemeColors.muted(this),false);
        note.setPadding(0,dp(7),0,dp(8));khatma.addView(note,full());
        Button openMushaf=new Button(this);openMushaf.setText("📖  افتح المصحف وابدأ القراءة");khatma.addView(openMushaf,full());
        khatmaToggle=new Button(this);khatma.addView(khatmaToggle,full());
        Button resetKhatma=new Button(this);
        resetKhatma.setText("↺ تصفير عداد الختمة");
        khatma.addView(resetKhatma,full());
        resetKhatma.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
                .setTitle("تصفير عداد الختمة")
                .setMessage("هل تريد بدء ختمة جديدة؟ سيتم تصفير الصفحات المسجلة وإيقاف متابعة الختمة الحالية.")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("تصفير", (dialog,which)->{
                    QuranKhatmaTracker.stop(this);
                    QuranKhatmaTracker.reset(this);
                    updateKhatma();
                    Toast.makeText(this,"تم تصفير عداد الختمة. بالتوفيق في ختمتك الجديدة 🌙",Toast.LENGTH_LONG).show();
                }).show());
        openMushaf.setOnClickListener(v->{
            if(!QuranKhatmaTracker.isTracking(this))QuranKhatmaTracker.start(this);
            updateKhatma();
            Intent open=new Intent(this,MushafPageActivity.class);
            open.putExtra("from_ramadan",true);
            open.putExtra("page",getSharedPreferences("zad_mushaf",MODE_PRIVATE).getInt("last_page",1));
            startActivity(open);
        });
        khatmaToggle.setOnClickListener(v->{
            if(QuranKhatmaTracker.isTracking(this))QuranKhatmaTracker.stop(this);
            else QuranKhatmaTracker.start(this);
            updateKhatma();
        });
        updateKhatma();

        LinearLayout adhkar=card("أذكار وأدعية رمضانية");
        addDua(adhkar,"دعاء الإفطار","ذهب الظمأ وابتلت العروق وثبت الأجر إن شاء الله.");
        addDua(adhkar,"دعاء جامع","اللهم إنك عفو تحب العفو فاعف عني.");
        addDua(adhkar,"دعاء القبول","ربنا تقبل منا إنك أنت السميع العليم، وتب علينا إنك أنت التواب الرحيم.");
        addDua(adhkar,"ذكر يسير","سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر.");
        addDua(adhkar,"الصلاة على النبي ﷺ","اللهم صل وسلم وبارك على نبينا محمد.");

        LinearLayout ramadanGuide=card("زادك في رمضان — آيات وأحاديث وأعمال");
        addDua(ramadanGuide,"رمضان شهر القرآن","قال الله تعالى: ﴿شَهْرُ رَمَضَانَ الَّذِي أُنْزِلَ فِيهِ الْقُرْآنُ هُدًى لِلنَّاسِ وَبَيِّنَاتٍ مِنَ الْهُدَى وَالْفُرْقَانِ﴾ [البقرة: 185].");
        addDua(ramadanGuide,"فضل الصيام","قال رسول الله ﷺ: «من صام رمضان إيمانًا واحتسابًا غُفر له ما تقدم من ذنبه» متفق عليه.");
        addDua(ramadanGuide,"فضل قيام ليلة القدر","قال رسول الله ﷺ: «من قام ليلة القدر إيمانًا واحتسابًا غُفر له ما تقدم من ذنبه» متفق عليه.");
        addDua(ramadanGuide,"اغتنم أيام الشهر","اجعل لك وردًا ثابتًا من القرآن، واحفظ لسانك، وأكثر من الدعاء والاستغفار، وتفقد المحتاجين، وأحسن إلى أهلك وجيرانك.");
        addDua(ramadanGuide,"لا تؤجل التوبة","ابدأ من اليوم؛ فالعبرة ليست بكثرة ما نخطط له، وإنما بصدق العمل والثبات عليه.");
        addDua(ramadanGuide,"خطة يومية بسيطة","بعد الفجر: أذكار الصباح وورد القرآن. أثناء النهار: حفظ اللسان وإتقان العمل. قبل المغرب: دعاء واستغفار. بعد العشاء: التراويح وما تيسر من القيام.");
        addDua(ramadanGuide,"الصيام جُنّة","قال رسول الله ﷺ: «الصيام جُنّة، فلا يرفث ولا يجهل» متفق عليه. فاحفظ صيامك من الغيبة والخصومة وسوء الخلق.");
        addDua(ramadanGuide,"باب الصدقة مفتوح","قال ابن عباس رضي الله عنهما: «كان رسول الله ﷺ أجود الناس، وكان أجود ما يكون في رمضان» متفق عليه. تصدق ولو بالقليل، وأطعم صائمًا، وتفقد محتاجًا.");
        addDua(ramadanGuide,"رمضان وصلة الرحم","اجعل في شهرك زيارة أو اتصالًا برحمك، وابدأ بالسلام والعفو، ولا تجعل خلافًا قديمًا يحرمك أجر الصلة.");
        addDua(ramadanGuide,"رمضان وحفظ اللسان","قال رسول الله ﷺ: «من لم يدع قول الزور والعمل به فليس لله حاجة في أن يدع طعامه وشرابه» رواه البخاري. ليس الصيام جوعًا وعطشًا فقط؛ بل تهذيب للقلب واللسان.");
        addDua(ramadanGuide,"العشر الأواخر","كان النبي ﷺ إذا دخل العشر شد مئزره وأحيا ليله وأيقظ أهله، متفق عليه. فاستعد لها من الآن، ولا تجعل آخر الشهر أضعف أيامك.");
        addDua(ramadanGuide,"ليلة القدر","قال الله تعالى: ﴿لَيْلَةُ الْقَدْرِ خَيْرٌ مِنْ أَلْفِ شَهْرٍ﴾ [القدر: 3]. وأكثر من الدعاء الذي علّمه النبي ﷺ لعائشة: «اللهم إنك عفو تحب العفو فاعف عني» رواه الترمذي وصححه.");
        addDua(ramadanGuide,"زكاة الفطر","تذكّر إخراج زكاة الفطر قبل صلاة العيد، طُهرةً للصائم وطُعمةً للمساكين؛ وأخرجها وفق المقدار المعتمد في بلدك.");
        addDua(ramadanGuide,"أدب الاختلاف","لا تجعل الصيام سببًا للغضب ورفع الصوت. إذا جادلك أحد فقل: إني صائم، كما أرشد النبي ﷺ في الحديث المتفق عليه.");
        addDua(ramadanGuide,"اجعل للقرآن أثرًا","لا يكن همّك عدد الصفحات وحده؛ اقرأ بتدبر، واسأل نفسك: ما الآية التي سأعمل بها اليوم؟ ثم خذ منها خطوة عملية.");

        Button guideMushaf=new Button(this);guideMushaf.setText("📖  انتقل إلى المصحف");ramadanGuide.addView(guideMushaf,full());
        guideMushaf.setOnClickListener(v->{Intent open=new Intent(this,MushafPageActivity.class);open.putExtra("from_ramadan",true);open.putExtra("page",getSharedPreferences("zad_mushaf",MODE_PRIVATE).getInt("last_page",1));startActivity(open);});

        LinearLayout settings=card("إعدادات الشهر والتنبيهات");
        Switch enabled=new Switch(this);enabled.setText("تفعيل تنبيهات رمضان");enabled.setTextColor(AppThemeColors.text(this));enabled.setChecked(prefs.getBoolean("enabled",false));settings.addView(enabled,full());
        TextView startLabel=text("الموعد الفلكي المتوقع لأول رمضان 1448: الاثنين 8 فبراير 2027؛ ويُعتمد الموعد النهائي بعد إعلان رؤية الهلال في مصر.",13,AppThemeColors.muted(this),false);settings.addView(startLabel,full());
        Button chooseStart=new Button(this);chooseStart.setText("تحديد أول يوم في رمضان");settings.addView(chooseStart,full());
        updateStartLabel(startLabel);
        chooseStart.setOnClickListener(v->{Calendar now=Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo"));long saved=prefs.getLong("start_at",0L);if(saved>0)now.setTimeInMillis(saved);else now.set(2027,Calendar.FEBRUARY,8,0,0,0);
            new DatePickerDialog(this,(picker,y,m,d)->{Calendar first=Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo"));first.set(y,m,d,0,0,0);first.set(Calendar.MILLISECOND,0);prefs.edit().putLong("start_at",first.getTimeInMillis()).apply();updateStartLabel(startLabel);RamadanReminderReceiver.scheduleAll(this);},now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();});
        settings.addView(text("تنبيه السحور",15,AppThemeColors.accent(this),true),full());
        TextView suhoorUploadLabel=text("اختر من مكتبة الابتهالات صوتًا لتنبيه السحور، أو اترك الاختيار الافتراضي ليعمل أذان التطبيق المرفق.",12,AppThemeColors.muted(this),false);settings.addView(suhoorUploadLabel,full());
        Switch suhoor=new Switch(this);suhoor.setText("تشغيل تنبيه السحور");suhoor.setTextColor(AppThemeColors.text(this));suhoor.setChecked(prefs.getBoolean("suhoor_enabled",true));settings.addView(suhoor,full());
        addMinutesControl(settings,"تنبيه السحور قبل الفجر بدقائق","suhoor_lead_minutes",45,10,120);
        TextView prayerTimesNote=text("يُحسب تنبيه السحور من موعد الفجر المحفوظ في التطبيق. يلزم ضبط مواقيت الصلاة والسماح بالتنبيهات الدقيقة.",12,AppThemeColors.muted(this),false);prayerTimesNote.setPadding(0,dp(7),0,dp(7));settings.addView(prayerTimesNote,full());
        enabled.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean("enabled",v).apply();RamadanReminderReceiver.scheduleAll(this);});
        suhoor.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean("suhoor_enabled",v).apply();RamadanReminderReceiver.scheduleAll(this);});

        LinearLayout audio=card("ابتهالات رمضان وتنبيه السحور");
        addSuhoorNasheedCard(audio);
        AdminTabContent.attach(this,root,"ramadan");
        setContentView(AppTabs.wrap(this,scroll,"ramadan"));
    }
    private Spinner suhoorArtistPicker,suhoorTrackPicker;
    private TextView suhoorNasheedStatus;
    private void addSuhoorNasheedCard(LinearLayout box){
        TextView intro=text("اختر ابتهالًا من مكتبة الشيخ نصر الدين طوبار أو الشيخ سيد النقشبندي. يمكنك الاستماع إليه، ثم اعتماده ليكون صوت تنبيه السحور.",13,AppThemeColors.muted(this),false);
        intro.setPadding(0,dp(8),0,dp(8));box.addView(intro,full());
        String[] artists=new String[AudioLibraryActivity.getArtistCount()];
        for(int i=0;i<artists.length;i++)artists[i]=AudioLibraryActivity.getArtistName(i);
        suhoorArtistPicker=new Spinner(this);
        suhoorArtistPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,artists));
        box.addView(suhoorArtistPicker,full());
        suhoorTrackPicker=new Spinner(this);box.addView(suhoorTrackPicker,full());
        int savedArtist=prefs.getInt("suhoor_nasheed_artist",1);
        int savedTrack=prefs.getInt("suhoor_nasheed_track",0);
        suhoorArtistPicker.setSelection(Math.max(0,Math.min(artists.length-1,savedArtist)));
        suhoorArtistPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                String[] tracks=new String[AudioLibraryActivity.getTrackCount(position)];
                for(int i=0;i<tracks.length;i++)tracks[i]=AudioLibraryActivity.getTrackName(position,i);
                suhoorTrackPicker.setAdapter(new ArrayAdapter<>(RamadanActivity.this,android.R.layout.simple_spinner_dropdown_item,tracks));
                int wanted=position==prefs.getInt("suhoor_nasheed_artist",1)?prefs.getInt("suhoor_nasheed_track",0):0;
                suhoorTrackPicker.setSelection(Math.max(0,Math.min(tracks.length-1,wanted)));
            }
        });
        suhoorNasheedStatus=text("",12,AppThemeColors.accent(this),true);
        suhoorNasheedStatus.setPadding(0,dp(8),0,dp(5));box.addView(suhoorNasheedStatus,full());
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button preview=new Button(this);preview.setText("▶ استمع للتجربة");
        Button apply=new Button(this);apply.setText("اعتماده لتنبيه السحور");
        buttons.addView(preview,new LinearLayout.LayoutParams(0,dp(50),1));
        buttons.addView(apply,new LinearLayout.LayoutParams(0,dp(50),1));
        box.addView(buttons,full());
        Button saveOffline=new Button(this);saveOffline.setText("⬇ حفظ نسخة على الهاتف دون إنترنت");
        box.addView(saveOffline,full());
        saveOffline.setOnClickListener(v->{
            if(!saveSelectedSuhoorNasheed())return;
            Toast.makeText(this,"بدأ حفظ نسخة صوت السحور على الهاتف. انتظر رسالة اكتمال الحفظ.",Toast.LENGTH_LONG).show();
        });
        Button stop=new Button(this);stop.setText("■ إيقاف الصوت");box.addView(stop,full());
        int volume=prefs.getInt("ramadan_volume_nasheed",100);
        TextView volumeLabel=text("مستوى صوت الابتهال: "+volume+"٪",12,AppThemeColors.muted(this),false);box.addView(volumeLabel,full());
        SeekBar volumeBar=new SeekBar(this);volumeBar.setMax(100);volumeBar.setProgress(volume);box.addView(volumeBar,full());
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int value,boolean fromUser){volumeLabel.setText("مستوى صوت الابتهال: "+value+"٪");if(fromUser)prefs.edit().putInt("ramadan_volume_nasheed",value).apply();}
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        preview.setOnClickListener(v->{
            int artist=suhoorArtistPicker.getSelectedItemPosition(),track=suhoorTrackPicker.getSelectedItemPosition();
            if(artist<0||track<0){Toast.makeText(this,"اختر الشيخ والابتهال أولًا",Toast.LENGTH_SHORT).show();return;}
            String previewUrl=AudioLibraryActivity.getTrackUrl(artist,track);
            if(previewUrl==null||!previewUrl.startsWith("https://")){suhoorNasheedStatus.setText("رابط التسجيل غير متاح.");return;}
            Intent play=new Intent(this,RamadanAudioService.class);play.putExtra("key","ramadan_suhoor_nasheed_preview");
            play.putExtra("nasheed_url",previewUrl);play.putExtra("volume",prefs.getInt("ramadan_volume_nasheed",100)/100f);
            try{if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(play);else startService(play);suhoorNasheedStatus.setText("معاينة: "+AudioLibraryActivity.getTrackName(artist,track));}
            catch(Exception e){suhoorNasheedStatus.setText("تعذر تشغيل الابتهال: "+e.getMessage());}
        });
        apply.setOnClickListener(v->{
            if(!saveSelectedSuhoorNasheed())return;
            Toast.makeText(this,"تم اعتماد الابتهال لتنبيه السحور",Toast.LENGTH_LONG).show();
        });
        stop.setOnClickListener(v->{
            Intent stopIntent=new Intent(this,RamadanAudioService.class);stopIntent.setAction(RamadanAudioService.ACTION_STOP);
            try{startService(stopIntent);suhoorNasheedStatus.setText("تم إيقاف الصوت.");}
            catch(Exception e){suhoorNasheedStatus.setText("تعذر إيقاف الصوت.");}
        });
        updateSuhoorNasheedStatus();
    }
    private boolean saveSelectedSuhoorNasheed(){
        int artist=suhoorArtistPicker.getSelectedItemPosition(),track=suhoorTrackPicker.getSelectedItemPosition();
        if(artist<0||track<0){Toast.makeText(this,"اختر الشيخ والابتهال أولًا",Toast.LENGTH_SHORT).show();return false;}
        String url=AudioLibraryActivity.getTrackUrl(artist,track);
        if(url==null||!url.startsWith("https://")){Toast.makeText(this,"رابط التسجيل غير متاح",Toast.LENGTH_LONG).show();return false;}
        String title=AudioLibraryActivity.getTrackName(artist,track);
        prefs.edit().putInt("suhoor_nasheed_artist",artist).putInt("suhoor_nasheed_track",track)
                .putString("suhoor_nasheed_url",url).putString("suhoor_nasheed_title",title)
                .putString("suhoor_nasheed_artist_name",AudioLibraryActivity.getArtistName(artist))
                .putBoolean("suhoor_nasheed_downloaded",false).apply();
        downloadSuhoorNasheed(url,title);
        updateSuhoorNasheedStatus();
        return true;
    }
    private void downloadSuhoorNasheed(String url,String title){
        if(!url.startsWith("https://"))return;
        if(suhoorNasheedStatus!=null)suhoorNasheedStatus.setText("جارٍ حفظ صوت السحور على الهاتف لاستخدامه دون إنترنت...");
        new Thread(()->{
            java.net.HttpURLConnection connection=null;
            try{
                connection=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
                connection.setConnectTimeout(20000);connection.setReadTimeout(30000);connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent","ZadAlmoslem Android");
                if(connection.getResponseCode()!=200)throw new java.io.IOException("HTTP "+connection.getResponseCode());
                java.io.File dir=new java.io.File(getFilesDir(),"ramadan_audio");if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("تعذر إنشاء مجلد الصوت");
                java.io.File temp=new java.io.File(dir,"suhoor_nasheed.tmp"),target=new java.io.File(dir,"suhoor_nasheed.mp3");
                try(java.io.InputStream in=connection.getInputStream();java.io.FileOutputStream out=new java.io.FileOutputStream(temp)){
                    byte[] buffer=new byte[8192];int n;long total=0;
                    while((n=in.read(buffer))!=-1){total+=n;if(total>80L*1024*1024)throw new java.io.IOException("حجم التسجيل أكبر من الحد المسموح");out.write(buffer,0,n);}
                    out.getFD().sync();
                }
                if(temp.length()<4096)throw new java.io.IOException("ملف الصوت غير مكتمل");
                if(target.exists()&&!target.delete())throw new java.io.IOException("تعذر استبدال التسجيل القديم");
                if(!temp.renameTo(target))throw new java.io.IOException("تعذر حفظ التسجيل");
                prefs.edit().putBoolean("suhoor_nasheed_downloaded",true).apply();
                runOnUiThread(()->{if(suhoorNasheedStatus!=null)suhoorNasheedStatus.setText("تم حفظ «"+title+"» على الهاتف — سيعمل دون إنترنت.");});
            }catch(Exception e){
                android.util.Log.e("ZadRamadan","Could not download suhoor audio",e);
                runOnUiThread(()->{if(suhoorNasheedStatus!=null)suhoorNasheedStatus.setText("تعذر الحفظ دون إنترنت. تحقق من الاتصال ثم أعد اعتماد التسجيل.");});
            }finally{if(connection!=null)connection.disconnect();}
        }).start();
    }
    private void updateSuhoorNasheedStatus(){
        if(suhoorNasheedStatus==null)return;
        String title=prefs.getString("suhoor_nasheed_title","");
        String artist=prefs.getString("suhoor_nasheed_artist_name","");
        java.io.File savedFile=new java.io.File(new java.io.File(getFilesDir(),"ramadan_audio"),"suhoor_nasheed.mp3");
        boolean downloaded=prefs.getBoolean("suhoor_nasheed_downloaded",false)&&savedFile.isFile()&&savedFile.length()>4096;
        if(title.isEmpty())suhoorNasheedStatus.setText("لم يتم اختيار ابتهال بعد؛ سيعمل أذان التطبيق المرفق كصوت افتراضي.");
        else if(downloaded)suhoorNasheedStatus.setText("✓ تم حفظ «"+title+"» على الهاتف — سيعمل دون إنترنت.");
        else suhoorNasheedStatus.setText("الابتهال المختار: "+title+" — "+artist+" | لم تُحفظ النسخة دون إنترنت بعد.");
    }
    private void updateStartLabel(TextView label){long at=prefs.getLong("start_at",0L);if(at<=0){label.setText("الموعد الفلكي المتوقع: الاثنين 8 فبراير 2027 (1 رمضان 1448). انتظر إعلان دار الإفتاء المصرية لتأكيد بداية الشهر.");return;}Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo"));c.setTimeInMillis(at);label.setText("بداية الشهر المحددة: "+c.get(Calendar.DAY_OF_MONTH)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR)+" — تنتهي التنبيهات تلقائيًا بعد 30 يومًا.");}
    private void addDua(LinearLayout box,String heading,String body){TextView h=text(heading,14,AppThemeColors.accent(this),true);h.setPadding(0,dp(9),0,dp(2));box.addView(h,full());box.addView(text(body,14,AppThemeColors.text(this),false),full());}
    private void updateFastCount(){
        int n=0;for(int i=1;i<=30;i++)if(prefs.getBoolean("fast_"+i,false))n++;
        if(fastCount!=null)fastCount.setText("أيام الصيام المسجلة");
        if(fastCompleted!=null)fastCompleted.setText("✓ "+n+"\nيوم صيام");
        if(fastRemaining!=null)fastRemaining.setText((30-n)+"\nيوم متبقٍ");
        if(fastPercent!=null)fastPercent.setText(Math.round(n*100f/30)+"٪\nإنجاز");
        View tracker=root==null?null:root.findViewWithTag("fastTrackerProgress");
        if(tracker instanceof ProgressBar)((ProgressBar)tracker).setProgress(n);
    }
    private void updateKhatma(){
        int n=Math.min(604,QuranKhatmaTracker.getReadPageCount(this));int percent=Math.round(n*100f/604);
        if(khatmaProgress!=null)khatmaProgress.setText("تمت متابعة "+n+" صفحة من 604  •  المتبقي "+(604-n)+" صفحة");
        if(khatmaRing!=null){khatmaRing.setProgress(percent);khatmaRing.setActive(QuranKhatmaTracker.isTracking(this));}
        if(khatmaToggle!=null)khatmaToggle.setText(QuranKhatmaTracker.isTracking(this)?"Ⅱ إيقاف متابعة الختمة":"▶ تشغيل متابعة الختمة");
    }
    private final class KhatmaRing extends View{
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF arc=new RectF();private int percent=0;private boolean active=false;
        KhatmaRing(){super(RamadanActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setProgress(int value){percent=Math.max(0,Math.min(100,value));invalidate();}
        void setActive(boolean value){active=value;invalidate();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f;float radius=Math.min(getWidth(),getHeight())*.39f;float stroke=dp(12);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0x335B8A70);canvas.drawCircle(cx,cy,radius,paint);
            arc.set(cx-radius,cy-radius,cx+radius,cy+radius);paint.setColor(active?0xFF2D8A62:0xFF9A875F);canvas.drawArc(arc,-90,360f*percent/100f,false,paint);
            paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));paint.setColor(AppThemeColors.text(RamadanActivity.this));paint.setTextSize(dp(31));canvas.drawText(percent+"٪",cx,cy+dp(5),paint);
            paint.setTextSize(dp(12));paint.setTypeface(Typeface.DEFAULT);paint.setColor(AppThemeColors.muted(RamadanActivity.this));canvas.drawText(active?"متابعة الختمة تعمل":"المتابعة متوقفة",cx,cy+dp(29),paint);
        }
    }
    private void addMinutesControl(LinearLayout box,String title,String key,int def,int min,int max){
        box.addView(text(title,13,AppThemeColors.text(this),false),full());TextView value=text(prefs.getInt(key,def)+" دقيقة",13,AppThemeColors.accent(this),true);box.addView(value,full());
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(Math.max(0,Math.min(max-min,prefs.getInt(key,def)-min)));box.addView(bar,full());
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){int n=p+min;value.setText(n+" دقيقة");if(u){prefs.edit().putInt(key,n).apply();RamadanReminderReceiver.scheduleAll(RamadanActivity.this);}}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
    }
    private void addExternalAudioButton(LinearLayout box,String label,String url){
        Button b=new Button(this);b.setText(label);box.addView(b,full());
        b.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
            catch(Exception e){Toast.makeText(this,"تعذر فتح مصدر الصوت: "+e.getMessage(),Toast.LENGTH_LONG).show();}});
    }
    private void addAudioTestButton(LinearLayout box,String label,String key){
        LinearLayout controls=new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button playButton=new Button(this);playButton.setText(label);
        Button stopButton=new Button(this);stopButton.setText("إيقاف");
        controls.addView(playButton,new LinearLayout.LayoutParams(0,dp(48),1));
        controls.addView(stopButton,new LinearLayout.LayoutParams(0,dp(48),1));
        box.addView(controls,full());

        String volumeKey="ramadan_volume_"+key;
        int savedVolume=prefs.getInt(volumeKey,100);
        TextView volumeLabel=text("مستوى الصوت: "+savedVolume+"٪",13,AppThemeColors.muted(this),false);
        box.addView(volumeLabel,full());
        SeekBar volumeBar=new SeekBar(this);volumeBar.setMax(100);volumeBar.setProgress(savedVolume);
        box.addView(volumeBar,full());
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int value,boolean fromUser){
                volumeLabel.setText("مستوى الصوت: "+value+"٪");
                if(fromUser)prefs.edit().putInt(volumeKey,value).apply();
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });

        playButton.setOnClickListener(v->{
            playButton.setEnabled(false);
            playButton.setText("جارٍ التحقق من التسجيل…");
            RemoteAudioAssets.refresh(this,()->runOnUiThread(()->{
                playButton.setEnabled(true);
                playButton.setText(label);
                Intent play=new Intent(this,RamadanAudioService.class);
                play.putExtra("key",key);
                play.putExtra("volume",prefs.getInt(volumeKey,100)/100f);
                try{if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(play);else startService(play);}
                catch(Exception e){Toast.makeText(this,"تعذر تشغيل الصوت: "+e.getMessage(),Toast.LENGTH_LONG).show();}
            }));
        });
        stopButton.setOnClickListener(v->{
            Intent stop=new Intent(this,RamadanAudioService.class);stop.putExtra("key","stop");
            try{startService(stop);}
            catch(Exception e){Toast.makeText(this,"تعذر إيقاف الصوت: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        });
    }
    @Override protected void onResume(){
        super.onResume();
        if(prefs!=null){
            updateKhatma();
            if(prefs.getBoolean("enabled",false))RamadanReminderReceiver.scheduleAll(this);
            updateSuhoorNasheedStatus();
        }
    }
}
