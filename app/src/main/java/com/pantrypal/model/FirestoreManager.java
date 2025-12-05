package com.pantrypal.model;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirestoreManager {

    private final FirebaseFirestore db;
    private final String COLLECTION_NAME = "pantry";
    private ListenerRegistration listenerRegistration; // To stop listening when app closes

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    // --- CALLBACK INTERFACES ---
    public interface FirestoreCallback {
        void onCallback(List<Item> list);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // --- 1. ADD ITEM ---
    public void addItem(Item item, ActionCallback callback) {
        db.collection(COLLECTION_NAME)
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Item added with ID: " + documentReference.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding item", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // --- 2. GET ITEMS (Filtered by User ID) ---
    // UPDATED: Now requires a userId to know whose data to load
    public void startListeningForItems(String userId, FirestoreCallback callback) {
        // Safety check: If no user ID is passed, don't load anything
        if (userId == null || userId.isEmpty()) {
            Log.w("Firestore", "No User ID provided. Cannot load items.");
            return;
        }

        // Stop any previous listener to avoid duplicates
        stopListening();

        listenerRegistration = db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId) // <--- THIS FILTERS THE DATA
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("Firestore", "Listen failed.", error);
                        return;
                    }

                    List<Item> items = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            try {
                                Item item = document.toObject(Item.class);
                                item.setDocumentId(document.getId());
                                items.add(item);
                            } catch (Exception e) {
                                Log.e("Firestore", "Error converting document", e);
                            }
                        }
                    }
                    callback.onCallback(items);
                });
    }

    // Call this in Activity's onDestroy() or onPause()
    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    // --- 3. DELETE ITEM ---
    public void deleteItem(String documentId, ActionCallback callback) {
        db.collection(COLLECTION_NAME).document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // --- 4. UPDATE ITEM ---
    public void updateItemQuantity(String documentId, int newQuantity, ActionCallback callback) {
        db.collection(COLLECTION_NAME).document(documentId)
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // --- 5. DELETE BY NAME (New Method) ---
    // This finds the item ID by name, then deletes it
    public void deleteItemByName(String userId, String itemName, ActionCallback callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("name", itemName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Found it! Get the ID of the first match
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        // Delegate to the standard delete method
                        deleteItem(docId, callback);
                    } else {
                        // No item found with that name
                        callback.onFailure(new Exception("Item not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
}
