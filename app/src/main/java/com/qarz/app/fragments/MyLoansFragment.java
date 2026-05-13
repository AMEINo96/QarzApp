package com.qarz.app.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.qarz.app.R;
import com.qarz.app.adapters.TabLoanAdapter;
import com.qarz.app.models.DisplayLoan;

import java.util.ArrayList;
import java.util.List;

public class MyLoansFragment extends Fragment {

    private static final String TAG = "MyLoansFragment";

    private ProgressBar progressBar;
    private RecyclerView rvMyLoans;
    private TextView tvEmptyLoans;
    private TabLoanAdapter adapter;
    private List<DisplayLoan> loansList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_loans, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        rvMyLoans = view.findViewById(R.id.rvMyLoans);
        tvEmptyLoans = view.findViewById(R.id.tvEmptyLoans);

        rvMyLoans.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TabLoanAdapter(loansList);
        rvMyLoans.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadMyLoans();
        }

        return view;
    }

    private void loadMyLoans() {
        progressBar.setVisibility(View.VISIBLE);

        // Lender == currentUser, so we pull active loans and fetch the BORROWER's name.
        db.collection("loans")
                .whereEqualTo("lenderId", currentUserId)
                .whereEqualTo("status", "active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        progressBar.setVisibility(View.GONE);
                        updateEmptyState(true);
                        return;
                    }
                    
                    if (value == null || value.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        loansList.clear();
                        adapter.notifyDataSetChanged();
                        updateEmptyState(true);
                        return;
                    }

                    List<DisplayLoan> tempLoans = new ArrayList<>();
                    int totalDocs = value.size();
                    int[] completedCount = {0}; // Hack for atomic counting in lambda

                    for (QueryDocumentSnapshot doc : value) {
                        String borrowerId = doc.getString("borrowerId");
                        Double amount = doc.getDouble("amount");
                        String desc = doc.getString("description");
                        
                        if (borrowerId == null) borrowerId = "unknown";

                        db.collection("users").document(borrowerId).get().addOnSuccessListener(userDoc -> {
                            String name = userDoc.getString("name");
                            if (name == null) name = "Unknown Borrower";
                            
                            Long createdAt = doc.getLong("createdAt");
                            long time = createdAt != null ? createdAt : 0L;
                            tempLoans.add(new DisplayLoan(doc.getId(), name, amount != null ? amount : 0, desc, time));
                            
                            completedCount[0]++;
                            if (completedCount[0] == totalDocs) {
                                progressBar.setVisibility(View.GONE);
                                java.util.Collections.sort(tempLoans, (l1, l2) -> Long.compare(l2.getCreatedAt(), l1.getCreatedAt()));
                                loansList.clear();
                                loansList.addAll(tempLoans);
                                adapter.notifyDataSetChanged();
                                updateEmptyState(loansList.isEmpty());
                            }
                        }).addOnFailureListener(e -> {
                            completedCount[0]++;
                            if (completedCount[0] == totalDocs) {
                                progressBar.setVisibility(View.GONE);
                                java.util.Collections.sort(tempLoans, (l1, l2) -> Long.compare(l2.getCreatedAt(), l1.getCreatedAt()));
                                loansList.clear();
                                loansList.addAll(tempLoans);
                                adapter.notifyDataSetChanged();
                                updateEmptyState(loansList.isEmpty());
                            }
                        });
                    }
                });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (tvEmptyLoans != null) {
            tvEmptyLoans.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (rvMyLoans != null) {
            rvMyLoans.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}
