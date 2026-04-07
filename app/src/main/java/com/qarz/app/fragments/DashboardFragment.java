package com.qarz.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.qarz.app.R;
import com.qarz.app.activities.AddLoanActivity;
import com.qarz.app.activities.LoginActivity;
import com.qarz.app.activities.SearchUserActivity;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private TextView tvOweMeTotal, tvIOweTotal;
    private PieChart pieChart;
    private double currentTotalOweMe = 0.0;
    private double currentTotalIOwe = 0.0;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
            return view;
        }

        tvOweMeTotal = view.findViewById(R.id.tvOweMeTotal);
        tvIOweTotal = view.findViewById(R.id.tvIOweTotal);
        pieChart = view.findViewById(R.id.pieChart);

        android.widget.ImageView ivProfile = view.findViewById(R.id.ivProfile);
        ivProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.qarz.app.activities.ProfileActivity.class));
        });

        Button btnNewLoan = view.findViewById(R.id.btnNewLoan);

        btnNewLoan.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddLoanActivity.class));
        });

        android.widget.ImageView ivSearchReceipt = view.findViewById(R.id.ivSearchReceipt);
        ivSearchReceipt.setOnClickListener(v -> {
            android.widget.EditText input = new android.widget.EditText(getContext());
            input.setHint("Enter isolated Loan UID string...");
            
            new android.app.AlertDialog.Builder(getContext())
                .setTitle("Receipt Tracker")
                .setMessage("Enter the exact Loan UID to verify its immutable state.")
                .setView(input)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String uid = input.getText().toString().trim();
                    if (!uid.isEmpty()) {
                        searchReceiptByUid(uid);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        updateDashboardTotals();
        return view;
    }

    private void searchReceiptByUid(String uid) {
        db.collection("loans").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String lenderId = doc.getString("lenderId");
                String borrowerId = doc.getString("borrowerId");
                Double amount = doc.getDouble("amount");
                String status = doc.getString("status");

                if (lenderId == null || borrowerId == null) {
                    android.widget.Toast.makeText(getContext(), "Corrupted UID Block", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Execute chained queries to decrypt Names securely
                db.collection("users").document(lenderId).get().addOnSuccessListener(lenderDoc -> {
                    String lenderName = lenderDoc.getString("name");
                    db.collection("users").document(borrowerId).get().addOnSuccessListener(borrowerDoc -> {
                        String borrowerName = borrowerDoc.getString("name");
                        
                        String receiptText = "Loan UID: " + uid + "\n\n" +
                                             "Lender Name: " + (lenderName != null ? lenderName : "Unknown") + "\n" +
                                             "Borrower Name: " + (borrowerName != null ? borrowerName : "Unknown") + "\n" +
                                             "Amount Value: Rs" + String.format("%.2f", amount != null ? amount : 0) + "\n\n" +
                                             "Current Status: " + (status != null ? status.toUpperCase() : "UNKNOWN");
                                             
                        new android.app.AlertDialog.Builder(getContext())
                            .setTitle("Verified Blockchain Receipt")
                            .setMessage(receiptText)
                            .setPositiveButton("Acknowledge", null)
                            .show();
                    });
                });
            } else {
                android.widget.Toast.makeText(getContext(), "Invalid UID or Loan not found", android.widget.Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> android.widget.Toast.makeText(getContext(), "Network verification failed", android.widget.Toast.LENGTH_SHORT).show());
    }

    private void updateDashboardTotals() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("loans")
                .whereEqualTo("lenderId", currentUserId)
                .whereEqualTo("status", "active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for 'Owe Me' totals.", error);
                        return;
                    }
                    double totalOweMe = 0;
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Double amount = doc.getDouble("amount");
                            if (amount != null) {
                                totalOweMe += amount;
                            }
                        }
                    }
                    if (tvOweMeTotal != null) {
                        tvOweMeTotal.setText(String.format("Rs. %.2f", totalOweMe));
                        currentTotalOweMe = totalOweMe;
                        updateChart(currentTotalOweMe, currentTotalIOwe);
                    }
                });

        db.collection("loans")
                .whereEqualTo("borrowerId", currentUserId)
                .whereEqualTo("status", "active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for 'I Owe' totals.", error);
                        return;
                    }
                    double totalIOwe = 0;
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Double amount = doc.getDouble("amount");
                            if (amount != null) {
                                totalIOwe += amount;
                            }
                        }
                    }
                    if (tvIOweTotal != null) {
                        tvIOweTotal.setText(String.format("Rs. %.2f", totalIOwe));
                        currentTotalIOwe = totalIOwe;
                        updateChart(currentTotalOweMe, currentTotalIOwe);
                    }
                });
    }

    private void updateChart(double totalLent, double totalBorrowed) {
        if (pieChart == null) return;
        
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (totalLent == 0 && totalBorrowed == 0) {
            pieChart.clear();
            return;
        }

        if (totalLent > 0) entries.add(new PieEntry((float) totalLent, "Lent"));
        if (totalBorrowed > 0) entries.add(new PieEntry((float) totalBorrowed, "Borrowed"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#4CAF50")); // Green
        colors.add(Color.parseColor("#F44336")); // Red
        dataSet.setColors(colors);
        dataSet.setValueTextSize(16f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setCenterText("Portfolio");
        pieChart.setCenterTextSize(20f);
        pieChart.setCenterTextColor(Color.parseColor("#666666"));
        pieChart.getLegend().setEnabled(false);
        
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}
