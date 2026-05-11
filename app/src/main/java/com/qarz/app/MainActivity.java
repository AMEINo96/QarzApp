package com.qarz.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.qarz.app.activities.LoginActivity;
import com.qarz.app.fragments.DashboardFragment;
import com.qarz.app.fragments.FriendsListFragment;
import com.qarz.app.fragments.MyDebtsFragment;
import com.qarz.app.fragments.MyLoansFragment;
import com.qarz.app.fragments.NotificationsFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.navigation_notifications) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.navigation_friends) {
                selectedFragment = new FriendsListFragment();
            } else if (itemId == R.id.navigation_loans) {
                selectedFragment = new MyLoansFragment();
            } else if (itemId == R.id.navigation_debts) {
                selectedFragment = new MyDebtsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Load the default fragment on startup
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.navigation_dashboard);
        }

        // Initialize Live Badge Counter
        listenForNotificationBadges();
    }

    private void listenForNotificationBadges() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("loans")
                .whereEqualTo("borrowerId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("MainActivity", "Listen failed.", error);
                        return;
                    }

                    int count = 0;
                    if (value != null) {
                        count = value.size(); // Number of pending requests
                    }

                    BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.navigation_notifications);
                    if (count > 0) {
                        badge.setVisible(true);
                        badge.setNumber(count);
                        badge.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        badge.setVisible(false);
                        badge.clearNumber();
                    }
                });
    }
}