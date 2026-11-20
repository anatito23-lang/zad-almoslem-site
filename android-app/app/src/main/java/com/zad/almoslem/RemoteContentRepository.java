package com.zad.almoslem;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Synchronizes published azkar and duas while retaining an offline cache. */
public final class RemoteContentRepository {
    private static final String PREFS = "zad_remote_content_cache";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private RemoteContentRepository() {}

    public static JSONArray cached(Context context, String collection) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try { return new JSONArray(prefs.getString(collection, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    public static void sync(Context context, String collection, Runnable onUpdated) {
        if (!"azkar".equals(collection) && !"duas".equals(collection)) return;
        Context app = context.getApplicationContext();
        String[] publicationFields = {"published", "تم النشر", "النشر"};
        Map<String, JSONObject> merged = new HashMap<>();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (String publicationField : publicationFields) {
            FirebaseFirestore.getInstance().collection(collection)
                    .whereEqualTo(publicationField, true)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        synchronized (merged) {
                            for (QueryDocumentSnapshot doc : snapshot) {
                                JSONObject row = merged.get(doc.getId());
                                if (row == null) row = new JSONObject();
                                try {
                                    Map<String, Object> data = doc.getData();
                                    Object text = data.containsKey("text") ? data.get("text") : data.get("نص");
                                    Object source = data.containsKey("source") ? data.get("source") : data.get("مصدر");
                                    Object category = data.containsKey("category") ? data.get("category") : data.get("فئة");
                                    Object count = data.containsKey("count") ? data.get("count") : data.get("عدد");
                                    if (text != null) row.put("text", text);
                                    if (source != null) row.put("source", source);
                                    if (category != null) row.put("category", category);
                                    if (count != null) row.put("count", count);
                                    row.put("id", doc.getId());
                                    row.put("published", true);
                                    merged.put(doc.getId(), row);
                                } catch (Exception ignored) {}
                            }
                        }
                        finishIfReady(app, collection, merged, completed, failures, onUpdated);
                    })
                    .addOnFailureListener(error -> {
                        failures.incrementAndGet();
                        finishIfReady(app, collection, merged, completed, failures, onUpdated);
                    });
        }
    }

    private static void finishIfReady(Context app, String collection,
            Map<String, JSONObject> merged, AtomicInteger completed,
            AtomicInteger failures, Runnable onUpdated) {
        if (completed.incrementAndGet() != 3) return;
        if (failures.get() == 3) return; // Preserve last known cache if every query failed.
        JSONArray rows = new JSONArray();
        synchronized (merged) {
            for (JSONObject row : merged.values()) rows.put(row);
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(collection, rows.toString()).apply();
        if (onUpdated != null) MAIN.post(onUpdated);
    }
}