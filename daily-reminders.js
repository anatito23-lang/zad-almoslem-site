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
   const adhanSources={
     doha:{
       الفجر:'https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_01_Fajr_Adhan.mp3',
       الظهر:'https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_02_Dhuhr_Adhan.mp3',
       العصر:'https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_03_Asr_Adhan.mp3',
       المغرب:'https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_04_Maghrib_Adhan.mp3',
       العشاء:'https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_05_Isha_Adhan.mp3'
     },
     basit:'https://cdn.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/Abdulbasit_Abdusamad_1_-_Egypt_(%D8%B9%D8%A8%D8%AF_%D8%A7%D9%84%D8%A8%D8%A7%D8%B3%D8%B7_%D8%B9%D8%A8%D8%AF_%D8%A7%D9%84%D8%B5%D9%85%D8%AF_-_%D9%85%D8%B5%D8%B1).mp3',
     mustafa:'https://cdn.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/Mustafa_Ismail_-_Egypt_(%D9%85%D8%B5%D8%B7%D9%81%D9%89_%D8%A5%D8%B3%D9%85%D8%A7%D8%B9%D9%8A%D9%84_-_%D9%85%D8%B5%D8%B1).mp3',
     husary:'https://cdn.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/Mahmoud_Khalil_Al_Hussary_-_Cairo_(%D9%85%D8%AD%D9%85%D9%88%D8%AF_%D8%AE%D9%84%D9%8A%D9%84_%D8%A7%D9%84%D8%AD%D8%B5%D8%B1%D9%8A_-_%D8%A7%D9%84%D9%82%D8%A7%D9%87%D8%B1%D8%A9).mp3',
     banna:'https://cdn.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/Mahmoud_Ali_Al_Banna_-_Cairo_(%D9%85%D8%AD%D9%85%D9%88%D8%AF_%D8%B9%D9%84%D9%8A_%D8%A7%D9%84%D8%A8%D9%86%D8%A7_-_%D8%A7%D9%84%D9%82%D8%A7%D9%87%D8%B1%D8%A9).mp3',
     haram:'https://cdn.jsdelivr.net/gh/Kiwifu/adhan-mp3@main/Ali_Ibn_Ahmad_Mala_1_-_Al_Haram_Al_Maki_(%D8%B9%D9%84%D9%8A_%D8%A8%D9%86_%D8%A3%D8%AD%D9%85%D8%AF_%D9%85%D9%84%D8%A7_-_%D8%A7%D9%84%D8%AD%D8%B1%D9%85_%D8%A7%D9%84%D9%85%D9%83%D9%8A).mp3',
   };
   function getAdhanUrls(voiceKey,prayer){
     const source=adhanSources[voiceKey];
     if(voiceKey==='doha')return source&&source[prayer]?[source[prayer]]:[];
     return typeof source==='string'?[source]:[];
   }
   const storedVoice=localStorage.getItem('zad_adhan_voice');
   const supportedVoices=['doha'];
   const savedVoice='doha';
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
     document.querySelectorAll('audio').forEach(audio=>{try{audio.volume=level/100;}catch(e){}});
     if(window.ZadAndroid&&typeof window.ZadAndroid.setAdhanVolume==='function')window.ZadAndroid.setAdhanVolume(level);
   }
   if(volumeControl)volumeControl.addEventListener('input',applyAdhanVolume);
   applyAdhanVolume();
   if(storedVoice!==savedVoice)localStorage.setItem('zad_adhan_voice',savedVoice);
   // Per-prayer adhan voices: stored locally and in Android preferences.
   const prayerVoiceDefaults={الفجر:'basit',الظهر:'basit',العصر:'mustafa',المغرب:'husary',العشاء:'banna'};
   const prayerVoiceSelects=[...document.querySelectorAll('.prayer-adhan-voice')];
   // Independent volume settings for each prayer, persisted on web and Android.
   const prayerVolumeControls=[...document.querySelectorAll('.prayer-adhan-volume')];
   function getPrayerVolume(prayer){
     const raw=localStorage.getItem('zad_adhan_volume_'+prayer);
     const parsed=raw===null?100:parseInt(raw,10);
     return Math.max(0,Math.min(100,Number.isFinite(parsed)?parsed:100));
   }
   prayerVolumeControls.forEach((control,index)=>{
     const prayer=control.dataset.prayer;
     const valueLabel=document.getElementById('adhanPrayerVolumeValue_'+index);
     const initial=getPrayerVolume(prayer);
     control.value=String(initial);
     if(valueLabel)valueLabel.textContent=initial+'%';
     if(window.ZadAndroid&&typeof window.ZadAndroid.setPrayerAdhanVolume==='function')window.ZadAndroid.setPrayerAdhanVolume(prayer,initial);
     control.addEventListener('input',()=>{
       const level=Math.max(0,Math.min(100,parseInt(control.value,10)||0));
       localStorage.setItem('zad_adhan_volume_'+prayer,String(level));
       if(valueLabel)valueLabel.textContent=level+'%';
       if(preview&&preview.dataset.prayer===prayer)preview.volume=level/100;
       if(window.ZadAndroid&&typeof window.ZadAndroid.setPrayerAdhanVolume==='function')window.ZadAndroid.setPrayerAdhanVolume(prayer,level);
     });
   });
   const prayerVoiceStatus=$('prayerAdhanDownloadStatus');
   const PRAYER_AUDIO_CACHE='zad-prayer-audio-v1';
   let prayerProgressBar=null,prayerProgressFill=null,prayerProgressText=null;
   function ensurePrayerProgress(){
     if(!prayerVoiceStatus)return null;
     if(!prayerProgressBar){
       prayerProgressBar=document.getElementById('zadPrayerDownloadProgress');
       prayerProgressText=document.getElementById('zadPrayerDownloadProgressText');
       prayerProgressFill=document.getElementById('zadPrayerDownloadProgressFill');
       if(!prayerProgressBar){
         prayerProgressBar=document.createElement('div');prayerProgressBar.id='zadPrayerDownloadProgress';
         prayerProgressBar.style.cssText='display:block;margin:10px 0 14px;padding:10px 12px;border:1px solid #d5e2da;border-radius:12px;background:#f7faf8;direction:rtl';
         prayerProgressText=document.createElement('div');prayerProgressText.id='zadPrayerDownloadProgressText';prayerProgressText.style.cssText='font-size:13px;margin-bottom:8px;color:#244b3d';
         prayerProgressText.textContent='جاهز لتنزيل التسجيلات — 0%';
         const track=document.createElement('div');track.style.cssText='height:12px;width:100%;border-radius:20px;overflow:hidden;background:#e5eee8;direction:ltr';
         prayerProgressFill=document.createElement('span');prayerProgressFill.id='zadPrayerDownloadProgressFill';prayerProgressFill.style.cssText='display:block;width:0;height:100%;border-radius:20px;background:#13866b;transition:width .15s ease';
         track.appendChild(prayerProgressFill);prayerProgressBar.append(prayerProgressText,track);
         prayerVoiceStatus.insertAdjacentElement('afterend',prayerProgressBar);
       }
     }
     return prayerProgressBar;
   }
   function updatePrayerProgress(percent,label){
     if(!ensurePrayerProgress())return;
     prayerProgressBar.style.display='block';
     if(prayerProgressText&&label)prayerProgressText.textContent=label+(percent===null||percent===undefined?'':' — '+Math.round(Math.max(0,Math.min(100,percent)))+'%');
     const value=percent===null||percent===undefined?0:Math.max(0,Math.min(100,percent));
     prayerProgressFill.style.animation='none';
     prayerProgressFill.style.transform='none';
     prayerProgressFill.style.width=value+'%';
     prayerProgressBar.setAttribute('role','progressbar');
     prayerProgressBar.setAttribute('aria-valuemin','0');
     prayerProgressBar.setAttribute('aria-valuemax','100');
     prayerProgressBar.setAttribute('aria-valuenow',String(value));
   }
   function finishPrayerProgress(label){
     updatePrayerProgress(100,label||'اكتمل حفظ التسجيلات داخل الموقع.');
     if(prayerProgressBar)prayerProgressBar.style.display='block';
   }
   window.onPrayerAudioDownloadProgress=function(index,total,percent,received,bytesTotal){
     const overall=percent===null||percent===undefined?null:Math.round(((index-1)+(percent/100))/total*100);
     updatePrayerProgress(overall,'تنزيل الملف '+index+' من '+total+(percent===null||percent===undefined?' — تم استلام '+(received/1048576).toFixed(2)+' ميجابايت':' — '+percent+'%'));
   };
   async function getCachedPrayerAudioUrl(url){
     try{
       const cache=await caches.open(PRAYER_AUDIO_CACHE);
       const saved=await cache.match(url);
       if(!saved)return url;
       const blob=await saved.blob();
       return URL.createObjectURL(blob);
     }catch(e){return url;}
   }
   prayerVoiceSelects.forEach(sel=>{
     const prayer=sel.dataset.prayer;
     const stored=localStorage.getItem('zad_adhan_voice_'+prayer)||prayerVoiceDefaults[prayer]||'basit';
     sel.value=[...sel.options].some(o=>o.value===stored)?stored:'basit';
     if(window.ZadAndroid&&typeof window.ZadAndroid.setPrayerAdhanVoice==='function')window.ZadAndroid.setPrayerAdhanVoice(prayer,sel.value);
     sel.addEventListener('change',()=>{
       localStorage.setItem('zad_adhan_voice_'+prayer,sel.value);
       if(window.ZadAndroid&&typeof window.ZadAndroid.setPrayerAdhanVoice==='function')window.ZadAndroid.setPrayerAdhanVoice(prayer,sel.value);
       if(prayerVoiceStatus)prayerVoiceStatus.textContent='تم اختيار '+sel.options[sel.selectedIndex].text+' لصلاة '+prayer+'. اضغط حفظ الإعدادات لتأكيد كل الاختيارات.';
     });
   });
   document.querySelectorAll('.prayer-adhan-preview').forEach(btn=>btn.addEventListener('click',()=>{
     const prayer=btn.dataset.prayer;
     const sel=prayerVoiceSelects.find(item=>item.dataset.prayer===prayer);
     if(!sel){if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذر تحديد صوت هذه الصلاة.';return;}
     const key=sel.value;
     if(!window.ZadAndroid){
       const urls=getAdhanUrls(key,prayer);
       if(!preview||!urls.length){if(prayerVoiceStatus)prayerVoiceStatus.textContent='مصدر معاينة هذا الصوت غير متاح حاليًا.';return;}
       preview.hidden=false;preview.controls=true;preview.dataset.prayer=prayer;
       preview.pause();
       if(preview.dataset.objectUrl){URL.revokeObjectURL(preview.dataset.objectUrl);delete preview.dataset.objectUrl;}
       preview.removeAttribute('src');
       getCachedPrayerAudioUrl(urls[0]).then(source=>{
         if(source.startsWith('blob:'))preview.dataset.objectUrl=source;
         preview.src=source;
         preview.volume=getPrayerVolume(prayer)/100;
         preview.load();
         if(prayerVoiceStatus)prayerVoiceStatus.textContent=(source.startsWith('blob:')?'تشغيل التسجيل المحفوظ داخل الموقع.':'تشغيل المعاينة من الإنترنت.')+' — '+sel.options[sel.selectedIndex].text+' لصلاة '+prayer+'.';
         const playing=preview.play();
         if(playing&&typeof playing.catch==='function')playing.catch(()=>{if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذر تشغيل المعاينة. تحقق من الاتصال وحاول مرة أخرى.';});
       });
       return;
     }
     if(typeof window.ZadAndroid.previewPrayerAdhan!=='function'){if(prayerVoiceStatus)prayerVoiceStatus.textContent='نسخة التطبيق الحالية لا تدعم معاينة المؤذن لكل صلاة. يلزم تحديث التطبيق بعد اكتمال الإصلاح.';return;}
     try{
       if(window.ZadAndroid.isAdhanDownloaded(key)){
         window.ZadAndroid.previewPrayerAdhan(key,prayer);
         if(prayerVoiceStatus)prayerVoiceStatus.textContent='يتم تشغيل معاينة '+sel.options[sel.selectedIndex].text+' لصلاة '+prayer+'.';
         return;
       }
       btn.disabled=true;
       showAdhanProgress();
       if(prayerVoiceStatus)prayerVoiceStatus.textContent='جارٍ تنزيل التسجيل لمعاينته...';
       const previewDone=(which,ok,reason)=>{
         btn.disabled=false;
         hideAdhanProgress();
         if(which!==key)return;
         finishPrayerProgress(ok?'اكتمل تنزيل التسجيل وحفظه.':'تعذر اكتمال تنزيل التسجيل.');
         if(ok){window.ZadAndroid.previewPrayerAdhan(key,prayer);if(prayerVoiceStatus)prayerVoiceStatus.textContent='تم تنزيل التسجيل وتشغيل معاينته.';}
         else if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذرت المعاينة: '+(reason||'فشل التنزيل');
       };
       previewDone.voiceKey=key;previewDone.taskIndex=1;previewDone.taskTotal=1;
       window.onPrayerPreviewDownloadComplete=previewDone;
       window.ZadAndroid.downloadAdhan(key);
     }catch(e){btn.disabled=false;hideAdhanProgress();if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذر بدء المعاينة أو التنزيل.';}
   }));
   window.onAdhanPreviewError=function(message){
     hideAdhanProgress();
     if(prayerVoiceStatus)prayerVoiceStatus.textContent=message||'تعذر تشغيل معاينة الأذان.';
   };
   const downloadPrayerVoices=$('downloadPrayerAdhans');
   if(downloadPrayerVoices)downloadPrayerVoices.addEventListener('click',()=>{
     if(window.ZadAndroid&&typeof window.ZadAndroid.downloadAdhan==='function'){
       const unique=[...new Set(prayerVoiceSelects.map(sel=>sel.value))];
       let index=0;downloadPrayerVoices.disabled=true;showAdhanProgress();
       const runNext=()=>{
         if(index>=unique.length){downloadPrayerVoices.disabled=false;hideAdhanProgress();finishPrayerProgress('اكتمل تنزيل '+unique.length+' تسجيلات وحفظها داخل التطبيق للعمل دون إنترنت.');if(prayerVoiceStatus)prayerVoiceStatus.textContent='اكتمل تنزيل '+unique.length+' تسجيلات وحفظها داخل التطبيق للعمل دون إنترنت.';return;}
         const voiceKey=unique[index++];
         if(window.ZadAndroid.isAdhanDownloaded(voiceKey)){runNext();return;}
         if(prayerVoiceStatus)prayerVoiceStatus.textContent='جارٍ حفظ التسجيل '+index+' من '+unique.length+' داخل التطبيق: '+voiceKey;
         updatePrayerProgress(Math.round((index-1)/unique.length*100),'حفظ التسجيل '+index+' من '+unique.length+' داخل التطبيق');
         const done=(which,ok,details)=>{
           if(prayerVoiceStatus)prayerVoiceStatus.textContent=(ok?'تم حفظ ':'تعذر حفظ ')+which+' — '+(details||'');
           updatePrayerProgress(Math.round(index/unique.length*100),'اكتمل '+index+' من '+unique.length+' تسجيلات');
           runNext();
         };
         done.voiceKey=voiceKey;done.taskIndex=index;done.taskTotal=unique.length;window.onPrayerAdhanDownloadComplete=done;
         try{window.ZadAndroid.downloadAdhan(voiceKey);}catch(e){done(voiceKey,false,e.message||'خطأ');}
       };
       runNext();return;
     }
     const tasks=[],seen=new Set();
     prayerVoiceSelects.forEach(sel=>{
       const voiceKey=sel.value,prayer=sel.dataset.prayer;
       const taskKey=voiceKey==='doha'?'doha:'+prayer:voiceKey;
       if(!seen.has(taskKey)){seen.add(taskKey);tasks.push({voiceKey,prayer});}
     });
     if(!tasks.length){if(prayerVoiceStatus)prayerVoiceStatus.textContent='اختر صوتًا واحدًا على الأقل.';return;}
     if(!('caches' in window)){if(prayerVoiceStatus)prayerVoiceStatus.textContent='المتصفح لا يدعم التخزين الداخلي للموقع.';return;}
     let index=0,failed=0,already=0;
     downloadPrayerVoices.disabled=true;
     updatePrayerProgress(0,'بدء حفظ التسجيلات داخل الموقع...');
     const downloadNext=async()=>{
       if(index===0&&navigator.storage&&typeof navigator.storage.persist==='function'){
         try{await navigator.storage.persist();}catch(error){}
       }
       if(index>=tasks.length){
         downloadPrayerVoices.disabled=false;
         const msg=failed?'اكتمل الحفظ مع تعذر '+failed+' ملف/ملفات.':'تم حفظ '+tasks.length+' تسجيلات داخل الموقع على هذا الجهاز، ويمكن تشغيلها دون إنترنت.';
         if(prayerVoiceStatus)prayerVoiceStatus.textContent=msg+(already?' ('+already+' محفوظة مسبقًا).':'');
         finishPrayerProgress(failed?'انتهى الحفظ مع وجود ملفات لم تكتمل.':'اكتمل حفظ التسجيلات داخل الموقع.');
         return;
       }
       const task=tasks[index],urls=getAdhanUrls(task.voiceKey,task.prayer);
       if(!urls.length){failed++;index++;downloadNext();return;}
       const url=urls[0];
       try{
         const cache=await caches.open(PRAYER_AUDIO_CACHE);
         const cached=await cache.match(url);
         if(cached){already++;index++;updatePrayerProgress(Math.round(index/tasks.length*100),'التسجيل '+index+' من '+tasks.length+' محفوظ مسبقًا داخل الموقع.');downloadNext();return;}
         if(prayerVoiceStatus)prayerVoiceStatus.textContent='جارٍ حفظ التسجيل '+(index+1)+' من '+tasks.length+' داخل الموقع: '+task.prayer;
         updatePrayerProgress(Math.round(index/tasks.length*100),'جارٍ تنزيل التسجيل '+(index+1)+' من '+tasks.length+' — 0%');
         const blob=await new Promise((resolve,reject)=>{
           const xhr=new XMLHttpRequest();xhr.open('GET',url,true);xhr.responseType='blob';
           xhr.onprogress=event=>{
             const percent=event.lengthComputable?Math.round(event.loaded/event.total*100):null;
             const overall=percent===null?null:Math.round((index+percent/100)/tasks.length*100);
             updatePrayerProgress(overall,'تنزيل التسجيل '+(index+1)+' من '+tasks.length+' — '+(event.loaded/1048576).toFixed(2)+' ميجابايت'+(event.lengthComputable?' — '+percent+'%':' — جارٍ حساب النسبة'));
           };
           xhr.onload=()=>xhr.status>=200&&xhr.status<300&&xhr.response&&xhr.response.size?resolve(xhr.response):reject(new Error('استجابة '+xhr.status));
           xhr.onerror=()=>reject(new Error('المصدر لا يسمح بالتحميل المباشر أو تعذر الاتصال.'));
           xhr.onabort=()=>reject(new Error('تم إيقاف التنزيل.'));
           xhr.send();
         });
         await cache.put(url,new Response(blob,{headers:{'Content-Type':'audio/mpeg','Cache-Control':'public, max-age=31536000'}}));
         index++;
         updatePrayerProgress(Math.round(index/tasks.length*100),'تم حفظ التسجيل '+index+' من '+tasks.length+' داخل الموقع.');
       }catch(error){
         failed++;index++;
         if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذر حفظ تسجيل '+task.prayer+' داخل الموقع: '+(error.message||'خطأ تحميل');
         updatePrayerProgress(Math.round(index/tasks.length*100),'تعذر ملف واحد — جارٍ متابعة بقية التسجيلات.');
       }
       downloadNext();
     };
     downloadNext();
   });
   const modeControl=$('adhanMode'), approachControl=$('approachAlertEnabled'), approachMinutes=$('approachAlertMinutes');
   const savedMode=localStorage.getItem('zad_adhan_mode')||'adhan';
   if(modeControl)modeControl.value=['adhan','tone','vibrate','off'].includes(savedMode)?savedMode:'adhan';
   if(approachControl)approachControl.checked=localStorage.getItem('zad_approach_enabled')!=='false';
   if(approachMinutes)approachMinutes.value=String(Math.max(1,Math.min(60,parseInt(localStorage.getItem('zad_approach_minutes')||'15',10)||15)));
   const adhanOn=localStorage.getItem('zad_adhan_enabled');
   if($('adhanEnabled'))$('adhanEnabled').checked=adhanOn===null?true:adhanOn==='true';
   document.querySelectorAll('.prayer-alert').forEach(el=>{const v=localStorage.getItem('zad_prayer_alert_'+el.dataset.prayer);el.checked=v===null?true:v==='true';});
   const downloadBtn=$('downloadAdhan');
   const adhanStatus=$('adhanSettingsStatus');
   let adhanProgress=null;
   let previewAfterDownload=null;
   const adhanVoiceNames={doha:'تسجيلات الأذان من الدوحة'};
   let adhanPathBox=$('adhanSavedPath');
   if(!adhanPathBox&&adhanStatus){
     adhanPathBox=document.createElement('div');
     adhanPathBox.id='adhanSavedPath';
     adhanPathBox.style.cssText='display:none;margin:10px 0;padding:12px;border:1px solid #cbdccf;border-radius:12px;background:#f2f8f2;color:#174b38;direction:rtl;text-align:right;line-height:1.8;overflow-wrap:anywhere';
     adhanStatus.insertAdjacentElement('afterend',adhanPathBox);
   }
   function showSavedAdhanPath(which,path){
     if(!adhanPathBox)return;
     if(!path){adhanPathBox.style.display='none';adhanPathBox.textContent='';return;}
     const name=adhanVoiceNames[which]||which;
     adhanPathBox.style.display='block';
     adhanPathBox.innerHTML='';
     const heading=document.createElement('strong');heading.textContent='مكان حفظ ملف الأذان';
     const voiceLine=document.createElement('div');voiceLine.textContent='المؤذن: '+name;
     const pathLine=document.createElement('div');pathLine.textContent='المسار: '+path;
     const fileLine=document.createElement('div');fileLine.textContent='الملف: '+path.split('/').pop();
     adhanPathBox.append(heading,voiceLine,pathLine,fileLine);
   }
   function refreshSavedAdhanPath(){
     const selected=voice?voice.value:'doha';
     showSavedAdhanPath(selected,localStorage.getItem('zad_adhan_public_path_'+selected));
   }
   function showAdhanProgress(){
     if(!adhanStatus)return;
     if(!document.getElementById('zadAdhanProgressStyle')){
       const style=document.createElement('style');style.id='zadAdhanProgressStyle';
       style.textContent='@keyframes zadAdhanLoading{0%{transform:translateX(115%)}100%{transform:translateX(-115%)}}#zadAdhanProgress{height:9px;width:100%;margin:10px 0 4px;border-radius:20px;overflow:hidden;background:#e6eee8;direction:ltr}#zadAdhanProgress>span{display:block;width:38%;height:100%;border-radius:20px;background:linear-gradient(90deg,#0c8067,#d6b76f,#0c8067);animation:zadAdhanLoading 1.15s ease-in-out infinite}';
       document.head.appendChild(style);
     }
     if(!adhanProgress){
       adhanProgress=document.createElement('div');adhanProgress.id='zadAdhanProgress';adhanProgress.setAttribute('role','progressbar');adhanProgress.setAttribute('aria-label','جاري تحميل صوت الأذان');
       adhanProgress.innerHTML='<span></span>';
     }
     adhanStatus.insertAdjacentElement('afterend',adhanProgress);
     adhanProgress.style.display='block';
   }
   function hideAdhanProgress(){if(adhanProgress)adhanProgress.style.display='none';}
   function updateAdhanDownloadState(){
     if(!window.ZadAndroid||!voice||!downloadBtn)return;
     const ready=window.ZadAndroid.isAdhanDownloaded(voice.value);
     downloadBtn.textContent=ready?'الصوت محفوظ ويعمل دون إنترنت ✓':'تنزيل الصوت للعمل دون إنترنت';
     if(ready)$('adhanSettingsStatus').textContent='التسجيل محفوظ على هذا الجهاز ويعمل دون إنترنت.';
   }
   window.onAdhanDownloadProgress=function(index,prayer,received,total,directory){
     const names={fajr:'الفجر',dhuhr:'الظهر',asr:'العصر',maghrib:'المغرب',isha:'العشاء'};
     const label=names[prayer]||adhanVoiceNames[prayer]||prayer;
     const mb=(received/1048576).toFixed(2);
     const totalMb=total>0?(total/1048576).toFixed(2):'غير محدد';
     const percent=total>0?Math.max(0,Math.min(100,Math.round(received/total*100))):null;
     const activeTask=window.onPrayerAdhanDownloadComplete||window.onPrayerPreviewDownloadComplete;
     const taskIndex=activeTask&&activeTask.taskIndex?activeTask.taskIndex:index;
     const taskTotal=activeTask&&activeTask.taskTotal?activeTask.taskTotal:(names[prayer]?5:1);
     const overall=percent===null?null:Math.round(((taskIndex-1)+(percent/100))/taskTotal*100);
     if(adhanProgress){
       adhanProgress.style.display='block';
       const fill=adhanProgress.querySelector('span');
       if(fill){fill.style.animation='none';fill.style.transform='none';fill.style.width=(percent===null?38:percent)+'%';}
     }
     const progressText='جارٍ تنزيل '+label+' ('+taskIndex+' من '+taskTotal+') — '+mb+' من '+totalMb+' ميجابايت'+(percent===null?' — المصدر لا يحدد الحجم الكلي':' — '+percent+'%');
     updatePrayerProgress(overall,progressText);
     if(overall===null&&prayerProgressFill){
       if(!document.getElementById('zadDownloadProgressStyle')){
         const style=document.createElement('style');style.id='zadDownloadProgressStyle';
         style.textContent='@keyframes zadDownloadIndeterminate{from{transform:translateX(130%)}to{transform:translateX(-130%)}}';
         document.head.appendChild(style);
       }
       prayerProgressFill.style.width='38%';prayerProgressFill.style.animation='zadDownloadIndeterminate 1s ease-in-out infinite alternate';
     }
     adhanStatus.textContent=progressText;
     if(prayerVoiceStatus)prayerVoiceStatus.textContent=progressText;
     if(directory&&adhanPathBox){
       adhanPathBox.style.display='block';
       const prayerKeys=['fajr','dhuhr','asr','maghrib','isha'];
       const filename=prayerKeys.includes(prayer)?'doha_'+prayer+'.mp3':prayer+'.mp3';
       adhanPathBox.textContent='مسار الحفظ داخل التطبيق: '+directory+'/'+filename;
     }
   };
   window.onAdhanDownloadComplete=function(which,ok,reason){
     const prayerDownloadCallback=window.onPrayerAdhanDownloadComplete;
     if(typeof prayerDownloadCallback==='function'&&prayerDownloadCallback.voiceKey===which){
       window.onPrayerAdhanDownloadComplete=null;
       prayerDownloadCallback(which,ok,reason);
       return;
     }
     const prayerPreviewCallback=window.onPrayerPreviewDownloadComplete;
     if(typeof prayerPreviewCallback==='function'&&prayerPreviewCallback.voiceKey===which){
       window.onPrayerPreviewDownloadComplete=null;
       prayerPreviewCallback(which,ok,reason);
       return;
     }
     if(!voice)return;
     if(downloadBtn)downloadBtn.disabled=false;
     hideAdhanProgress();
     finishPrayerProgress();
     if(which!==voice.value){return;}
     adhanStatus.textContent=ok?'تم تحميل الأذان وحفظ نسخة في مجلد الهاتف.':'تعذر تحميل التسجيل: '+(reason||'تحقق من الإنترنت وحاول مرة أخرى.');
     if(ok&&reason&&reason.indexOf('المسار الداخلي: ')!==-1){
       const internalPath=reason.split('المسار الداخلي: ').pop().trim();
       if(adhanPathBox){adhanPathBox.style.display='block';adhanPathBox.textContent='اكتمل تنزيل الملفات الخمسة. مجلد الحفظ داخل التطبيق: '+internalPath;}
     }else if(!ok){showSavedAdhanPath(which,localStorage.getItem('zad_adhan_public_path_'+which));}
     if(downloadBtn)downloadBtn.disabled=false;
     updateAdhanDownloadState();
     if(ok&&previewAfterDownload===which){
       previewAfterDownload=null;
       if(window.ZadAndroid&&typeof window.ZadAndroid.previewAdhan==='function'){
         window.ZadAndroid.previewAdhan(which);
         adhanStatus.textContent='تم حفظ التسجيل، وجارٍ تشغيله من الجهاز.';
       }
     }else if(!ok&&previewAfterDownload===which){previewAfterDownload=null;}
   };
   function ensureSelectedAdhanDownloaded(){
     if(!window.ZadAndroid||!voice||!downloadBtn)return;
     const enabled=$('adhanEnabled');
     if(enabled&&!enabled.checked)return;
     if(modeControl&&modeControl.value!=='adhan')return;
     try{
       if(!window.ZadAndroid.isAdhanDownloaded(voice.value)&&!downloadBtn.disabled){
         adhanStatus.textContent='جارٍ تنزيل صوت الأذان المختار تلقائيًا ليعمل عند دخول وقت الصلاة...';
         downloadBtn.click();
       }
     }catch(error){
       if(adhanStatus)adhanStatus.textContent='تعذر التحقق من ملف الأذان. يمكنك إعادة المحاولة من زر التنزيل.';
     }
   }
   if(downloadBtn)downloadBtn.addEventListener('click',async()=>{
     const selected=voice?voice.value:'doha';
     if(!adhanSources[selected]){$('adhanSettingsStatus').textContent='هذا التسجيل غير متاح للتنزيل حاليًا.';return;}
     downloadBtn.disabled=true;
     adhanStatus.textContent='جارٍ تحميل صوت الأذان، من فضلك انتظر...';
     showAdhanProgress();
     if(window.ZadAndroid&&typeof window.ZadAndroid.downloadAdhan==='function'){
       try{window.ZadAndroid.downloadAdhan(selected);return;}
       catch(error){hideAdhanProgress();downloadBtn.disabled=false;adhanStatus.textContent='تعذر بدء التحميل داخل التطبيق. حاول مرة أخرى.';return;}
     }
     const filename=(adhanVoiceNames[selected]||'adhan').replace(/[^\\p{L}\\p{N}_-]+/gu,'_')+'.mp3';
     const urls=getAdhanUrls(selected);
     try{
       let response=null;
       for(const url of urls){try{const candidate=await fetch(url,{mode:'cors',cache:'no-store'});if(candidate.ok){response=candidate;break;}}catch(e){}}
       if(!response)throw new Error('download failed');
       const blob=await response.blob();
       if(!blob.size)throw new Error('empty audio');
       const objectUrl=URL.createObjectURL(blob);
       const link=document.createElement('a');link.href=objectUrl;link.download=filename;document.body.appendChild(link);link.click();link.remove();
       setTimeout(()=>URL.revokeObjectURL(objectUrl),30000);
       adhanStatus.textContent='تم تنزيل ملف الأذان إلى مجلد التنزيلات. لتشغيله دون إنترنت من الموقع، افتح الملف من جهازك.';
     }catch(error){adhanStatus.textContent='تعذر تنزيل الصوت من المصدر حاليًا. تحقق من الاتصال وحاول مرة أخرى.';}
     finally{hideAdhanProgress();downloadBtn.disabled=false;}
   });
   if(previewBtn)previewBtn.addEventListener('click',()=>{
     const selected=voice?voice.value:'doha';
     if(window.ZadAndroid&&typeof window.ZadAndroid.isAdhanDownloaded==='function'){
       if(window.ZadAndroid.isAdhanDownloaded(selected)){
         try{window.ZadAndroid.previewAdhan(selected);$('adhanSettingsStatus').textContent='يتم تشغيل معاينة الأذان من الملف المحفوظ على الجهاز.';return;}
         catch(e){$('adhanSettingsStatus').textContent='تعذر تشغيل الملف المحفوظ.';return;}
       }
       if(typeof window.ZadAndroid.downloadAdhan==='function'){
         previewAfterDownload=selected;
         $('adhanSettingsStatus').textContent='التسجيل غير محفوظ. جارٍ تنزيله ثم تشغيله تلقائيًا...';
         try{window.ZadAndroid.downloadAdhan(selected);return;}
         catch(e){previewAfterDownload=null;$('adhanSettingsStatus').textContent='تعذر بدء تنزيل التسجيل.';return;}
       }
     }
     if(!adhanSources[selected]){$('adhanSettingsStatus').textContent='معاينة هذا التسجيل غير متاحة حاليًا.';return;}
     if(!preview){$('adhanSettingsStatus').textContent='مشغل الصوت غير موجود في الصفحة. حدّث الصفحة وحاول مرة أخرى.';return;}
     try{
       preview.pause();preview.removeAttribute('src');preview.load();
       preview.dataset.sourceIndex='0';
       preview.src=getAdhanUrls(selected)[0];
       preview.load();
       preview.volume=getPrayerVolume(prayer)/100;
       $('adhanSettingsStatus').textContent='جارٍ تشغيل معاينة الأذان...';
       const playing=preview.play();
       if(playing&&typeof playing.catch==='function')playing.catch(()=>{$('adhanSettingsStatus').textContent='تعذر تشغيل المعاينة. تحقق من الاتصال ثم حاول مرة أخرى.';});
     }catch(error){$('adhanSettingsStatus').textContent='تعذر تشغيل المعاينة. حاول مرة أخرى.';}
   });
   if(preview)preview.addEventListener('error',()=>{
     if(!window.ZadAndroid){
       if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذر تحميل معاينة الصوت من المصدر. تحقق من الاتصال وحاول مرة أخرى.';
       return;
     }
     const selected=voice?voice.value:'doha';
     const urls=getAdhanUrls(selected,preview.dataset.prayer||'الظهر');
     const next=(parseInt(preview.dataset.sourceIndex||'0',10)||0)+1;
     if(next>=urls.length){$('adhanSettingsStatus').textContent='تعذر تحميل ملف الأذان من مصادر الصوت المتاحة. تحقق من الاتصال وحاول لاحقًا.';return;}
     preview.dataset.sourceIndex=String(next);
     preview.src=urls[next];preview.load();preview.play().catch(()=>{});
   });
   if(preview)preview.addEventListener('loadedmetadata',()=>{
     if(!window.ZadAndroid&&prayerVoiceStatus)prayerVoiceStatus.textContent='تم تحميل التسجيل. يمكنك تشغيله من مشغل الصوت أعلاه.';
   });
   if(voice)voice.addEventListener('change',()=>{preview.pause();preview.currentTime=0;updateAdhanDownloadState();refreshSavedAdhanPath();});
   updateAdhanDownloadState();
   refreshSavedAdhanPath();
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
     prayerVoiceSelects.forEach(sel=>{const prayer=sel.dataset.prayer;localStorage.setItem('zad_adhan_voice_'+prayer,sel.value);if(window.ZadAndroid&&typeof window.ZadAndroid.setPrayerAdhanVoice==='function')window.ZadAndroid.setPrayerAdhanVoice(prayer,sel.value);});
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