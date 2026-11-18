(() => {
  const root = document.getElementById('khutbahList');
  if (!root) return;
  const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[ch]));
  fetch('./khutbahs.json?updated=' + encodeURIComponent('2026-11-18'))
    .then(response => { if (!response.ok) throw new Error('تعذر تحميل الملف'); return response.json(); })
    .then(items => {
      if (!Array.isArray(items) || !items.length) {
        root.innerHTML = '<div class="card"><div class="icon">🕌</div><h3>قريبًا بإذن الله</h3><p>ستُنشر هنا الخطب الجديدة بعد إضافتها من إدارة الموقع.</p></div>';
        return;
      }
      root.innerHTML = items.slice().reverse().map(item => {
        const title = esc(item.title || 'خطبة الجمعة');
        const date = esc(item.date || '');
        const category = esc(item.category || 'خطبة');
        const body = esc(item.body || '').replace(/\n/g, '<br>');
        const audio = item.audio ? '<audio controls preload="none" style="width:100%"><source src="' + esc(item.audio) + '"></audio>' : '';
        const pdf = item.pdf ? '<a class="btn light" href="' + esc(item.pdf) + '" target="_blank" rel="noopener">قراءة ملف الخطبة</a>' : '';
        return '<article class="card khutbah-card"><span class="topic-tag">' + category + '</span><h3>' + title + '</h3>' +
          (date ? '<p class="small">' + date + '</p>' : '') +
          (body ? '<details><summary>اقرأ الخطبة كاملة</summary><p style="white-space:normal;line-height:2;margin-top:10px">' + body + '</p></details>' : '') +
          audio + pdf + '</article>';
      }).join('');
    })
    .catch(() => { root.innerHTML = '<div class="card"><p>تعذر تحميل الخطب الآن. حاول تحديث الصفحة بعد قليل.</p></div>'; });
})();