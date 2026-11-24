(() => {
  const root = document.getElementById('khutbahList');
  if (!root) return;
  const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[ch]));
  fetch('./khutbahs.json?updated=20261124')
    .then(response => { if (!response.ok) throw new Error('تعذر تحميل الملف'); return response.json(); })
    .then(items => {
      if (!Array.isArray(items) || !items.length) {
        root.innerHTML = '<div class="card"><h3>قريبًا بإذن الله</h3><p>ستُنشر هنا الخطب الجديدة بعد إضافتها من إدارة الموقع.</p></div>';
        return;
      }
      root.innerHTML = items.slice().reverse().map((item, index) => {
        const title = esc(item.title || 'خطبة الجمعة');
        const date = esc(item.date || '');
        const category = esc(item.category || 'خطبة الجمعة');
        const body = esc(item.body || '').replace(/\n/g, '<br>');
        const audio = item.audio ? '<audio controls preload="none" style="width:100%;margin-top:12px"><source src="' + esc(item.audio) + '"></audio>' : '';
        const pdf = item.pdf ? '<a class="btn light" href="' + esc(item.pdf) + '" target="_blank" rel="noopener noreferrer">قراءة ملف الخطبة</a>' : '';
        const source = item.sourceUrl ? '<a class="btn green" href="' + esc(item.sourceUrl) + '" target="_blank" rel="noopener noreferrer">المصدر الرسمي للخطبة</a>' : '';
        const content = body ? '<details class="khutbah-reader"><summary>افتح الخطبة</summary><div class="khutbah-full-text">' + body + '</div></details>' : '<p class="small">النص الكامل غير متاح حاليًا.</p>';
        return '<article class="card khutbah-card" data-khutbah-index="' + index + '"><span class="topic-tag">' + category + '</span><h3>' + title + '</h3>' +
          (date ? '<p class="small khutbah-date">' + date + '</p>' : '') + content + audio +
          (pdf || source ? '<div class="khutbah-extra-links">' + pdf + source + '</div>' : '') + '</article>';
      }).join('');
    })
    .catch(() => { root.innerHTML = '<div class="card"><p>تعذر تحميل الخطب الآن. حاول تحديث الصفحة بعد قليل.</p></div>'; });
})();