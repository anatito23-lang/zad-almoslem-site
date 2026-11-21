const CACHE_NAME = "zad-almoslem-shell-v86";
const APP_SHELL = ["./", "./index.html", "./manifest.webmanifest", "./icon.svg", "./daily-reminders.js?v=20261119native-diagnostics1", "./app-pages.js?v=20261119prayer-tab-fix4"];
self.addEventListener("install", event => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    await Promise.all(APP_SHELL.map(async path => {
      try {
        const response = await fetch(new Request(path, {cache:"reload"}));
        if (response && response.ok) await cache.put(path, response);
      } catch (error) {}
    }));
    await self.skipWaiting();
  })());
});
self.addEventListener("activate", event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key !== CACHE_NAME && key !== "zad-mushaf-offline-pages-v2").map(key => caches.delete(key)))).then(() => self.clients.claim()));
});
self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  const isMushafPage =
    (url.origin === "https://raw.githubusercontent.com" &&
      url.pathname.startsWith("/Miftah-Fentaw/Quran_webp/main/png/") &&
      url.pathname.endsWith(".png")) ||
    (url.origin === "https://cdn.jsdelivr.net" &&
      url.pathname.startsWith("/gh/Miftah-Fentaw/Quran_webp@main/png/") &&
      url.pathname.endsWith(".png"));
  if (isMushafPage) {
    event.respondWith((async () => {
      const cache = await caches.open("zad-mushaf-offline-pages-v2");
      const saved = await cache.match(request);
      if (saved) return saved;
      try {
        const response = await fetch(request);
        if (response && (response.ok || response.type === "opaque")) await cache.put(request, response.clone());
        return response;
      } catch (error) {
        return new Response("صفحة المصحف غير محفوظة على هذا الجهاز. اتصل بالإنترنت لتنزيلها.", {status:503,headers:{"Content-Type":"text/plain; charset=utf-8"}});
      }
    })());
    return;
  }
  if (url.origin !== self.location.origin) return;
  const isAppDocument = request.mode === "navigate" || /\/(?:index\.html|app-pages\.js|daily-reminders\.js)$/.test(url.pathname);
  if (isAppDocument) {
    event.respondWith((async () => {
      try {
        const fresh = await fetch(request, {cache:"no-store"});
        if (fresh && fresh.ok) {
          const cache = await caches.open(CACHE_NAME);
          await cache.put(request, fresh.clone());
        }
        return fresh;
      } catch (error) {
        return (await caches.match(request)) || (await caches.match("./index.html"));
      }
    })());
    return;
  }
  event.respondWith(caches.match(request).then(cached => cached || fetch(request).then(response => {
    if (response && response.ok) {
      const copy = response.clone();
      caches.open(CACHE_NAME).then(cache => cache.put(request, copy));
    }
    return response;
  }).catch(() => caches.match("./index.html"))));
});
