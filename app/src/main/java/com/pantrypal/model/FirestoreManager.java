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
    private ListenerRegistration listenerRegistration;

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    // Interfaces

    public interface FirestoreCallback {
        void onCallback(List<Item> list);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Add Item

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

    // Get Items

    public void startListeningForItems(String userId, FirestoreCallback callback) {
        if (userId == null || userId.isEmpty()) {
            Log.w("Firestore", "No User ID provided. Cannot load items.");
            return;
        }

        stopListening();

        listenerRegistration = db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
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

    // Stop Listening

    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    // Delete Item

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

    // Update Item

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

    // Delete Item By Name

    public void deleteItemByName(String userId, String itemName, ActionCallback callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("name", itemName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        deleteItem(docId, callback);
                    } else {
                        callback.onFailure(new Exception("Item not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
}