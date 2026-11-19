(function(){
'use strict';
const $=id=>document.getElementById(id);
const prayers=[['الفجر','fajr'],['الظهر','dhuhr'],['العصر','asr'],['المغرب','maghrib'],['العشاء','isha']];
const voices=['عبد الباسط عبد الصمد','محمود خليل الحصري','مصطفى إسماعيل','محمود البنا','محمد صديق المنشاوي','محمد رفعت','النقشبندي','نصر الدين طوبار','أذان الحرم المكي','تسجيل أذان الفجر المرفق'];
const defaults={enabled:true,lead:15,iqama:20,silent:15,silentEnabled:false,dua:true,items:{}};
prayers.forEach(([name,key])=>defaults.items[key]={enabled:true,voice:key==='fajr'?'عبد الباسط عبد الصمد':'عبد الباسط عبد الصمد',style:key==='fajr'?'full':'full',volume:80});
function read(){try{return Object.assign({},defaults,JSON.parse(localStorage.getItem('zad_adhan_settings')||'{}'))}catch(e){return JSON.parse(JSON.stringify(defaults))}}
let state=read();
function render(){
 $('adhanMasterEnabled').checked=state.enabled;
 $('adhanLeadMinutes').value=String(state.lead);
 $('adhanIqamaMinutes').value=String(state.iqama);
 $('adhanSilentMinutes').value=String(state.silent);
 $('adhanSilentEnabled').checked=state.silentEnabled;
 $('adhanDuaEnabled').checked=state.dua;
 $('adhanPrayerSettings').innerHTML=prayers.map(([name,key])=>{
  const v=state.items[key]||defaults.items[key];
  return '<div class="adhan-prayer-row" data-prayer="'+key+'"><div class="adhan-prayer-name">'+name+'</div>'+
   '<select class="adhan-voice" aria-label="مؤذن '+name+'">'+voices.map(x=>'<option '+(x===v.voice?'selected':'')+'>'+x+'</option>').join('')+'</select>'+
   '<select class="adhan-style" aria-label="نوع أذان '+name+'"><option value="full" '+(v.style==='full'?'selected':'')+'>الأذان كاملًا</option><option value="takbir" disabled>تكبيرتان فقط (يتطلب تسجيلًا مستقلًا)</option><option value="vibrate" '+(v.style==='vibrate'?'selected':'')+'>اهتزاز فقط</option><option value="off" '+(v.style==='off'?'selected':'')+'>بدون أذان</option></select>'+
   '<div class="adhan-prayer-options"><label><input class="adhan-enabled" type="checkbox" '+(v.enabled?'checked':'')+'> تفعيل الصلاة</label><label>مستوى الصوت <input class="adhan-volume" type="range" min="0" max="100" value="'+Number(v.volume||80)+'"></label><button class="btn light adhan-import" type="button">إضافة ملف الصوت</button><button class="btn light adhan-test" type="button">تجربة الصوت</button></div></div>';
 }).join('');
}
function collect(){
 state.enabled=$('adhanMasterEnabled').checked;
 state.lead=Number($('adhanLeadMinutes').value);
 state.iqama=Number($('adhanIqamaMinutes').value);
 state.silent=Number($('adhanSilentMinutes').value);
 state.silentEnabled=$('adhanSilentEnabled').checked;
 state.dua=$('adhanDuaEnabled').checked;
 document.querySelectorAll('.adhan-prayer-row').forEach(row=>{
  const key=row.dataset.prayer;
  state.items[key]={voice:row.querySelector('.adhan-voice').value,style:row.querySelector('.adhan-style').value,enabled:row.querySelector('.adhan-enabled').checked,volume:Number(row.querySelector('.adhan-volume').value)};
 });
}
$('adhanPrayerSettings').addEventListener('click',e=>{
 const row=e.target.closest('.adhan-prayer-row');if(!row)return;
 const voice=row.querySelector('.adhan-voice').value;
 if(e.target.closest('.adhan-import')){
  if(window.ZadAndroid&&window.ZadAndroid.importAdhanAudio){window.ZadAndroid.importAdhanAudio(voice);$('adhanSettingsStatus').textContent='اختر ملف الأذان لهذا المؤذن من ملفات الهاتف.';}
  else $('adhanSettingsStatus').textContent='إضافة ملفات الصوت متاحة داخل تطبيق أندرويد فقط.';
  return;
 }
 if(e.target.closest('.adhan-test')){
  const volume=Number(row.querySelector('.adhan-volume').value);
  if(window.ZadAndroid&&window.ZadAndroid.playAdhanTest){
   const ok=window.ZadAndroid.playAdhanTest(voice,volume);
   $('adhanSettingsStatus').textContent=ok?'بدأ تشغيل تجربة أذان '+voice+'.':'تسجيل هذا المؤذن غير موجود على الجهاز بعد. أضف ملفه أولًا.';
  } else $('adhanSettingsStatus').textContent='اختبار الصوت متاح داخل تطبيق أندرويد بعد إضافة التسجيل.';
 }
});
window.onAdhanAudioImported=function(voice,ok){
 $('adhanSettingsStatus').textContent=ok?'تم حفظ ملف صوت '+voice+' على الجهاز ويمكن تجربته دون إنترنت.':'لم يتم حفظ الملف. تأكد أنه ملف صوتي صالح ثم حاول مرة أخرى.';
};
$('saveAdhanSettings').addEventListener('click',()=>{
 collect();localStorage.setItem('zad_adhan_settings',JSON.stringify(state));
 if(window.ZadAndroid&&window.ZadAndroid.saveAdhanSettings){
  window.ZadAndroid.saveAdhanSettings(JSON.stringify(state));
  if(window.ZadAndroid.canScheduleExactAlarms&&!window.ZadAndroid.canScheduleExactAlarms()&&window.ZadAndroid.requestExactAlarmPermission){
   $('adhanSettingsStatus').textContent='تم حفظ الإعدادات. اسمح للتطبيق بالمنبهات الدقيقة حتى تعمل مواقيت الصلاة في موعدها.';
   window.ZadAndroid.requestExactAlarmPermission();
  } else $('adhanSettingsStatus').textContent='تم حفظ الإعدادات. تأكد من وجود تسجيل لكل مؤذن اخترته باستخدام زر تجربة الصوت.';
 } else $('adhanSettingsStatus').textContent='تم حفظ الإعدادات على هذا المتصفح فقط؛ تشغيل الأذان بالخلفية يحتاج تطبيق أندرويد.';
});
$('adhanMasterEnabled').addEventListener('change',()=>{state.enabled=$('adhanMasterEnabled').checked;});
$('adhanSilentEnabled').addEventListener('change',()=>{
 if($('adhanSilentEnabled').checked && window.ZadAndroid&&window.ZadAndroid.hasDndAccess&&!window.ZadAndroid.hasDndAccess()){
  $('adhanSettingsStatus').textContent='اسمح للتطبيق بالوصول إلى وضع عدم الإزعاج من إعدادات أندرويد حتى يتمكن من جعل الهاتف صامتًا وقت الصلاة.';
  if(window.ZadAndroid.requestDndAccess)window.ZadAndroid.requestDndAccess();
 }
});
render();
})();