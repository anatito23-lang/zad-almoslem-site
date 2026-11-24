import { before, after, test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { initializeTestEnvironment, assertSucceeds, assertFails } from "@firebase/rules-unit-testing";
import {
  collection, doc, getDoc, getDocs, query, where, setDoc, updateDoc, deleteDoc
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
    await setDoc(doc(db, "waqafat", "custom_tabs_config"), { tabs: [{ id: "hidden", visible: false }] });
    await setDoc(doc(db, "waqafat", "custom_tabs_public"), { tabs: [{ id: "visible", visible: true }] });
    await setDoc(doc(db, "waqafat", "app_navigation_config"), { tabs: [{ key: "custom-app", visible: true }] });
    await setDoc(doc(db, "site_custom_tabs", "site_custom_tabs_config"), { tabs: [{ id: "site-only", visible: true }] });
    await setDoc(doc(db, "site_custom_tabs", "site_custom_tabs_public"), { tabs: [{ id: "site-only", visible: true }] });
    await setDoc(doc(db, "site_custom_content", "site-published"), { sectionId: "site-only", published: true, title: "website only" });
    await setDoc(doc(db, "site_custom_content", "site-draft"), { sectionId: "site-only", published: false, title: "website draft" });
    await setDoc(doc(db, "waqafat", "custom-published"), { tabId: "visible", published: true, title: "public" });
    await setDoc(doc(db, "waqafat", "custom-draft"), { tabId: "visible", published: false, title: "draft" });
    await setDoc(doc(db, "future-content", "published"), { published: true, title: "compatible" });
    await setDoc(doc(db, "future-content", "draft"), { published: false, title: "private" });
    await setDoc(doc(db, "app_content", "app-public"), { published: true, title: "app content" });
    await setDoc(doc(db, "app_content", "app-draft"), { published: false, title: "private app content" });
    await setDoc(doc(db, "library_books", "book-public"), { published: true, title: "public book" });
    await setDoc(doc(db, "site_settings", "website"), { heroTitle: "public presentation settings" });
    await setDoc(doc(db, "site_settings", "private-secrets"), { secret: "never public" });
    await setDoc(doc(db, "app_admin_config", "private-config"), { secret: "never public" });
  });
});

after(async () => {
  await env?.cleanup();
});

test("anonymous users can read only the public tab projection", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, "waqafat", "custom_tabs_public")));
  await assertFails(getDoc(doc(db, "waqafat", "custom_tabs_config")));
  await assertSucceeds(getDoc(doc(db, "waqafat", "app_navigation_config")));
  await assertSucceeds(getDoc(doc(db, "site_custom_tabs", "site_custom_tabs_public")));
  await assertFails(getDoc(doc(db, "site_custom_tabs", "site_custom_tabs_config")));
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

test("website-managed tabs and content stay in website-only collections", async () => {
  const publicDb = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(publicDb, "site_custom_content", "site-published")));
  await assertFails(getDoc(doc(publicDb, "site_custom_content", "site-draft")));
  await assertFails(setDoc(doc(publicDb, "site_custom_content", "attacker"), { sectionId: "site-only", published: true }));
  const adminDb = env.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(setDoc(doc(adminDb, "site_custom_tabs", "site_custom_tabs_config"), { tabs: [{ id: "admin-site" }] }));
  await assertSucceeds(setDoc(doc(adminDb, "site_custom_content", "admin-site-item"), { sectionId: "admin-site", published: true }));
});

test("anonymous users cannot create, edit, or delete content", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(setDoc(doc(db, "azkar", "attacker"), { published: true, text: "no" }));
  await assertFails(updateDoc(doc(db, "azkar", "published-new"), { text: "tampered" }));
  await assertFails(deleteDoc(doc(db, "azkar", "published-new")));
});

test("only the configured admin can read drafts and write content", async () => {
  const adminDb = env.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(getDoc(doc(adminDb, "azkar", "draft")));
  await assertSucceeds(setDoc(doc(adminDb, "azkar", "admin-created"), { published: false }));
  await assertSucceeds(updateDoc(doc(adminDb, "azkar", "draft"), { text: "admin-edited" }));
  await assertSucceeds(deleteDoc(doc(adminDb, "azkar", "admin-created")));
  await assertSucceeds(getDocs(collection(adminDb, "waqafat")));
  const otherDb = env.authenticatedContext("not-the-admin").firestore();
  await assertFails(getDoc(doc(otherDb, "azkar", "draft")));
  await assertFails(setDoc(doc(otherDb, "azkar", "blocked"), { published: true }));
});

test("only allowlisted public collections expose published content", async () => {
  const publicDb = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(publicDb, "app_content", "app-public")));
  await assertFails(getDoc(doc(publicDb, "app_content", "app-draft")));
  await assertSucceeds(getDoc(doc(publicDb, "library_books", "book-public")));
  await assertSucceeds(getDoc(doc(publicDb, "site_settings", "website")));
  await assertFails(getDoc(doc(publicDb, "site_settings", "private-secrets")));
  await assertFails(getDoc(doc(publicDb, "app_admin_config", "private-config")));
  await assertFails(getDoc(doc(publicDb, "future-content", "published")));
  await assertFails(getDoc(doc(publicDb, "future-content", "draft")));
  await assertFails(setDoc(doc(publicDb, "future-content", "attacker"), { published: true }));

  const adminDb = env.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(getDoc(doc(adminDb, "future-content", "draft")));
  await assertSucceeds(setDoc(doc(adminDb, "future-content", "admin-created"), { published: false }));
});
