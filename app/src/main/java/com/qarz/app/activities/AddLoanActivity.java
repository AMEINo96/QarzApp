package com.qarz.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.qarz.app.R;
import com.qarz.app.models.Loan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddLoanActivity extends AppCompatActivity {

    private Spinner spinnerFriends;
    private EditText etLoanAmount;
    private EditText etLoanDescription;
    private Button btnSelectLoanDate;
    private Button btnSelectDueDate;
    private Button btnSaveLoan;
    private Button btnSelectSharedFriends;
    private TextView tvFriendLabel;
    private TextView tvSharedSelectionSummary;
    private LinearLayout layoutSharedLoan;
    private RadioButton rbIndividualLoan;
    private RadioButton rbSharedLoan;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<String> friendNamesList;
    private List<String> friendIdsList;
    private ArrayAdapter<String> spinnerAdapter;
    private boolean[] sharedSelections = new boolean[0];

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
        btnSelectSharedFriends = findViewById(R.id.btnSelectSharedFriends);
        tvFriendLabel = findViewById(R.id.tvFriendLabel);
        tvSharedSelectionSummary = findViewById(R.id.tvSharedSelectionSummary);
        layoutSharedLoan = findViewById(R.id.layoutSharedLoan);
        rbIndividualLoan = findViewById(R.id.rbIndividualLoan);
        rbSharedLoan = findViewById(R.id.rbSharedLoan);

        friendNamesList = new ArrayList<>();
        friendIdsList = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, friendNamesList);
        spinnerFriends.setAdapter(spinnerAdapter);

        loadFriends();
        updateLoanModeUi();

        rbIndividualLoan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                updateLoanModeUi();
            }
        });
        rbSharedLoan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                updateLoanModeUi();
            }
        });

        btnSelectSharedFriends.setOnClickListener(v -> showSharedFriendsDialog());
        btnSelectLoanDate.setOnClickListener(v -> showDatePicker(true));
        btnSelectDueDate.setOnClickListener(v -> showDatePicker(false));
        btnSaveLoan.setOnClickListener(v -> saveLoan());
    }

    private void updateLoanModeUi() {
        boolean isSharedLoan = rbSharedLoan.isChecked();
        spinnerFriends.setVisibility(isSharedLoan ? android.view.View.GONE : android.view.View.VISIBLE);
        layoutSharedLoan.setVisibility(isSharedLoan ? android.view.View.VISIBLE : android.view.View.GONE);
        tvFriendLabel.setText(isSharedLoan ? "Select Friends" : "Select Friend");
        if (!isSharedLoan) {
            tvSharedSelectionSummary.setText("No friends selected yet.");
        } else {
            updateSharedSelectionSummary();
        }
    }

    private void showSharedFriendsDialog() {
        if (friendNamesList.isEmpty()) {
            Toast.makeText(this, "No friends found. Add friends first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sharedSelections.length != friendNamesList.size()) {
            sharedSelections = new boolean[friendNamesList.size()];
        }

        String[] namesArray = friendNamesList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Choose friends for shared loan")
                .setMultiChoiceItems(namesArray, sharedSelections, (dialog, which, isChecked) -> sharedSelections[which] = isChecked)
                .setPositiveButton("Done", (dialog, which) -> updateSharedSelectionSummary())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSharedSelectionSummary() {
        List<String> selectedNames = new ArrayList<>();
        for (int i = 0; i < friendNamesList.size(); i++) {
            if (i < sharedSelections.length && sharedSelections[i]) {
                selectedNames.add(friendNamesList.get(i));
            }
        }

        if (selectedNames.isEmpty()) {
            tvSharedSelectionSummary.setText("No friends selected yet.");
            return;
        }

        String summary = String.format(Locale.getDefault(), "%d friend(s): %s",
                selectedNames.size(), android.text.TextUtils.join(", ", selectedNames));
        tvSharedSelectionSummary.setText(summary);
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
                        selectedLoanDate = selectedCal.getTimeInMillis();
                        String formattedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        btnSelectLoanDate.setText("Loan Date: " + formattedDate);
                    } else {
                        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                        int currentMinute = calendar.get(Calendar.MINUTE);

                        new android.app.TimePickerDialog(AddLoanActivity.this,
                                (tView, hourOfDay, minute) -> {
                                    selectedCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    selectedCal.set(Calendar.MINUTE, minute);
                                    selectedDueDate = selectedCal.getTimeInMillis();
                                    String formattedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                                    btnSelectDueDate.setText("Due: " + formattedDate + " " + formattedTime);
                                }, currentHour, currentMinute, true).show();
                    }
                },
                currentYear, currentMonth, currentDay);
        datePickerDialog.show();
    }

    private void loadFriends() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("connections").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> friendsMap = documentSnapshot.getData();
                        for (String friendId : friendsMap.keySet()) {
                            Object isConnected = friendsMap.get(friendId);
                            if ((isConnected instanceof Boolean && (Boolean) isConnected)
                                    || "true".equals(isConnected)) {
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
                            sharedSelections = new boolean[friendNamesList.size()];
                            spinnerAdapter.notifyDataSetChanged();
                            updateSharedSelectionSummary();
                        }
                    }
                });
    }

    private void saveLoan() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String amountText = etLoanAmount.getText().toString().trim();
        String description = etLoanDescription.getText().toString().trim();

        if (amountText.isEmpty()) {
            etLoanAmount.setError("Enter amount");
            return;
        }

        double totalAmount;
        try {
            totalAmount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            etLoanAmount.setError("Invalid amount");
            return;
        }

        if (totalAmount <= 0) {
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

        String currentUserId = mAuth.getCurrentUser().getUid();
        boolean isSharedLoan = rbSharedLoan.isChecked();
        List<String> selectedBorrowerIds = resolveSelectedBorrowerIds(isSharedLoan);

        if (selectedBorrowerIds.isEmpty()) {
            Toast.makeText(this, isSharedLoan ? "Please select at least one friend" : "Please select an existing friend first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveLoan.setEnabled(false);

        if (isSharedLoan) {
            createSharedLoans(currentUserId, selectedBorrowerIds, totalAmount, description);
        } else {
            createIndividualLoan(currentUserId, selectedBorrowerIds.get(0), totalAmount, description);
        }
    }

    private List<String> resolveSelectedBorrowerIds(boolean isSharedLoan) {
        List<String> selectedBorrowerIds = new ArrayList<>();
        if (isSharedLoan) {
            for (int i = 0; i < friendIdsList.size(); i++) {
                if (i < sharedSelections.length && sharedSelections[i]) {
                    selectedBorrowerIds.add(friendIdsList.get(i));
                }
            }
        } else {
            int selectedPosition = spinnerFriends.getSelectedItemPosition();
            if (selectedPosition != Spinner.INVALID_POSITION && !friendIdsList.isEmpty()) {
                selectedBorrowerIds.add(friendIdsList.get(selectedPosition));
            }
        }
        return selectedBorrowerIds;
    }

    private void createIndividualLoan(String lenderId, String borrowerId, double amount, String description) {
        String newLoanId = db.collection("loans").document().getId();
        Loan loan = buildLoan(newLoanId, lenderId, borrowerId, amount, description, false, null, 1, amount);

        db.collection("loans").document(newLoanId)
                .set(loan)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Loan saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveLoan.setEnabled(true);
                    Toast.makeText(this, "Failed to save loan", Toast.LENGTH_SHORT).show();
                });
    }

    private void createSharedLoans(String lenderId, List<String> borrowerIds, double totalAmount, String description) {
        String sharedGroupId = db.collection("shared_loan_groups").document().getId();
        int splitCount = borrowerIds.size();
        double perFriendAmount = totalAmount / splitCount;

        WriteBatch batch = db.batch();
        for (String borrowerId : borrowerIds) {
            String newLoanId = db.collection("loans").document().getId();
            Loan loan = buildLoan(newLoanId, lenderId, borrowerId, perFriendAmount, description, true, sharedGroupId, splitCount, totalAmount);
            batch.set(db.collection("loans").document(newLoanId), loan);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    String message = String.format(Locale.getDefault(),
                            "Shared loan sent to %d friend(s). Each proposal is Rs. %.2f",
                            splitCount, perFriendAmount);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveLoan.setEnabled(true);
                    Toast.makeText(this, "Failed to send shared loan proposal", Toast.LENGTH_SHORT).show();
                });
    }

    private Loan buildLoan(String loanId,
                           String lenderId,
                           String borrowerId,
                           double amount,
                           String description,
                           boolean isSharedLoan,
                           String sharedGroupId,
                           int splitCount,
                           double originalTotalAmount) {
        Loan loan = new Loan(
                loanId,
                lenderId,
                borrowerId,
                amount,
                description,
                System.currentTimeMillis(),
                "pending",
                selectedLoanDate,
                selectedDueDate
        );
        loan.setSharedLoan(isSharedLoan);
        loan.setSharedGroupId(sharedGroupId);
        loan.setSplitCount(splitCount);
        loan.setOriginalTotalAmount(originalTotalAmount);
        loan.setSettlementRequested(false);
        loan.setSettlementRequestedBy(null);
        loan.setSettlementRequestedAt(0L);
        return loan;
    }
}
