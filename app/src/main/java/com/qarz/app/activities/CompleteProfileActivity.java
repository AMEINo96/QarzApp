package com.qarz.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.qarz.app.MainActivity;
import com.qarz.app.R;
import com.qarz.app.utils.InputFormatters;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shown to Google Sign-In users on their first login.
 * Collects missing required profile fields: CNIC, phone, DOB, city, address, guarantor.
 */
public class CompleteProfileActivity extends AppCompatActivity {

    private EditText etCpName, etCpEmail, etCpCnic, etCpPhone, etCpCity, etCpDob, etCpAddress;
    private EditText etCpGuarantorName, etCpGuarantorPhone, etCpGuarantorRelation;
    private Button btnCompleteProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String CNIC_PATTERN = "^\\d{5}-\\d{7}-\\d{1}$";
    private static final String PHONE_PATTERN = "^03\\d{9}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // If user is somehow not signed in, bounce back
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        prefillFromGoogle();
        attachFormatters();

        btnCompleteProfile.setOnClickListener(v -> validateAndSave());
    }

    /**
     * Back button: sign the user out and return to Login.
     * This prevents the loop where pressing back re-shows CompleteProfileActivity on next cold start.
     */
    @Override
    public void onBackPressed() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        etCpName = findViewById(R.id.etCpName);
        etCpEmail = findViewById(R.id.etCpEmail);
        etCpCnic = findViewById(R.id.etCpCnic);
        etCpPhone = findViewById(R.id.etCpPhone);
        etCpCity = findViewById(R.id.etCpCity);
        etCpDob = findViewById(R.id.etCpDob);
        etCpAddress = findViewById(R.id.etCpAddress);
        etCpGuarantorName = findViewById(R.id.etCpGuarantorName);
        etCpGuarantorPhone = findViewById(R.id.etCpGuarantorPhone);
        etCpGuarantorRelation = findViewById(R.id.etCpGuarantorRelation);
        btnCompleteProfile = findViewById(R.id.btnCompleteProfile);
    }

    /** Pre-fill name and email from the Google account. Email is locked (not editable). */
    private void prefillFromGoogle() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                etCpName.setText(user.getDisplayName());
            }
            if (user.getEmail() != null) {
                // Always lowercase — Google emails can't have caps but enforce defensively
                etCpEmail.setText(user.getEmail().toLowerCase());
            }
        }
    }

    private void attachFormatters() {
        InputFormatters.attachCnicFormatter(etCpCnic);
        InputFormatters.attachPhoneFormatter(etCpPhone);
        InputFormatters.attachPhoneFormatter(etCpGuarantorPhone);
        InputFormatters.attachDateFormatter(etCpDob);

        // Force name to proper-case (capitalize each word)
        etCpName.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormatting) return;
                String text = editable.toString();
                String formatted = toProperCase(text);
                if (!text.equals(formatted)) {
                    isFormatting = true;
                    etCpName.setText(formatted);
                    etCpName.setSelection(formatted.length());
                    isFormatting = false;
                }
            }
        });
    }

    private static String toProperCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : s.toCharArray()) {
            if (c == ' ') { nextUpper = true; sb.append(c); }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }

    private void validateAndSave() {
        String name    = etCpName.getText().toString().trim();
        String email   = etCpEmail.getText().toString().trim().toLowerCase();
        String cnic    = etCpCnic.getText().toString().trim();
        String phone   = etCpPhone.getText().toString().trim();
        String city    = etCpCity.getText().toString().trim();
        String dob     = etCpDob.getText().toString().trim();
        String address = etCpAddress.getText().toString().trim();
        String gName   = etCpGuarantorName.getText().toString().trim();
        String gPhone  = etCpGuarantorPhone.getText().toString().trim();
        String gRelation = etCpGuarantorRelation.getText().toString().trim();

        // Emptiness
        if (name.isEmpty() || cnic.isEmpty() || phone.isEmpty() || city.isEmpty()
                || dob.isEmpty() || address.isEmpty() || gName.isEmpty()
                || gPhone.isEmpty() || gRelation.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // CNIC format
        if (!Pattern.matches(CNIC_PATTERN, cnic)) {
            Toast.makeText(this, "CNIC must follow 12345-1234567-1 format.", Toast.LENGTH_LONG).show();
            return;
        }

        // Phone format – strip display dash for validation
        String rawPhone = phone.replaceAll("\\D", "");
        if (!Pattern.matches(PHONE_PATTERN, rawPhone)) {
            Toast.makeText(this, "Phone must be 11 digits starting with 03.", Toast.LENGTH_LONG).show();
            return;
        }

        String rawGPhone = gPhone.replaceAll("\\D", "");
        if (!Pattern.matches(PHONE_PATTERN, rawGPhone)) {
            Toast.makeText(this, "Guarantor phone must be 11 digits starting with 03.", Toast.LENGTH_LONG).show();
            return;
        }

        // DOB validation
        String dobError = InputFormatters.validateDob(dob);
        if (dobError != null) {
            Toast.makeText(this, dobError, Toast.LENGTH_LONG).show();
            return;
        }

        btnCompleteProfile.setEnabled(false);

        // Uniqueness checks: phone then CNIC
        db.collection("users").whereEqualTo("phone", rawPhone).get()
            .addOnCompleteListener(t1 -> {
                if (t1.isSuccessful() && !t1.getResult().isEmpty()) {
                    btnCompleteProfile.setEnabled(true);
                    Toast.makeText(this, "An account with this phone already exists!", Toast.LENGTH_LONG).show();
                } else {
                    db.collection("users").whereEqualTo("cnic", cnic).get()
                        .addOnCompleteListener(t2 -> {
                            if (t2.isSuccessful() && !t2.getResult().isEmpty()) {
                                btnCompleteProfile.setEnabled(true);
                                Toast.makeText(this, "An account with this CNIC already exists!", Toast.LENGTH_LONG).show();
                            } else {
                                saveProfile(name, email, cnic, rawPhone, city, dob, address, gName, rawGPhone, gRelation);
                            }
                        });
                }
            });
    }

    private void saveProfile(String name, String email, String cnic, String phone,
                             String city, String dob, String address,
                             String gName, String gPhone, String gRelation) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUid());
        userData.put("name", name);
        userData.put("email", email);
        userData.put("cnic", cnic);
        userData.put("phone", phone);
        userData.put("city", city);
        userData.put("dob", dob);
        userData.put("address", address);

        Map<String, String> guarantor = new HashMap<>();
        guarantor.put("name", gName);
        guarantor.put("phone", gPhone);
        guarantor.put("relation", gRelation);
        userData.put("guarantor", guarantor);

        db.collection("users").document(user.getUid()).set(userData)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Profile completed! Welcome to QarzApp.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                btnCompleteProfile.setEnabled(true);
                // Show the real Firestore error so it can be diagnosed
                String errMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                Toast.makeText(this, "Save failed: " + errMsg, Toast.LENGTH_LONG).show();
            });
    }
}
