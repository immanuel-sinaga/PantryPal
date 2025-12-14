package com.pantrypal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pantrypal.model.FirestoreManager;
import com.pantrypal.model.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // --- FIREBASE & DATA ---
    private FirestoreManager dbManager;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // --- UI COMPONENTS ---
    private ListView listViewItems;
    private TextView tvEmptyMessage;
    private ImageButton btnProfile;

    // --- ADAPTER & DATA LIST ---
    private ItemAdapter itemAdapter;
    private List<Item> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // --- AUTHENTICATION CHECK ---
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = currentUser.getUid();

        // --- UI CONFIGURATION ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int sidePadding = (int) (16 * getResources().getDisplayMetrics().density);
            int topBottomPadding = (int) (16 * getResources().getDisplayMetrics().density);

            v.setPadding(
                    systemBars.left + sidePadding,
                    systemBars.top + topBottomPadding,
                    systemBars.right + sidePadding,
                    systemBars.bottom + topBottomPadding
            );
            return insets;
        });

        dbManager = new FirestoreManager();

        // --- VIEW INITIALIZATION ---
        listViewItems = findViewById(R.id.listViewItems);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnProfile = findViewById(R.id.btnProfile);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        // --- ADAPTER SETUP ---
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(this, itemList);
        listViewItems.setAdapter(itemAdapter);

        itemAdapter.setOnDeleteClickListener(this::showDeleteConfirmation);

        // --- EVENT LISTENERS ---
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddActivity.class);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        listViewItems.setOnItemLongClickListener((parent, view, position, id) -> {
            Item selectedItem = itemList.get(position);
            showDeleteConfirmation(selectedItem);
            return true;
        });
    }

    // --- LIFECYCLE CALLBACKS ---
    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dbManager.stopListening();
    }

    // --- DIALOG HELPERS ---
    private void showDeleteConfirmation(Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete '" + item.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- DATABASE OPERATIONS ---
    private void deleteItem(Item item) {
        dbManager.deleteItem(item.getDocumentId(), new FirestoreManager.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Deleted: " + item.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadItems() {
        dbManager.startListeningForItems(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onCallback(List<Item> list) {
                itemList.clear();

                if (list.isEmpty()) {
                    tvEmptyMessage.setVisibility(android.view.View.VISIBLE);
                    listViewItems.setVisibility(android.view.View.GONE);
                } else {
                    Collections.sort(list, new Comparator<Item>() {
                        @Override
                        public int compare(Item i1, Item i2) {
                            return Long.compare(i1.getDaysUntilExpiry(), i2.getDaysUntilExpiry());
                        }
                    });

                    itemList.addAll(list);

                    tvEmptyMessage.setVisibility(android.view.View.GONE);
                    listViewItems.setVisibility(android.view.View.VISIBLE);
                }

                itemAdapter.notifyDataSetChanged();
            }
        });
    }
}