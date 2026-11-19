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