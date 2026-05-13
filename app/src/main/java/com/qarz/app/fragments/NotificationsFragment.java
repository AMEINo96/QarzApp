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
    private TextView tvEmptyNotifications;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        llRequestsContainer = view.findViewById(R.id.llRequestsContainer);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyNotifications = view.findViewById(R.id.tvEmptyNotifications);

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
        llRequestsContainer.addView(progressBar);
        llRequestsContainer.addView(tvEmptyNotifications);
        tvEmptyNotifications.setVisibility(View.GONE);
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
                    java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = new java.util.ArrayList<>(queryDocumentSnapshots.getDocuments());
                    java.util.Collections.sort(docs, (d1, d2) -> {
                        Long t1 = d1.getLong("createdAt");
                        Long t2 = d2.getLong("createdAt");
                        if (t1 == null) t1 = 0L;
                        if (t2 == null) t2 = 0L;
                        return t2.compareTo(t1);
                    });
                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                        loadLoanRequestUI((QueryDocumentSnapshot) doc);
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
                    tvEmptyNotifications.setVisibility(llRequestsContainer.getChildCount() <= 2 ? View.VISIBLE : View.GONE);
                    java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = new java.util.ArrayList<>(queryDocumentSnapshots.getDocuments());
                    java.util.Collections.sort(docs, (d1, d2) -> {
                        Long t1 = d1.getLong("createdAt");
                        Long t2 = d2.getLong("createdAt");
                        if (t1 == null) t1 = 0L;
                        if (t2 == null) t2 = 0L;
                        return t2.compareTo(t1);
                    });
                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                        loadSettlementAppealUI((QueryDocumentSnapshot) doc);
                    }
                    tvEmptyNotifications.setVisibility(llRequestsContainer.getChildCount() <= 2 ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyNotifications.setVisibility(llRequestsContainer.getChildCount() <= 2 ? View.VISIBLE : View.GONE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load settlement appeals.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFriendRequestUI(String friendId) {
        if (getContext() == null) return;
        View cardView = getLayoutInflater().inflate(R.layout.item_notification, llRequestsContainer, false);
        llRequestsContainer.addView(cardView);

        db.collection("users").document(friendId).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            if (name == null) {
                name = "Unknown User";
            }

            LinearLayout llContent = cardView.findViewById(R.id.llNotificationContent);

            TextView txt = new TextView(getContext());
            txt.setText("Friend Request from: " + name);
            txt.setTextSize(16);
            txt.setPadding(0, 0, 0, 16);

            LinearLayout buttonsGroup = new LinearLayout(getContext());
            buttonsGroup.setOrientation(LinearLayout.HORIZONTAL);

            com.google.android.material.button.MaterialButton btnAccept = new com.google.android.material.button.MaterialButton(getContext());
            btnAccept.setText("Accept");
            btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            btnAccept.setTextColor(Color.WHITE);
            btnAccept.setCornerRadius(50);

            com.google.android.material.button.MaterialButton btnReject = new com.google.android.material.button.MaterialButton(getContext());
            btnReject.setText("Reject");
            btnReject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#C62828")));
            btnReject.setTextColor(Color.WHITE);
            btnReject.setCornerRadius(50);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            btnAccept.setLayoutParams(params);

            btnAccept.setOnClickListener(v -> acceptFriendRequest(friendId, cardView));
            btnReject.setOnClickListener(v -> rejectFriendRequest(friendId, cardView));

            buttonsGroup.addView(btnAccept);
            buttonsGroup.addView(btnReject);

            llContent.addView(txt);
            llContent.addView(buttonsGroup);

            tvEmptyNotifications.setVisibility(View.GONE);
        });
    }

    private void acceptFriendRequest(String friendId, View cardView) {
        db.collection("connections").document(currentUserId).update(friendId, "true")
                .addOnSuccessListener(aVoid -> {
                    db.collection("connections").document(friendId).update(currentUserId, "true");
                    llRequestsContainer.removeView(cardView);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Friend Accepted", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rejectFriendRequest(String friendId, View cardView) {
        db.collection("connections").document(currentUserId).update(friendId, com.google.firebase.firestore.FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    llRequestsContainer.removeView(cardView);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Friend Request Rejected", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadLoanRequestUI(QueryDocumentSnapshot loanDoc) {
        if (getContext() == null) return;
        View cardView = getLayoutInflater().inflate(R.layout.item_notification, llRequestsContainer, false);
        llRequestsContainer.addView(cardView);

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
            String name = userDoc.getString("name");
            if (name == null) {
                name = "Unknown";
            }

            LinearLayout llContent = cardView.findViewById(R.id.llNotificationContent);

            StringBuilder details = new StringBuilder();
            details.append("Loan (").append(status != null ? status.toUpperCase() : "PENDING").append(")\n")
                    .append("From: ").append(name)
                    .append("\nAmount: Rs. ").append(String.format(java.util.Locale.getDefault(), "%,.0f", amount != null ? amount : 0.0))
                    .append("\nDesc: ").append(desc != null ? desc : "No description");

            if (Boolean.TRUE.equals(sharedLoan) && splitCount != null && splitCount > 1) {
                details.append("\nShared split: ")
                        .append(splitCount)
                        .append(" friends");
                if (originalTotal != null && originalTotal > 0) {
                    details.append(" from Rs. ")
                            .append(String.format(java.util.Locale.getDefault(), "%,.0f", originalTotal));
                }
            }

            if ("active".equals(status) && Boolean.TRUE.equals(settlementRequested)) {
                details.append("\nSettlement appeal: waiting on lender");
            }

            TextView txt = new TextView(getContext());
            txt.setText(details.toString());
            txt.setTextSize(16);
            txt.setPadding(0, 0, 0, 16);

            llContent.addView(txt);

            if ("pending".equals(status)) {
                LinearLayout buttonsGroup = new LinearLayout(getContext());
                buttonsGroup.setOrientation(LinearLayout.HORIZONTAL);

                com.google.android.material.button.MaterialButton btnAccept = new com.google.android.material.button.MaterialButton(getContext());
                btnAccept.setText("Approve");
                btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                btnAccept.setTextColor(Color.WHITE);
                btnAccept.setCornerRadius(50);

                com.google.android.material.button.MaterialButton btnReject = new com.google.android.material.button.MaterialButton(getContext());
                btnReject.setText("Reject");
                btnReject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#C62828")));
                btnReject.setTextColor(Color.WHITE);
                btnReject.setCornerRadius(50);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 16, 0);
                btnAccept.setLayoutParams(params);

                btnAccept.setOnClickListener(v -> {
                    btnAccept.setEnabled(false);
                    btnReject.setEnabled(false);
                    db.collection("loans").document(loanId).update("status", "active")
                            .addOnSuccessListener(aVoid -> {
                                llRequestsContainer.removeView(cardView);
                                Toast.makeText(getContext(), "Loan Approved!", Toast.LENGTH_SHORT).show();
                                loadAllRequests();
                            });
                });

                btnReject.setOnClickListener(v -> {
                    btnAccept.setEnabled(false);
                    btnReject.setEnabled(false);
                    db.collection("loans").document(loanId).update("status", "rejected")
                            .addOnSuccessListener(aVoid -> {
                                llRequestsContainer.removeView(cardView);
                                Toast.makeText(getContext(), "Loan Rejected.", Toast.LENGTH_SHORT).show();
                                loadAllRequests();
                            });
                });

                buttonsGroup.addView(btnAccept);
                buttonsGroup.addView(btnReject);
                llContent.addView(buttonsGroup);

            } else if ("active".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText(Boolean.TRUE.equals(settlementRequested) ? "Appeal sent to lender" : "Approved");
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                llContent.addView(tvStatus);

            } else if ("rejected".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText("Rejected");
                tvStatus.setTextColor(Color.parseColor("#C62828"));
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                llContent.addView(tvStatus);

            } else if ("settled".equals(status)) {
                TextView tvStatus = new TextView(getContext());
                tvStatus.setText("Settled by lender");
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextSize(15);
                llContent.addView(tvStatus);
            }
            
            tvEmptyNotifications.setVisibility(View.GONE);
        });
    }

    private void loadSettlementAppealUI(QueryDocumentSnapshot loanDoc) {
        if (getContext() == null) return;
        View cardView = getLayoutInflater().inflate(R.layout.item_notification, llRequestsContainer, false);
        llRequestsContainer.addView(cardView);

        String loanId = loanDoc.getId();
        String borrowerId = loanDoc.getString("borrowerId");
        Double amount = loanDoc.getDouble("amount");
        String desc = loanDoc.getString("description");

        db.collection("users").document(borrowerId).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            if (name == null) {
                name = "Unknown borrower";
            }

            LinearLayout llContent = cardView.findViewById(R.id.llNotificationContent);

            TextView txt = new TextView(getContext());
            txt.setText(String.format(java.util.Locale.getDefault(),
                    "Settlement Appeal\nFrom: %s\nAmount: Rs. %,.0f\nDesc: %s",
                    name, amount != null ? amount : 0.0, desc != null ? desc : "No description"));
            txt.setTextSize(16);
            txt.setPadding(0, 0, 0, 16);

            LinearLayout buttonsGroup = new LinearLayout(getContext());
            buttonsGroup.setOrientation(LinearLayout.HORIZONTAL);

            com.google.android.material.button.MaterialButton btnApprove = new com.google.android.material.button.MaterialButton(getContext());
            btnApprove.setText("Settle");
            btnApprove.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            btnApprove.setTextColor(Color.WHITE);
            btnApprove.setCornerRadius(50);

            com.google.android.material.button.MaterialButton btnDecline = new com.google.android.material.button.MaterialButton(getContext());
            btnDecline.setText("Keep Active");
            btnDecline.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#6D4C41")));
            btnDecline.setTextColor(Color.WHITE);
            btnDecline.setCornerRadius(50);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            btnApprove.setLayoutParams(params);

            btnApprove.setOnClickListener(v -> {
                btnApprove.setEnabled(false);
                btnDecline.setEnabled(false);
                db.collection("loans").document(loanId).update(
                        "status", "settled",
                        "settlementRequested", false,
                        "settlementRequestedBy", null,
                        "settlementRequestedAt", 0L
                ).addOnSuccessListener(aVoid -> {
                    llRequestsContainer.removeView(cardView);
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
                    llRequestsContainer.removeView(cardView);
                    Toast.makeText(getContext(), "Settlement appeal declined.", Toast.LENGTH_SHORT).show();
                    loadAllRequests();
                });
            });

            buttonsGroup.addView(btnApprove);
            buttonsGroup.addView(btnDecline);

            llContent.addView(txt);
            llContent.addView(buttonsGroup);

            tvEmptyNotifications.setVisibility(View.GONE);
        });
    }
}
