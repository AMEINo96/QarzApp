package com.qarz.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.qarz.app.MainActivity;
import com.qarz.app.R;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;
    private android.view.View btnGoogleSignIn;
    private TextView tvGoToRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;

    // MUST be declared as a field and registered in onCreate BEFORE onStart()
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ── Step 1: Configure Google Sign-In ────────────────────────────────
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // ── Step 2: Register result launcher (MUST happen before onStart) ───
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task =
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleSignInResult(task);
                });

        // ── Step 3: Bind views ───────────────────────────────────────────────
        etLoginEmail    = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin        = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvGoToRegister  = findViewById(R.id.tvGoToRegister);

        // ── Step 4: Attach listeners ─────────────────────────────────────────

        // Force email to lowercase as user types
        etLoginEmail.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormatting) return;
                String text = editable.toString();
                String lower = text.toLowerCase();
                if (!text.equals(lower)) {
                    isFormatting = true;
                    etLoginEmail.setText(lower);
                    etLoginEmail.setSelection(lower.length());
                    isFormatting = false;
                }
            }
        });

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // ── Step 5: Auto-login check (AFTER everything is set up) ────────────
        // Must be LAST so that if routeSignedInUser signs out and stays on this
        // screen, all views and launchers are already ready for use.
        if (mAuth.getCurrentUser() != null) {
            routeSignedInUser(mAuth.getCurrentUser(), false);
        }
    }

    // ── Email / Password Login ───────────────────────────────────────────────

    private void loginUser() {
        String email    = etLoginEmail.getText().toString().trim().toLowerCase();
        String password = etLoginPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Login Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ── Google Sign-In ───────────────────────────────────────────────────────

    private void startGoogleSignIn() {
        // Sign out of any cached Google account so the chooser always shows
        googleSignInClient.signOut().addOnCompleteListener(task ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        if (completedTask == null) {
            Toast.makeText(this, "Google Sign-In: result task is null", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                Toast.makeText(this, "Google Sign-In: account is null", Toast.LENGTH_LONG).show();
                return;
            }
            String idToken = account.getIdToken();
            if (idToken == null) {
                Toast.makeText(this, "Google Sign-In: idToken is null. Make sure Google Sign-In is enabled in Firebase Console and the Web Client ID is correct.", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Google account selected. Authenticating...", Toast.LENGTH_SHORT).show();
            firebaseAuthWithGoogle(idToken);
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in ApiException, status: " + e.getStatusCode(), e);
            String hint = "";
            switch (e.getStatusCode()) {
                case 10:  hint = " (DEVELOPER_ERROR: SHA-1 or package mismatch)"; break;
                case 12500: hint = " (SIGN_IN_FAILED)"; break;
                case 12501: hint = " (SIGN_IN_CANCELLED)"; break;
                case 7:   hint = " (NETWORK_ERROR)"; break;
            }
            Toast.makeText(this,
                    "Google Sign-In failed (code " + e.getStatusCode() + hint + ")",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Fresh Google sign-in: route to CompleteProfileActivity if needed
                        routeSignedInUser(mAuth.getCurrentUser(), true);
                    } else {
                        Exception ex = task.getException();
                        String msg = ex != null ? ex.getMessage() : "Unknown Firebase error";
                        Log.e(TAG, "Firebase signInWithCredential failed: " + msg, ex);
                        Toast.makeText(this,
                                "Firebase auth failed: " + msg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ── Routing ──────────────────────────────────────────────────────────────

    /**
     * Routes the signed-in user based on their Firestore profile completeness.
     *
     * @param fromFreshGoogleSignIn
     *   true  — came from tapping the Google button;
     *            incomplete profile → go to CompleteProfileActivity.
     *   false — cold-start auto-login;
     *            incomplete profile → sign out and stay on Login.
     */
    private void routeSignedInUser(FirebaseUser user, boolean fromFreshGoogleSignIn) {
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("cnic") != null) {
                        // Profile complete → home screen
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else if (fromFreshGoogleSignIn) {
                        // Fresh Google login, profile incomplete → collect details
                        startActivity(new Intent(this, CompleteProfileActivity.class));
                        finish();
                    } else {
                        // Cold start, profile incomplete → sign out, stay on Login
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore read failed in routeSignedInUser: " + e.getMessage(), e);
                    // Firestore rules are likely blocking the read, or network error.
                    // We cannot bypass the profile check. Force them to CompleteProfileActivity.
                    Toast.makeText(this,
                            "Profile check failed: " + e.getMessage() + ". Proceeding to profile setup.",
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, CompleteProfileActivity.class));
                    finish();
                });
    }
}
