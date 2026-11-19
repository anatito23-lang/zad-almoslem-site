(function(){
'use strict';
const $=id=>document.getElementById(id);
const prayers=[['الفجر','fajr'],['الظهر','dhuhr'],['العصر','asr'],['المغرب','maghrib'],['العشاء','isha']];
const voices=['عبد الباسط عبد الصمد','محمود خليل الحصري','مصطفى إسماعيل','محمود البنا','محمد صديق المنشاوي','محمد رفعت','النقشبندي','نصر الدين طوبار','أذان الحرم المكي'];
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
   '<select class="adhan-style" aria-label="نوع أذان '+name+'"><option value="full" '+(v.style==='full'?'selected':'')+'>الأذان كاملًا</option><option value="takbir" '+(v.style==='takbir'?'selected':'')+'>تكبيرتان فقط</option><option value="vibrate" '+(v.style==='vibrate'?'selected':'')+'>اهتزاز فقط</option><option value="off" '+(v.style==='off'?'selected':'')+'>بدون أذان</option></select>'+
   '<div class="adhan-prayer-options"><label><input class="adhan-enabled" type="checkbox" '+(v.enabled?'checked':'')+'> تفعيل الصلاة</label><label>مستوى الصوت <input class="adhan-volume" type="range" min="0" max="100" value="'+Number(v.volume||80)+'"></label><button class="btn light adhan-test" type="button">تجربة الصوت</button></div></div>';
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
 const btn=e.target.closest('.adhan-test');if(!btn)return;
 $('adhanSettingsStatus').textContent='سيعمل اختبار الصوت بعد تثبيت ملفات المؤذنين وربط مشغل الأذان.';
});
$('saveAdhanSettings').addEventListener('click',()=>{
 collect();localStorage.setItem('zad_adhan_settings',JSON.stringify(state));
 if(window.ZadAndroid&&window.ZadAndroid.saveAdhanSettings)window.ZadAndroid.saveAdhanSettings(JSON.stringify(state));
 $('adhanSettingsStatus').textContent='تم حفظ اختيارات المؤذنين والتنبيهات لكل صلاة على هذا الجهاز.';
});
$('adhanMasterEnabled').addEventListener('change',()=>{state.enabled=$('adhanMasterEnabled').checked;});
render();
})();