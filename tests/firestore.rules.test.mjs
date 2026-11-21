import { before, after, test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { initializeTestEnvironment, assertSucceeds, assertFails } from "@firebase/rules-unit-testing";
import {
  collection, doc, getDoc, getDocs, query, where, setDoc
} from "firebase/firestore";

const PROJECT_ID = "demo-zad-almoslem";
const ADMIN_UID = "5UkeBQyyGGZcICXULT9LUD0S4lt2";
let env;

before(async () => {
  const rules = await readFile(new URL("../firestore.rules", import.meta.url), "utf8");
  env = await initializeTestEnvironment({ projectId: PROJECT_ID, firestore: { rules } });
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    await setDoc(doc(db, "azkar", "published-new"), { published: true, text: "public" });
    await setDoc(doc(db, "azkar", "published-arabic"), { "تم النشر": true, text: "legacy" });
    await setDoc(doc(db, "azkar", "published-old-arabic"), { "النشر": true, text: "legacy" });
    await setDoc(doc(db, "azkar", "draft"), { published: false, text: "private" });
    await setDoc(doc(db, "waqafat", "__custom_tabs__"), { tabs: [{ id: "hidden", visible: false }] });
    await setDoc(doc(db, "waqafat", "__custom_tabs_public__"), { tabs: [{ id: "visible", visible: true }] });
    await setDoc(doc(db, "waqafat", "custom-published"), { tabId: "visible", published: true, title: "public" });
    await setDoc(doc(db, "waqafat", "custom-draft"), { tabId: "visible", published: false, title: "draft" });
  });
});

after(async () => {
  await env?.cleanup();
});

test("anonymous users can read only the public tab projection", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "waqafat", "__custom_tabs_public__")));
  await assertFails(getDoc(doc(db, "waqafat", "__custom_tabs__")));
});

test("anonymous users can read documents published with current and legacy flags", async () => {
  const db = env.unauthenticatedContext().firestore();
  for (const id of ["published-new", "published-arabic", "published-old-arabic"]) {
    await assertSucceeds(getDoc(doc(db, "azkar", id)));
  }
  await assertFails(getDoc(doc(db, "azkar", "draft")));
});

test("public list queries must explicitly filter to a publication flag", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDocs(collection(db, "azkar")));
  await assertSucceeds(getDocs(query(collection(db, "azkar"), where("published", "==", true))));
  await assertSucceeds(getDocs(query(collection(db, "azkar"), where("تم النشر", "==", true))));
  await assertSucceeds(getDocs(query(collection(db, "azkar"), where("النشر", "==", true))));
  await assertSucceeds(getDocs(query(
    collection(db, "waqafat"),
    where("tabId", "==", "visible"),
    where("published", "==", true)
  )));
});

test("anonymous users cannot create, edit, or delete content", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(setDoc(doc(db, "azkar", "attacker"), { published: true, text: "no" }));
});

test("only the configured admin can read drafts and write content", async () => {
  const adminDb = env.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(getDoc(doc(adminDb, "azkar", "draft")));
  await assertSucceeds(setDoc(doc(adminDb, "azkar", "admin-created"), { published: false }));
  await assertSucceeds(getDocs(collection(adminDb, "waqafat")));
  const otherDb = env.authenticatedContext("not-the-admin").firestore();
  await assertFails(getDoc(doc(otherDb, "azkar", "draft")));
  await assertFails(setDoc(doc(otherDb, "azkar", "blocked"), { published: true }));
});

test("all unlisted collections are denied", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "private-settings", "secret")));
});
