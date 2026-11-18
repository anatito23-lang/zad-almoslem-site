(function(){
 const $=id=>document.getElementById(id);
 const keys={morning:'zad_morning_reminder',qiyam:'zad_qiyam_reminder'};
 const defaults={morning:{enabled:true,time:'07:00',title:'حان وقت أذكار الصباح'},qiyam:{enabled:true,time:'04:00',title:'حان وقت قيام الليل'}};
 function read(k){try{return JSON.parse(localStorage.getItem(keys[k]))||defaults[k]}catch(e){return defaults[k]}}
 function save(k,v){localStorage.setItem(keys[k],JSON.stringify(v))}
 function schedule(k,v){
   if(!window.ZadAndroid)return;
   if(v.enabled){const p=v.time.split(':').map(Number);window.ZadAndroid.scheduleDailyReminder(k,p[0],p[1],defaults[k].title)}
   else window.ZadAndroid.cancelDailyReminder(k);
 }
 function init(){
   const city=$('city'); if(city){const saved=localStorage.getItem('zad_selected_city');if(saved&&[...city.options].some(o=>o.value===saved))city.value=saved;city.addEventListener('change',()=>localStorage.setItem('zad_selected_city',city.value));}
   ['morning','qiyam'].forEach(k=>{const v=read(k);const t=$(k+'ReminderTime'),e=$(k+'ReminderEnabled');if(t)t.value=v.time;if(e)e.checked=!!v.enabled;});
   const adhanToggle=$('adhanEnabled');
   const adhanSaved=localStorage.getItem('zad_adhan_enabled');
   if(adhanToggle) adhanToggle.checked=adhanSaved===null?true:adhanSaved==='true';
   if(window.ZadAndroid && adhanToggle) window.ZadAndroid.setAdhanEnabled(adhanToggle.checked);

   // Adhan voice and per-prayer controls.
   const voice=$('adhanVoice'), preview=$('adhanPreview'), previewBtn=$('previewAdhan');
   const alertMode=$('adhanAlertMode');
   const savedMode=localStorage.getItem('zad_adhan_mode')||'adhan';
   if(alertMode)alertMode.value=savedMode;
   if(window.ZadAndroid && typeof window.ZadAndroid.setAdhanMode==='function') window.ZadAndroid.setAdhanMode(savedMode);
   const adhanSources={sabah:'https://upload.wikimedia.org/wikipedia/commons/e/e7/Adhan.ogg',beautiful:'https://upload.wikimedia.org/wikipedia/commons/b/b0/Beautiful_adhan.ogg'};
   const importedVoices=['mustafa','fajr','banna','basit'];
   const importFile=$('adhanImportFile'); let localPreviewUrl=null;
   const savedVoice=localStorage.getItem('zad_adhan_voice')||'sabah';
   if(voice)voice.value=savedVoice;
   const adhanOn=localStorage.getItem('zad_adhan_enabled');
   if($('adhanEnabled'))$('adhanEnabled').checked=adhanOn===null?true:adhanOn==='true';
   document.querySelectorAll('.prayer-alert').forEach(el=>{const v=localStorage.getItem('zad_prayer_alert_'+el.dataset.prayer);el.checked=v===null?true:v==='true';});
   const downloadBtn=$('downloadAdhan');
   function updateAdhanDownloadState(){
     if(!window.ZadAndroid||!voice||!downloadBtn)return;
     const ready=window.ZadAndroid.isAdhanDownloaded(voice.value);
     downloadBtn.textContent=ready?'التسجيل محفوظ ✓ — استبدال التسجيل':'اختيار ملف التسجيل من الهاتف';
     if(ready)$('adhanSettingsStatus').textContent='التسجيل محفوظ على هذا الجهاز ويعمل دون إنترنت.';
   }
   window.onAdhanDownloadComplete=function(which,ok,reason){
     if(!voice||which!==voice.value)return;
     $('adhanSettingsStatus').textContent=ok?'اكتمل التحميل. أصبح الأذان محفوظًا على الجهاز ويعمل دون إنترنت.':'تعذر تحميل التسجيل: '+(reason||'تحقق من الإنترنت وحاول مرة أخرى.');
     if(downloadBtn)downloadBtn.disabled=false;
     updateAdhanDownloadState();
   };
   if(downloadBtn)downloadBtn.addEventListener('click',()=>{
     if(importedVoices.includes(voice.value)){
       if(importFile)importFile.click();
       else $('adhanSettingsStatus').textContent='اختر ملف MP3 من جهازك لاستيراده.';
       return;
     }
     if(!window.ZadAndroid){$('adhanSettingsStatus').textContent='التنزيل دون إنترنت متاح داخل تطبيق أندرويد فقط.';return;}
     downloadBtn.disabled=true;
     $('adhanSettingsStatus').textContent='جارٍ تحميل الصوت العام...';
     window.ZadAndroid.downloadAdhan(voice.value);
   });
   if(importFile)importFile.addEventListener('change',()=>{
     const file=importFile.files&&importFile.files[0]; if(!file)return;
     if(!/^audio\//i.test(file.type)&&!file.name.toLowerCase().endsWith('.mp3')&&!file.name.toLowerCase().endsWith('.ogg')){
       $('adhanSettingsStatus').textContent='اختر ملفًا صوتيًا بصيغة MP3 أو OGG.'; importFile.value=''; return;
     }
     if(file.size>8*1024*1024){$('adhanSettingsStatus').textContent='حجم التسجيل أكبر من 8 ميجابايت.';importFile.value='';return;}
     if(localPreviewUrl)URL.revokeObjectURL(localPreviewUrl);
     localPreviewUrl=URL.createObjectURL(file); preview.src=localPreviewUrl;
     const reader=new FileReader();
     reader.onload=()=>{
       const data=String(reader.result||'').split(',')[1]||'';
       if(window.ZadAndroid&&typeof window.ZadAndroid.saveAdhanAudio==='function'){
         const ok=window.ZadAndroid.saveAdhanAudio(voice.value,data);
         $('adhanSettingsStatus').textContent=ok?'تم حفظ تسجيل '+voice.options[voice.selectedIndex].text+' على الجهاز.':'تعذر حفظ التسجيل؛ حاول مرة أخرى.';
         updateAdhanDownloadState();
       }else{
         $('adhanSettingsStatus').textContent='تم تجهيز التسجيل للمعاينة في المتصفح. الحفظ الدائم والتشغيل عند إغلاق التطبيق يتطلبان استيراده من تطبيق أندرويد.';
         preview.play().catch(()=>{});
       }
       importFile.value='';
     };
     reader.onerror=()=>{$('adhanSettingsStatus').textContent='تعذر قراءة الملف الصوتي.';importFile.value='';};
     reader.readAsDataURL(file);
   });
   if(previewBtn)previewBtn.addEventListener('click',()=>{
     if(window.ZadAndroid){
       if(window.ZadAndroid.isAdhanDownloaded(voice.value)){window.ZadAndroid.previewAdhan(voice.value);$('adhanSettingsStatus').textContent='يتم تشغيل معاينة الأذان من الملف المحفوظ على الجهاز.';}
       else $('adhanSettingsStatus').textContent='حمّل صوت الأذان أولًا ليعمل من داخل التطبيق دون إنترنت.';
       return;
     }
     if(importedVoices.includes(voice.value)&&localPreviewUrl){preview.src=localPreviewUrl;}
     else preview.src=adhanSources[voice.value]||adhanSources.sabah;
     preview.play().catch(()=>{$('adhanSettingsStatus').textContent='اضغط مرة أخرى للسماح بتشغيل الصوت.';});
   });
   if(voice)voice.addEventListener('change',()=>{preview.pause();preview.currentTime=0;if(importedVoices.includes(voice.value))downloadBtn.textContent='اختيار ملف التسجيل من الهاتف';else updateAdhanDownloadState();});
   updateAdhanDownloadState();
   document.querySelectorAll('.prayer-silent').forEach(el=>{
     el.checked=localStorage.getItem('zad_prayer_silent_'+el.dataset.prayer)==='true';
   });
   const silentStatus=$('silentSettingsStatus');
   const permissionBtn=$('grantSilentPermission');
   if(permissionBtn)permissionBtn.addEventListener('click',()=>{
     if(!window.ZadAndroid){if(silentStatus)silentStatus.textContent='خاصية الصامت التلقائي متاحة داخل تطبيق أندرويد فقط.';return;}
     window.ZadAndroid.requestSilentPermission();
     if(silentStatus)silentStatus.textContent=window.ZadAndroid.hasSilentPermission()?'الإذن متاح على هذا الجهاز.':'افتح إعدادات أندرويد واسمح لتطبيق زاد المسلم بالتحكم في وضع عدم الإزعاج.';
   });
   if(window.ZadAndroid&&silentStatus) silentStatus.textContent=window.ZadAndroid.hasSilentPermission()?'الإذن متاح. اختر الصلوات ثم احفظ.':'لم يُمنح إذن التحكم في وضع عدم الإزعاج بعد.';
   const saveAdhan=$('saveAdhanSettings');
   if(saveAdhan)saveAdhan.addEventListener('click',()=>{
     const enabled=$('adhanEnabled').checked, selected=voice.value, mode=alertMode?alertMode.value:'adhan';
     localStorage.setItem('zad_adhan_mode',mode);
     localStorage.setItem('zad_adhan_enabled',String(enabled));
     localStorage.setItem('zad_adhan_voice',selected);
     if(window.ZadAndroid){window.ZadAndroid.setAdhanEnabled(enabled);window.ZadAndroid.setAdhanVoice(selected);if(typeof window.ZadAndroid.setAdhanMode==='function')window.ZadAndroid.setAdhanMode(mode);}
     document.querySelectorAll('.prayer-alert').forEach(el=>{
       localStorage.setItem('zad_prayer_alert_'+el.dataset.prayer,String(el.checked));
       if(window.ZadAndroid)window.ZadAndroid.setPrayerAdhanEnabled(el.dataset.prayer,el.checked);
     });
     document.querySelectorAll('.prayer-silent').forEach(el=>{
       localStorage.setItem('zad_prayer_silent_'+el.dataset.prayer,String(el.checked));
       if(window.ZadAndroid)window.ZadAndroid.setPrayerSilentEnabled(el.dataset.prayer,el.checked);
     });
     $('adhanSettingsStatus').textContent='تم حفظ إعدادات الأذان والصامت على هذا الجهاز.';
     if(silentStatus&&window.ZadAndroid) silentStatus.textContent=window.ZadAndroid.hasSilentPermission()?'تم حفظ الصلوات المختارة. سيعمل الصامت 15 دقيقة بعد دخول وقت الصلاة.':'تم حفظ الاختيارات، لكن يجب منح إذن التحكم في وضع عدم الإزعاج أولًا.';
   });
   if(window.ZadAndroid){
     window.ZadAndroid.setAdhanEnabled(!($('adhanEnabled'))||$('adhanEnabled').checked);
     window.ZadAndroid.setAdhanVoice(savedVoice);
     document.querySelectorAll('.prayer-alert').forEach(el=>window.ZadAndroid.setPrayerAdhanEnabled(el.dataset.prayer,el.checked));
   }
   // Daily prayer completion tracker, resets automatically on a new date.
   const todayKey='zad_prayer_tracker_'+new Date().toISOString().slice(0,10);
   let done={};try{done=JSON.parse(localStorage.getItem(todayKey)||'{}')}catch(e){}
   const updateTracker=()=>{const boxes=[...document.querySelectorAll('.prayer-done')];boxes.forEach(el=>el.checked=!!done[el.dataset.prayer]);$('prayerTrackerCount').textContent=boxes.filter(el=>el.checked).length+' / 5';};
   document.querySelectorAll('.prayer-done').forEach(el=>el.addEventListener('change',()=>{done[el.dataset.prayer]=el.checked;localStorage.setItem(todayKey,JSON.stringify(done));updateTracker();}));
   updateTracker();

   const saveBtn=$('saveDailyReminders');
   if(saveBtn)saveBtn.addEventListener('click',async()=>{
     const vals={morning:{enabled:$('morningReminderEnabled').checked,time:$('morningReminderTime').value,title:defaults.morning.title},qiyam:{enabled:$('qiyamReminderEnabled').checked,time:$('qiyamReminderTime').value,title:defaults.qiyam.title}};
     if(!vals.morning.time||!vals.qiyam.time){$('dailyReminderStatus').textContent='اختر وقتًا صحيحًا لكل تنبيه.';return;}
     Object.keys(vals).forEach(k=>{save(k,vals[k]);schedule(k,vals[k]);});
     const adhanOn=$('adhanEnabled').checked;
     localStorage.setItem('zad_adhan_enabled',String(adhanOn));
     if(window.ZadAndroid) window.ZadAndroid.setAdhanEnabled(adhanOn);
     if(window.ZadAndroid){window.ZadAndroid.requestNotificationPermission();}
     else if('Notification' in window && Notification.permission==='default'){try{await Notification.requestPermission()}catch(e){}}
     $('dailyReminderStatus').textContent=window.ZadAndroid?'تم حفظ الإعدادات وجدولة التنبيهات على التطبيق.':'تم حفظ الإعدادات على هذا الجهاز. للتنبيه والتطبيق مغلق يلزم استخدام نسخة أندرويد التي تدعم التنبيهات.';
   });
   $('qiyamReminderTime').addEventListener('input',()=>{$('qiyamReminderTime').dataset.userEdited='1'});
   // Reapply stored daily schedules whenever the app is opened.
   ['morning','qiyam'].forEach(k=>{const stored=localStorage.getItem(keys[k]);if(k==='morning'||stored)schedule(k,read(k));});
 }
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();