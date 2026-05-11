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
        } else if (getContext() != null) {
            Toast.makeText(getContext(), "Not authenticated", Toast.LENGTH_SHORT).show();
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
                    loadBorrowerLoanRequests();
                })
                .addOnFailureListener(e -> loadBorrowerLoanRequests());
    }

    private void loadBorrowerLoanRequests() {
        db.collection("loans")
                .whereEqualTo("borrowerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        loadLoanRequestUI(doc);
                    }
                    loadSettlementAppealsForLender();
                })
                .addOnFailureListener(e -> {
                    loadSettlementAppealsForLender();
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load borrower requests.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSettlementAppealsForLender() {
        db.collection("loans")
                .whereEqualTo("lenderId", currentUserId)
                .whereEqualTo("status", "active")
                .whereEqualTo("settlementRequested", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        loadSettlementAppealUI(doc);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load settlement appeals.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFriendRequestUI(String friendId) {
        db.collection("users").document(friendId).get().addOnSuccessListener(userDoc -> {
            if (getContext() == null) {
                return;
            }
            String name = userDoc.getString("name");
            if (name == null) {
                name = "Unknown User";
            }

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
        Boolean sharedLoan = loanDoc.getBoolean("sharedLoan");
        Long splitCount = loanDoc.getLong("splitCount");
        Double originalTotal = loanDoc.getDouble("originalTotalAmount");
        Boolean settlementRequested = loanDoc.getBoolean("settlementRequested");

        db.collection("users").document(lenderId).get().addOnSuccessListener(userDoc -> {
            if (getContext() == null) {
                return;
            }
            String name = userDoc.getString("name");
            if (name == null) {
                name = "Unknown";
            }

            StringBuilder details = new StringBuilder();
            details.append("\nLoan (").append(status != null ? status.toUpperCase() : "PENDING").append(")\n")
                    .append("From: ").append(name)
                    .append("\nAmount: Rs. ").append(String.format(java.util.Locale.getDefault(), "%.2f", amount != null ? amount : 0.0))
                    .append("\nDesc: ").append(desc != null ? desc : "No description");

            if (Boolean.TRUE.equals(sharedLoan) && splitCount != null && splitCount > 1) {
                details.append("\nShared split: ")
                        .append(splitCount)
                        .append(" friends");
                if (originalTotal != null && originalTotal > 0) {
                    details.append(" from Rs. ")
                            .append(String.format(java.util.Locale.getDefault(), "%.2f", originalTotal));
                }
            }

            if ("active".equals(status) && Boolean.TRUE.equals(settlementRequested)) {
                details.append("\nSettlement appeal: waiting on lender");
            }

            TextView txt = new TextView(getContext());
            txt.setText(details.toString());
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
                                loadAllRequests();
                            });
                });

                btnReject.setOnClickListener(v -> {
                    btnAccept.setEnabled(false);
                    btnReject.setEnabled(false);
                    db.collection("loans").document(loanId).update("status", "rejected")
                            .addOnSuccessListener(aVoid -> {
                                llRequestsContainer.removeView(txt);
                                llRequestsContainer.removeView(buttonsGroup);
                                Toast.makeText(getContext(), "Loan Rejected.", Toast.LENGTH_SHORT).show();
                                loadAllRequests();
                            });
                });

                buttonsGroup.addView(btnAccept);
                buttonsGroup.addView(btnReject);
                llRequestsContainer.addView(buttonsGroup);

            } else if ("active".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText(Boolean.TRUE.equals(settlementRequested) ? "Appeal sent to lender" : "Approved");
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                tvStatus.setPadding(0, 0, 0, 16);
                llRequestsContainer.addView(tvStatus);

            } else if ("rejected".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText("Rejected");
                tvStatus.setTextColor(Color.parseColor("#C62828"));
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                tvStatus.setPadding(0, 0, 0, 16);
                llRequestsContainer.addView(tvStatus);

            } else if ("settled".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText("Settled by lender");
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                tvStatus.setPadding(0, 0, 0, 16);
                llRequestsContainer.addView(tvStatus);
            }
        });
    }

    private void loadSettlementAppealUI(QueryDocumentSnapshot loanDoc) {
        String loanId = loanDoc.getId();
        String borrowerId = loanDoc.getString("borrowerId");
        Double amount = loanDoc.getDouble("amount");
        String desc = loanDoc.getString("description");

        db.collection("users").document(borrowerId).get().addOnSuccessListener(userDoc -> {
            if (getContext() == null) {
                return;
            }
            String name = userDoc.getString("name");
            if (name == null) {
                name = "Unknown borrower";
            }

            TextView txt = new TextView(getContext());
            txt.setText(String.format(java.util.Locale.getDefault(),
                    "\nSettlement Appeal\nFrom: %s\nAmount: Rs. %.2f\nDesc: %s",
                    name, amount != null ? amount : 0.0, desc != null ? desc : "No description"));
            txt.setTextSize(16);
            txt.setPadding(0, 16, 0, 16);

            LinearLayout buttonsGroup = new LinearLayout(getContext());
            buttonsGroup.setOrientation(LinearLayout.HORIZONTAL);

            Button btnApprove = new Button(getContext());
            btnApprove.setText("Settle");
            btnApprove.setBackgroundColor(Color.parseColor("#2E7D32"));
            btnApprove.setTextColor(Color.WHITE);

            Button btnDecline = new Button(getContext());
            btnDecline.setText("Keep Active");
            btnDecline.setBackgroundColor(Color.parseColor("#6D4C41"));
            btnDecline.setTextColor(Color.WHITE);

            btnApprove.setOnClickListener(v -> {
                btnApprove.setEnabled(false);
                btnDecline.setEnabled(false);
                db.collection("loans").document(loanId).update(
                        "status", "settled",
                        "settlementRequested", false,
                        "settlementRequestedBy", null,
                        "settlementRequestedAt", 0L
                ).addOnSuccessListener(aVoid -> {
                    llRequestsContainer.removeView(txt);
                    llRequestsContainer.removeView(buttonsGroup);
                    Toast.makeText(getContext(), "Loan settled.", Toast.LENGTH_SHORT).show();
                    loadAllRequests();
                });
            });

            btnDecline.setOnClickListener(v -> {
                btnApprove.setEnabled(false);
                btnDecline.setEnabled(false);
                db.collection("loans").document(loanId).update(
                        "settlementRequested", false,
                        "settlementRequestedBy", null,
                        "settlementRequestedAt", 0L
                ).addOnSuccessListener(aVoid -> {
                    llRequestsContainer.removeView(txt);
                    llRequestsContainer.removeView(buttonsGroup);
                    Toast.makeText(getContext(), "Settlement appeal declined.", Toast.LENGTH_SHORT).show();
                    loadAllRequests();
                });
            });

            buttonsGroup.addView(btnApprove);
            buttonsGroup.addView(btnDecline);

            llRequestsContainer.addView(txt);
            llRequestsContainer.addView(buttonsGroup);
        });
    }
}
