package com.zad.almoslem;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Read-only remote content synchronizer.
 * Collections: azkar, hadiths, duas. Documents must contain published=true.
 * Content is cached locally so the app can continue working offline.
 */
public final class RemoteContentRepository {
    private static final String PREFS = "zad_remote_content_cache";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private RemoteContentRepository() {}

    public static JSONArray cached(Context context, String collection) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(collection, "[]");
        try { return new JSONArray(raw); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    public static void sync(Context context, String collection, Runnable onUpdated) {
        Context app = context.getApplicationContext();
        FirebaseFirestore.getInstance().collection(collection)
                .whereEqualTo("published", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    JSONArray rows = new JSONArray();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        JSONObject row = new JSONObject();
                        for (String key : doc.getData().keySet()) {
                            Object value = doc.get(key);
                            try { row.put(key, value); } catch (Exception ignored) {}
                        }
                        try { row.put("id", doc.getId()); } catch (Exception ignored) {}
                        rows.put(row);
                    }
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit().putString(collection, rows.toString()).apply();
                    if (onUpdated != null) MAIN.post(onUpdated);
                })
                .addOnFailureListener(error -> {
                    // Keep the last successful cache; offline use remains available.
                });
    }
}
