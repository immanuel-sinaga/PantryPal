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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pantrypal.model.FirestoreManager;
import com.pantrypal.model.Item;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirestoreManager dbManager;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // UI Components
    private EditText etDeleteName;
    private TextView tvResults;
    private Button btnLogout, btnDelete;
    private FloatingActionButton fabAdd; // Changed from Button to FAB

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
        // Note: etName and etQuantity are removed
        tvResults = findViewById(R.id.tvResults);
        btnLogout = findViewById(R.id.btnLogout);

        // FAB for Adding
        fabAdd = findViewById(R.id.fabAdd);

        // Delete Views
        etDeleteName = findViewById(R.id.etDeleteName);
        btnDelete = findViewById(R.id.btnDelete);

        // 3. Set Click Listeners

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

        // Delete Listener
        btnDelete.setOnClickListener(v -> deleteItem());
    }

    // --- AUTOMATIC UPDATES ---
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list whenever we come back to this screen (e.g., after adding an item)
        loadItems();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dbManager.stopListening();
    }

    // --- DELETE ITEM ---
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
                            .append(" ").append(item.getUnit()) // Added Unit display
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
