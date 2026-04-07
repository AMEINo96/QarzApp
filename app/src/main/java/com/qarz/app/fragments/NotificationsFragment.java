package com.qarz.app.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.qarz.app.R;

import java.util.Map;

public class NotificationsFragment extends Fragment {

    private LinearLayout llRequestsContainer;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        llRequestsContainer = view.findViewById(R.id.llRequestsContainer);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            loadAllRequests();
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Not authenticated", Toast.LENGTH_SHORT).show();
            }
        }
        return view;
    }

    private void loadAllRequests() {
        llRequestsContainer.removeAllViews();
        progressBar.setVisibility(View.VISIBLE);

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
                    loadLoanRequests();
                })
                .addOnFailureListener(e -> loadLoanRequests());
    }

    private void loadLoanRequests() {
        db.collection("loans")
                .whereEqualTo("borrowerId", currentUserId)
                //.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        loadLoanRequestUI(doc);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load loans. Firebase Index might be building.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFriendRequestUI(String friendId) {
        db.collection("users").document(friendId).get().addOnSuccessListener(userDoc -> {
            if (getContext() == null) return;
            String name = userDoc.getString("name");
            if (name == null) name = "Unknown User";

            TextView txt = new TextView(getContext());
            txt.setText("\nFriend Request from: " + name);
            txt.setTextSize(16);
            txt.setPadding(0, 16, 0, 16);
            
            Button btnAccept = new Button(getContext());
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
        db.collection("connections").document(currentUserId).update(friendId, "true")
            .addOnSuccessListener(aVoid -> {
                db.collection("connections").document(friendId).update(currentUserId, "true");
                llRequestsContainer.removeView(textCard);
                llRequestsContainer.removeView(btn);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Friend Accepted", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadLoanRequestUI(QueryDocumentSnapshot loanDoc) {
        String loanId = loanDoc.getId();
        String lenderId = loanDoc.getString("lenderId");
        Double amount = loanDoc.getDouble("amount");
        String desc = loanDoc.getString("description");
        String status = loanDoc.getString("status");

        db.collection("users").document(lenderId).get().addOnSuccessListener(userDoc -> {
            if (getContext() == null) return;
            String name = userDoc.getString("name");
            if (name == null) name = "Unknown";

            TextView txt = new TextView(getContext());
            txt.setText(String.format("\nLoan (%s)\nFrom: %s\nAmount: $%.2f\nDesc: %s", status != null ? status.toUpperCase() : "PENDING", name, amount != null ? amount : 0.0, desc));
            txt.setTextSize(16);
            txt.setPadding(0, 16, 0, 16);
            
            llRequestsContainer.addView(txt);

            if ("pending".equals(status)) {
                LinearLayout buttonsGroup = new LinearLayout(getContext());
                buttonsGroup.setOrientation(LinearLayout.HORIZONTAL);

                Button btnAccept = new Button(getContext());
                btnAccept.setText("Approve");
                btnAccept.setBackgroundColor(Color.parseColor("#2E7D32"));
                btnAccept.setTextColor(Color.WHITE);

                Button btnReject = new Button(getContext());
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
                            Toast.makeText(getContext(), "Loan Approved!", Toast.LENGTH_SHORT).show();
                            loadAllRequests(); // Refresh history log visually
                        });
                });

                btnReject.setOnClickListener(v -> {
                    btnAccept.setEnabled(false);
                    btnReject.setEnabled(false);
                    // Updated logic: Set status to rejected instead of deleting
                    db.collection("loans").document(loanId).update("status", "rejected")
                        .addOnSuccessListener(aVoid -> {
                            llRequestsContainer.removeView(txt);
                            llRequestsContainer.removeView(buttonsGroup);
                            Toast.makeText(getContext(), "Loan Rejected.", Toast.LENGTH_SHORT).show();
                            loadAllRequests(); // Refresh history log visually
                        });
                });

                buttonsGroup.addView(btnAccept);
                buttonsGroup.addView(btnReject);
                llRequestsContainer.addView(buttonsGroup);

            } else if ("active".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText("Approved");
                tvStatus.setTextColor(Color.parseColor("#2E7D32")); // Green
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                tvStatus.setPadding(0, 0, 0, 16);
                llRequestsContainer.addView(tvStatus);
                
            } else if ("rejected".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText("Rejected");
                tvStatus.setTextColor(Color.parseColor("#C62828")); // Red
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                tvStatus.setPadding(0, 0, 0, 16);
                llRequestsContainer.addView(tvStatus);
                
            } else if ("settled".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText("Settled by Lender");
                tvStatus.setTextColor(Color.parseColor("#2E7D32")); // Appealing green
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                tvStatus.setPadding(0, 0, 0, 16);
                llRequestsContainer.addView(tvStatus);
            }
        });
    }
}
