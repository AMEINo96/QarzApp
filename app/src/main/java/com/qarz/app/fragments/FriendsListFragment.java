package com.qarz.app.fragments;

import android.content.Intent;
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
import com.qarz.app.R;
import com.qarz.app.activities.DealingsActivity;

import java.util.Map;

public class FriendsListFragment extends Fragment {

    private LinearLayout llFriendsContainer;
    private ProgressBar progressBar;
    private TextView tvEmptyFriends;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        llFriendsContainer = view.findViewById(R.id.llFriendsContainer);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyFriends = view.findViewById(R.id.tvEmptyFriends);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        android.widget.ImageView ivAddFriend = view.findViewById(R.id.ivAddFriend);
        ivAddFriend.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.qarz.app.activities.SearchUserActivity.class));
        });

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            loadFriends();
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Not authenticated", Toast.LENGTH_SHORT).show();
            }
        }
        return view;
    }

    private void loadFriends() {
        llFriendsContainer.removeAllViews();
        progressBar.setVisibility(View.VISIBLE);

        db.collection("connections").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    boolean hasFriends = false;
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> connections = documentSnapshot.getData();
                        for (Map.Entry<String, Object> entry : connections.entrySet()) {
                            Object status = entry.getValue();
                            if (("true".equals(status)) || (status instanceof Boolean && (Boolean) status)) {
                                hasFriends = true;
                                loadFriendProfile(entry.getKey());
                            }
                        }
                    }
                    tvEmptyFriends.setVisibility(hasFriends ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyFriends.setVisibility(View.VISIBLE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load friends.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFriendProfile(String friendId) {
        db.collection("users").document(friendId).get().addOnSuccessListener(userDoc -> {
            if (getContext() == null || getActivity() == null) return;
            String name = userDoc.getString("name");
            String email = userDoc.getString("email");
            
            if (name == null) name = "Unknown User";
            if (email == null) email = "No email provided";

            // Inflate our card layout for each friend cleanly
            View itemFriend = getLayoutInflater().inflate(R.layout.item_friend, null);
            TextView tvName = itemFriend.findViewById(R.id.tvFriendName);
            TextView tvEmail = itemFriend.findViewById(R.id.tvFriendEmail);
            Button btnCheckDealings = itemFriend.findViewById(R.id.btnCheckDealings);

            tvName.setText(name);
            tvEmail.setText(email);

            // Pass identity forward to deep-dive activity
            final String fName = name;
            btnCheckDealings.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), DealingsActivity.class);
                intent.putExtra("FRIEND_ID", friendId);
                intent.putExtra("FRIEND_NAME", fName);
                startActivity(intent);
            });

            Button btnRemoveFriend = itemFriend.findViewById(R.id.btnRemoveFriend);
            btnRemoveFriend.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle("Remove Friend")
                        .setMessage("Are you sure you want to remove " + fName + "?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            btnRemoveFriend.setEnabled(false);
                            db.collection("connections").document(currentUserId).update(friendId, com.google.firebase.firestore.FieldValue.delete())
                                    .addOnSuccessListener(aVoid -> {
                                        db.collection("connections").document(friendId).update(currentUserId, com.google.firebase.firestore.FieldValue.delete());
                                        llFriendsContainer.removeView(itemFriend);
                                        Toast.makeText(getContext(), fName + " removed.", Toast.LENGTH_SHORT).show();
                                        if (llFriendsContainer.getChildCount() == 0) {
                                            tvEmptyFriends.setVisibility(View.VISIBLE);
                                        }
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            tvEmptyFriends.setVisibility(View.GONE);
            llFriendsContainer.addView(itemFriend);
        });
    }
}
