const ALLOWED_ORIGIN = "https://zad-almoslem.github.io";
const FIREBASE_PROJECT_ID = "zad-almoslem";
const ADMIN_UID = "5UkeBQyyGGZcICXULT9LUD0S4lt2";
const GITHUB_OWNER = "zad-almoslem";
const GITHUB_REPO = "zad-almoslem.github.io";
const GITHUB_BRANCH = "main";
const UPLOAD_DIRECTORY = "audio-uploads";
const REEL_IMAGE_DIRECTORY = "reels-images";
const MAX_FILE_SIZE = 8 * 1024 * 1024;
let jwksCache = null;
let jwksExpiresAt = 0;

function corsHeaders(origin) {
  return {
    "Access-Control-Allow-Origin": origin === ALLOWED_ORIGIN ? ALLOWED_ORIGIN : "null",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Access-Control-Max-Age": "86400",
    "Vary": "Origin"
  };
}
function json(data, status, origin) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders(origin), "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" }
  });
}
function decodeBase64Url(value) {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - value.length % 4) % 4);
  const binary = atob(base64);
  return Uint8Array.from(binary, c => c.charCodeAt(0));
}
function decodeJwtPart(value) {
  return JSON.parse(new TextDecoder().decode(decodeBase64Url(value)));
}
async function getFirebaseKeys() {
  if (jwksCache && Date.now() < jwksExpiresAt) return jwksCache;
  const response = await fetch("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com", {
    headers: { "Accept": "application/json" }
  });
  if (!response.ok) throw new Error("تعذر التحقق من مفاتيح Firebase.");
  const data = await response.json();
  jwksCache = data.keys || [];
  jwksExpiresAt = Date.now() + 60 * 60 * 1000;
  return jwksCache;
}
async function verifyAdminToken(request) {
  const authorization = request.headers.get("Authorization") || "";
  if (!authorization.startsWith("Bearer ")) throw new Error("يلزم تسجيل الدخول بحساب المسؤول.");
  const token = authorization.slice(7).trim();
  const parts = token.split(".");
  if (parts.length !== 3) throw new Error("رمز تسجيل الدخول غير صالح.");
  const header = decodeJwtPart(parts[0]);
  const claims = decodeJwtPart(parts[1]);
  if (header.alg !== "RS256" || !header.kid) throw new Error("نوع رمز الدخول غير مدعوم.");
  const now = Math.floor(Date.now() / 1000);
  if (claims.iss !== "https://securetoken.google.com/" + FIREBASE_PROJECT_ID ||
      claims.aud !== FIREBASE_PROJECT_ID || claims.sub !== ADMIN_UID ||
      !Number.isFinite(claims.exp) || claims.exp <= now ||
      !Number.isFinite(claims.iat) || claims.iat > now + 60 ||
      !Number.isFinite(claims.auth_time) || claims.auth_time > now + 60) {
    throw new Error("هذا الحساب غير مصرح له برفع الملفات.");
  }
  const jwks = await getFirebaseKeys();
  const jwk = jwks.find(key => key.kid === header.kid && key.kty === "RSA");
  if (!jwk) throw new Error("مفتاح التحقق من تسجيل الدخول غير متاح.");
  const publicKey = await crypto.subtle.importKey(
    "jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]
  );
  const valid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5", publicKey, decodeBase64Url(parts[2]),
    new TextEncoder().encode(parts[0] + "." + parts[1])
  );
  if (!valid) throw new Error("تعذر التحقق من صلاحية تسجيل الدخول.");
}
function bytesToBase64(bytes) {
  let binary = "";
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}
function fileExtension(name) {
  const match = String(name || "").toLowerCase().match(/\.([a-z0-9]+)$/);
  return match ? match[1] : "";
}
async function uploadToGitHub(file, kind, env) {
  const githubToken = env.GITHUB_TOKEN || env.GITHUB || env.GH_TOKEN;
  if (!githubToken) throw new Error("إعداد الرفع غير مكتمل على الخادم: أضف سر GitHub باسم GITHUB_TOKEN أو GITHUB في إعدادات Worker.");
  const ext = fileExtension(file.name);
  const audioTypes = { mp3: ["audio/mpeg", "audio/mp3"], m4a: ["audio/mp4", "audio/x-m4a", "audio/m4a"], wav: ["audio/wav", "audio/x-wav"], ogg: ["audio/ogg", "application/ogg"] };
  const imageTypes = { jpg: ["image/jpeg"], jpeg: ["image/jpeg"], png: ["image/png"], webp: ["image/webp"] };
  const allowed = kind === "audio" ? audioTypes : imageTypes;
  if (!allowed[ext]) throw new Error(kind === "audio" ? "الصيغ المدعومة للصوت: MP3 وM4A وWAV وOGG." : "الصيغ المدعومة للصورة: JPG وPNG وWebP.");
  const declaredType = (file.type || "").toLowerCase();
  if (declaredType && !allowed[ext].includes(declaredType)) throw new Error("نوع الملف لا يطابق امتداده.");
  if (!file.size || file.size > MAX_FILE_SIZE) throw new Error("حجم الملف يجب ألا يتجاوز 8 ميجابايت.");
  const directory = kind === "audio" ? UPLOAD_DIRECTORY : REEL_IMAGE_DIRECTORY;
  const filename = Date.now() + "-" + crypto.randomUUID() + "." + ext;
  const path = directory + "/" + filename;
  const bytes = new Uint8Array(await file.arrayBuffer());
  const apiPath = path.split("/").map(encodeURIComponent).join("/");
  const response = await fetch("https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/contents/" + apiPath, {
    method: "PUT",
    headers: {
      "Authorization": "Bearer " + githubToken,
      "Accept": "application/vnd.github+json",
      "Content-Type": "application/json",
      "X-GitHub-Api-Version": "2022-11-28"
    },
    body: JSON.stringify({
      message: "Upload " + (kind === "audio" ? "reel audio" : "reel image") + " asset",
      content: bytesToBase64(bytes),
      branch: GITHUB_BRANCH
    })
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) {
    const safeMessage = typeof result.message === "string" ? result.message.slice(0, 240) : "GitHub API returned an error.";
    throw new Error("فشل رفع الملف إلى GitHub (HTTP " + response.status + "): " + safeMessage);
  }
  return "https://zad-almoslem.github.io/" + path;
}
export default {
  async fetch(request, env) {
    const origin = request.headers.get("Origin") || "";
    if (request.method === "OPTIONS") {
      if (origin !== ALLOWED_ORIGIN) return new Response(null, { status: 403 });
      return new Response(null, { status: 204, headers: corsHeaders(origin) });
    }
    if (origin !== ALLOWED_ORIGIN) return json({ ok: false, error: "المصدر غير مسموح." }, 403, origin);
    if (request.method !== "POST") return json({ ok: false, error: "طريقة الطلب غير مدعومة." }, 405, origin);
    try {
      await verifyAdminToken(request);
      const form = await request.formData();
      const audio = form.get("audio");
      const image = form.get("image");
      let file, kind;
      if (audio instanceof File && !(image instanceof File)) { file = audio; kind = "audio"; }
      else if (image instanceof File && !(audio instanceof File)) { file = image; kind = "image"; }
      else throw new Error("أرسل ملفًا واحدًا فقط في الحقل audio أو image.");
      const url = await uploadToGitHub(file, kind, env);
      return json({ ok: true, url, kind, filename: file.name }, 200, origin);
    } catch (error) {
      return json({ ok: false, error: error?.message || "حدث خطأ غير متوقع." }, 400, origin);
    }
  }
};
