(function(){
'use strict';
if(!['anatito23-lang.github.io','zad-almoslem.github.io'].includes(location.hostname))return;
document.documentElement.classList.add('zad-public-website');
document.body.classList.add('zad-public-website');
const selectors=[
 '#daily-reminder-settings',
 '.prayer-tracker','.adhan-settings','#zadAudioSettingsSlot',
 '.tasbeeh-float','#tasbeehPanel','#tasbeehFab','#zad-page-tasbeeh',
 '#qibla','#zad-page-qibla',
 '#adhan','#adhan-settings'
];
const linkPattern=/#(?:reminders|daily-reminder-settings|qibla|tasbeeh(?:Fab)?|adhan(?:-settings)?|prayer-tracker)(?:$|[?#])/i;
function clean(){
 for(const selector of selectors)document.querySelectorAll(selector).forEach(el=>el.remove());
 document.querySelectorAll('a[href]').forEach(a=>{
  const href=a.getAttribute('href')||'';
  const label=(a.textContent||'').trim();
  if(linkPattern.test(href)||/^(السبحة الإلكترونية|السبحة|بوصلة القبلة|القبلة|متتبع الصلاة|إعدادات الأذان)$/.test(label))a.remove();
 });
 const countdown=document.querySelector('#nextPrayerCountdown .next-prayer-clock .next-prayer-kicker');
 if(countdown)countdown.textContent='متبقي على الصلاة';
}
const style=document.createElement('style');
style.id='zad-website-hard-cleanup';
style.textContent=`
body.zad-public-website #daily-reminder-settings,
body.zad-public-website .prayer-tracker,
body.zad-public-website .adhan-settings,
body.zad-public-website #zadAudioSettingsSlot,
body.zad-public-website .tasbeeh-float,
body.zad-public-website #tasbeehPanel,
body.zad-public-website #tasbeehFab,
body.zad-public-website #zad-page-tasbeeh,
body.zad-public-website #qibla,
body.zad-public-website #zad-page-qibla,
body.zad-public-website #adhan,
body.zad-public-website #adhan-settings{
display:none!important;visibility:hidden!important;pointer-events:none!important;height:0!important;min-height:0!important;margin:0!important;padding:0!important
}
`;
document.head.append(style);
clean();

// كرر اختصارات الشريط العلوي نفسها أسفل الموقع، دون إنشاء روابط مختلفة.
function addBottomShortcuts(){
 const source=document.querySelector('.top nav');
 if(!source || document.querySelector('.zad-bottom-nav'))return;
 const bottom=document.createElement('nav');
 bottom.className='zad-bottom-nav';
 bottom.setAttribute('aria-label','اختصارات الموقع السفلية');
 source.querySelectorAll('a[href]').forEach(link=>{
  const item=link.cloneNode(true);
  item.classList.remove('is-current-tab');
  bottom.appendChild(item);
 });
 if(!bottom.children.length)return;
 document.body.appendChild(bottom);
 document.body.style.paddingBottom='76px';
}
addBottomShortcuts();
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',addBottomShortcuts,{once:true});

const bottomStyle=document.createElement('style');
bottomStyle.id='zad-bottom-shortcuts-style';
bottomStyle.textContent=`
body.zad-public-website{padding-bottom:76px!important}
body.zad-public-website .zad-bottom-nav{
 position:fixed!important;right:0!important;left:0!important;bottom:0!important;
 z-index:99999!important;display:flex!important;flex-direction:row!important;
 align-items:stretch!important;gap:7px!important;width:100%!important;
 padding:9px 10px calc(9px + env(safe-area-inset-bottom))!important;
 overflow-x:auto!important;overflow-y:hidden!important;white-space:nowrap!important;
 background:#fff!important;border-top:1px solid #dce6dc!important;
 box-shadow:0 -5px 20px rgba(20,55,40,.12)!important;
 scrollbar-width:thin!important;overscroll-behavior-x:contain!important;
}
body.zad-public-website .zad-bottom-nav a{
 display:flex!important;flex:0 0 auto!important;align-items:center!important;
 justify-content:center!important;min-height:42px!important;padding:8px 13px!important;
 border:1px solid #dce6dc!important;border-radius:11px!important;
 background:#f3f7f2!important;color:#285747!important;
 font:700 13px/1.4 Tahoma,Arial,sans-serif!important;text-decoration:none!important;
}
body.zad-public-website .zad-bottom-nav a:active,
body.zad-public-website .zad-bottom-nav a:focus{background:#e5efe6!important;color:#173b30!important}
`;
document.head.append(bottomStyle);

// All website-only controls are present by the time this footer script runs.
})();