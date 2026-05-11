package com.qarz.app.activities;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.qarz.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DealingsActivity extends AppCompatActivity {

    private static final String TAG = "DealingsActivity";

    private TextView tvDealingsTitle, tvIOweThem, tvTheyOweMe;
    private LinearLayout layout_active_loans, layout_past_loans;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String currentUserId, friendId, friendName;

    private List<QueryDocumentSnapshot> theyOweMeList = new ArrayList<>();
    private List<QueryDocumentSnapshot> iOweThemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dealings);

        tvDealingsTitle = findViewById(R.id.tvDealingsTitle);
        tvIOweThem = findViewById(R.id.tvIOweThem);
        tvTheyOweMe = findViewById(R.id.tvTheyOweMe);
        layout_active_loans = findViewById(R.id.layout_active_loans);
        layout_past_loans = findViewById(R.id.layout_past_loans);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        friendId = getIntent().getStringExtra("FRIEND_ID");
        friendName = getIntent().getStringExtra("FRIEND_NAME");

        if (friendId == null) {
            Toast.makeText(this, "Friend details missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDealingsTitle.setText("Dealings with " + (friendName != null ? friendName : "Friend"));
        btnBack.setOnClickListener(v -> finish());

        calculateDealings();
    }

    private void calculateDealings() {
        // Query 1: They Owe Me (fetch both active and settled)
        db.collection("loans")
                .whereEqualTo("lenderId", currentUserId)
                .whereIn("status", Arrays.asList("active", "settled"))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Failed listening to 'They Owe Me'", error);
                        return;
                    }
                    theyOweMeList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            String borrower = doc.getString("borrowerId");
                            if (friendId.equals(borrower)) {
                                theyOweMeList.add(doc);
                            }
                        }
                    }
                    renderAllLoans();
                });

        // Query 2: I Owe Them (fetch both active and settled)
        db.collection("loans")
                .whereEqualTo("borrowerId", currentUserId)
                .whereIn("status", Arrays.asList("active", "settled"))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Failed listening to 'I Owe Them'", error);
                        return;
                    }
                    iOweThemList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            String lender = doc.getString("lenderId");
                            if (friendId.equals(lender)) {
                                iOweThemList.add(doc);
                            }
                        }
                    }
                    renderAllLoans();
                });
    }

    private void renderAllLoans() {
        layout_active_loans.removeAllViews();
        layout_past_loans.removeAllViews();
        
        double totalTheyOweMe = 0;
        double totalIOweThem = 0;

        // Render "They Owe Me" Loans
        for (QueryDocumentSnapshot doc : theyOweMeList) {
            String status = doc.getString("status");
            Double amt = doc.getDouble("amount");
            if ("active".equals(status)) {
                if (amt != null) totalTheyOweMe += amt;
            }
            addLoanToUI(doc, true, status);
        }

        // Render "I Owe Them" Loans
        for (QueryDocumentSnapshot doc : iOweThemList) {
            String status = doc.getString("status");
            Double amt = doc.getDouble("amount");
            if ("active".equals(status)) {
                if (amt != null) totalIOweThem += amt;
            }
            addLoanToUI(doc, false, status);
        }

        tvTheyOweMe.setText(String.format("Rs. %.2f", totalTheyOweMe));
        tvIOweThem.setText(String.format("Rs. %.2f", totalIOweThem));
    }

    private void addLoanToUI(QueryDocumentSnapshot doc, boolean iAmLender, String status) {
        String loanId = doc.getId();
        Double amt = doc.getDouble("amount");
        String desc = doc.getString("description");
        Long dueDate = doc.getLong("dueDate");
        String borrowerId = doc.getString("borrowerId");
        
        if (desc == null || desc.trim().isEmpty()) desc = "No description";
        
        View view = getLayoutInflater().inflate(R.layout.item_loan, null);
        TextView tvDesc = view.findViewById(R.id.tvLoanDesc);
        TextView tvContext = view.findViewById(R.id.tvLoanContext);
        TextView tvAmt = view.findViewById(R.id.tvLoanAmount);
        Button btnSettle = view.findViewById(R.id.btnSettle);
        TextView tvLoanUid = view.findViewById(R.id.tvLoanUid);
        android.widget.ImageView ivCopyUid = view.findViewById(R.id.ivCopyUid);
        
        tvLoanUid.setText("UID: " + loanId);
        ivCopyUid.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Loan UID", loanId);
            clipboard.setPrimaryClip(clip);
            android.widget.Toast.makeText(this, "UID Copied to Clipboard", android.widget.Toast.LENGTH_SHORT).show();
        });
        
        LinearLayout llOverdueGuarantor = view.findViewById(R.id.llOverdueGuarantor);
        TextView tvGuarantorName = view.findViewById(R.id.tvGuarantorName);
        TextView tvGuarantorPhone = view.findViewById(R.id.tvGuarantorPhone);

        // Escalation Protocol: Trigger if active, user is lender, and System Time > dueDate
        if ("active".equals(status) && iAmLender && dueDate != null && dueDate > 0 && System.currentTimeMillis() > dueDate) {
            llOverdueGuarantor.setVisibility(View.VISIBLE);
            if (borrowerId != null) {
                db.collection("users").document(borrowerId).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        Object guarantorObj = userDoc.get("guarantor");
                        if (guarantorObj instanceof java.util.Map) {
                            java.util.Map<String, String> guarantor = (java.util.Map<String, String>) guarantorObj;
                            String gName = guarantor.get("name");
                            String gPhone = guarantor.get("phone");
                            tvGuarantorName.setText("Name: " + (gName != null ? gName : "N/A"));
                            tvGuarantorPhone.setText("Phone: " + (gPhone != null ? gPhone : "N/A"));
                        } else {
                            tvGuarantorName.setText("Name: Not Provided");
                            tvGuarantorPhone.setText("Phone: Not Provided");
                        }
                    }
                });
            }
        }

        // Add settled status tag to description if settled
        if ("settled".equals(status)) {
            tvDesc.setText(desc + " (Settled)");
        } else {
            tvDesc.setText(desc);
        }
        tvAmt.setText(String.format("Rs. %.2f", amt != null ? amt : 0.0));

        if (iAmLender) {
            tvContext.setText("You lent them");
            tvContext.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnSettle.setVisibility(View.VISIBLE);
            
            btnSettle.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Settle Debt")
                    .setMessage("Are you sure this debt is paid? This will move it to past dealings.")
                    .setPositiveButton("Yes, Settle", (dialog, which) -> settleLoan(loanId))
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        } else {
            tvContext.setText("You borrowed from them");
            tvContext.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            // Borrower cannot delete their own debt until the lender presses settle.
            btnSettle.setVisibility(View.GONE);
        }

        if ("settled".equals(status)) {
            btnSettle.setVisibility(View.GONE);
            view.setBackgroundColor(Color.parseColor("#F5F5F5")); // Light grey for historical
            tvDesc.setTextColor(Color.parseColor("#888888"));
            tvAmt.setTextColor(Color.parseColor("#888888"));
            tvContext.setTextColor(Color.parseColor("#888888"));
            layout_past_loans.addView(view);
        } else {
            layout_active_loans.addView(view);
        }
    }

    private void settleLoan(String loanId) {
        db.collection("loans").document(loanId).update("status", "settled")
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Debt marked as Settled!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to settle debt.", Toast.LENGTH_SHORT).show());
    }
}
