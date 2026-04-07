package com.qarz.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.qarz.app.R;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText etProfName, etProfEmail, etProfCnic, etProfPhone, etProfCity, etProfDob, etProfAddress;
    private EditText etProfGName, etProfGPhone, etProfGRelation;
    private Button btnToggleEdit, btnLogout;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        initViews();
        toggleFields(false); // Lock by default
        loadUserData();

        btnBack.setOnClickListener(v -> finish());

        btnToggleEdit.setOnClickListener(v -> {
            if (!isEditMode) {
                // Switch to edit mode
                isEditMode = true;
                btnToggleEdit.setText("Save Updates");
                toggleFields(true);
            } else {
                // Switch to save mode
                saveUpdates();
            }
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnToggleEdit = findViewById(R.id.btnToggleEdit);
        btnLogout = findViewById(R.id.btnLogout);

        etProfName = findViewById(R.id.etProfName);
        etProfEmail = findViewById(R.id.etProfEmail);
        etProfCnic = findViewById(R.id.etProfCnic);
        etProfPhone = findViewById(R.id.etProfPhone);
        etProfCity = findViewById(R.id.etProfCity);
        etProfDob = findViewById(R.id.etProfDob);
        etProfAddress = findViewById(R.id.etProfAddress);

        etProfGName = findViewById(R.id.etProfGName);
        etProfGPhone = findViewById(R.id.etProfGPhone);
        etProfGRelation = findViewById(R.id.etProfGRelation);
    }

    private void toggleFields(boolean enable) {
        etProfName.setEnabled(enable);
        etProfPhone.setEnabled(enable);
        etProfCity.setEnabled(enable);
        etProfDob.setEnabled(enable);
        etProfAddress.setEnabled(enable);
        
        etProfGName.setEnabled(enable);
        etProfGPhone.setEnabled(enable);
        etProfGRelation.setEnabled(enable);

        // Explicitly maintain Email and CNIC as locked irrespective of the toggle state
        etProfEmail.setEnabled(false);
        etProfCnic.setEnabled(false);
    }

    private void loadUserData() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    etProfName.setText(documentSnapshot.getString("name"));
                    etProfEmail.setText(documentSnapshot.getString("email"));
                    etProfCnic.setText(documentSnapshot.getString("cnic"));
                    etProfPhone.setText(documentSnapshot.getString("phone"));
                    etProfCity.setText(documentSnapshot.getString("city"));
                    etProfDob.setText(documentSnapshot.getString("dob"));
                    etProfAddress.setText(documentSnapshot.getString("address"));

                    // Fetch Nested Guarantor
                    Object guarantorObj = documentSnapshot.get("guarantor");
                    if (guarantorObj instanceof Map) {
                        Map<String, String> guarantor = (Map<String, String>) guarantorObj;
                        etProfGName.setText(guarantor.get("name"));
                        etProfGPhone.setText(guarantor.get("phone"));
                        etProfGRelation.setText(guarantor.get("relation"));
                    }
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed loading profile.", Toast.LENGTH_SHORT).show());
    }

    private void saveUpdates() {
        // Collect updated text
        String name = etProfName.getText().toString().trim();
        String phone = etProfPhone.getText().toString().trim();
        String city = etProfCity.getText().toString().trim();
        String dob = etProfDob.getText().toString().trim();
        String address = etProfAddress.getText().toString().trim();
        
        String gName = etProfGName.getText().toString().trim();
        String gPhone = etProfGPhone.getText().toString().trim();
        String gRelation = etProfGRelation.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Core fields cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnToggleEdit.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("city", city);
        updates.put("dob", dob);
        updates.put("address", address);

        Map<String, String> guarantor = new HashMap<>();
        guarantor.put("name", gName);
        guarantor.put("phone", gPhone);
        guarantor.put("relation", gRelation);
        updates.put("guarantor", guarantor);

        db.collection("users").document(currentUserId).update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                isEditMode = false;
                btnToggleEdit.setText("Edit Mode");
                btnToggleEdit.setEnabled(true);
                toggleFields(false); // Relock fields
            })
            .addOnFailureListener(e -> {
                btnToggleEdit.setEnabled(true);
                Toast.makeText(this, "Update Failed.", Toast.LENGTH_SHORT).show();
            });
    }
}
