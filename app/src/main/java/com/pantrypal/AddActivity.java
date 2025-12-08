package com.pantrypal;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // 1. Import ImageButton
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

public class AddActivity extends AppCompatActivity {

    // UI Components
    private EditText etItemName, etQuantity, etDaysFromNow;
    private Spinner spinnerUnit;
    private TextView tvPurchaseDate, tvExpiryDateValue;
    private RadioGroup rgExpiryOptions;
    private RadioButton rbExpiryDate, rbDaysFromNow;
    private LinearLayout containerExpiryDate;
    private Button btnAddItem;
    private ImageButton btnClose; // 2. Declare the Close Button

    // Firebase & Helpers
    private FirebaseAuth mAuth;
    private FirestoreManager firestoreManager;
    private SharedPreferences prefs; // To save settings

    // Date Formatters
    private final java.text.SimpleDateFormat uiDateFormat = new java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Define the extra padding (matching your LoginActivity logic)
            int padding = 40;
            try {
                // Tries to get the dimension if it exists, otherwise sticks to 40
                padding = getResources().getDimensionPixelSize(R.dimen.login_padding);
            } catch (Exception e) {
                // If the resource is missing, we stick to the default 40
            }

            // Apply: System Insets + Your Custom Padding
            v.setPadding(systemBars.left + padding, systemBars.top, systemBars.right + padding, systemBars.bottom);

            return insets;
        });

        // Initialize Firebase & Prefs
        mAuth = FirebaseAuth.getInstance();
        firestoreManager = new FirestoreManager();
        prefs = getSharedPreferences("PantryPalPrefs", Context.MODE_PRIVATE);

        initializeViews();
        setDefaultDates();
        setupListeners();

        // RESTORE PREVIOUS SELECTION
        restoreExpiryPreference();
        restoreUnitPreference(); // <--- Add this single line
    }

    private void initializeViews() {
        etItemName = findViewById(R.id.etItemName);
        etQuantity = findViewById(R.id.etQuantity);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        tvPurchaseDate = findViewById(R.id.tvPurchaseDate);

        rgExpiryOptions = findViewById(R.id.rgExpiryOptions);
        rbExpiryDate = findViewById(R.id.rbExpiryDate);
        rbDaysFromNow = findViewById(R.id.rbDaysFromNow);

        containerExpiryDate = findViewById(R.id.containerExpiryDate);
        tvExpiryDateValue = findViewById(R.id.tvExpiryDateValue);
        etDaysFromNow = findViewById(R.id.etDaysFromNow);

        btnAddItem = findViewById(R.id.btnAddItem);

        // 3. Find the button by ID
        btnClose = findViewById(R.id.btnClose);
    }

    private void setDefaultDates() {
        Calendar calendar = Calendar.getInstance();
        String today = uiDateFormat.format(calendar.getTime());
        tvPurchaseDate.setText(today);
        tvExpiryDateValue.setText(today);
    }

    private void restoreExpiryPreference() {
        // Default to "days" if nothing saved, or check what was saved
        String preferredOption = prefs.getString("expiry_preference", "days");

        if (preferredOption.equals("date")) {
            rbExpiryDate.setChecked(true);
            rbDaysFromNow.setChecked(false);
        } else {
            rbDaysFromNow.setChecked(true);
            rbExpiryDate.setChecked(false);
        }
    }

    private void restoreUnitPreference() {
        String savedUnit = prefs.getString("last_unit", null);

        if (savedUnit != null) {
            // Loop through spinner items to find the index that matches the saved string
            for (int i = 0; i < spinnerUnit.getCount(); i++) {
                if (spinnerUnit.getItemAtPosition(i).toString().equals(savedUnit)) {
                    spinnerUnit.setSelection(i);
                    break;
                }
            }
        }
    }


    private void saveExpiryPreference(String preference) {
        prefs.edit().putString("expiry_preference", preference).apply();
    }

    private void setupListeners() {
        tvPurchaseDate.setOnClickListener(v -> showDatePicker(tvPurchaseDate));

        // 4. Close Activity Logic
        btnClose.setOnClickListener(v -> finish());

        // --- 1. UNIT SPINNER LOGIC (WHOLE VS DECIMAL) ---
        spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUnit = parent.getItemAtPosition(position).toString();

                // If "pcs" is selected, force integer input. Otherwise allow decimals.
                if (selectedUnit.equalsIgnoreCase("pcs")) {
                    etQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
                } else {
                    etQuantity.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }

                // Move cursor to the end if user was already typing
                if (etQuantity.getText().length() > 0) {
                    etQuantity.setSelection(etQuantity.getText().length());
                }

                // [NEW] Save the unit preference when changed
                prefs.edit().putString("last_unit", selectedUnit).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // --- 2. AUTO-SELECT EXPIRY LOGIC ---

        // If user clicks the Date Container -> Select Date Radio & Show Picker
        containerExpiryDate.setOnClickListener(v -> {
            rbExpiryDate.performClick();
            showDatePicker(tvExpiryDateValue);
        });

        // If user focuses on or clicks the Days input -> Select Days Radio
        etDaysFromNow.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                rbDaysFromNow.performClick();
            }
        });
        etDaysFromNow.setOnClickListener(v -> rbDaysFromNow.performClick());

        // --- 3. RADIO BUTTON LOGIC (ALREADY EXISTS) ---

        rbExpiryDate.setOnClickListener(v -> {
            rbExpiryDate.setChecked(true);
            rbDaysFromNow.setChecked(false);
            etDaysFromNow.setText("");
            etDaysFromNow.setError(null);
            // This line already saves the radio button preference
            saveExpiryPreference("date");
        });

        rbDaysFromNow.setOnClickListener(v -> {
            rbDaysFromNow.setChecked(true);
            rbExpiryDate.setChecked(false);
            etDaysFromNow.requestFocus();
            // This line already saves the radio button preference
            saveExpiryPreference("days");
        });

        btnAddItem.setOnClickListener(v -> saveItem());
    }


    private void showDatePicker(TextView targetTextView) {
        Calendar currentSelection = Calendar.getInstance();
        try {
            String dateString = targetTextView.getText().toString();
            if (!dateString.isEmpty()) {
                currentSelection.setTime(uiDateFormat.parse(dateString));
            }
        } catch (Exception e) {
            // Default to now
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            currentSelection.set(Calendar.YEAR, year);
            currentSelection.set(Calendar.MONTH, month);
            currentSelection.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            targetTextView.setText(uiDateFormat.format(currentSelection.getTime()));
        },
                currentSelection.get(Calendar.YEAR),
                currentSelection.get(Calendar.MONTH),
                currentSelection.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveItem() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = etItemName.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String unit = spinnerUnit.getSelectedItem().toString();

        String purchaseDateStr = tvPurchaseDate.getText().toString();
        LocalDate purchaseDateObj;
        LocalDate expiryDateObj;

        // --- VALIDATION START ---
        if (TextUtils.isEmpty(name)) {
            etItemName.setError("Item name required");
            etItemName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Quantity required");
            etQuantity.requestFocus();
            return;
        }

        // Validate Quantity Number
        double quantityVal;
        try {
            quantityVal = Double.parseDouble(quantityStr);

            if (quantityVal <= 0) {
                etQuantity.setError("Must be > 0");
                etQuantity.requestFocus();
                return;
            }

            // If unit is "pcs", check if it is a whole number (prevent 1.5 pcs)
            if (unit.equalsIgnoreCase("pcs")) {
                if (quantityVal % 1 != 0) {
                    etQuantity.setError("Pieces must be whole numbers");
                    etQuantity.requestFocus();
                    return;
                }
            }

        } catch (NumberFormatException e) {
            etQuantity.setError("Invalid number");
            return;
        }

        // Validate Purchase Date
        try {
            purchaseDateObj = LocalDate.parse(purchaseDateStr, localDateFormatter);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Purchase Date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate & Validate Expiry
        try {
            if (rbExpiryDate.isChecked()) {
                String expiryStr = tvExpiryDateValue.getText().toString();
                expiryDateObj = LocalDate.parse(expiryStr, localDateFormatter);
            } else {
                String daysStr = etDaysFromNow.getText().toString().trim();
                if (daysStr.isEmpty()) {
                    etDaysFromNow.setError("Enter days");
                    etDaysFromNow.requestFocus();
                    return;
                }
                long daysToAdd = Long.parseLong(daysStr);
                expiryDateObj = LocalDate.now().plusDays(daysToAdd);
            }
        } catch (Exception e) {
            etDaysFromNow.setError("Invalid input");
            return;
        }
        // --- VALIDATION END ---

        // Create Item (Ensure your Item.java constructor accepts 'double' for quantity, or cast to int if strictly int)
        // If Item.java only accepts int, use: (int) quantityVal
        // Ideally, update Item.java to use double for flexibility.

        Item newItem = new Item(
                currentUser.getUid(),
                name,
                quantityVal, // Using the double value we parsed
                unit,
                purchaseDateObj,
                expiryDateObj
        );

        btnAddItem.setEnabled(false);
        btnAddItem.setText("Saving...");

        firestoreManager.addItem(newItem, new FirestoreManager.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddActivity.this, "Added " + name + " to Pantry!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                btnAddItem.setEnabled(true);
                btnAddItem.setText("Add to Checklist");
                Toast.makeText(AddActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
