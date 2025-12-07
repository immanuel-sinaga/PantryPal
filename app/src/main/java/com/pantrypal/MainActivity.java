package com.pantrypal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
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

    private FirestoreManager dbManager;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // UI Components
    private ListView listViewItems;
    private TextView tvEmptyMessage;
    private Button btnLogout;
    private FloatingActionButton fabAdd;

    // Adapter and Data
    private ItemAdapter itemAdapter;
    private List<Item> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // --- 1. AUTH CHECK ---
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = currentUser.getUid();

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

        // 2. Find Views
        listViewItems = findViewById(R.id.listViewItems);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnLogout = findViewById(R.id.btnLogout);
        fabAdd = findViewById(R.id.fabAdd);

        // 3. Initialize List and Adapter
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(this, itemList);
        listViewItems.setAdapter(itemAdapter);

        // 4. Set Click Listeners

        // Navigate to AddActivity when FAB is clicked
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Long press to delete item
        listViewItems.setOnItemLongClickListener((parent, view, position, id) -> {
            Item selectedItem = itemList.get(position);
            showDeleteConfirmation(selectedItem);
            return true;
        });

        // Optional: Click to view/edit (you can implement this later)
        listViewItems.setOnItemClickListener((parent, view, position, id) -> {
            Item selectedItem = itemList.get(position);
            Toast.makeText(this, "Clicked: " + selectedItem.getName(), Toast.LENGTH_SHORT).show();
            // TODO: Open EditActivity here if you create one
        });
    }

    // --- AUTOMATIC UPDATES ---
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list whenever we come back to this screen
        loadItems();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dbManager.stopListening();
    }

    // --- DELETE CONFIRMATION DIALOG ---
    private void showDeleteConfirmation(Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete '" + item.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- DELETE ITEM ---
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

    // --- LOAD ITEMS ---
    private void loadItems() {
        dbManager.startListeningForItems(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onCallback(List<Item> list) {
                itemList.clear();

                if (list.isEmpty()) {
                    // Show empty message
                    tvEmptyMessage.setVisibility(android.view.View.VISIBLE);
                    listViewItems.setVisibility(android.view.View.GONE);
                } else {
                    // Sort by expiry date (closest first)
                    Collections.sort(list, new Comparator<Item>() {
                        @Override
                        public int compare(Item i1, Item i2) {
                            return Long.compare(i1.getDaysUntilExpiry(), i2.getDaysUntilExpiry());
                        }
                    });

                    itemList.addAll(list);

                    // Show list
                    tvEmptyMessage.setVisibility(android.view.View.GONE);
                    listViewItems.setVisibility(android.view.View.VISIBLE);
                }

                itemAdapter.notifyDataSetChanged();
            }
        });
    }
}