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
   const adhanSources={doha:'https://archive.org/download/adhan.recordings.from.doha.qatar/Adhan_Doha_Qatar_02_Dhuhr_Adhan.mp3'};
   function getAdhanUrls(voiceKey){return adhanSources[voiceKey]?[adhanSources[voiceKey]]:[];}
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
     if(preview)preview.volume=level/100;
     if(window.ZadAndroid&&typeof window.ZadAndroid.setAdhanVolume==='function')window.ZadAndroid.setAdhanVolume(level);
   }
   if(volumeControl)volumeControl.addEventListener('input',applyAdhanVolume);
   applyAdhanVolume();
   if(storedVoice!==savedVoice)localStorage.setItem('zad_adhan_voice',savedVoice);
   // Per-prayer adhan voices: stored locally and in Android preferences.
   const prayerVoiceDefaults={الفجر:'tobar',الظهر:'basit',العصر:'mustafa',المغرب:'husary',العشاء:'banna'};
   const prayerVoiceSelects=[...document.querySelectorAll('.prayer-adhan-voice')];
   const prayerVoiceStatus=$('prayerAdhanDownloadStatus');
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
     if(!sel||!window.ZadAndroid||typeof window.ZadAndroid.previewAdhan!=='function'){
       if(prayerVoiceStatus)prayerVoiceStatus.textContent='معاينة التسجيلات المختارة متاحة داخل تطبيق أندرويد.';
       return;
     }
     const key=sel.value;
     try{
       if(window.ZadAndroid.isAdhanDownloaded(key)){
         window.ZadAndroid.previewAdhan(key);
         if(prayerVoiceStatus)prayerVoiceStatus.textContent='يتم تشغيل معاينة '+sel.options[sel.selectedIndex].text+' لصلاة '+prayer+'.';
         return;
       }
       btn.disabled=true;
       if(prayerVoiceStatus)prayerVoiceStatus.textContent='جارٍ تنزيل التسجيل لمعاينته...';
       window.onPrayerPreviewDownloadComplete=(which,ok,reason)=>{
         btn.disabled=false;
         if(which!==key)return;
         if(ok){window.ZadAndroid.previewAdhan(key);if(prayerVoiceStatus)prayerVoiceStatus.textContent='تم تنزيل التسجيل وتشغيل معاينته.';}
         else if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذرت المعاينة: '+(reason||'فشل التنزيل');
       };
       window.ZadAndroid.downloadAdhan(key);
     }catch(e){btn.disabled=false;if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذر بدء المعاينة أو التنزيل.';}
   }));
   const downloadPrayerVoices=$('downloadPrayerAdhans');
   if(downloadPrayerVoices)downloadPrayerVoices.addEventListener('click',()=>{
     if(!window.ZadAndroid||typeof window.ZadAndroid.downloadAdhan!=='function'){
       if(prayerVoiceStatus)prayerVoiceStatus.textContent='تنزيل التسجيلات للعمل دون إنترنت متاح من داخل تطبيق أندرويد.';
       return;
     }
     const unique=[...new Set(prayerVoiceSelects.map(sel=>sel.value))];
     let index=0;
     downloadPrayerVoices.disabled=true;
     const runNext=()=>{
       if(index>=unique.length){downloadPrayerVoices.disabled=false;if(prayerVoiceStatus)prayerVoiceStatus.textContent='انتهى طلب تنزيل '+unique.length+' تسجيلات مختلفة. راجع رسائل الحالة للتأكد من نجاح كل ملف.';return;}
       const voiceKey=unique[index++];
       if(window.ZadAndroid.isAdhanDownloaded(voiceKey)){runNext();return;}
       if(prayerVoiceStatus)prayerVoiceStatus.textContent='جارٍ تنزيل التسجيل '+index+' من '+unique.length+': '+voiceKey;
       window.onPrayerAdhanDownloadComplete=(which,ok,details)=>{
         if(prayerVoiceStatus)prayerVoiceStatus.textContent=(ok?'تم تنزيل ':'تعذر تنزيل ')+which+' — '+(details||'');
         runNext();
       };
       try{window.ZadAndroid.downloadAdhan(voiceKey);}catch(e){if(prayerVoiceStatus)prayerVoiceStatus.textContent='تعذر بدء تنزيل '+voiceKey;runNext();}
     };
     runNext();
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
     if(!voice)return;
     const names={fajr:'الفجر',dhuhr:'الظهر',asr:'العصر',maghrib:'المغرب',isha:'العشاء'};
     const label=names[prayer]||prayer;
     const mb=(received/1048576).toFixed(2);
     const totalMb=total>0?(total/1048576).toFixed(2):'؟';
     const percent=total>0?Math.max(0,Math.min(100,Math.round(received/total*100))):null;
     if(adhanProgress){
       adhanProgress.style.display='block';
       const fill=adhanProgress.querySelector('span');
       if(fill){fill.style.animation='none';fill.style.transform='none';fill.style.width=(percent===null?38:percent)+'%';}
     }
     adhanStatus.textContent='جارٍ تنزيل '+label+' ('+index+' من 5) — '+mb+' من '+totalMb+' ميجابايت'+(percent===null?'':' — '+percent+'%');
     if(prayerVoiceStatus)prayerVoiceStatus.textContent='جارٍ تنزيل '+label+' — '+mb+' من '+totalMb+' ميجابايت'+(percent===null?'':' — '+percent+'%');
     if(directory){
       if(!adhanPathBox)return;
       adhanPathBox.style.display='block';
       adhanPathBox.textContent='مسار الحفظ داخل التطبيق: '+directory+'/doha_'+prayer+'.mp3';
     }
   };
   window.onAdhanDownloadComplete=function(which,ok,reason){
     const prayerDownloadCallback=window.onPrayerAdhanDownloadComplete;window.onPrayerAdhanDownloadComplete=null;
     if(typeof prayerDownloadCallback==='function')prayerDownloadCallback(which,ok,reason);
     const prayerPreviewCallback=window.onPrayerPreviewDownloadComplete;window.onPrayerPreviewDownloadComplete=null;
     if(typeof prayerPreviewCallback==='function')prayerPreviewCallback(which,ok,reason);
     if(!voice)return;
     if(downloadBtn)downloadBtn.disabled=false;
     if(which!==voice.value){updateAdhanDownloadState();return;}
     hideAdhanProgress();
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
       preview.volume=volumeControl?Math.max(0,Math.min(100,parseInt(volumeControl.value,10)||0))/100:savedVolume/100;
       $('adhanSettingsStatus').textContent='جارٍ تشغيل معاينة الأذان...';
       const playing=preview.play();
       if(playing&&typeof playing.catch==='function')playing.catch(()=>{$('adhanSettingsStatus').textContent='تعذر تشغيل المعاينة. تحقق من الاتصال ثم حاول مرة أخرى.';});
     }catch(error){$('adhanSettingsStatus').textContent='تعذر تشغيل المعاينة. حاول مرة أخرى.';}
   });
   if(preview)preview.addEventListener('error',()=>{
     const selected=voice?voice.value:'basit';
     const urls=getAdhanUrls(selected);
     const next=(parseInt(preview.dataset.sourceIndex||'0',10)||0)+1;
     if(next>=urls.length){$('adhanSettingsStatus').textContent='تعذر تحميل ملف الأذان من مصادر الصوت المتاحة. تحقق من الاتصال وحاول لاحقًا.';return;}
     preview.dataset.sourceIndex=String(next);
     preview.src=urls[next];
     preview.load();
     preview.play().catch(()=>{});
   });
   if(preview)preview.addEventListener('loadedmetadata',()=>{
     $('adhanSettingsStatus').textContent='تم تحميل معاينة الأذان. اضغط تشغيل لسماع الصوت.';
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
     $('adhanSettingsStatus').textContent='تم حفظ إعدادات الأذان والصامت على هذا الجهاز.';
     ensureSelectedAdhanDownloaded();
     if(silentStatus&&window.ZadAndroid) silentStatus.textContent=window.ZadAndroid.hasSilentPermission()?'تم حفظ الصلوات المختارة. سيعمل الصامت 15 دقيقة بعد دخول وقت الصلاة.':'تم حفظ الاختيارات، لكن يجب منح إذن التحكم في وضع عدم الإزعاج أولًا.';
   });
   if(window.ZadAndroid){
     window.ZadAndroid.setAdhanEnabled(!($('adhanEnabled'))||$('adhanEnabled').checked);
     window.ZadAndroid.setAdhanVoice(savedVoice);
     window.ZadAndroid.setAdhanMode(modeControl?modeControl.value:'adhan');
     window.ZadAndroid.setApproachAlertEnabled(approachControl?approachControl.checked:true);
     window.ZadAndroid.setApproachAlertMinutes(approachMinutes?Math.max(1,Math.min(60,parseInt(approachMinutes.value,10)||15)):15);
     document.querySelectorAll('.prayer-alert').forEach(el=>window.ZadAndroid.setPrayerAdhanEnabled(el.dataset.prayer,el.checked));
     ensureSelectedAdhanDownloaded();
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