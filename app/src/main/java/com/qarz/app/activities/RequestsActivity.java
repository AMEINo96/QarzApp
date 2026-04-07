package com.qarz.app.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.qarz.app.R;

import java.util.Map;

public class RequestsActivity extends AppCompatActivity {

    private LinearLayout llRequestsContainer;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        llRequestsContainer = findViewById(R.id.llRequestsContainer);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            loadAllRequests();
        } else {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadAllRequests() {
        llRequestsContainer.removeAllViews();
        progressBar.setVisibility(View.VISIBLE);

        // Load Friend Requests First
        db.collection("connections").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> connections = documentSnapshot.getData();
                        for (Map.Entry<String, Object> entry : connections.entrySet()) {
                            if ("requested".equals(entry.getValue())) {
                                loadFriendRequestUI(entry.getKey());
                            }
                        }
                    }
                    // Load Loan Proposals Second
                    loadLoanRequests();
                })
                .addOnFailureListener(e -> loadLoanRequests());
    }

    private void loadLoanRequests() {
        db.collection("loans")
                .whereEqualTo("borrowerId", currentUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        loadLoanRequestUI(doc);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load loans", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFriendRequestUI(String friendId) {
        db.collection("users").document(friendId).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            if (name == null) name = "Unknown User";

            TextView txt = new TextView(this);
            txt.setText("\nFriend Request from: " + name);
            txt.setTextSize(16);
            txt.setPadding(0, 16, 0, 16);
            
            Button btnAccept = new Button(this);
            btnAccept.setText("Accept Friend");
            btnAccept.setBackgroundColor(Color.parseColor("#2E7D32"));
            btnAccept.setTextColor(Color.WHITE);

            btnAccept.setOnClickListener(v -> acceptFriendRequest(friendId, txt, btnAccept));

            llRequestsContainer.addView(txt);
            llRequestsContainer.addView(btnAccept);
        });
    }

    private void acceptFriendRequest(String friendId, View textCard, Button btn) {
        btn.setEnabled(false);
        // Set my doc to true string
        db.collection("connections").document(currentUserId).update(friendId, "true")
            .addOnSuccessListener(aVoid -> {
                // Set their doc to true string
                db.collection("connections").document(friendId).update(currentUserId, "true");
                llRequestsContainer.removeView(textCard);
                llRequestsContainer.removeView(btn);
                Toast.makeText(this, "Friend Accepted", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadLoanRequestUI(QueryDocumentSnapshot loanDoc) {
        String loanId = loanDoc.getId();
        String lenderId = loanDoc.getString("lenderId");
        Double amount = loanDoc.getDouble("amount");
        String desc = loanDoc.getString("description");

        db.collection("users").document(lenderId).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            if (name == null) name = "Unknown";

            TextView txt = new TextView(this);
            txt.setText(String.format("\nLoan Proposal\nFrom: %s\nAmount: $%.2f\nDesc: %s", name, amount != null ? amount : 0.0, desc));
            txt.setTextSize(16);
            txt.setPadding(0, 16, 0, 16);

            LinearLayout buttonsGroup = new LinearLayout(this);
            buttonsGroup.setOrientation(LinearLayout.HORIZONTAL);

            Button btnAccept = new Button(this);
            btnAccept.setText("Approve");
            btnAccept.setBackgroundColor(Color.parseColor("#2E7D32"));
            btnAccept.setTextColor(Color.WHITE);

            Button btnReject = new Button(this);
            btnReject.setText("Reject");
            btnReject.setBackgroundColor(Color.parseColor("#C62828"));
            btnReject.setTextColor(Color.WHITE);

            btnAccept.setOnClickListener(v -> {
                btnAccept.setEnabled(false);
                btnReject.setEnabled(false);
                db.collection("loans").document(loanId).update("status", "active")
                    .addOnSuccessListener(aVoid -> {
                        llRequestsContainer.removeView(txt);
                        llRequestsContainer.removeView(buttonsGroup);
                        Toast.makeText(this, "Loan Approved!", Toast.LENGTH_SHORT).show();
                    });
            });

            btnReject.setOnClickListener(v -> {
                btnAccept.setEnabled(false);
                btnReject.setEnabled(false);
                // "the database should not keep record of rejected loans"
                db.collection("loans").document(loanId).delete()
                    .addOnSuccessListener(aVoid -> {
                        llRequestsContainer.removeView(txt);
                        llRequestsContainer.removeView(buttonsGroup);
                        Toast.makeText(this, "Loan Rejected & Deleted.", Toast.LENGTH_SHORT).show();
                    });
            });

            buttonsGroup.addView(btnAccept);
            buttonsGroup.addView(btnReject);

            llRequestsContainer.addView(txt);
            llRequestsContainer.addView(buttonsGroup);
        });
    }
}
