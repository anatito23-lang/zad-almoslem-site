(function(){
 const $=id=>document.getElementById(id);
 const keys={morning:'zad_morning_reminder',evening:'zad_evening_reminder',qiyam:'zad_qiyam_reminder'};
 const defaults={morning:{enabled:true,time:'07:00',title:'حان وقت أذكار الصباح'},evening:{enabled:true,time:'17:00',title:'حان وقت أذكار المساء'},qiyam:{enabled:true,time:'04:00',title:'حان وقت قيام الليل، لا تنسَ نصيبك من صلاة الليل'}};
 function read(k){try{return JSON.parse(localStorage.getItem(keys[k]))||defaults[k]}catch(e){return defaults[k]}}
 function save(k,v){localStorage.setItem(keys[k],JSON.stringify(v))}
 function schedule(k,v){
   if(!window.ZadAndroid)return;
   if(v.enabled){const p=v.time.split(':').map(Number);window.ZadAndroid.scheduleDailyReminder(k,p[0],p[1],defaults[k].title)}
   else window.ZadAndroid.cancelDailyReminder(k);
 }
 let reminderAudioDownloadPending=false;
 function ensureReminderAudioDownloaded(){
   if(!window.ZadAndroid||reminderAudioDownloadPending)return;
   if(window.ZadAndroid.isReminderAudioDownloaded()){
     const status=$('dailyReminderStatus');
     if(status)status.textContent='ملفات أذكار الصباح والمساء ودعاء الشعراوي محفوظة على الجهاز وتعمل دون إنترنت.';
     return;
   }
   reminderAudioDownloadPending=true;
   window.onReminderAudioDownload=function(ok,details){
     reminderAudioDownloadPending=false;
     const status=$('dailyReminderStatus');
     if(status)status.textContent=ok
       ?'تم التأكد من حفظ ملفات الأذكار ودعاء الشعراوي للعمل دون إنترنت.'
       :'تعذر تنزيل بعض الملفات. '+(details||'افحص اتصال الإنترنت ثم اضغط حفظ الإعدادات لإعادة المحاولة.');
   };
   window.ZadAndroid.downloadReminderAudio();
 }
 function init(){
   const city=$('city'); if(city){const saved=localStorage.getItem('zad_selected_city');if(saved&&[...city.options].some(o=>o.value===saved))city.value=saved;city.addEventListener('change',()=>localStorage.setItem('zad_selected_city',city.value));}
   ['morning','evening','qiyam'].forEach(k=>{const v=read(k);const t=$(k+'ReminderTime'),e=$(k+'ReminderEnabled');if(t)t.value=v.time;if(e)e.checked=!!v.enabled;});
   const adhanToggle=$('adhanEnabled');
   const adhanSaved=localStorage.getItem('zad_adhan_enabled');
   if(adhanToggle) adhanToggle.checked=adhanSaved===null?true:adhanSaved==='true';
   if(window.ZadAndroid && adhanToggle) window.ZadAndroid.setAdhanEnabled(adhanToggle.checked);
   const duaAfterAdhan=$('duaAfterAdhanEnabled');
   const savedDuaAfterAdhan=localStorage.getItem('zad_dua_after_adhan_enabled');
   if(duaAfterAdhan)duaAfterAdhan.checked=savedDuaAfterAdhan===null?true:savedDuaAfterAdhan==='true';
   if(window.ZadAndroid&&duaAfterAdhan)window.ZadAndroid.setDuaAfterAdhanEnabled(duaAfterAdhan.checked);
   if(duaAfterAdhan)duaAfterAdhan.addEventListener('change',()=>{
     localStorage.setItem('zad_dua_after_adhan_enabled',String(duaAfterAdhan.checked));
     if(window.ZadAndroid)window.ZadAndroid.setDuaAfterAdhanEnabled(duaAfterAdhan.checked);
   });

   // Adhan voice and per-prayer controls.
   const voice=$('adhanVoice'), preview=$('adhanPreview'), previewBtn=$('previewAdhan');
   const adhanSources={basit:'https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/041--.mp3',mustafa:'https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/068-.mp3',husary:'https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/044--.mp3',rifaat:'https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/042--.mp3',banna:'https://archive.org/download/90---azan---90---azan--many----sound----mp3---alazan/043--.mp3',naqshbandi:'https://cdn1.esm3.com//music/2603/esm3.com.198098.mp3'};
   const storedVoice=localStorage.getItem('zad_adhan_voice');
   const supportedVoices=['basit','husary','mustafa','rifaat','banna','naqshbandi','tobar','haram'];
   const savedVoice=supportedVoices.includes(storedVoice)?storedVoice:'basit';
   if(voice)voice.value=savedVoice;
   const volumeControl=$('adhanVolume'), volumeValue=$('adhanVolumeValue');
   const storedVolume=localStorage.getItem('zad_adhan_volume');
   const savedVolume=Math.max(0,Math.min(100,storedVolume===null?100:parseInt(storedVolume,10)||0));
   if(volumeControl)volumeControl.value=String(savedVolume);
   if(volumeValue)volumeValue.textContent=savedVolume+'%';
   function applyAdhanVolume(){
     const level=volumeControl?Math.max(0,Math.min(100,parseInt(volumeControl.value,10)||0)):savedVolume;
     if(volumeValue)volumeValue.textContent=level+'%';
     localStorage.setItem('zad_adhan_volume',String(level));
     if(preview)preview.volume=level/100;
     if(window.ZadAndroid&&typeof window.ZadAndroid.setAdhanVolume==='function')window.ZadAndroid.setAdhanVolume(level);
   }
   if(volumeControl)volumeControl.addEventListener('input',applyAdhanVolume);
   applyAdhanVolume();
   if(storedVoice!==savedVoice)localStorage.setItem('zad_adhan_voice',savedVoice);
   const modeControl=$('adhanMode'), approachControl=$('approachAlertEnabled'), approachMinutes=$('approachAlertMinutes');
   const savedMode=localStorage.getItem('zad_adhan_mode')||'adhan';
   if(modeControl)modeControl.value=['adhan','tone','vibrate','off'].includes(savedMode)?savedMode:'adhan';
   if(approachControl)approachControl.checked=localStorage.getItem('zad_approach_enabled')!=='false';
   if(approachMinutes)approachMinutes.value=String(Math.max(1,Math.min(60,parseInt(localStorage.getItem('zad_approach_minutes')||'15',10)||15)));
   const adhanOn=localStorage.getItem('zad_adhan_enabled');
   if($('adhanEnabled'))$('adhanEnabled').checked=adhanOn===null?true:adhanOn==='true';
   document.querySelectorAll('.prayer-alert').forEach(el=>{const v=localStorage.getItem('zad_prayer_alert_'+el.dataset.prayer);el.checked=v===null?true:v==='true';});
   const downloadBtn=$('downloadAdhan');
   function updateAdhanDownloadState(){
     if(!window.ZadAndroid||!voice||!downloadBtn)return;
     const ready=window.ZadAndroid.isAdhanDownloaded(voice.value);
     downloadBtn.textContent=ready?'الصوت محفوظ ويعمل دون إنترنت ✓':'تنزيل الصوت للعمل دون إنترنت';
     if(ready)$('adhanSettingsStatus').textContent='التسجيل محفوظ على هذا الجهاز ويعمل دون إنترنت.';
   }
   window.onAdhanDownloadComplete=function(which,ok,reason){
     if(!voice||which!==voice.value)return;
     $('adhanSettingsStatus').textContent=ok?'اكتمل التحميل. أصبح الأذان محفوظًا على الجهاز ويعمل دون إنترنت.':'تعذر تحميل التسجيل: '+(reason||'تحقق من الإنترنت وحاول مرة أخرى.');
     if(downloadBtn)downloadBtn.disabled=false;
     updateAdhanDownloadState();
   };
   if(downloadBtn)downloadBtn.addEventListener('click',()=>{
     if(!window.ZadAndroid){$('adhanSettingsStatus').textContent='تنزيل صوت الأذان للعمل دون إنترنت متاح داخل تطبيق أندرويد فقط.';return;}
     if(['tobar','haram'].includes(voice.value)){ $('adhanSettingsStatus').textContent='هذا التسجيل يحتاج إضافته من ملف صوتي عبر خانة «إضافة تسجيل المؤذن من جهازك».'; return; }
     downloadBtn.disabled=true;
     $('adhanSettingsStatus').textContent='جارٍ تنزيل صوت الأذان...';
     window.ZadAndroid.downloadAdhan(voice.value);
   });
   if(previewBtn)previewBtn.addEventListener('click',()=>{
     if(window.ZadAndroid){
       if(window.ZadAndroid.isAdhanDownloaded(voice.value)){window.ZadAndroid.previewAdhan(voice.value);$('adhanSettingsStatus').textContent='يتم تشغيل معاينة الأذان من الملف المحفوظ على الجهاز.';}
       else $('adhanSettingsStatus').textContent='حمّل صوت الأذان أولًا ليعمل من داخل التطبيق دون إنترنت.';
       return;
     }
     preview.src=adhanSources[voice.value]||adhanSources.basit;
     preview.volume=volumeControl?Math.max(0,Math.min(100,parseInt(volumeControl.value,10)||0))/100:savedVolume/100;
     preview.play().catch(()=>{$('adhanSettingsStatus').textContent='اضغط مرة أخرى للسماح بتشغيل الصوت.';});
   });
   if(voice)voice.addEventListener('change',()=>{preview.pause();preview.currentTime=0;updateAdhanDownloadState();});
   const importFile=$('adhanImportFile'), importStatus=$('adhanImportStatus');
   if(importFile)importFile.addEventListener('change',()=>{
     const file=importFile.files&&importFile.files[0]; if(!file)return;
     if(!file.type.startsWith('audio/')&&!/\.(mp3|ogg|m4a|wav)$/i.test(file.name)){if(importStatus)importStatus.textContent='اختَر ملفًا صوتيًا صالحًا.';return;}
     if(file.size>20*1024*1024){if(importStatus)importStatus.textContent='حجم التسجيل أكبر من 20 ميجابايت.';return;}
     if(!window.ZadAndroid||typeof window.ZadAndroid.saveAdhanAudio!=='function'){if(importStatus)importStatus.textContent='إضافة التسجيلات المحفوظة متاحة داخل تطبيق أندرويد.';return;}
     if(importStatus)importStatus.textContent='جارٍ حفظ التسجيل داخل التطبيق...';
     const reader=new FileReader();
     reader.onload=()=>{
       try{
         const encoded=String(reader.result||'').split(',')[1]||'';
         const ok=window.ZadAndroid.saveAdhanAudio(voice.value,encoded);
         if(importStatus)importStatus.textContent=ok?'تم حفظ تسجيل '+voice.options[voice.selectedIndex].text+' على الجهاز ويعمل دون إنترنت.':'تعذر حفظ الملف؛ تأكد أنه تسجيل صوتي صالح وأقل من 20 ميجابايت.';
         updateAdhanDownloadState();
       }catch(e){if(importStatus)importStatus.textContent='تعذر حفظ التسجيل على الجهاز.';}
     };
     reader.onerror=()=>{if(importStatus)importStatus.textContent='تعذر قراءة الملف.';};
     reader.readAsDataURL(file);
   });
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
     const enabled=$('adhanEnabled').checked, selected=voice.value;
     applyAdhanVolume();
     const selectedMode=modeControl?modeControl.value:'adhan';
     const approachOn=approachControl?approachControl.checked:true;
     const approachMins=approachMinutes?Math.max(1,Math.min(60,parseInt(approachMinutes.value,10)||15)):15;
     localStorage.setItem('zad_adhan_mode',selectedMode);
     localStorage.setItem('zad_approach_enabled',String(approachOn));
     localStorage.setItem('zad_approach_minutes',String(approachMins));
     if(window.ZadAndroid){
       window.ZadAndroid.setAdhanMode(selectedMode);
       window.ZadAndroid.setApproachAlertEnabled(approachOn);
       window.ZadAndroid.setApproachAlertMinutes(approachMins);
     }
     localStorage.setItem('zad_adhan_enabled',String(enabled));
     localStorage.setItem('zad_adhan_voice',selected);
     if(window.ZadAndroid){window.ZadAndroid.setAdhanEnabled(enabled);window.ZadAndroid.setAdhanVoice(selected);}
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
     window.ZadAndroid.setAdhanMode(modeControl?modeControl.value:'adhan');
     window.ZadAndroid.setApproachAlertEnabled(approachControl?approachControl.checked:true);
     window.ZadAndroid.setApproachAlertMinutes(approachMinutes?Math.max(1,Math.min(60,parseInt(approachMinutes.value,10)||15)):15);
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
     const vals={morning:{enabled:$('morningReminderEnabled').checked,time:$('morningReminderTime').value,title:defaults.morning.title},evening:{enabled:$('eveningReminderEnabled').checked,time:$('eveningReminderTime').value,title:defaults.evening.title},qiyam:{enabled:$('qiyamReminderEnabled').checked,time:$('qiyamReminderTime').value,title:defaults.qiyam.title}};
     if(!vals.morning.time||!vals.evening.time||!vals.qiyam.time){$('dailyReminderStatus').textContent='اختر وقتًا صحيحًا لكل تنبيه.';return;}
     Object.keys(vals).forEach(k=>{save(k,vals[k]);schedule(k,vals[k]);});
     const anyReminderEnabled=Object.values(vals).some(v=>v.enabled);
     if(window.ZadAndroid&&anyReminderEnabled){
       $('dailyReminderStatus').textContent='تم حفظ مواعيد التذكير. جارٍ التأكد من ملفات الصوت...';
       ensureReminderAudioDownloaded();
     }
     const duaControl=$('duaAfterAdhanEnabled');
     if(duaControl){
       localStorage.setItem('zad_dua_after_adhan_enabled',String(duaControl.checked));
       if(window.ZadAndroid)window.ZadAndroid.setDuaAfterAdhanEnabled(duaControl.checked);
     }
     const adhanControl=$('adhanEnabled');
     if(adhanControl){
       const adhanOn=adhanControl.checked;
       localStorage.setItem('zad_adhan_enabled',String(adhanOn));
       if(window.ZadAndroid) window.ZadAndroid.setAdhanEnabled(adhanOn);
     }
     if(window.ZadAndroid){window.ZadAndroid.requestExactAlarmPermission();window.ZadAndroid.requestNotificationPermission();}
     else if('Notification' in window && Notification.permission==='default'){try{await Notification.requestPermission()}catch(e){}}
     if(!window.ZadAndroid)$('dailyReminderStatus').textContent='تم حفظ الإعدادات على هذا الجهاز. للتنبيه والتطبيق مغلق يلزم استخدام نسخة أندرويد.';
     else if(!anyReminderEnabled)$('dailyReminderStatus').textContent='تم حفظ الإعدادات وإيقاف كل التذكيرات اليومية.';
     else if(!reminderAudioDownloadPending&&window.ZadAndroid.isReminderAudioDownloaded())$('dailyReminderStatus').textContent='تم حفظ الإعدادات، وملفات الصوت موجودة على الجهاز.';
   });
   $('qiyamReminderTime').addEventListener('input',()=>{$('qiyamReminderTime').dataset.userEdited='1'});
   // Reapply stored daily schedules whenever the app is opened.
   ['morning','evening','qiyam'].forEach(k=>{const stored=localStorage.getItem(keys[k]);if(k==='morning'||k==='evening'||stored)schedule(k,read(k));});
   if(window.ZadAndroid)ensureReminderAudioDownloaded();
 }
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();