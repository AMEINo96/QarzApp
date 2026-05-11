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
import com.qarz.app.activities.CompleteProfileActivity;
import com.qarz.app.activities.LoginActivity;
import com.qarz.app.fragments.DashboardFragment;
import com.qarz.app.fragments.FriendsListFragment;
import com.qarz.app.fragments.MyDebtsFragment;
import com.qarz.app.fragments.MyLoansFragment;
import com.qarz.app.fragments.NotificationsFragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import com.qarz.app.utils.OverdueReminderWorker;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private int pendingBorrowerRequests = 0;
    private int pendingSettlementAppeals = 0;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    scheduleOverdueReminders();
                } else {
                    Log.w("MainActivity", "Notification permission denied. Overdue reminders won't show.");
                }
            });

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

        // Ensure the signed-in user has a complete Firestore profile (CNIC present).
        // Guards against Google-signed users who haven't filled in their details.
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || doc.getString("cnic") == null) {
                        startActivity(new Intent(this, CompleteProfileActivity.class));
                        finish();
                    } else {
                        initMainScreen();
                    }
                })
                .addOnFailureListener(e -> {
                    // Firestore read failed (likely rules issue or network).
                    Log.e("MainActivity", "Profile check failed: " + e.getMessage());
                    // We MUST NOT let them into the main screen if we can't verify their profile,
                    // otherwise they end up with empty details.
                    // Force them to CompleteProfileActivity.
                    startActivity(new Intent(this, CompleteProfileActivity.class));
                    finish();
                });
    }

    /** Sets up bottom navigation and badge listeners. Called after profile check passes. */
    private void initMainScreen() {
        bottomNav = findViewById(R.id.bottom_navigation);

        checkNotificationPermissionAndSchedule();

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
        bottomNav.setSelectedItemId(R.id.navigation_dashboard);

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
                    pendingBorrowerRequests = value != null ? value.size() : 0;
                    updateNotificationBadge();
                });

        db.collection("loans")
                .whereEqualTo("lenderId", currentUserId)
                .whereEqualTo("status", "active")
                .whereEqualTo("settlementRequested", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("MainActivity", "Settlement appeal listener failed.", error);
                        return;
                    }
                    pendingSettlementAppeals = value != null ? value.size() : 0;
                    updateNotificationBadge();
                });
    }

    private void updateNotificationBadge() {
        if (bottomNav == null) return;
        int count = pendingBorrowerRequests + pendingSettlementAppeals;
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.navigation_notifications);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
            badge.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    private void checkNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                scheduleOverdueReminders();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            scheduleOverdueReminders();
        }
    }

    private void scheduleOverdueReminders() {
        // Run daily to check for overdue loans
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                OverdueReminderWorker.class, 24, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "OverdueReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest);
    }
}
