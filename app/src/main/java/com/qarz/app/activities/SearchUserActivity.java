package com.qarz.app.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.qarz.app.R;

import java.util.HashMap;
import java.util.Map;

public class SearchUserActivity extends AppCompatActivity {

    private static final String TAG = "SearchUserActivity";

    private EditText etSearchEmail;
    private Button btnSearch, btnAddFriend;
    private ProgressBar progressBar;
    private LinearLayout resultLayout;
    private TextView tvFoundUserName, tvFoundUserEmail;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String foundUserId = null;
    private String currentUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            // Unauthenticated state fallback (useful for dev/test before Auth is fully built)
            currentUserId = "test_user_id";
            Toast.makeText(this, "Not authenticated! Using mock ID.", Toast.LENGTH_SHORT).show();
        }

        etSearchEmail = findViewById(R.id.etSearchEmail);
        btnSearch = findViewById(R.id.btnSearch);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        progressBar = findViewById(R.id.progressBar);
        resultLayout = findViewById(R.id.resultLayout);
        tvFoundUserName = findViewById(R.id.tvFoundUserName);
        tvFoundUserEmail = findViewById(R.id.tvFoundUserEmail);

        btnSearch.setOnClickListener(v -> searchUser());
        btnAddFriend.setOnClickListener(v -> addFriend());
    }

    private void searchUser() {
        String email = etSearchEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etSearchEmail.setError("Please enter an email");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.GONE);
        foundUserId = null;

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // Email should be unique, so we take the first match
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        
                        foundUserId = document.getString("userId");
                        if (foundUserId == null) {
                            foundUserId = document.getId(); // Fallback to doc ID if the field is missing
                        }
                        
                        String name = document.getString("name");
                        String foundEmail = document.getString("email");

                        tvFoundUserName.setText(name != null ? name : "Unknown Name");
                        tvFoundUserEmail.setText(foundEmail != null ? foundEmail : "");
                        
                        // Prevent adding yourself
                        if (currentUserId.equals(foundUserId)) {
                            Toast.makeText(this, "You cannot add yourself", Toast.LENGTH_SHORT).show();
                        } else {
                            resultLayout.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(SearchUserActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SearchUserActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Search failed", e);
                });
    }

    private void addFriend() {
        if (foundUserId == null || currentUserId == null) return;
        
        btnAddFriend.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // 1. Add connection for the current user (sent request)
        Map<String, Object> myConnection = new HashMap<>();
        myConnection.put(foundUserId, "pending");

        db.collection("connections").document(currentUserId)
                .set(myConnection, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // 2. Add the reverse connection for the friend (received request)
                    Map<String, Object> friendConnection = new HashMap<>();
                    friendConnection.put(currentUserId, "requested");
                    
                    db.collection("connections").document(foundUserId)
                            .set(friendConnection, SetOptions.merge())
                            .addOnSuccessListener(aVoid1 -> {
                                progressBar.setVisibility(View.GONE);
                                btnAddFriend.setText("Request Sent");
                                Toast.makeText(SearchUserActivity.this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> handleAddError(e));
                })
                .addOnFailureListener(e -> handleAddError(e));
    }
    
    private void handleAddError(Exception e) {
        progressBar.setVisibility(View.GONE);
        btnAddFriend.setEnabled(true); // Re-enable for retry
        Toast.makeText(SearchUserActivity.this, "Failed to add friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Connection failed", e);
    }
}
