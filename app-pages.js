(function(){
'use strict';
const $=id=>document.getElementById(id), main=document.querySelector('main.wrap'), header=document.querySelector('header.hero');
const nativeApp=!!(window.ZadAndroid&&typeof window.ZadAndroid.isNativeApp==='function'&&window.ZadAndroid.isNativeApp());
// Keep the website and Android app section lists identical: the Kids section is disabled in both.
document.getElementById('kids')?.remove();
document.querySelectorAll('a[href="#kids"]').forEach(link=>link.remove());
if(!main||!header||$('zadAppPages'))return;

const meta=[['calendar','التقويم الشهري','🗓️'],['prayer','مواقيت الصلاة','🕰️'],['quran','استماع القرآن','📖'],['mushaf','المصحف الشريف','۞'],['ruqyah','الرقية الشرعية','🤲'],['radio-egypt','إذاعة القرآن الكريم','📻'],['azkar','الأذكار اليومية','☀️'],['books-library','مكتبة الكتب الإسلامية','📚'],...(!nativeApp?[['kids','ركن الأطفال','🧒']]:[]),['reminders','تنبيهات الصلاة والأذان','🕌'],['hadith-dua','السنة والدعاء','🌿'],['daily-wird','وردي اليومي','📿'],['friday-wird','ورد الجمعة','🌙'],['qalbi-itmaann','قلبي اطمأن','💚'],['library','نفحات إيمانية','✨'],['khutbahs','خطب الجمعة','🎙️'],['nasheed','الإنشاد والابتهالات','🎵']];
const root=document.createElement('div');root.id='zadAppPages';
const home=document.createElement('div');home.className='zad-page is-active';home.id='zad-page-home';home.dataset.page='home';
const pages=document.createElement('div');pages.className='zad-pages-content';
const footer=document.querySelector('footer');
root.append(home);
const items=[];
meta.forEach(([id,title,icon])=>{if($(id))items.push({id,title,icon});});
home.innerHTML='<div class="zad-home-brand"><span>۞</span><div><h1>زاد المسلم</h1><p>اختر القسم الذي تريد فتحه</p></div></div><div class="zad-home-grid" id="zadHomeGrid"></div>';
const homeGrid=home.querySelector('#zadHomeGrid');
items.forEach(x=>{
 const b=document.createElement('button');
 b.type='button';b.className='zad-home-item';b.dataset.target=x.id;
 b.innerHTML='<span class="zad-home-icon" aria-hidden="true">'+x.icon+'</span><strong>'+x.title+'</strong><span class="zad-home-arrow" aria-hidden="true">←</span>';
 b.addEventListener('click',()=>showPage(x.id));
 homeGrid.append(b);
});
const more=document.createElement('section');more.className='zad-page';more.id='zad-page-more';more.dataset.page='more';more.innerHTML='<div class="zad-page-heading"><span>✦</span><div><h2>كل خدمات زاد المسلم</h2><p>اختر الخدمة لفتح صفحتها المستقلة.</p></div></div><div class="zad-more-grid" id="zadMoreGrid"></div>';
root.append(pages,more);document.body.insertBefore(root,document.querySelector('.book-reader-modal')||document.body.firstChild);
const panel=$('tasbeehPanel');
if(panel){const p=document.createElement('div');p.className='zad-page';p.id='zad-page-tasbeeh';p.dataset.page='tasbeeh';p.innerHTML='<div class="zad-page-heading"><span>📿</span><div><h2>السبحة الإلكترونية</h2><p>اختر الذكر والعدد واضغط للتسبيح. يُحفظ تقدمك على الجهاز.</p></div></div>';panel.classList.remove('hidden');panel.classList.add('zad-tasbeeh-card');panel.querySelector('.tasbeeh-panel-head')?.remove();if($('closeTasbeeh'))$('closeTasbeeh').style.display='none';p.append(panel);pages.append(p);}
document.querySelector('.tasbeeh-float')?.remove();$('mushafFloatingButton')?.remove();
const q=document.createElement('div');q.className='zad-page';q.id='zad-page-qibla';q.dataset.page='qibla';
q.innerHTML='<div class="zad-page-heading"><span>🧭</span><div><h2>بوصلة القبلة</h2><p>اتجاه الكعبة من موقعك مع مستشعر اتجاه الهاتف.</p></div></div><div class="card zad-qibla-card"><div class="zad-qibla-location"><span>موقع الحساب</span><strong id="zadQiblaLocation">المدينة المختارة</strong></div><div class="zad-compass"><span class="zad-cardinal zad-n">شمال</span><span class="zad-cardinal zad-e">شرق</span><span class="zad-cardinal zad-s">جنوب</span><span class="zad-cardinal zad-w">غرب</span><div class="zad-qibla-needle" id="zadQiblaNeedle">▲<b>القبلة</b></div><div class="zad-compass-center">۞</div></div><div class="zad-qibla-degree" id="zadQiblaDegree">جارٍ حساب الاتجاه...</div><p class="small" id="zadCompassStatus" role="status">اسمح بحركة الهاتف وحرّكه بشكل رقم 8 لمعايرة البوصلة.</p><div class="row zad-qibla-actions"><button class="btn green" id="zadCompassStart" type="button">تشغيل البوصلة</button><button class="btn light" id="zadQiblaLocate" type="button">تحديد موقعي</button></div><div class="notice">لأفضل دقة فعّل الموقع وأبعد الهاتف عن المعادن والمغناطيس. قد تختلف الدقة حسب مستشعر الهاتف.</div></div>';pages.append(q);
const settings=document.createElement('div');settings.className='zad-page';settings.id='zad-page-settings';settings.dataset.page='settings';
settings.innerHTML='<div class="zad-page-heading"><span>⚙️</span><div><h2>إعدادات البرنامج</h2><p>تحكم في الصوت والتنبيهات والمظهر والكتابة من مكان واحد</p></div></div><section class="zad-settings-group"><h3>🌅 الأذكار الصوتية والتنبيهات</h3><p>مواعيد أذكار الصباح والمساء وقيام الليل.</p><div id="zadReminderSettingsSlot"></div></section><section class="zad-settings-group"><h3>🎨 المظهر والألوان</h3><p>اختر الشكل والألوان المناسبة لك، وسيتم حفظ اختيارك تلقائيًا.</p><div class="zad-theme-options"><button type="button" data-zad-theme="light">فاتح أخضر</button><button type="button" data-zad-theme="dark">داكن</button><button type="button" data-zad-theme="blue">أزرق</button><button type="button" data-zad-theme="sand">رملي</button></div></section><section class="zad-settings-group"><h3>🔤 الخط والكتابة</h3><label for="zadFontFamily">نوع الخط</label><select id="zadFontFamily"><option value="Tahoma,Arial,sans-serif">Tahoma</option><option value="Arial,sans-serif">Arial</option><option value="system-ui,sans-serif">خط الجهاز</option><option value="Georgia,serif">Georgia</option></select><label for="zadFontSize">حجم الكتابة: <b id="zadFontSizeValue">16</b>px</label><input id="zadFontSize" type="range" min="14" max="22" step="1" value="16"><p class="small">تغيير حجم الخط يطبق على نصوص البرنامج، مع الحفاظ على تنسيق المصحف.</p></section>';
pages.append(settings);
const settingsTile=document.createElement('button');settingsTile.type='button';settingsTile.className='zad-more-item';settingsTile.dataset.target='settings';settingsTile.innerHTML='<span>⚙️</span><b>إعدادات البرنامج</b>';settingsTile.addEventListener('click',()=>showPage('settings'));
const cities={Cairo:[30.0444,31.2357,'القاهرة'],Zagazig:[30.5877,31.502,'الزقازيق'],Alexandria:[31.2001,29.9187,'الإسكندرية'],Giza:[30.0131,31.2089,'الجيزة'],Mansoura:[31.0409,31.3785,'المنصورة'],Tanta:[30.7865,31.0004,'طنطا'],Ismailia:[30.5965,32.2715,'الإسماعيلية'],Suez:[29.9668,32.5498,'السويس'],'Port Said':[31.2653,32.3019,'بورسعيد'],Asyut:[27.1809,31.1837,'أسيوط'],Luxor:[25.6872,32.6396,'الأقصر'],Aswan:[24.0889,32.8998,'أسوان']};
const rad=d=>d*Math.PI/180,deg=r=>(r*180/Math.PI+360)%360,kaaba=[21.4225,39.8262];let coords=null,bearing=0,heading=null;
function calc(lat,lon){const a=rad(lat),b=rad(kaaba[0]),dl=rad(kaaba[1]-lon);return deg(Math.atan2(Math.sin(dl)*Math.cos(b),Math.cos(a)*Math.sin(b)-Math.sin(a)*Math.cos(b)*Math.cos(dl)));}
function rotate(){if($('zadQiblaNeedle'))$('zadQiblaNeedle').style.transform='rotate('+(bearing-(heading||0))+'deg)';}
function update(){const c=cities[$('city')?.value]||cities.Cairo;const p=coords||c;bearing=calc(p[0],p[1]);$('zadQiblaLocation').textContent=coords?'موقعك الحالي':'مدينة '+c[2];$('zadQiblaDegree').textContent='اتجاه القبلة: '+Math.round(bearing)+'° من الشمال';rotate();}
function orientation(e){let h=typeof e.webkitCompassHeading==='number'?e.webkitCompassHeading:typeof e.alpha==='number'?(360-e.alpha+(screen.orientation?.angle||0))%360:null;if(h!==null){heading=h;$('zadCompassStatus').textContent='البوصلة تعمل. حرّك الهاتف ببطء حتى يستقر المؤشر.';rotate();}}
$('zadCompassStart').addEventListener('click',()=>{if(typeof DeviceOrientationEvent==='undefined'){$('zadCompassStatus').textContent='مستشعر اتجاه الهاتف غير مدعوم في هذا الجهاز.';return;}if(typeof DeviceOrientationEvent.requestPermission==='function'){DeviceOrientationEvent.requestPermission().then(v=>{if(v==='granted')window.addEventListener('deviceorientation',orientation,true);else $('zadCompassStatus').textContent='لم يتم السماح بالبوصلة.';}).catch(()=>{$('zadCompassStatus').textContent='تعذر تشغيل مستشعر الاتجاه.';});}else{window.addEventListener('deviceorientation',orientation,true);window.addEventListener('deviceorientationabsolute',orientation,true);$('zadCompassStatus').textContent='جارٍ انتظار مستشعر الهاتف...';}});
$('zadQiblaLocate').addEventListener('click',()=>{if(!navigator.geolocation){$('zadCompassStatus').textContent='تحديد الموقع غير مدعوم؛ سنستخدم المدينة المختارة.';return;}$('zadCompassStatus').textContent='جارٍ تحديد موقعك...';navigator.geolocation.getCurrentPosition(pos=>{coords=[pos.coords.latitude,pos.coords.longitude];update();$('zadCompassStatus').textContent='تم تحديد موقعك. شغّل البوصلة لمعرفة الاتجاه أثناء تحريك الهاتف.';},()=>{$('zadCompassStatus').textContent='تعذر تحديد الموقع؛ سنستخدم المدينة المختارة.';},{enableHighAccuracy:true,timeout:12000,maximumAge:300000});});
$('city')?.addEventListener('change',()=>{if(!coords)update();});update();
const grid=$('zadMoreGrid');grid.append(settingsTile);items.forEach(x=>{const b=document.createElement('button');b.type='button';b.className='zad-more-item';b.innerHTML='<span>'+x.icon+'</span><b>'+x.title+'</b>';b.addEventListener('click',()=>showPage(x.id));grid.append(b);});
const nav=document.createElement('nav');nav.className='bottom-nav zad-bottom-nav';nav.setAttribute('aria-label','التنقل بين صفحات التطبيق');
[['home','الرئيسية','⌂'],['prayer','المواقيت','◷'],['calendar','التقويم','🗓️'],['quran','القرآن','▤'],['mushaf','المصحف','۞'],['qibla','القبلة','🧭'],['tasbeeh','السبحة','📿'],['more','المزيد','☷']].forEach(([id,label,icon])=>{const b=document.createElement('button');b.type='button';b.className='zad-nav-item';b.dataset.target=id;b.innerHTML='<span>'+icon+'</span><small>'+label+'</small>';b.addEventListener('click',()=>showPage(id));nav.append(b);});document.body.append(nav);
const back=document.createElement('button');back.type='button';back.id='zadSectionBack';back.className='zad-section-back';back.textContent='→ الرئيسية';back.addEventListener('click',()=>showPage('home'));document.body.append(back);
nav.addEventListener('click',event=>{const button=event.target.closest('.zad-nav-item');if(button){event.preventDefault();event.stopPropagation();showPage(button.dataset.target);}},true);nav.style.pointerEvents='auto';nav.style.position='fixed';nav.style.bottom='0';nav.style.left='0';nav.style.right='0';nav.style.zIndex='99999';
function showPage(id,fromPop=false){const isSection=items.some(x=>x.id===id);const target=isSection?$(id):$('zad-page-'+id);if(!target)return false;const homeActive=id==='home';const fullReader=!homeActive&&id!=='more'&&id!=='settings';root.querySelectorAll('.zad-page').forEach(p=>{const active=!isSection&&p===target;p.classList.toggle('is-active',active);p.setAttribute('aria-hidden',active?'false':'true');p.style.setProperty('display',active?'block':'none','important');});nav.querySelectorAll('.zad-nav-item').forEach(b=>{const active=b.dataset.target===id;b.classList.toggle('active',active);b.setAttribute('aria-current',active?'page':'false');});document.body.classList.toggle('zad-mushaf-reading',id==='mushaf');document.body.classList.toggle('zad-section-reading',isSection&&id!=='mushaf');document.body.classList.toggle('zad-virtual-reading',!isSection&&fullReader);back.style.display=fullReader&&id!=='mushaf'?'block':'none';nav.style.display=id==='mushaf'?'none':'flex';header.style.display='none';main.style.display=isSection?'':'none';if(footer)footer.style.display='none';if(isSection){[...main.children].forEach(child=>{const keep=child===target||(id==='prayer'&&child.classList.contains('date-strip'));child.style.setProperty('display',keep?'block':'none','important');});target.style.setProperty('display','block','important');}root.style.display='block';window.scrollTo(0,0);if(!fromPop&&location.hash!=='#'+id)history.pushState({zadPage:id},'','#'+id);if(id==='mushaf'&&$('mushafContent')?.classList.contains('hidden'))$('mushafToggle')?.click();if(id==='qibla')update();return true;}
$('mushafExitReader')?.addEventListener('click',()=>showPage('home'));
window.zadShowPage=showPage;const valid=['home','prayer','quran','qibla','tasbeeh','more','settings',...items.map(x=>x.id)];let initial=location.hash.slice(1);showPage(valid.includes(initial)?initial:'home',true);window.addEventListener('popstate',()=>{const id=location.hash.slice(1);showPage(valid.includes(id)?id:'home',true);});
document.querySelectorAll('a[href^="#"]').forEach(a=>{const id=a.getAttribute('href').slice(1);if(valid.includes(id))a.addEventListener('click',e=>{e.preventDefault();showPage(id);});});
const css=document.createElement('style');
css.textContent+=`
.zad-page#zad-page-tasbeeh .tasbeeh-panel{position:relative!important;inset:auto!important;display:block!important;width:100%!important;max-width:100%!important;margin:12px 0!important;box-sizing:border-box!important}.zad-page#zad-page-tasbeeh .tasbeeh-progress-wrap{width:100%}.zad-page#zad-page-tasbeeh .tasbeeh-count-btn{margin:9px auto 12px!important}#zad-page-home{max-width:760px;min-height:calc(100dvh - 76px);padding:22px 16px 34px}
.zad-home-brand{display:flex;align-items:center;justify-content:center;gap:13px;margin:8px auto 26px;padding:18px 12px;text-align:right}
.zad-home-brand>span{width:58px;height:58px;display:grid;place-items:center;border-radius:18px;background:linear-gradient(145deg,#0c3b32,#176149);color:#f3d99e;font-size:32px;box-shadow:0 8px 22px #0c3b3228}
.zad-home-brand h1{margin:0;color:#0c3b32;font-size:27px;line-height:1.4}
.zad-home-brand p{margin:2px 0 0;color:#71817b;font-size:13px}
.zad-home-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}
.zad-home-item{position:relative;min-width:0;min-height:116px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:9px;padding:13px 7px;border:1px solid #e2eae2;border-radius:18px;background:linear-gradient(160deg,#fff,#f9fbf8);color:#18332d;font:inherit;box-shadow:0 5px 18px #12372b08;transition:transform .18s,border-color .18s,box-shadow .18s}
.zad-home-item:active{transform:scale(.97)}
.zad-home-item:hover{border-color:#c9a45c;box-shadow:0 9px 22px #12372b12}
.zad-home-icon{width:49px;height:49px;display:grid;place-items:center;border-radius:15px;background:#eaf2ed;color:#0c3b32;font-size:27px}
.zad-home-item strong{font-size:12px;line-height:1.5;text-align:center}
.zad-home-arrow{position:absolute;left:9px;top:8px;color:#b49a64;font-size:13px}
@media(max-width:380px){.zad-home-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.zad-home-item{min-height:108px}.zad-home-brand h1{font-size:24px}}
`;
css.textContent+="\nbody.zad-mushaf-reading{padding:0!important;margin:0!important;overflow:hidden!important;background:#080b0a!important}\nbody.zad-mushaf-reading .top,body.zad-mushaf-reading .azkar-ticker,body.zad-mushaf-reading header.hero,body.zad-mushaf-reading footer,body.zad-mushaf-reading .zad-bottom-nav,body.zad-mushaf-reading .zad-page-heading{display:none!important}\nbody.zad-mushaf-reading #zadAppPages{display:none!important}\nbody.zad-mushaf-reading main.wrap{display:block!important;position:absolute!important;inset:0!important;width:100%!important;max-width:none!important;margin:0!important;padding:0!important;overflow-y:auto!important;background:#080b0a!important}\nbody.zad-mushaf-reading #mushaf{display:block!important;position:relative!important;width:100%!important;min-height:100dvh!important;height:100dvh!important;max-width:none!important;margin:0!important;padding:8px 8px 12px!important;overflow-y:auto!important;box-sizing:border-box!important;background:#080b0a!important;color:#f5f0e5!important}\nbody.zad-mushaf-reading #mushaf>.section-head{display:none!important}\nbody.zad-mushaf-reading #mushaf .mushaf-card{display:flex!important;flex-direction:column!important;gap:7px!important;width:100%!important;max-width:none!important;height:100%!important;min-height:0!important;margin:0!important;padding:6px!important;border:0!important;border-radius:0!important;box-shadow:none!important;background:#080b0a!important;color:#f5f0e5!important}\nbody.zad-mushaf-reading #mushafContent{display:flex!important;flex:1 1 auto!important;min-height:0!important;flex-direction:column!important;gap:7px!important}\nbody.zad-mushaf-reading #mushafExitReader{align-self:flex-start!important;flex:0 0 auto!important;padding:7px 12px!important}\nbody.zad-mushaf-reading .mushaf-controls{display:grid!important;grid-template-columns:minmax(0,1fr) minmax(0,1fr)!important;gap:7px!important;flex:0 0 auto!important}\nbody.zad-mushaf-reading .mushaf-page-jump{grid-template-columns:auto minmax(45px,1fr) auto!important;gap:4px!important}\nbody.zad-mushaf-reading .mushaf-navigation{display:flex!important;align-items:center!important;justify-content:space-between!important;gap:5px!important;flex:0 0 auto!important}\nbody.zad-mushaf-reading .mushaf-navigation button{padding:7px 9px!important;font-size:12px!important}\nbody.zad-mushaf-reading .mushaf-heading h3{font-size:14px!important;margin:0!important}\nbody.zad-mushaf-reading #mushafStatus{margin:0!important;font-size:11px!important;flex:0 0 auto!important}\nbody.zad-mushaf-reading #mushafSwipeArea{flex:1 1 auto!important;min-height:0!important;width:100%!important;height:auto!important;max-width:none!important;margin:0!important;padding:0!important;overflow:hidden!important;border:0!important;border-radius:3px!important;background:#111!important}\nbody.zad-mushaf-reading #mushafPageImage{width:auto!important;height:auto!important;max-width:100%!important;max-height:calc(100dvh - 205px)!important;object-fit:contain!important;object-position:center!important}\nbody.zad-mushaf-reading .mushaf-swipe-hint,body.zad-mushaf-reading .mushaf-source{display:none!important}\n@media(max-width:600px){body.zad-mushaf-reading #mushaf{padding:5px 5px 8px!important}body.zad-mushaf-reading #mushafPageImage{max-height:calc(100dvh - 205px)!important}}\n";css.textContent+=`
body.zad-section-reading{padding:0!important;margin:0!important;background:#f7f8f5!important}
body.zad-virtual-reading{padding:0!important;margin:0!important;overflow:hidden!important;background:#f7f8f5!important}
body.zad-virtual-reading #zadAppPages{display:block!important;position:fixed!important;inset:0!important;z-index:99998!important;overflow-y:auto!important;padding:calc(env(safe-area-inset-top) + 54px) 0 calc(env(safe-area-inset-bottom) + 12px)!important;background:#f7f8f5!important;box-sizing:border-box!important}
body.zad-virtual-reading #zadAppPages .zad-page.is-active{display:block!important;min-height:calc(100dvh - 66px)!important;padding:14px!important}

body.zad-section-reading header.hero,body.zad-section-reading footer,body.zad-section-reading .top,body.zad-section-reading .azkar-ticker{display:none!important}\nbody.zad-section-reading .zad-bottom-nav{display:flex!important;position:fixed!important;left:0!important;right:0!important;bottom:0!important;z-index:100000!important}
body.zad-section-reading #zadAppPages{display:none!important}
body.zad-section-reading main.wrap{display:block!important;position:fixed!important;inset:0!important;width:100vw!important;height:100dvh!important;max-width:none!important;margin:0!important;padding:calc(env(safe-area-inset-top) + 54px) 0 calc(env(safe-area-inset-bottom) + 12px)!important;overflow-y:auto!important;overscroll-behavior:contain!important;background:#f7f8f5!important;z-index:99998!important;box-sizing:border-box!important}
body.zad-section-reading main.wrap>*{max-width:100%!important}
body.zad-section-reading main.wrap>.section{padding-left:12px!important;padding-right:12px!important}
.zad-section-back{display:none;position:fixed;z-index:100001;top:calc(env(safe-area-inset-top) + 8px);right:12px;padding:9px 16px;border:1px solid #dce8df;border-radius:22px;background:#0c3b32;color:#fff;font:600 13px inherit;box-shadow:0 4px 14px #12372b25}
.zad-home-welcome{max-width:440px;margin:24px auto;padding:30px 20px;text-align:center;border:1px solid #e5ebe5;border-radius:20px;background:#fff;color:#426057;box-shadow:0 8px 24px #12372b0b}
.zad-home-welcome span{display:block;color:#b28a3c;font-size:34px;margin-bottom:12px}
.zad-home-welcome p{margin:0;line-height:1.9;font-size:15px}
`;
css.textContent+=`
.zad-home-shortcuts,.zad-home-daily{margin:16px 0;padding:18px;border-radius:20px;background:var(--paper,#fff);color:var(--ink,#18332d);border:1px solid var(--line,#e5ebe5)}.zad-home-shortcuts h2{margin:0 0 14px;font-size:20px}.zad-home-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.zad-home-grid button{display:flex;flex-direction:column;align-items:flex-start;gap:5px;text-align:right;padding:15px 12px;border:1px solid var(--line,#e5ebe5);border-radius:16px;background:var(--soft,#eaf2ed);color:var(--ink,#18332d)}.zad-home-grid button span{font-size:25px}.zad-home-grid button b{font-size:14px}.zad-home-grid button small{font-size:11px;color:var(--muted,#71817b)}.zad-home-daily>span{font-weight:800;color:var(--green,#0c3b32)}.zad-home-daily p{font-size:18px;line-height:2;margin:8px 0}.zad-home-daily small{color:var(--muted,#71817b)}.zad-home-welcome h2{margin:5px 0;font-size:20px}.zad-home-welcome p{margin:5px 0}@media(min-width:650px){.zad-home-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}.zad-settings-group{margin:14px 0;padding:16px;border:1px solid #e2eae2;border-radius:18px;background:var(--paper,#fff);color:var(--ink,#18332d)}
.zad-settings-group h3{margin:0 0 5px;color:var(--green,#0c3b32);font-size:17px}
.zad-settings-group>p{margin:0 0 13px;color:var(--muted,#71817b);font-size:13px}
.zad-settings-group label{display:block;margin:10px 0 5px;font-size:13px}
.zad-settings-group select,.zad-settings-group input[type=range]{width:100%;margin:4px 0 10px}
.zad-theme-options{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px}
.zad-theme-options button{padding:12px 8px;border:1px solid var(--line,#e5ebe5);border-radius:12px;background:var(--soft,#eaf2ed);color:var(--ink,#18332d);font:inherit}
.zad-theme-options button.active{outline:2px solid var(--gold,#c9a45c);font-weight:800}
#zadAudioSettingsSlot>.adhan-settings,#zadReminderSettingsSlot>#daily-reminder-settings{display:block!important;margin:0!important}
#zadAudioSettingsSlot>.adhan-settings{padding:10px!important;border-radius:12px!important;font-size:13px!important}
#zadAudioSettingsSlot>.adhan-settings>div{margin-top:7px!important}
#zadAudioSettingsSlot>.adhan-settings h3{font-size:14px!important;margin:0 0 4px!important}
#zadAudioSettingsSlot>.adhan-settings p{font-size:11px!important;line-height:1.45!important;margin:3px 0 6px!important}
#zadAudioSettingsSlot>.adhan-settings .row{gap:6px!important;flex-wrap:wrap!important;margin:6px 0!important}
#zadAudioSettingsSlot>.adhan-settings select,#zadAudioSettingsSlot>.adhan-settings input:not([type=checkbox]):not([type=range]){min-height:34px!important;padding:5px 8px!important;font-size:12px!important}
#zadAudioSettingsSlot>.adhan-settings button{min-height:34px!important;padding:6px 9px!important;font-size:12px!important}
#zadAudioSettingsSlot>.adhan-settings>.card{padding:8px!important;margin:7px 0!important;border-radius:10px!important}
#zadAudioSettingsSlot>.adhan-settings .prayer-alert-list{gap:5px!important;margin:6px 0!important}
#zadAudioSettingsSlot>.adhan-settings .prayer-alert-list label{font-size:12px!important;margin:2px 0!important}
#zadAudioSettingsSlot>.adhan-settings .adhan-master{font-size:12px!important;margin:7px 0!important}
#zadAdhanProgress{height:5px!important;width:100%!important;margin:3px 0 6px!important;border-radius:8px!important;flex:0 0 100%!important;order:-1!important}

#zadReminderSettingsSlot #daily-reminder-settings .section-head{display:none!important}
#zadReminderSettingsSlot #daily-reminder-settings .grid{grid-template-columns:1fr!important}
#zadReminderSettingsSlot #daily-reminder-settings .card{margin:8px 0!important}
body.zad-theme-dark{--bg:#101a22;--paper:#172632;--ink:#e8f0f5;--muted:#a9bacb;--green:#75cdbb;--green2:#32b9a6;--line:#294253;--soft:#203744;--shadow:0 8px 24px #0003;background:var(--bg)!important;color:var(--ink)!important}
body.zad-theme-dark .zad-settings-group,body.zad-theme-dark .zad-page-heading,body.zad-theme-dark .card,body.zad-theme-dark .date-strip,body.zad-theme-dark .surah-item,body.zad-theme-dark .zikr-item{background:var(--paper)!important;color:var(--ink)!important;border-color:var(--line)!important}
body.zad-theme-dark .zad-page-heading h2,body.zad-theme-dark .zad-settings-group h3,body.zad-theme-dark .card h3,body.zad-theme-dark .section h2{color:var(--green)!important}
body.zad-theme-dark input,body.zad-theme-dark select{background:#10202b!important;color:var(--ink)!important;border-color:var(--line)!important}
body.zad-theme-blue{--bg:#edf3fa;--paper:#fff;--ink:#20364f;--muted:#667d96;--green:#244e78;--green2:#3977a8;--line:#d6e2ef;--soft:#e2edf7;--gold:#c49b55;background:var(--bg)!important;color:var(--ink)!important}
body.zad-theme-sand{--bg:#f5efe3;--paper:#fffaf1;--ink:#493c2d;--muted:#88765f;--green:#725333;--green2:#987344;--line:#e9dcc6;--soft:#f0e5d2;--gold:#b78b45;background:var(--bg)!important;color:var(--ink)!important}
body.zad-theme-blue .zad-settings-group,body.zad-theme-sand .zad-settings-group{background:var(--paper)!important;color:var(--ink)!important}
`;
css.textContent+='body{padding-bottom:calc(72px + env(safe-area-inset-bottom))}.zad-page{display:none;max-width:1180px;margin:0 auto;padding:16px 14px 30px;min-height:55vh}.zad-page.is-active{display:block}.zad-page-heading{display:flex;align-items:center;gap:12px;margin:8px 0 18px;padding:13px 15px;border:1px solid #e5ebe5;border-radius:16px;background:#fff}.zad-page-heading>span{width:46px;height:46px;display:grid;place-items:center;border-radius:13px;background:#eaf2ed;font-size:24px}.zad-page-heading h2{margin:0;color:#0c3b32;font-size:21px}.zad-page-heading p{margin:3px 0 0;color:#71817b;font-size:12px}.zad-bottom-nav{z-index:1500;justify-content:space-around;background:#102236f5;border-top:1px solid #243d53;padding:6px 3px calc(6px + env(safe-area-inset-bottom));gap:1px}.zad-nav-item{border:0;background:transparent;color:#a9bacb;display:flex;flex:1;min-width:0;flex-direction:column;align-items:center;gap:2px;padding:4px 1px;font:inherit}.zad-nav-item>span{font-size:20px;line-height:1.2;color:#32c7b5}.zad-nav-item small{font-size:9px;white-space:nowrap}.zad-nav-item.active{color:#f3d99e}.zad-nav-item.active>span{color:#f3d99e}.zad-more-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px}.zad-more-item{display:flex;align-items:center;gap:9px;text-align:right;padding:13px;border:1px solid #e5ebe5;border-radius:14px;background:#fff;color:#18332d;font:inherit}.zad-more-item span{font-size:22px}.zad-more-item b{font-size:13px}#tasbeehPanel{display:block!important;visibility:visible!important;pointer-events:auto!important;position:static!important;right:auto!important;bottom:auto!important;width:100%!important;max-width:440px!important;margin:0 auto!important}.zad-tasbeeh-card{position:static!important;display:block!important;visibility:visible!important;pointer-events:auto!important;width:100%!important;max-width:440px;margin:0 auto;padding:18px!important;box-shadow:0 10px 30px #12372b12!important}.zad-tasbeeh-card .tasbeeh-progress-wrap{padding:18px 0}.zad-qibla-card{max-width:520px;margin:auto;text-align:center}.zad-qibla-location{display:flex;justify-content:space-between;gap:8px;color:#71817b;font-size:13px}.zad-qibla-location strong{color:#0c3b32}.zad-compass{width:min(78vw,290px);aspect-ratio:1;margin:25px auto;position:relative;border-radius:50%;border:8px solid #dce8df;background:radial-gradient(circle,#fff 0 57%,#edf4ee 58%);box-shadow:inset 0 0 0 2px #fff,0 8px 24px #12372b18}.zad-compass:before{content:"";position:absolute;inset:10px;border:1px dashed #b9cbbd;border-radius:50%}.zad-cardinal{position:absolute;font-size:12px;font-weight:800;color:#456257}.zad-n{top:10px;left:50%;transform:translateX(-50%);color:#b28a3c}.zad-s{bottom:10px;left:50%;transform:translateX(-50%)}.zad-e{right:10px;top:50%;transform:translateY(-50%)}.zad-w{left:10px;top:50%;transform:translateY(-50%)}.zad-qibla-needle{position:absolute;left:50%;top:50%;margin-left:-22px;margin-top:-38px;transform-origin:center center;display:flex;flex-direction:column;align-items:center;color:#b28a3c;font-size:60px;line-height:1;transition:transform .18s linear}.zad-qibla-needle b{font-size:11px;background:#fff;padding:2px 7px;border-radius:8px}.zad-compass-center{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:32px;height:32px;display:grid;place-items:center;border-radius:50%;background:#0c3b32;color:#f3d99e;border:3px solid #fff}.zad-qibla-degree{font-size:19px;font-weight:800;color:#0c3b32}.zad-qibla-actions{justify-content:center;margin:14px auto;max-width:350px}.zad-qibla-actions>*{flex:1}.zad-home-header{display:block}@media(max-width:520px){.zad-page{padding:12px 10px 26px}.zad-page-heading h2{font-size:18px}.zad-nav-item small{font-size:8px}.zad-nav-item>span{font-size:18px}}';
css.textContent+=`
body.zad-mushaf-reading{padding:0!important;margin:0!important;overflow:hidden!important;background:#f8f3e8!important}
body.zad-mushaf-reading .top,body.zad-mushaf-reading .azkar-ticker,body.zad-mushaf-reading header.hero,body.zad-mushaf-reading footer,body.zad-mushaf-reading .zad-bottom-nav,body.zad-mushaf-reading .zad-page-heading{display:none!important}
body.zad-mushaf-reading #zadAppPages{display:none!important}
body.zad-mushaf-reading main.wrap{display:block!important;position:fixed!important;inset:0!important;width:100vw!important;height:100dvh!important;max-width:none!important;margin:0!important;padding:0!important;overflow:hidden!important;background:#f8f3e8!important;z-index:99998!important}
body.zad-mushaf-reading #mushaf{display:block!important;position:absolute!important;inset:0!important;width:100%!important;height:100%!important;min-height:0!important;max-width:none!important;margin:0!important;padding:0!important;overflow:hidden!important;box-sizing:border-box!important;background:#f8f3e8!important;color:#fff!important}
body.zad-mushaf-reading #mushaf>.section-head,body.zad-mushaf-reading #mushaf .mushaf-toggle,body.zad-mushaf-reading .mushaf-swipe-hint,body.zad-mushaf-reading .mushaf-source{display:none!important}
body.zad-mushaf-reading #mushaf .mushaf-card{display:block!important;position:absolute!important;inset:0!important;width:100%!important;height:100%!important;min-height:0!important;max-width:none!important;margin:0!important;padding:0!important;border:0!important;border-radius:0!important;box-shadow:none!important;background:#f8f3e8!important}
body.zad-mushaf-reading #mushafContent{display:flex!important;position:absolute!important;inset:0!important;width:100%!important;height:100%!important;min-height:0!important;flex-direction:column!important;gap:0!important;margin:0!important;padding:0!important;background:#f8f3e8!important}
body.zad-mushaf-reading #mushafExitReader{position:absolute!important;z-index:5!important;top:calc(env(safe-area-inset-top) + 8px)!important;left:10px!important;align-self:auto!important;padding:7px 11px!important;border:1px solid #ffffff55!important;border-radius:20px!important;background:#101b22dd!important;color:#fff!important;font-size:12px!important;opacity:.88}
body.zad-mushaf-reading .mushaf-controls{display:none!important;position:absolute!important;z-index:4!important;top:calc(env(safe-area-inset-top) + 48px)!important;left:8px!important;right:8px!important;grid-template-columns:1fr!important;gap:6px!important;padding:8px!important;border:1px solid #ffffff33!important;border-radius:12px!important;background:#101b22ed!important}
body.zad-mushaf-reading .mushaf-navigation{display:none!important;position:absolute!important;z-index:4!important;left:8px!important;right:8px!important;bottom:calc(env(safe-area-inset-bottom) + 8px)!important;margin:0!important;padding:6px!important;border:1px solid #ffffff33!important;border-radius:12px!important;background:#101b22ed!important;color:#fff!important}
body.zad-mushaf-reading .mushaf-navigation button{padding:7px 9px!important;font-size:11px!important}
body.zad-mushaf-reading .mushaf-heading h3,body.zad-mushaf-reading .mushaf-heading span{color:#fff!important;font-size:11px!important}
body.zad-mushaf-reading #mushafStatus{display:none!important;position:absolute!important;z-index:4!important;left:10px!important;right:10px!important;bottom:calc(env(safe-area-inset-bottom) + 58px)!important;margin:0!important;padding:5px!important;border-radius:8px!important;background:#101b22dd!important;color:#fff!important;font-size:10px!important}
body.zad-mushaf-reading #mushafSwipeArea{display:flex!important;position:absolute!important;inset:0!important;width:100%!important;height:100%!important;min-height:0!important;max-width:none!important;margin:0!important;padding:0!important;align-items:center!important;justify-content:center!important;overflow:hidden!important;border:0!important;border-radius:0!important;background:#f8f3e8!important;touch-action:pan-y!important}
body.zad-mushaf-reading #mushafPageImage{display:block!important;width:100%!important;height:100%!important;max-width:100vw!important;max-height:100dvh!important;margin:0 auto!important;object-fit:contain!important;object-position:center!important;background:transparent!important;filter:none!important;opacity:1!important;forced-color-adjust:none!important}
body.zad-mushaf-reading #mushafContent.zad-reader-controls-visible .mushaf-controls,body.zad-mushaf-reading #mushafContent.zad-reader-controls-visible .mushaf-navigation,body.zad-mushaf-reading #mushafContent.zad-reader-controls-visible #mushafStatus{display:grid!important}
body.zad-mushaf-reading #mushafContent.zad-reader-controls-visible .mushaf-navigation{display:flex!important}
`;
const swipe=document.getElementById('mushafSwipeArea');
if(swipe&&!swipe.dataset.readerTapBound){swipe.dataset.readerTapBound='1';swipe.addEventListener('click',()=>{const content=document.getElementById('mushafContent');if(content)content.classList.toggle('zad-reader-controls-visible');});}

/* Radio playback controls are wired only in the native Android app. */
if(nativeApp){
 const radio=$('egyptQuranRadio'), fallback=$('radioFallbackBtn'), primary=$('radioPrimaryBtn'), status=$('radioStatus');
 if(radio&&!radio.dataset.zadRadioBound){
  radio.dataset.zadRadioBound='1';
  const cairo='https://service.webvideocore.net/CL1olYogIrDWvwqiIKK7eCxOS4PStqG9DuEjAr2ZjZQtvS3d4y9r0cvRhvS17SGN/a_7a4vuubc6mo8.m3u8';
  const backup='https://qurango.net/radio/tarateel';
  const setStatus=t=>{if(status)status.textContent=t;};
  const stop=document.createElement('button'); stop.type='button'; stop.className='btn light'; stop.textContent='إيقاف الإذاعة'; stop.style.marginTop='8px'; radio.insertAdjacentElement('afterend',stop);
  radio.hidden=true;
  const playSource=(url,label,which)=>{
   setStatus('جارٍ تشغيل '+label+' في الخلفية...');
   try { window.ZadAndroid.playRadio(which,label); setStatus(label+' تعمل في الخلفية. يمكنك الخروج من التطبيق.'); }
   catch(e) { setStatus('تعذر بدء تشغيل الإذاعة.'); }
  };
  fallback?.addEventListener('click',()=>playSource(backup,'البث البديل لتلاوات القرآن','backup'));
  primary?.addEventListener('click',()=>playSource(cairo,'إذاعة القرآن الكريم من القاهرة','cairo'));
  stop.addEventListener('click',()=>{try{window.ZadAndroid.stopRadio();setStatus('تم طلب إيقاف الإذاعة.');}catch(e){setStatus('تعذر إيقاف الإذاعة.');}});

 }
}
// تثبيت إعدادات الأذان داخل صفحة مواقيت الصلاة في موضع واضح ومستقل.
const prayerSection=$('prayer');
const adhanSettings=prayerSection?.querySelector('.adhan-settings');
if(adhanSettings){
 let audioSlot=$('zadAudioSettingsSlot');
 if(!audioSlot){audioSlot=document.createElement('div');audioSlot.id='zadAudioSettingsSlot';audioSlot.className='zad-prayer-audio-settings';adhanSettings.parentNode.insertBefore(audioSlot,adhanSettings);}
 audioSlot.append(adhanSettings);
}
// تبقى تنبيهات الأذكار اليومية ضمن إعدادات البرنامج.
const reminderSettings=$('daily-reminder-settings');
if(reminderSettings)$('zadReminderSettingsSlot').append(reminderSettings);
const themeNames=['light','dark','blue','sand'];
function applySettings(){
 const theme=localStorage.getItem('zad_theme')||'light';
 document.body.classList.remove('zad-theme-dark','zad-theme-blue','zad-theme-sand');
 if(theme!=='light')document.body.classList.add('zad-theme-'+theme);
 document.querySelectorAll('[data-zad-theme]').forEach(b=>b.classList.toggle('active',b.dataset.zadTheme===theme));
 const family=localStorage.getItem('zad_font_family')||'Tahoma,Arial,sans-serif';
 const size=Math.max(14,Math.min(22,Number(localStorage.getItem('zad_font_size')||16)));
 document.body.style.fontFamily=family;
 document.body.style.fontSize=size+'px';
 if($('zadFontFamily'))$('zadFontFamily').value=family;
 if($('zadFontSize'))$('zadFontSize').value=String(size);
 if($('zadFontSizeValue'))$('zadFontSizeValue').textContent=String(size);
}
document.querySelectorAll('[data-zad-theme]').forEach(b=>b.addEventListener('click',()=>{localStorage.setItem('zad_theme',b.dataset.zadTheme);applySettings();}));
$('zadFontFamily')?.addEventListener('change',e=>{localStorage.setItem('zad_font_family',e.target.value);applySettings();});
$('zadFontSize')?.addEventListener('input',e=>{localStorage.setItem('zad_font_size',e.target.value);applySettings();});
applySettings();
document.head.append(css);

/* Custom content tabs managed from the admin panel. */
(async function loadManagedTabs(){
 try{
  const {initializeApp,getApps}=await import("https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js");
  const {getFirestore,doc,getDoc,collection,getDocs,query,where}=await import("https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js");
  const cfg={apiKey:"AIzaSyAezfJxlPIlreW_iKuCmk7skF5eQ59nC2s",authDomain:"zad-almoslem.firebaseapp.com",projectId:"zad-almoslem",storageBucket:"zad-almoslem.firebasestorage.app",messagingSenderId:"138327285052",appId:"1:138327285052:web:2024f2f17aba593fa2d239"};
  const app=getApps().length?getApps()[0]:initializeApp(cfg),db=getFirestore(app);
  const tabsSnap=await getDoc(doc(db,"waqafat","__custom_tabs__"));
  if(!tabsSnap.exists())return;
  const tabs=Array.isArray(tabsSnap.data().tabs)?tabsSnap.data().tabs.filter(t=>t&&t.id&&t.title&&t.visible!==false):[];
  if(!tabs.length)return;
  const safe=v=>String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  const kindNames={articles:"خواطر ومقالات",stories:"قصص",biographies:"سير وتراجم",general:""};
  tabs.sort((a,b)=>(Number(a.order)||0)-(Number(b.order)||0));
   for (const tab of tabs) {
   if($(tab.id)||items.some(x=>x.id===tab.id))return;
   const section=document.createElement("section");section.className="section";section.id=tab.id;
   const heading=document.createElement("div");heading.className="section-head";
   heading.innerHTML='<div><h2>'+safe(tab.icon||"✨")+' '+safe(tab.title)+'</h2><p>'+safe(kindNames[tab.kind]||"")+'</p></div>';
   section.append(heading);
    const publishedQuery=query(collection(db,"waqafat"),where("tabId","==",tab.id),where("published","==",true));
    const contentSnap=await getDocs(publishedQuery);
    const records=contentSnap.docs.map(d=>({id:d.id,...d.data()})).sort((a,b)=>(a.createdAt?.seconds||0)-(b.createdAt?.seconds||0));
   if(!records.length){const empty=document.createElement("p");empty.className="small muted";empty.textContent="لا يوجد محتوى منشور في هذا القسم حاليًا.";section.append(empty);}
   records.forEach(item=>{
    const article=document.createElement("article");article.className="card";article.style.margin="12px 0";
    const h=document.createElement("h3");h.textContent=item.title||tab.title;
    const body=document.createElement("p");body.style.whiteSpace="pre-wrap";body.style.lineHeight="2";body.textContent=item.body||"";
    article.append(h,body);
    if(item.source){const source=document.createElement("p");source.className="small";source.style.marginTop="10px";source.textContent="المصدر: "+item.source;article.append(source);}
    section.append(article);
   });
   main.append(section);items.push({id:tab.id,title:tab.title,icon:tab.icon||"✨"});
   const addTile=(container,cls)=>{const b=document.createElement("button");b.type="button";b.className=cls;b.dataset.target=tab.id;b.innerHTML='<span class="'+(cls==="zad-home-item"?"zad-home-icon":"")+'">'+safe(tab.icon||"✨")+'</span><strong>'+safe(tab.title)+'</strong>';b.addEventListener("click",()=>showPage(tab.id));container.append(b);};
   addTile(homeGrid,"zad-home-item");
   const moreGrid=$("zadMoreGrid");if(moreGrid)addTile(moreGrid,"zad-more-item");
   valid.push(tab.id);
  });
  const hash=location.hash.slice(1);if(tabs.some(t=>t.id===hash))showPage(hash,true);
 }catch(error){console.warn("تعذر تحميل التبويبات الإضافية",error);}
})();

})();