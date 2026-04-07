# QarzApp 📱💸

QarzApp is a professional, production-ready Android Loan Management System engineered for transparency, accountability, and real-time ledger tracking. 

## 🌟 Key Features

*   **Comprehensive KYC & Authentication:** Robust user registration backed by Firebase Auth, featuring strict regex data validations for CNIC (`12345-1234567-1`), Guarantors, and Phone Numbers (`03XXXXXXXXX`).
*   **FinTech Dashboard & Analytics:** Real-time localized (PKR / Rs.) portfolio snapshots powered by integrated MPAndroidChart PieCharts, differentiating between your "Owe Me" and "I Owe" ledgers globally.
*   **Temporal Integrity & Due Dates:** Implements Epoch timestamps ensuring loans abide by strict temporal logic. Users can precisely map deadlines upon proposal creation.
*   **Escalation Protocol:** Actively protects lending capital by monitoring deadline thresholds. If a loan is overdue, the user interface actively triggers an escalation UI exposing the borrower's Guarantor Data exclusively to the Lender.
*   **Receipt Verification Engine:** Every initiated loan is tracked via a strict Firestore UID. Users can fetch global transaction states dynamically by inputting unique verifiable receipts directly from the Dashboard.
*   **Live App Badging & Logs:** Integrated bottom navigation listeners push live notification badges dynamically whenever there is a structurally "pending" transaction.
*   **Immutable Settlements:** Complete role-based security preventing unverified mutation. "Settled" loans enter an immutable historical archive for clean ledger logging.

## 🛠️ Technologies Used

*   **Language:** Native Java
*   **Platform:** Android SDK (Min SDK 21 / Target SDK 34)
*   **Backend & DB:** Firebase Authentication & Cloud Firestore (NoSQL)
*   **UI/UX Toolkit:** Google Material Design Components
*   **Data Visualization:** MPAndroidChart (v3.1.0)
*   **Background Threading:** Android WorkManager (For advanced pending Notifications)

## 💻 Setup Instructions (Laptop / Developer)

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/AMEINo96/QarzApp.git
   ```
2. **Open in Android Studio:**
   Navigate into the downloaded folder and open it via Android Studio. Allow Gradle to execute its environment initializations and mapping procedures.
3. **Establish Firebase Topology:**
   Ensure you place an active `google-services.json` file inside the strictly partitioned `app/` directory (This maps your local Android codebase directly into your specific Firebase project limits/rules).
4. **Compile & Run:**
   Execute a build directly onto an AVD (Android Virtual Device) Emulator or a physically plugged USB device.

## 📱 Mobile Installation (APK)

1. Complete the Laptop developer setup first.
2. Under Android Studio's navigation, select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. Locate the finished `.apk` file (Usually within `app/build/outputs/apk/debug/`).
4. Transfer this APK file directly to your physical Android device.
5. In your Android device settings, make sure to enable **"Install Unknown Apps/Sources"**.
6. Tap the `.apk` on your mobile and select **Install**. Launch the application natively!
