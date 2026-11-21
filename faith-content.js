import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getFirestore, collection, getDocs, query, where } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyAezfJxlPIlreW_iKuCmk7skF5eQ59nC2s",
  authDomain: "zad-almoslem.firebaseapp.com",
  projectId: "zad-almoslem",
  storageBucket: "zad-almoslem.firebasestorage.app",
  messagingSenderId: "138327285052",
  appId: "1:138327285052:web:2024f2f17aba593fa2d239"
};

const db = getFirestore(initializeApp(firebaseConfig));
const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, ch => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
}[ch]));
const safeUrl = value => {
  try {
    const url = new URL(String(value || ""));
    return url.protocol === "https:" ? escapeHtml(url.href) : "";
  } catch (_) { return ""; }
};
const bodyHtml = value => escapeHtml(value).replace(/\r?\n/g, "<br>");

function khutbahCard(item) {
  const title = escapeHtml(item.title || "خطبة الجمعة");
  const category = escapeHtml(item.category || "خطبة الجمعة");
  const date = escapeHtml(item.date || "");
  const body = bodyHtml(item.body || item.text || "");
  const audio = safeUrl(item.audio);
  const pdf = safeUrl(item.pdf);
  return '<article class="card khutbah-card"><span class="topic-tag">' + category +
    '</span><h3>' + title + '</h3>' + (date ? '<p class="small">' + date + '</p>' : '') +
    (body ? '<details><summary>اقرأ الخطبة كاملة</summary><p style="white-space:normal;line-height:2;margin-top:10px">' + body + '</p></details>' : '') +
    (audio ? '<audio controls preload="none" style="width:100%"><source src="' + audio + '"></audio>' : '') +
    (pdf ? '<a class="btn light" href="' + pdf + '" target="_blank" rel="noopener">قراءة ملف الخطبة</a>' : '') +
    '</article>';
}

function waqfaCard(item) {
  return '<article class="card"><div class="icon">🌿</div><h3>' +
    escapeHtml(item.title || "وقفة إيمانية") + '</h3><p style="white-space:pre-line;line-height:2">' +
    bodyHtml(item.body || item.text || "") + '</p>' +
    (item.date ? '<p class="small" style="margin-top:10px">' + escapeHtml(item.date) + '</p>' : '') +
    '</article>';
}

async function renderCollection(name, targetId, renderer) {
  const target = document.getElementById(targetId);
  if (!target) return;
  try {
    const snapshot = await getDocs(query(collection(db, name), where("published", "==", true)));
    const rows = snapshot.docs.filter(doc => doc.id !== "__app_navigation__").map(doc => ({ id: doc.id, ...doc.data() }));
    rows.sort((a, b) => String(b.date || "").localeCompare(String(a.date || "")));
    target.innerHTML = rows.length ? rows.map(renderer).join("") : "";
  } catch (error) {
    console.warn("تعذر تحميل " + name, error);
    target.innerHTML = "";
  }
}

renderCollection("khutbahs", "remoteKhutbahList", khutbahCard);
renderCollection("waqafat", "remoteWaqafatList", waqfaCard);
