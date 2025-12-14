package com.pantrypal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.pantrypal.model.FirestoreManager;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirestoreManager firestoreManager;

    private TextView tvUserName, tvUserEmail;
    private EditText etNewName, etCurrentPasswordPass, etNewPassword, etConfirmPassword;
    private Button btnUpdateName, btnUpdatePassword, btnDeleteAccount, btnLogout;
    private ImageButton btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int padding = 40;
            try {
                padding = getResources().getDimensionPixelSize(R.dimen.login_padding);
            } catch (Exception e) {
                // Default padding
            }
            v.setPadding(systemBars.left + padding, systemBars.top, systemBars.right + padding, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestoreManager = new FirestoreManager();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        loadUserData();
        setupListeners();
    }

    private void initializeViews() {
        btnClose = findViewById(R.id.btnClose);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        etNewName = findViewById(R.id.etNewName);
        etCurrentPasswordPass = findViewById(R.id.etCurrentPasswordPass);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnUpdateName = findViewById(R.id.btnUpdateName);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserData() {
        String name = currentUser.getDisplayName();
        String email = currentUser.getEmail();

        tvUserName.setText(name != null ? name : "No Name");
        tvUserEmail.setText(email != null ? email : "No Email");
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());

        btnUpdateName.setOnClickListener(v -> updateName());
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        btnLogout.setOnClickListener(v -> logout());
    }

    // UPDATE NAME
    private void updateName() {
        String newName = etNewName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            etNewName.setError("Name cannot be empty");
            etNewName.requestFocus();
            return;
        }

        btnUpdateName.setEnabled(false);
        btnUpdateName.setText("Updating...");

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    btnUpdateName.setEnabled(true);
                    btnUpdateName.setText("Update Name");

                    if (task.isSuccessful()) {
                        tvUserName.setText(newName);
                        etNewName.setText("");
                        Toast.makeText(this, "Name updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // UPDATE PASSWORD
    private void updatePassword() {
        String currentPassword = etCurrentPasswordPass.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPasswordPass.setError("Current password required");
            etCurrentPasswordPass.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("New password required");
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 8) {
            etNewPassword.setError("Password must be at least 8 characters");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Updating...");

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    btnUpdatePassword.setEnabled(true);
                                    btnUpdatePassword.setText("Update Password");

                                    if (updateTask.isSuccessful()) {
                                        etCurrentPasswordPass.setText("");
                                        etNewPassword.setText("");
                                        etConfirmPassword.setText("");
                                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, "Error: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        btnUpdatePassword.setEnabled(true);
                        btnUpdatePassword.setText("Update Password");
                        etCurrentPasswordPass.setError("Wrong password");
                        Toast.makeText(this, "Authentication failed. Wrong password.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // DELETE ACCOUNT CONFIRMATION
    private void showDeleteAccountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);
        EditText etPasswordConfirm = dialogView.findViewById(R.id.etPasswordConfirm);

        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This action cannot be undone. All your pantry data will be permanently deleted.")
                .setView(dialogView)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String password = etPasswordConfirm.getText().toString().trim();

                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    deleteAccount(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // DELETE ACCOUNT
    private void deleteAccount(String password) {
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Delete all user's pantry items first
                        String userId = currentUser.getUid();

                        // Then delete the Firebase account
                        currentUser.delete()
                                .addOnCompleteListener(deleteTask -> {
                                    if (deleteTask.isSuccessful()) {
                                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Error: " + deleteTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Authentication failed. Wrong password.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // LOGOUT
    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}