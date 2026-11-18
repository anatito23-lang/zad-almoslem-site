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
   ['morning','qiyam'].forEach(k=>{const v=read(k);const t=$(k+'ReminderTime'),e=$(k+'ReminderEnabled');if(t)t.value=v.time;if(e)e.checked=!!v.enabled;});\n   const adhanToggle=$('adhanEnabled');\n   const adhanSaved=localStorage.getItem('zad_adhan_enabled');\n   if(adhanToggle) adhanToggle.checked=adhanSaved===null?true:adhanSaved==='true';\n   if(window.ZadAndroid && adhanToggle) window.ZadAndroid.setAdhanEnabled(adhanToggle.checked);
   const saveBtn=$('saveDailyReminders');
   if(saveBtn)saveBtn.addEventListener('click',async()=>{
     const vals={morning:{enabled:$('morningReminderEnabled').checked,time:$('morningReminderTime').value,title:defaults.morning.title},qiyam:{enabled:$('qiyamReminderEnabled').checked,time:$('qiyamReminderTime').value,title:defaults.qiyam.title}};
     if(!vals.morning.time||!vals.qiyam.time){$('dailyReminderStatus').textContent='اختر وقتًا صحيحًا لكل تنبيه.';return;}
     Object.keys(vals).forEach(k=>{save(k,vals[k]);schedule(k,vals[k]);});\n     const adhanOn=$('adhanEnabled').checked;\n     localStorage.setItem('zad_adhan_enabled',String(adhanOn));\n     if(window.ZadAndroid) window.ZadAndroid.setAdhanEnabled(adhanOn);
     if(window.ZadAndroid){window.ZadAndroid.requestNotificationPermission();}
     else if('Notification' in window && Notification.permission==='default'){try{await Notification.requestPermission()}catch(e){}}
     $('dailyReminderStatus').textContent=window.ZadAndroid?'تم حفظ الإعدادات وجدولة التنبيهات على التطبيق.':'تم حفظ الإعدادات على هذا الجهاز. للتنبيه والتطبيق مغلق يلزم استخدام نسخة أندرويد التي تدعم التنبيهات.';
   });
   // Keep the selected city and prayer alarm schedule in sync with the existing prayer-times API.
   const originalFetch=window.fetch.bind(window);
   window.fetch=function(input,init){
     return originalFetch(input,init).then(response=>{
       const url=typeof input==='string'?input:(input&&input.url)||'';
       if(url.includes('api.aladhan.com/v1/timingsByCity')){
         response.clone().json().then(data=>{
           if(!data||data.code!==200||!data.data||!data.data.timings)return;
           const t=data.data.timings,now=new Date(),names=[['الفجر','Fajr'],['الظهر','Dhuhr'],['العصر','Asr'],['المغرب','Maghrib'],['العشاء','Isha']];
           if(!$('qiyamReminderTime').dataset.userEdited){
             const fp=t.Fajr.split(' ')[0].split(':').map(Number),d=new Date(now.getFullYear(),now.getMonth(),now.getDate(),fp[0],fp[1]-30);
             $('qiyamReminderTime').value=String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0');
           }
           if(window.ZadAndroid){
             names.forEach(([name,key])=>{const a=t[key].split(' ')[0].split(':').map(Number);const at=new Date(now.getFullYear(),now.getMonth(),now.getDate(),a[0],a[1]).getTime();if(at>Date.now())window.ZadAndroid.schedulePrayer(name,at);});
           }
         }).catch(()=>{});
       }
       return response;
     });
   };
   $('qiyamReminderTime').addEventListener('input',()=>{$('qiyamReminderTime').dataset.userEdited='1'});
   // Reapply stored daily schedules whenever the app is opened.
   ['morning','qiyam'].forEach(k=>{const stored=localStorage.getItem(keys[k]);if(k==='morning'||stored)schedule(k,read(k));});
 }
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();