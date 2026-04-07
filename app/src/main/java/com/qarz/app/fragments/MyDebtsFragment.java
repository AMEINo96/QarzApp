package com.qarz.app.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

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

public class MyDebtsFragment extends Fragment {

    private static final String TAG = "MyDebtsFragment";

    private ProgressBar progressBar;
    private RecyclerView rvMyDebts;
    private TabLoanAdapter adapter;
    private List<DisplayLoan> debtsList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_debts, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        rvMyDebts = view.findViewById(R.id.rvMyDebts);

        rvMyDebts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TabLoanAdapter(debtsList);
        rvMyDebts.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadMyDebts();
        }

        return view;
    }

    private void loadMyDebts() {
        progressBar.setVisibility(View.VISIBLE);

        // Borrower == currentUser, so we pull active loans and fetch the LENDER'S name.
        db.collection("loans")
                .whereEqualTo("borrowerId", currentUserId)
                .whereEqualTo("status", "active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    
                    if (value == null || value.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        debtsList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    List<DisplayLoan> tempDebts = new ArrayList<>();
                    int totalDocs = value.size();
                    int[] completedCount = {0};

                    for (QueryDocumentSnapshot doc : value) {
                        String lenderId = doc.getString("lenderId");
                        Double amount = doc.getDouble("amount");
                        String desc = doc.getString("description");
                        
                        if (lenderId == null) lenderId = "unknown";

                        db.collection("users").document(lenderId).get().addOnSuccessListener(userDoc -> {
                            String name = userDoc.getString("name");
                            if (name == null) name = "Unknown Lender";
                            
                            tempDebts.add(new DisplayLoan(doc.getId(), name, amount != null ? amount : 0, desc));
                            
                            completedCount[0]++;
                            if (completedCount[0] == totalDocs) {
                                progressBar.setVisibility(View.GONE);
                                debtsList.clear();
                                debtsList.addAll(tempDebts);
                                adapter.notifyDataSetChanged();
                            }
                        }).addOnFailureListener(e -> {
                            completedCount[0]++;
                            if (completedCount[0] == totalDocs) {
                                progressBar.setVisibility(View.GONE);
                                debtsList.clear();
                                debtsList.addAll(tempDebts);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
    }
}
