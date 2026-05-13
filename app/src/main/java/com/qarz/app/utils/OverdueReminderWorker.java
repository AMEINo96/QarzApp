package com.qarz.app.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class OverdueReminderWorker extends Worker {
    public OverdueReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return Result.success();
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long currentTime = System.currentTimeMillis();

        try {
            Task<QuerySnapshot> task = FirebaseFirestore.getInstance().collection("loans")
                    .whereEqualTo("borrowerId", currentUserId)
                    .whereEqualTo("status", "active")
                    .get();

            QuerySnapshot snapshot = Tasks.await(task);
            if (snapshot != null) {
                int overdueCount = 0;
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                    Long dueDate = doc.getLong("dueDate");
                    if (dueDate != null && dueDate > 0 && currentTime > dueDate) {
                        overdueCount++;
                    }
                }
                
                if (overdueCount > 0) {
                    NotificationHelper.showNotification(
                            getApplicationContext(),
                            "Overdue Loans Alert!",
                            "You have " + overdueCount + " overdue loan(s). Please settle them!",
                            1001
                    );
                }
            }
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            // Using Result.retry() or Result.success() to prevent permanent failure loop
            // but for simplicity failure is returned
            return Result.failure();
        }
    }
}
