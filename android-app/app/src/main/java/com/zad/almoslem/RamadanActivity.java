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

public class RamadanActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView fastCount, khatmaProgress, audioStatus;
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
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));c.setBackground(shape(AppThemeColors.surface(this),18));
        LinearLayout.LayoutParams p=full();p.topMargin=dp(12);root.addView(c,p);
        c.addView(text(title,18,AppThemeColors.text(this),true),full());return c;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);AppThemeColors.apply(this);prefs=getSharedPreferences("zad_ramadan",MODE_PRIVATE);RemoteAudioAssets.refresh(this);build();
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15),dp(14),dp(15),dp(24));root.setBackgroundColor(AppThemeColors.bg(this));scroll.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(15),dp(30),dp(15),dp(30));hero.setBackground(shape(Color.rgb(31,43,70),24));
        hero.addView(new SectionArtworkView(this,"ramadan"),new LinearLayout.LayoutParams(-1,dp(164)));
        TextView moon=text("☾  ✦  🏮",32,0xFFFFD98A,true);moon.setGravity(Gravity.CENTER);hero.addView(moon,full());
        TextView title=text("رمضان شهر الصيام والقرآن",23,Color.WHITE,true);title.setGravity(Gravity.CENTER);hero.addView(title,full());
        TextView subtitle=text("رمضان كريم  •  جدّد نيتك، واملأ أيام الشهر بالطاعة  •  ✦",13,0xFFFFE9BE,false);subtitle.setGravity(Gravity.CENTER);hero.addView(subtitle,full());
        root.addView(hero,full());

        LinearLayout fast=card("متتبع الصيام — ثلاثون يومًا");
        fastCount=text("",14,AppThemeColors.accent(this),true);fast.addView(fastCount,full());
        GridLayout days=new GridLayout(this);days.setColumnCount(5);days.setUseDefaultMargins(true);
        for(int i=1;i<=30;i++){
            final int day=i;CheckBox check=new CheckBox(this);check.setText(String.valueOf(i));check.setTextSize(13);
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(AppThemeColors.accent(this)));
            check.setTextColor(AppThemeColors.text(this));check.setChecked(prefs.getBoolean("fast_"+i,false));
            check.setOnCheckedChangeListener((b,checked)->{prefs.edit().putBoolean("fast_"+day,checked).apply();updateFastCount();});
            GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);days.addView(check,p);
        }
        fast.addView(days,full());updateFastCount();

        LinearLayout khatma=card("ختمة القرآن الكريم");
        TextView invitation=text("اجعل لك وردًا من القرآن في رمضان 🌙\\nاضغط على الزر لفتح المصحف، وسنتابع الصفحات التي تتنقل بينها تلقائيًا ما دامت الختمة قيد التشغيل.",14,AppThemeColors.text(this),false);
        invitation.setPadding(0,dp(8),0,dp(10));khatma.addView(invitation,full());
        khatmaRing=new KhatmaRing();LinearLayout ringHolder=new LinearLayout(this);ringHolder.setGravity(Gravity.CENTER);ringHolder.addView(khatmaRing,new LinearLayout.LayoutParams(dp(178),dp(178)));khatma.addView(ringHolder,full());
        khatmaProgress=text("",15,AppThemeColors.accent(this),true);khatmaProgress.setGravity(Gravity.CENTER);khatma.addView(khatmaProgress,full());
        TextView note=text("يتابع المؤشر صفحات المصحف التي تفتحها أثناء تشغيل الختمة، ولا يدّعي التحقق من القراءة الفعلية. تقدمك محفوظ عند الإيقاف والعودة.",12,AppThemeColors.muted(this),false);
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
        Button guideMushaf=new Button(this);guideMushaf.setText("📖  انتقل إلى المصحف");ramadanGuide.addView(guideMushaf,full());
        guideMushaf.setOnClickListener(v->{Intent open=new Intent(this,MushafPageActivity.class);open.putExtra("page",getSharedPreferences("zad_mushaf",MODE_PRIVATE).getInt("last_page",1));startActivity(open);});

        LinearLayout settings=card("إعدادات الشهر والتنبيهات");
        Switch enabled=new Switch(this);enabled.setText("تفعيل تنبيهات رمضان");enabled.setTextColor(AppThemeColors.text(this));enabled.setChecked(prefs.getBoolean("enabled",false));settings.addView(enabled,full());
        TextView startLabel=text("",13,AppThemeColors.muted(this),false);settings.addView(startLabel,full());
        Button chooseStart=new Button(this);chooseStart.setText("تحديد أول يوم في رمضان");settings.addView(chooseStart,full());
        updateStartLabel(startLabel);
        chooseStart.setOnClickListener(v->{Calendar now=Calendar.getInstance();long saved=prefs.getLong("start_at",0L);if(saved>0)now.setTimeInMillis(saved);
            new DatePickerDialog(this,(picker,y,m,d)->{Calendar first=Calendar.getInstance();first.set(y,m,d,0,0,0);first.set(Calendar.MILLISECOND,0);prefs.edit().putLong("start_at",first.getTimeInMillis()).apply();updateStartLabel(startLabel);RamadanReminderReceiver.scheduleAll(this);},now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();});
        settings.addView(text("تنبيه السحور",15,AppThemeColors.accent(this),true),full());
        TextView suhoorUploadLabel=text("سيعمل صوت أذان مرفق بالتطبيق تلقائيًا لتنبيه السحور، ويمكن استبداله بتسجيل مسحراتي من لوحة الإدارة.",12,AppThemeColors.muted(this),false);settings.addView(suhoorUploadLabel,full());
        Switch suhoor=new Switch(this);suhoor.setText("تشغيل تنبيه السحور");suhoor.setTextColor(AppThemeColors.text(this));suhoor.setChecked(prefs.getBoolean("suhoor_enabled",true));settings.addView(suhoor,full());
        addMinutesControl(settings,"تنبيه السحور قبل الفجر بدقائق","suhoor_lead_minutes",45,10,120);
        TextView prayerTimesNote=text("يُحسب تنبيه السحور من موعد الفجر المحفوظ في التطبيق. يلزم ضبط مواقيت الصلاة والسماح بالتنبيهات الدقيقة.",12,AppThemeColors.muted(this),false);prayerTimesNote.setPadding(0,dp(7),0,dp(7));settings.addView(prayerTimesNote,full());
        enabled.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean("enabled",v).apply();RamadanReminderReceiver.scheduleAll(this);});
        suhoor.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean("suhoor_enabled",v).apply();RamadanReminderReceiver.scheduleAll(this);});

        LinearLayout audio=card("تنبيه السحور — تسجيل المسحراتي");
        audioStatus=text("يُدار تسجيل السحور من لوحة الإدارة، ويُحدّثه التطبيق تلقائيًا ليعمل حتى دون إنترنت.",12,AppThemeColors.muted(this),false);
        audio.addView(audioStatus,full());
        addAudioTestButton(audio,"تجربة صوت تنبيه السحور","ramadan_suhoor_voice_test");
        AdminTabContent.attach(this,root,"ramadan");
        setContentView(AppTabs.wrap(this,scroll,"ramadan"));
    }
    private void updateStartLabel(TextView label){long at=prefs.getLong("start_at",0L);if(at<=0){label.setText("لم يتم تحديد بداية الشهر بعد؛ اختر أول يوم حتى تتوقف التنبيهات تلقائيًا بعد 30 يومًا.");return;}Calendar c=Calendar.getInstance();c.setTimeInMillis(at);label.setText("بداية الشهر المحددة: "+c.get(Calendar.DAY_OF_MONTH)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR)+" — تنتهي التنبيهات تلقائيًا بعد 30 يومًا.");}
    private void addDua(LinearLayout box,String heading,String body){TextView h=text(heading,14,AppThemeColors.accent(this),true);h.setPadding(0,dp(9),0,dp(2));box.addView(h,full());box.addView(text(body,14,AppThemeColors.text(this),false),full());}
    private void updateFastCount(){int n=0;for(int i=1;i<=30;i++)if(prefs.getBoolean("fast_"+i,false))n++;if(fastCount!=null)fastCount.setText("أيام الصيام المسجلة: "+n+" من 30");}
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
            // Re-read the admin-published URLs whenever the Ramadan tab returns
            // to the foreground, then refresh the local audio cache.
            if(audioStatus!=null)audioStatus.setText("جارٍ تحديث تسجيلات رمضان المنشورة من لوحة الإدارة…");
            RemoteAudioAssets.refreshWithResult(this,message->{
                if(audioStatus!=null)audioStatus.setText(message);
            });
        }
    }
}
