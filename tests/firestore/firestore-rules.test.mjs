import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { after, before, test } from "node:test";
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where
} from "firebase/firestore";

const PROJECT_ID = "demo-zad-almoslem";
const ADMIN_UID = "5UkeBQyyGGZcICXULT9LUD0S4lt2";
let env;

before(async () => {
  const rules = await readFile(new URL("../../firestore.rules", import.meta.url), "utf8");
  env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules }
  });

  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "azkar", "published-en"), { text: "ذكر منشور", published: true });
    await setDoc(doc(db, "azkar", "published-ar"), { text: "ذكر منشور", "تم النشر": true });
    await setDoc(doc(db, "azkar", "published-alt"), { text: "ذكر منشور", "النشر": true });
    await setDoc(doc(db, "azkar", "draft"), {
      text: "مسودة",
      published: false,
      "تم النشر": false,
      "النشر": false
    });
    await setDoc(doc(db, "duas", "draft"), { text: "دعاء مسودة", published: false });
    await setDoc(doc(db, "duas", "published-en"), { text: "دعاء منشور", published: true });
    await setDoc(doc(db, "duas", "published-ar"), { text: "دعاء منشور", "تم النشر": true });
    await setDoc(doc(db, "duas", "published-alt"), { text: "دعاء منشور", "النشر": true });
    await setDoc(doc(db, "khutbahs", "published"), { title: "خطبة منشورة", body: "نص الخطبة", published: true });
    await setDoc(doc(db, "khutbahs", "draft"), { title: "خطبة مسودة", body: "نص الخطبة", published: false });
    await setDoc(doc(db, "waqafat", "published"), { title: "وقفة منشورة", body: "نص الوقفة", published: true });
    await setDoc(doc(db, "waqafat", "draft"), { title: "وقفة مسودة", body: "نص الوقفة", published: false });
  });
});

after(async () => {
  await env?.cleanup();
});

test("public users can read documents published with any supported field", async () => {
  const db = env.unauthenticatedContext().firestore();
  for (const id of ["published-en", "published-ar", "published-alt"]) {
    await assertSucceeds(getDoc(doc(db, "azkar", id)));
  }
  await assertFails(getDoc(doc(db, "azkar", "draft")));
  await assertFails(getDoc(doc(db, "duas", "draft")));
  for (const id of ["published-en", "published-ar", "published-alt"]) {
    await assertSucceeds(getDoc(doc(db, "duas", id)));
  }
});

test("public publication queries used by Android sync are allowed", async () => {
  const db = env.unauthenticatedContext().firestore();
  for (const collectionName of ["azkar", "duas", "khutbahs", "waqafat"]) {
    const fields = collectionName === "azkar" || collectionName === "duas"
      ? ["published", "تم النشر", "النشر"]
      : ["published"];
    for (const field of fields) {
      const result = await assertSucceeds(
        getDocs(query(collection(db, collectionName), where(field, "==", true)))
      );
      assert.equal(result.empty, false, `${collectionName}.${field} should return published documents`);
    }
  }
});

test("only the configured admin can read drafts and perform content CRUD", async () => {
  const adminDb = env.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(getDoc(doc(adminDb, "azkar", "draft")));
  await assertSucceeds(getDoc(doc(adminDb, "duas", "draft")));
  await assertSucceeds(getDoc(doc(adminDb, "khutbahs", "draft")));
  await assertSucceeds(getDoc(doc(adminDb, "waqafat", "draft")));

  const newDua = doc(adminDb, "duas", "admin-created");
  await assertSucceeds(setDoc(newDua, { text: "دعاء جديد", source: "سورة البقرة: 201", published: false }));
  await assertSucceeds(updateDoc(newDua, { text: "دعاء بعد التعديل" }));
  await assertSucceeds(deleteDoc(newDua));

  const newKhutbah = doc(adminDb, "khutbahs", "admin-created");
  await assertSucceeds(setDoc(newKhutbah, { title: "خطبة جديدة", body: "النص", published: false }));
  await assertSucceeds(updateDoc(newKhutbah, { title: "خطبة معدلة" }));
  await assertSucceeds(deleteDoc(newKhutbah));

  const newWaqfa = doc(adminDb, "waqafat", "admin-created");
  await assertSucceeds(setDoc(newWaqfa, { title: "وقفة جديدة", body: "النص", published: false }));
  await assertSucceeds(updateDoc(newWaqfa, { title: "وقفة معدلة" }));
  await assertSucceeds(deleteDoc(newWaqfa));

  const newDoc = doc(adminDb, "azkar", "admin-created");
  await assertSucceeds(setDoc(newDoc, { text: "ذكر جديد", published: false }));
  await assertSucceeds(updateDoc(newDoc, { text: "ذكر بعد التعديل" }));
  await assertSucceeds(deleteDoc(newDoc));
});

test("authenticated non-admin users cannot read drafts or write content", async () => {
  const userDb = env.authenticatedContext("ordinary-user").firestore();
  await assertFails(getDoc(doc(userDb, "azkar", "draft")));
  await assertFails(getDoc(doc(userDb, "khutbahs", "draft")));
  await assertFails(getDoc(doc(userDb, "waqafat", "draft")));
  await assertFails(setDoc(doc(userDb, "khutbahs", "not-allowed"), { title: "محاولة", published: false }));
  await assertFails(setDoc(doc(userDb, "waqafat", "not-allowed"), { title: "محاولة", published: false }));
  await assertFails(setDoc(doc(userDb, "azkar", "not-allowed"), { text: "محاولة" }));
  await assertFails(updateDoc(doc(userDb, "azkar", "published-en"), { text: "تغيير غير مصرح" }));
  await assertFails(deleteDoc(doc(userDb, "azkar", "published-en")));
  await assertFails(setDoc(doc(userDb, "duas", "not-allowed"), { text: "محاولة", published: false }));
  await assertFails(updateDoc(doc(userDb, "duas", "published"), { text: "تغيير غير مصرح" }));
  await assertFails(deleteDoc(doc(userDb, "duas", "published")));
});
