package com.pantrypal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pantrypal.model.FirestoreManager;
import com.pantrypal.model.Item;

import java.time.LocalDate;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirestoreManager dbManager;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // UI Components
    private EditText etName, etQuantity, etDeleteName; // Added etDeleteName
    private TextView tvResults;
    private Button btnSave, btnLogout, btnDelete; // Added btnDelete

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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbManager = new FirestoreManager();

        // 2. Find Views
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        tvResults = findViewById(R.id.tvResults);
        btnSave = findViewById(R.id.btnSave);
        btnLogout = findViewById(R.id.btnLogout);

        // NEW: Find Delete Views
        etDeleteName = findViewById(R.id.etDeleteName);
        btnDelete = findViewById(R.id.btnDelete);

        // 3. Set Click Listeners
        btnSave.setOnClickListener(v -> saveItem());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // NEW: Delete Listener
        btnDelete.setOnClickListener(v -> deleteItem());
    }

    // --- AUTOMATIC UPDATES ---
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

    // --- SAVE ITEM ---
    private void saveItem() {
        String name = etName.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name required");
            return;
        }
        if (TextUtils.isEmpty(qtyStr)) {
            etQuantity.setError("Quantity required");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            etQuantity.setError("Invalid number");
            return;
        }

        Item newItem = new Item(
                currentUserId,
                name,
                quantity,
                "pcs",
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        );

        dbManager.addItem(newItem, new FirestoreManager.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
                etName.setText("");
                etQuantity.setText("");
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- NEW: DELETE ITEM ---
    private void deleteItem() {
        String nameToDelete = etDeleteName.getText().toString().trim();

        if (TextUtils.isEmpty(nameToDelete)) {
            etDeleteName.setError("Enter item name");
            return;
        }

        // Use the Manager to find and delete
        dbManager.deleteItemByName(currentUserId, nameToDelete, new FirestoreManager.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Deleted: " + nameToDelete, Toast.LENGTH_SHORT).show();
                etDeleteName.setText(""); // Clear input
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, "Could not delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- LOAD ITEMS ---
    private void loadItems() {
        tvResults.setText("Loading...");

        dbManager.startListeningForItems(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onCallback(List<Item> list) {
                StringBuilder builder = new StringBuilder();

                for (Item item : list) {
                    builder.append("• ")
                            .append(item.getName())
                            .append(" (Qty: ")
                            .append(item.getQuantity())
                            .append(")\n");
                }

                if (builder.length() == 0) {
                    tvResults.setText("Your pantry is empty.");
                } else {
                    tvResults.setText(builder.toString());
                }
            }
        });
    }
}
