package com.qarz.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.qarz.app.MainActivity;
import com.qarz.app.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegName, etRegEmail, etRegPassword;
    private EditText etRegCnic, etRegPhone, etRegCity, etRegDob, etRegAddress;
    private EditText etGuarantorName, etGuarantorPhone, etGuarantorRelation;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // RegEx Patterns
    private static final String CNIC_PATTERN = "^\\d{5}-\\d{7}-\\d{1}$";
    private static final String PHONE_PATTERN = "^03\\d{9}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Basic Info
        etRegName = findViewById(R.id.etRegName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);

        // KYC
        etRegCnic = findViewById(R.id.etRegCnic);
        etRegPhone = findViewById(R.id.etRegPhone);
        etRegCity = findViewById(R.id.etRegCity);
        etRegDob = findViewById(R.id.etRegDob);
        etRegAddress = findViewById(R.id.etRegAddress);

        // Guarantor
        etGuarantorName = findViewById(R.id.etGuarantorName);
        etGuarantorPhone = findViewById(R.id.etGuarantorPhone);
        etGuarantorRelation = findViewById(R.id.etGuarantorRelation);

        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> validateAndRegister());

        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void validateAndRegister() {
        String name = etRegName.getText().toString().trim();
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();
        
        String cnic = etRegCnic.getText().toString().trim();
        String phone = etRegPhone.getText().toString().trim();
        String city = etRegCity.getText().toString().trim();
        String dob = etRegDob.getText().toString().trim();
        String address = etRegAddress.getText().toString().trim();
        
        String gName = etGuarantorName.getText().toString().trim();
        String gPhone = etGuarantorPhone.getText().toString().trim();
        String gRelation = etGuarantorRelation.getText().toString().trim();

        // 1. Emptiness Checks
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() ||
            cnic.isEmpty() || phone.isEmpty() || city.isEmpty() || 
            dob.isEmpty() || address.isEmpty() || gName.isEmpty() || 
            gPhone.isEmpty() || gRelation.isEmpty()) {
            Toast.makeText(this, "Please fill all fields natively.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Strict Format Validations
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid Email Format.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Pattern.matches(CNIC_PATTERN, cnic)) {
            Toast.makeText(this, "CNIC must follow 12345-1234567-1 format", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Pattern.matches(PHONE_PATTERN, phone)) {
            Toast.makeText(this, "Phone must be 11 digits starting with 03", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Pattern.matches(PHONE_PATTERN, gPhone)) {
            Toast.makeText(this, "Guarantor Phone must be 11 digits starting with 03", Toast.LENGTH_LONG).show();
            return;
        }

        btnRegister.setEnabled(false);

        // 3. Uniqueness Check: Sequential querying to ensure older SDK stability
        // First check phone...
        db.collection("users").whereEqualTo("phone", phone).get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful() && !task1.getResult().isEmpty()) {
                btnRegister.setEnabled(true);
                Toast.makeText(this, "Account with this Phone already exists!", Toast.LENGTH_LONG).show();
            } else {
                // Phone is unique. Now check CNIC...
                db.collection("users").whereEqualTo("cnic", cnic).get().addOnCompleteListener(task2 -> {
                     if (task2.isSuccessful() && !task2.getResult().isEmpty()) {
                         btnRegister.setEnabled(true);
                         Toast.makeText(this, "Account with this CNIC already exists!", Toast.LENGTH_LONG).show();
                     } else {
                         // Both are entirely unique. Proceed with account block creation.
                         createUserAccount(
                             name, email, password, cnic, phone, city, dob, address, 
                             gName, gPhone, gRelation
                         );
                     }
                });
            }
        });
    }

    private void createUserAccount(String name, String email, String password, 
                                   String cnic, String phone, String city, String dob, String address, 
                                   String gName, String gPhone, String gRelation) {
        
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(
                                user.getUid(), name, email, cnic, phone, city, dob, address,
                                gName, gPhone, gRelation
                            );
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Registration Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email, 
                                     String cnic, String phone, String city, String dob, String address, 
                                     String gName, String gPhone, String gRelation) {
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", uid);
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
        
        // Nest Guarantor metadata safely
        userData.put("guarantor", guarantor);

        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Secure Account Created Successfully", Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Failed to save secure user data.", Toast.LENGTH_LONG).show();
                });
    }
}
