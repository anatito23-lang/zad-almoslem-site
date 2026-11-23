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

/** Synchronizes published faith content and keeps the last complete offline cache. */
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
        if (!"azkar".equals(collection) && !"duas".equals(collection)
                && !"khutbahs".equals(collection) && !"waqafat".equals(collection) && !"reels".equals(collection)) return;
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
                                if ("waqafat".equals(collection) && "app_navigation_config".equals(doc.getId())) continue;
                                JSONObject row = merged.get(doc.getId());
                                if (row == null) row = new JSONObject();
                                try {
                                    Map<String, Object> data = doc.getData();
                                    putIfPresent(row, "id", doc.getId());
                                    putIfPresent(row, "published", true);
                                    if ("reels".equals(collection)) {
                                        putIfPresent(row, "title", data.get("title"));
                                        putIfPresent(row, "description", data.get("description"));
                                        putIfPresent(row, "imageUrl", data.get("imageUrl"));
                                        putIfPresent(row, "audioUrl", data.get("audioUrl"));
                                        putIfPresent(row, "durationSeconds", data.get("durationSeconds"));
                                        putIfPresent(row, "order", data.get("order"));
                                        putIfPresent(row, "createdAt", data.get("createdAt"));
                                    } else if ("khutbahs".equals(collection) || "waqafat".equals(collection)) {
                                        putIfPresent(row, "title", data.get("title"));
                                        Object body = first(data, "body", "text", "نص");
                                        putIfPresent(row, "body", body);
                                        putIfPresent(row, "date", data.get("date"));
                                        putIfPresent(row, "category", data.get("category"));
                                    } else {
                                        Object text = first(data, "text", "نص");
                                        Object source = first(data, "source", "مصدر");
                                        Object category = first(data, "category", "فئة");
                                        Object count = first(data, "count", "عدد");
                                        putIfPresent(row, "text", text);
                                        putIfPresent(row, "source", source);
                                        putIfPresent(row, "category", category);
                                        putIfPresent(row, "count", count);
                                    }
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

    private static Object first(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private static void putIfPresent(JSONObject row, String key, Object value) throws Exception {
        if (value != null) row.put(key, value);
    }

    private static void finishIfReady(Context app, String collection,
            Map<String, JSONObject> merged, AtomicInteger completed,
            AtomicInteger failures, Runnable onUpdated) {
        if (completed.incrementAndGet() != 3) return;
        if (failures.get() > 0) return;
        JSONArray rows = new JSONArray();
        synchronized (merged) {
            for (JSONObject row : merged.values()) rows.put(row);
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(collection, rows.toString()).apply();
        if (onUpdated != null) MAIN.post(onUpdated);
    }
}
