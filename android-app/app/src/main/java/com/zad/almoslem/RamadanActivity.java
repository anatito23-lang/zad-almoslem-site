package com.zad.almoslem;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

public class RamadanActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView fastCount, khatmaProgress, audioStatus;
    private static final int PICK_IFTAR=4101, PICK_CANNON=4102, PICK_SUHOOR=4103;

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
        super.onCreate(state);AppThemeColors.apply(this);prefs=getSharedPreferences("zad_ramadan",MODE_PRIVATE);build();
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15),dp(14),dp(15),dp(24));root.setBackgroundColor(AppThemeColors.bg(this));scroll.addView(root);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER);hero.setPadding(dp(15),dp(22),dp(15),dp(22));hero.setBackground(shape(AppThemeColors.primary(this),22));
        TextView moon=text("☾",42,0xFFE9CF91,true);moon.setGravity(Gravity.CENTER);hero.addView(moon,full());
        TextView title=text("رمضان شهر الصيام والقرآن",23,Color.WHITE,true);title.setGravity(Gravity.CENTER);hero.addView(title,full());
        TextView subtitle=text("جدّد نيتك، واملأ أيام الشهر بالطاعة",13,0xFFE9E1CE,false);subtitle.setGravity(Gravity.CENTER);hero.addView(subtitle,full());
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
        khatmaProgress=text("",15,AppThemeColors.accent(this),true);khatma.addView(khatmaProgress,full());
        TextView target=text("الهدف التقريبي: 20 صفحة يوميًا لإتمام 604 صفحات في ثلاثين يومًا.",13,AppThemeColors.muted(this),false);target.setPadding(0,dp(7),0,dp(7));khatma.addView(target,full());
        SeekBar page=new SeekBar(this);page.setMax(604);page.setProgress(prefs.getInt("khatma_page",0));khatma.addView(page,full());
        page.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean u){if(u){prefs.edit().putInt("khatma_page",n).apply();updateKhatma();}}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        LinearLayout buttons=new LinearLayout(this);Button plus=new Button(this);plus.setText("+ 20 صفحة");Button reset=new Button(this);reset.setText("بدء ختمة جديدة");
        buttons.addView(plus,new LinearLayout.LayoutParams(0,dp(48),1));buttons.addView(reset,new LinearLayout.LayoutParams(0,dp(48),1));khatma.addView(buttons,full());
        plus.setOnClickListener(v->{int n=Math.min(604,prefs.getInt("khatma_page",0)+20);prefs.edit().putInt("khatma_page",n).apply();page.setProgress(n);updateKhatma();});
        reset.setOnClickListener(v->{prefs.edit().putInt("khatma_page",0).apply();page.setProgress(0);updateKhatma();});updateKhatma();

        LinearLayout adhkar=card("أذكار وأدعية رمضانية");
        addDua(adhkar,"دعاء الإفطار","ذهب الظمأ وابتلت العروق وثبت الأجر إن شاء الله.");
        addDua(adhkar,"دعاء جامع","اللهم إنك عفو تحب العفو فاعف عني.");
        addDua(adhkar,"دعاء القبول","ربنا تقبل منا إنك أنت السميع العليم، وتب علينا إنك أنت التواب الرحيم.");
        addDua(adhkar,"ذكر يسير","سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر.");
        addDua(adhkar,"الصلاة على النبي ﷺ","اللهم صل وسلم وبارك على نبينا محمد.");

        LinearLayout settings=card("إعدادات الشهر والتنبيهات");
        Switch enabled=new Switch(this);enabled.setText("تفعيل تنبيهات رمضان");enabled.setTextColor(AppThemeColors.text(this));enabled.setChecked(prefs.getBoolean("enabled",false));settings.addView(enabled,full());
        TextView startLabel=text("",13,AppThemeColors.muted(this),false);settings.addView(startLabel,full());
        Button chooseStart=new Button(this);chooseStart.setText("تحديد أول يوم في رمضان");settings.addView(chooseStart,full());
        updateStartLabel(startLabel);
        chooseStart.setOnClickListener(v->{Calendar now=Calendar.getInstance();long saved=prefs.getLong("start_at",0L);if(saved>0)now.setTimeInMillis(saved);
            new DatePickerDialog(this,(picker,y,m,d)->{Calendar first=Calendar.getInstance();first.set(y,m,d,0,0,0);first.set(Calendar.MILLISECOND,0);prefs.edit().putLong("start_at",first.getTimeInMillis()).apply();updateStartLabel(startLabel);RamadanReminderReceiver.scheduleAll(this);},now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();});
        Switch iftarVoice=new Switch(this);iftarVoice.setText("تنبيه صوتي قبل المغرب");iftarVoice.setTextColor(AppThemeColors.text(this));iftarVoice.setChecked(prefs.getBoolean("iftar_voice_enabled",true));settings.addView(iftarVoice,full());
        Switch cannon=new Switch(this);cannon.setText("إطلاق صوت مدفع الإفطار مع المغرب");cannon.setTextColor(AppThemeColors.text(this));cannon.setChecked(prefs.getBoolean("cannon_enabled",true));settings.addView(cannon,full());
        Switch suhoor=new Switch(this);suhoor.setText("تنبيه المسحراتي قبل الفجر");suhoor.setTextColor(AppThemeColors.text(this));suhoor.setChecked(prefs.getBoolean("suhoor_enabled",true));settings.addView(suhoor,full());
        addMinutesControl(settings,"التنبيه قبل المغرب بدقائق","iftar_lead_minutes",5,1,30);
        addMinutesControl(settings,"تنبيه السحور قبل الفجر بدقائق","suhoor_lead_minutes",45,10,120);
        TextView note=text("تُحسب المواعيد من مواقيت المغرب والفجر المحفوظة في التطبيق. يلزم ضبط مواقيت الصلاة والسماح بالتنبيهات الدقيقة.",12,AppThemeColors.muted(this),false);note.setPadding(0,dp(7),0,dp(7));settings.addView(note,full());
        enabled.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean("enabled",v).apply();RamadanReminderReceiver.scheduleAll(this);});
        iftarVoice.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean("iftar_voice_enabled",v).apply();RamadanReminderReceiver.scheduleAll(this);});
        cannon.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean("cannon_enabled",v).apply();RamadanReminderReceiver.scheduleAll(this);});
        suhoor.setOnCheckedChangeListener((b,v)->{prefs.edit().putBoolean("suhoor_enabled",v).apply();RamadanReminderReceiver.scheduleAll(this);});

        LinearLayout audio=card("الأصوات المحفوظة على الهاتف");
        audioStatus=text("أضف تسجيلًا صوتيًا بصيغة MP3 لكل تنبيه؛ سيُحفظ داخل التطبيق ليعمل دون إنترنت.",12,AppThemeColors.muted(this),false);audio.addView(audioStatus,full());
        addAudioPicker(audio,"اختيار تسجيل تنبيه الإفطار","iftar_voice.mp3",PICK_IFTAR);
        addAudioPicker(audio,"اختيار صوت مدفع الإفطار","iftar_cannon.mp3",PICK_CANNON);
        addAudioPicker(audio,"اختيار تسجيل المسحراتي","mesaharaty.mp3",PICK_SUHOOR);
        setContentView(AppTabs.wrap(this,scroll,"ramadan"));
    }
    private void updateStartLabel(TextView label){long at=prefs.getLong("start_at",0L);if(at<=0){label.setText("لم يتم تحديد بداية الشهر بعد؛ اختر أول يوم حتى تتوقف التنبيهات تلقائيًا بعد 30 يومًا.");return;}Calendar c=Calendar.getInstance();c.setTimeInMillis(at);label.setText("بداية الشهر المحددة: "+c.get(Calendar.DAY_OF_MONTH)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR)+" — تنتهي التنبيهات تلقائيًا بعد 30 يومًا.");}
    private void addDua(LinearLayout box,String heading,String body){TextView h=text(heading,14,AppThemeColors.accent(this),true);h.setPadding(0,dp(9),0,dp(2));box.addView(h,full());box.addView(text(body,14,AppThemeColors.text(this),false),full());}
    private void updateFastCount(){int n=0;for(int i=1;i<=30;i++)if(prefs.getBoolean("fast_"+i,false))n++;if(fastCount!=null)fastCount.setText("أيام الصيام المسجلة: "+n+" من 30");}
    private void updateKhatma(){int n=prefs.getInt("khatma_page",0);if(khatmaProgress!=null)khatmaProgress.setText("أنجزت "+n+" من 604 صفحة — "+Math.round(n*100f/604)+"٪");}
    private void addMinutesControl(LinearLayout box,String title,String key,int def,int min,int max){
        box.addView(text(title,13,AppThemeColors.text(this),false),full());TextView value=text(prefs.getInt(key,def)+" دقيقة",13,AppThemeColors.accent(this),true);box.addView(value,full());
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(Math.max(0,Math.min(max-min,prefs.getInt(key,def)-min)));box.addView(bar,full());
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){int n=p+min;value.setText(n+" دقيقة");if(u){prefs.edit().putInt(key,n).apply();RamadanReminderReceiver.scheduleAll(RamadanActivity.this);}}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
    }
    private void addAudioPicker(LinearLayout box,String label,String file,int request){
        Button b=new Button(this);b.setText(label);box.addView(b,full());b.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,request);});
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
        String name=request==PICK_IFTAR?"iftar_voice.mp3":request==PICK_CANNON?"iftar_cannon.mp3":"mesaharaty.mp3";
        try{
            File dir=new File(getFilesDir(),"ramadan_audio");if(!dir.exists()&&!dir.mkdirs())throw new Exception("تعذر إنشاء مجلد الأصوات");
            Uri uri=data.getData();try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(new File(dir,name))){
                if(in==null)throw new Exception("تعذر فتح التسجيل");byte[] buf=new byte[8192];int len;while((len=in.read(buf))!=-1)out.write(buf,0,len);
            }
            Toast.makeText(this,"تم حفظ التسجيل داخل التطبيق ويمكن تشغيله دون إنترنت.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"تعذر حفظ التسجيل: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    @Override protected void onResume(){super.onResume();if(prefs!=null&&prefs.getBoolean("enabled",false))RamadanReminderReceiver.scheduleAll(this);}
}
