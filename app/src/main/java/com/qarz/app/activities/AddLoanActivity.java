package com.qarz.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.qarz.app.R;
import com.qarz.app.models.Loan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class AddLoanActivity extends AppCompatActivity {

    private Spinner spinnerFriends;
    private EditText etLoanAmount, etLoanDescription;
    private Button btnSelectLoanDate, btnSelectDueDate, btnSaveLoan;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<String> friendNamesList;
    private List<String> friendIdsList;
    private ArrayAdapter<String> spinnerAdapter;

    private long selectedLoanDate = 0;
    private long selectedDueDate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_loan);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spinnerFriends = findViewById(R.id.spinnerFriends);
        etLoanAmount = findViewById(R.id.etLoanAmount);
        etLoanDescription = findViewById(R.id.etLoanDescription);
        
        btnSelectLoanDate = findViewById(R.id.btnSelectLoanDate);
        btnSelectDueDate = findViewById(R.id.btnSelectDueDate);
        btnSaveLoan = findViewById(R.id.btnSaveLoan);

        friendNamesList = new ArrayList<>();
        friendIdsList = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, friendNamesList);
        spinnerFriends.setAdapter(spinnerAdapter);

        loadFriends();

        btnSelectLoanDate.setOnClickListener(v -> showDatePicker(true));
        btnSelectDueDate.setOnClickListener(v -> showDatePicker(false));
        btnSaveLoan.setOnClickListener(v -> saveLoan());
    }

    private void showDatePicker(boolean isLoanDate) {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedCal.set(Calendar.MILLISECOND, 0);

                    if (isLoanDate) {
                        long epochMillis = selectedCal.getTimeInMillis();
                        selectedLoanDate = epochMillis;
                        String formattedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        btnSelectLoanDate.setText("Loan Date: " + formattedDate);
                    } else {
                        // For Due Date, chain TimePickerDialog
                        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                        int currentMinute = calendar.get(Calendar.MINUTE);
                        
                        new android.app.TimePickerDialog(AddLoanActivity.this,
                                (tView, hourOfDay, minute) -> {
                                    selectedCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    selectedCal.set(Calendar.MINUTE, minute);
                                    long epochMillis = selectedCal.getTimeInMillis();
                                    
                                    selectedDueDate = epochMillis;
                                    String formattedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                                    String formattedTime = String.format("%02d:%02d", hourOfDay, minute);
                                    btnSelectDueDate.setText("Due: " + formattedDate + " " + formattedTime);
                                }, currentHour, currentMinute, true).show();
                    }
                },
                currentYear, currentMonth, currentDay);
        datePickerDialog.show();
    }

    private void loadFriends() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("connections").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> friendsMap = documentSnapshot.getData();
                        for (String friendId : friendsMap.keySet()) {
                            Object isConnected = friendsMap.get(friendId);
                            if ((isConnected instanceof Boolean && (Boolean) isConnected) || 
                                "true".equals(isConnected)) {
                                fetchFriendName(friendId);
                            }
                        }
                    } else {
                        Toast.makeText(this, "No friends found. Add friends first.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load connections", Toast.LENGTH_SHORT).show());
    }

    private void fetchFriendName(String friendId) {
        db.collection("users").document(friendId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null) {
                            friendIdsList.add(friendId);
                            friendNamesList.add(name);
                            spinnerAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void saveLoan() {
        if (mAuth.getCurrentUser() == null) return;
        
        int selectedPosition = spinnerFriends.getSelectedItemPosition();
        if (selectedPosition == Spinner.INVALID_POSITION || friendIdsList.isEmpty()) {
            Toast.makeText(this, "Please select an existing friend first", Toast.LENGTH_SHORT).show();
            return;
        }

        String borrowerId = friendIdsList.get(selectedPosition);
        String amountText = etLoanAmount.getText().toString().trim();
        String description = etLoanDescription.getText().toString().trim();

        if (amountText.isEmpty()) {
            etLoanAmount.setError("Enter amount");
            return;
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            etLoanAmount.setError("Invalid amount");
            return;
        }

        if (amount <= 0) {
            etLoanAmount.setError("Amount must be greater than 0");
            return;
        }

        if (selectedLoanDate == 0 || selectedDueDate == 0) {
            Toast.makeText(this, "Please select both Loan Date and Due Date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDueDate <= selectedLoanDate) {
            Toast.makeText(this, "Due Date must be strictly after the Loan Date", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveLoan.setEnabled(false);

        String currentUserId = mAuth.getCurrentUser().getUid();
        
        String newLoanId = db.collection("loans").document().getId();
        
        Loan loan = new Loan(
                newLoanId,
                currentUserId, // The current user lends the money
                borrowerId,    // The friend is the borrower
                amount,
                description,
                System.currentTimeMillis(),
                "pending",
                selectedLoanDate,
                selectedDueDate
        );

        db.collection("loans").document(newLoanId)
                .set(loan)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Loan saved successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Safely return to dashboard
                })
                .addOnFailureListener(e -> {
                    btnSaveLoan.setEnabled(true);
                    Toast.makeText(this, "Failed to save loan", Toast.LENGTH_SHORT).show();
                });
    }
}
