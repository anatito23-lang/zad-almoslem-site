(function(){
'use strict';
if(window.__zadSiteResilienceLoaded)return;
window.__zadSiteResilienceLoaded=true;

const egyptCoords={
 Cairo:[30.0444,31.2357],Zagazig:[30.5877,31.5020],Alexandria:[31.2001,29.9187],Giza:[30.0131,31.2089],
 Mansoura:[31.0409,31.3785],Tanta:[30.7865,31.0004],Ismailia:[30.5965,32.2715],Suez:[29.9668,32.5498],
 'Port Said':[31.2653,32.3019],Asyut:[27.1783,31.1859],Luxor:[25.6872,32.6398],Aswan:[24.0889,32.8998],
 Damanhur:[31.0341,30.4682],Hurghada:[27.2579,33.8116],Fayoum:[29.3084,30.8428],
 'Shebin El Kom':[30.5526,31.0090],Minya:[28.1099,30.7503],Banha:[30.4667,31.1833],
 Kharga:[25.4390,30.5586],'Beni Suef':[29.0661,31.0994],'Tor Sinai':[28.2417,33.6222],
 'Kafr El Sheikh':[31.1117,30.9399],'Marsa Matruh':[31.3525,27.2453],Qena:[26.1551,32.7160],
 Arish:[31.1316,33.7984],Sohag:[26.5569,31.6948]
};
let coordinatesPromise=null;
let forcePrayerFallback=false;
const radians=d=>d*Math.PI/180;
const degrees=r=>r*180/Math.PI;
function localPrayerTimings(day,lat,lon,method){
 const d=new Date(day.getFullYear(),day.getMonth(),day.getDate(),12);
 const n=Math.floor((Date.UTC(d.getFullYear(),d.getMonth(),d.getDate())-Date.UTC(d.getFullYear(),0,0))/86400000);
 const g=2*Math.PI/365*(n-1);
 const eq=229.18*(0.000075+0.001868*Math.cos(g)-0.032077*Math.sin(g)-0.014615*Math.cos(2*g)-0.040849*Math.sin(2*g));
 const dec=0.006918-0.399912*Math.cos(g)+0.070257*Math.sin(g)-0.006758*Math.cos(2*g)+0.000907*Math.sin(2*g)-0.002697*Math.cos(3*g)+0.00148*Math.sin(3*g);
 const tz=-d.getTimezoneOffset()/60;
 const noon=720-4*lon-eq+tz*60;
 const hourAngle=alt=>{
  const c=(Math.sin(radians(alt))-Math.sin(radians(lat))*Math.sin(dec))/(Math.cos(radians(lat))*Math.cos(dec));
  return Math.acos(Math.max(-1,Math.min(1,c)));
 };
 const sunrise=noon-4*degrees(hourAngle(-0.833));
 const sunset=noon+4*degrees(hourAngle(-0.833));
 const fajrAngle=method==='5'?19.5:18;
 const ishaAngle=method==='5'?17.5:17;
 const fajr=noon-4*degrees(hourAngle(-fajrAngle));
 const isha=noon+4*degrees(hourAngle(-ishaAngle));
 const asrAltitude=degrees(Math.atan(1/(1+Math.tan(Math.abs(radians(lat)-dec)))));
 const asr=noon+4*degrees(hourAngle(asrAltitude));
 const fmt=m=>{
  m=((m%1440)+1440)%1440;
  const h=Math.floor(m/60),min=Math.round(m%60);
  return String((h+(min===60?1:0))%24).padStart(2,'0')+':'+String(min===60?0:min).padStart(2,'0');
 };
 return {Fajr:fmt(fajr),Sunrise:fmt(sunrise),Dhuhr:fmt(noon+2),Asr:fmt(asr),Maghrib:fmt(sunset),Isha:fmt(isha)};
}
async function getCoordinates(){
 const settings=typeof window.prayerSettings==='function'?window.prayerSettings():{city:'Cairo',country:'Egypt'};
 if(settings.country==='Egypt'&&egyptCoords[settings.city])return egyptCoords[settings.city];
 if(coordinatesPromise)return coordinatesPromise;
 coordinatesPromise=(async()=>{
  const city=String(settings.city||'').trim(),country=String(settings.country||'').trim();
  if(!city)throw new Error('اسم المدينة مطلوب لحساب المواقيت.');
  const url='https://geocoding-api.open-meteo.com/v1/search?name='+encodeURIComponent(city+', '+country)+'&count=5&language=ar&format=json';
  const response=await fetch(url,{cache:'no-store'});
  if(!response.ok)throw new Error('تعذر تحديد إحداثيات المدينة.');
  const json=await response.json(),items=json.results||[];
  const match=items.find(x=>String(x.country||'').toLowerCase()===country.toLowerCase())||items[0];
  if(!match||!Number.isFinite(match.latitude)||!Number.isFinite(match.longitude))throw new Error('لم نعثر على إحداثيات المدينة.');
  return [match.latitude,match.longitude];
 })();
 try{return await coordinatesPromise;}finally{coordinatesPromise=null;}
}
function hijriDatePayload(day){
 const format=(options)=>new Intl.DateTimeFormat('ar-SA-u-ca-islamic-umalqura',options).format(day);
 return {weekday:{ar:new Intl.DateTimeFormat('ar-EG',{weekday:'long'}).format(day)},day:format({day:'numeric'}),month:{ar:format({month:'long'})},year:format({year:'numeric'})};
}
async function fallbackPrayerData(day){
 const settings=typeof window.prayerSettings==='function'?window.prayerSettings():{city:'Cairo',country:'Egypt',method:'5'};
 const [lat,lon]=await getCoordinates();
 const timings=localPrayerTimings(day,lat,lon,settings.method);
 const date=String(day.getDate()).padStart(2,'0')+'-'+String(day.getMonth()+1).padStart(2,'0')+'-'+day.getFullYear();
 window.zadPrayerUsingFallback=true;
 return {code:200,status:'OK',data:{timings,date:{gregorian:{date},hijri:hijriDatePayload(day)}}};
}
if(typeof window.getPrayerData==='function'&&!window.getPrayerData.__zadResilient){
 const original=window.getPrayerData.bind(window);
 const resilient=async function(city,day){
  if(forcePrayerFallback)return fallbackPrayerData(day instanceof Date?day:new Date());
  try{return await original(city,day);}
  catch(error){
   try{return await fallbackPrayerData(day instanceof Date?day:new Date());}
   catch(fallbackError){console.warn('Zad prayer fallback unavailable',error,fallbackError);throw error;}
  }
 };
 resilient.__zadResilient=true;
 window.getPrayerData=resilient;
}
if(typeof window.getTomorrowPrayerTimings==='function'){
 window.getTomorrowPrayerTimings=async function(day){
  const result=await fallbackPrayerData(day instanceof Date?day:new Date());
  return result.data.timings;
 };
}

const surahNames=[
'الفاتحة','البقرة','آل عمران','النساء','المائدة','الأنعام','الأعراف','الأنفال','التوبة','يونس',
'هود','يوسف','الرعد','إبراهيم','الحجر','النحل','الإسراء','الكهف','مريم','طه',
'الأنبياء','الحج','المؤمنون','النور','الفرقان','الشعراء','النمل','القصص','العنكبوت','الروم',
'لقمان','السجدة','الأحزاب','سبأ','فاطر','يس','الصافات','ص','الزمر','غافر',
'فصلت','الشورى','الزخرف','الدخان','الجاثية','الأحقاف','محمد','الفتح','الحجرات','ق',
'الذاريات','الطور','النجم','القمر','الرحمن','الواقعة','الحديد','المجادلة','الحشر','الممتحنة',
'الصف','الجمعة','المنافقون','التغابن','الطلاق','التحريم','الملك','القلم','الحاقة','المعارج',
'نوح','الجن','المزمل','المدثر','القيامة','الإنسان','المرسلات','النبأ','النازعات','عبس',
'التكوير','الانفطار','المطففين','الانشقاق','البروج','الطارق','الأعلى','الغاشية','الفجر','البلد',
'الشمس','الليل','الضحى','الشرح','التين','العلق','القدر','البينة','الزلزلة','العاديات',
'القارعة','التكاثر','العصر','الهمزة','الفيل','قريش','الماعون','الكوثر','الكافرون','النصر',
'المسد','الإخلاص','الفلق','الناس'
];
function populateLocalSurahs(){
 const select=document.getElementById('surahSelect');
 if(!select)return;
 const current=select.value;
 if(select.options.length>1&&select.options[0].textContent!=='تعذر تحميل السور')return;
 select.innerHTML=surahNames.map((name,index)=>'<option value="'+(index+1)+'">'+(index+1)+'. سورة '+name+'</option>').join('');
 if(current&&Number(current)>=1&&Number(current)<=114)select.value=current;
 const status=document.getElementById('quranStatus');
 if(status)status.textContent='فهرس السور جاهز محليًا. إذا تعذر تحميل القراء من الشبكة، جرّب أحد القراء المتاحين احتياطيًا.';
}
const quranApiTimer=setInterval(()=>{
 const select=document.getElementById('surahSelect');
 if(select&&select.options.length===1&&/تعذر تحميل السور|جارٍ تحميل السور/.test(select.options[0].textContent))populateLocalSurahs();
 if(select&&select.options.length>1)clearInterval(quranApiTimer);
},1200);
setTimeout(()=>{clearInterval(quranApiTimer);populateLocalSurahs();},12000);

const fallbackEditions={basit:'ar.abdulbasit',minshawi:'ar.minshawi',husary:'ar.husary'};
function bindQuranFallback(){
 const button=document.getElementById('showSurah');
 if(!button||button.dataset.zadFallbackBound)return;
 button.dataset.zadFallbackBound='1';
 button.addEventListener('click',event=>{
  const key=document.getElementById('reciter')?.value;
  const edition=fallbackEditions[key];
  if(window.zadReciterData?.[key]){const existingAudio=document.getElementById('audio');if(existingAudio)delete existingAudio.dataset.zadFallbackTried;return;}
  if(!edition)return;
  const surah=Number(document.getElementById('surahSelect')?.value);
  if(!surah)return;
  event.preventDefault();event.stopImmediatePropagation();
  const audio=document.getElementById('audio'),status=document.getElementById('quranStatus');
  if(!audio)return;
  const names={basit:'عبد الباسط عبد الصمد',minshawi:'محمد صديق المنشاوي',husary:'محمود خليل الحصري'};
  audio.pause();audio.dataset.zadFallbackTried='1';
  audio.src='https://cdn.islamic.network/quran/audio-surah/128/'+edition+'/'+surah+'.mp3';
  audio.classList.remove('hidden');audio.load();
  if(status)status.textContent='تلاوة '+names[key]+' — سورة '+surahNames[surah-1]+' من مكتبة AlQuran Cloud.';
  audio.play().catch(()=>{if(status)status.textContent='تعذر بدء الصوت من هذه الشبكة. جرّب فتح الموقع من شبكة أخرى أو اسمح لمصدر الصوت بالعمل.';});
 },true);
}
bindQuranFallback();
document.getElementById('reciter')?.addEventListener('change',()=>{
 const key=document.getElementById('reciter')?.value;
 if(!window.zadReciterData?.[key]&&fallbackEditions[key]){
  const status=document.getElementById('quranStatus');
  if(status)status.textContent='إذا تعذرت قائمة القراء الأساسية، يمكن تشغيل '+({basit:'عبد الباسط',minshawi:'المنشاوي',husary:'الحصري'}[key])+' من المصدر الاحتياطي بعد اختيار السورة.';
 }
});
const quranCard=document.querySelector('#quran .card');
if(quranCard&&!document.getElementById('zadQuranSourceNotice')){
 const notice=document.createElement('p');
 notice.id='zadQuranSourceNotice';notice.className='small muted';
 notice.style.marginTop='10px';
 notice.innerHTML='مصدر احتياطي للتلاوات: <a href="https://alquran.cloud/cdn" target="_blank" rel="noopener noreferrer">AlQuran Cloud</a>. تُستخدم التلاوات في السياق التعليمي، ويجب مراجعة شروط أصحاب التسجيلات قبل تفعيل الإعلانات التجارية على الموقع.';
 quranCard.append(notice);
}
document.getElementById('audio')?.addEventListener('error',()=>{
 const audio=document.getElementById('audio'),status=document.getElementById('quranStatus');
 const key=document.getElementById('reciter')?.value,edition=fallbackEditions[key];
 const surah=Number(document.getElementById('surahSelect')?.value);
 if(edition&&surah&&audio&&!audio.dataset.zadFallbackTried&&audio.src&&!audio.src.includes('cdn.islamic.network')){
  audio.dataset.zadFallbackTried='1';
  audio.src='https://cdn.islamic.network/quran/audio-surah/128/'+edition+'/'+surah+'.mp3';
  audio.load();
  if(status)status.textContent='تعذر المصدر الأول؛ نجرب مصدرًا احتياطيًا للتلاوة...';
  audio.play().catch(()=>{if(status)status.textContent='تعذر تشغيل التلاوة من المصدرين. قد تحجب شبكة العمل خوادم الصوت؛ جرّب شبكة أخرى أو افتح مصدر التلاوة مباشرة.';});
  return;
 }
 if(status)status.textContent='فشل تحميل ملف التلاوة. قد تكون شبكة العمل تحجب خادم الصوت؛ جرّب شبكة أخرى أو افتح مصدر التلاوة مباشرة.';
});

// If the first prayer request is blocked, retry through the local calculation fallback.
window.zadCalculatePrayerFallback=fallbackPrayerData;
setTimeout(()=>{if(!window.zadPrayerTimings&&typeof window.loadPrayer==='function'){forcePrayerFallback=true;window.loadPrayer();}},3000);

})();
