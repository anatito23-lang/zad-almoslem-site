(function(){
'use strict';
if(location.hostname!=='anatito23-lang.github.io')return;
document.documentElement.classList.add('zad-public-website');
document.body.classList.add('zad-public-website');
const selectors=[
 '.azkar-ticker','#daily-reminder-settings',
 '.prayer-tracker','.adhan-settings','#zadAudioSettingsSlot',
 '.tasbeeh-float','#tasbeehPanel','#tasbeehFab','#zad-page-tasbeeh',
 '#qibla','#zad-page-qibla','.bottom-nav','.zad-bottom-nav',
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
body.zad-public-website .azkar-ticker,
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
body.zad-public-website .bottom-nav,
body.zad-public-website .zad-bottom-nav,
body.zad-public-website #adhan,
body.zad-public-website #adhan-settings{
display:none!important;visibility:hidden!important;pointer-events:none!important;height:0!important;min-height:0!important;margin:0!important;padding:0!important
}
`;
document.head.append(style);
clean();
// All website-only controls are present by the time this footer script runs.
// A document-wide observer only repeats full-page scans during content updates.

})();