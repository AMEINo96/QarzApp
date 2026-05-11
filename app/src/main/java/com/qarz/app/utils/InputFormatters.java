package com.qarz.app.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.Calendar;

/**
 * Centralized input formatting and validation utilities for CNIC, Phone, and DOB fields.
 * Reuse these across all Activities to ensure consistent formatting behaviour.
 */
public final class InputFormatters {

    private InputFormatters() {
        // Prevent instantiation
    }

    // ── CNIC Formatter (12345-1234567-1) ───────────────────────────────────

    public static void attachCnicFormatter(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormatting) return;

                String digitsOnly = editable.toString().replaceAll("\\D", "");
                if (digitsOnly.length() > 13) {
                    digitsOnly = digitsOnly.substring(0, 13);
                }

                String formatted = formatCnic(digitsOnly);
                if (!formatted.equals(editable.toString())) {
                    isFormatting = true;
                    editText.setText(formatted);
                    editText.setSelection(formatted.length());
                    isFormatting = false;
                }
            }
        });
    }

    public static String formatCnic(String digitsOnly) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digitsOnly.length(); i++) {
            if (i == 5 || i == 12) {
                builder.append('-');
            }
            builder.append(digitsOnly.charAt(i));
        }
        return builder.toString();
    }

    // ── Phone Formatter (03XX-XXXXXXX → display as 03XX-XXXXXXX) ──────────

    public static void attachPhoneFormatter(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormatting) return;

                String digitsOnly = editable.toString().replaceAll("\\D", "");
                if (digitsOnly.length() > 11) {
                    digitsOnly = digitsOnly.substring(0, 11);
                }

                String formatted = formatPhone(digitsOnly);
                if (!formatted.equals(editable.toString())) {
                    isFormatting = true;
                    editText.setText(formatted);
                    editText.setSelection(formatted.length());
                    isFormatting = false;
                }
            }
        });
    }

    public static String formatPhone(String digitsOnly) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digitsOnly.length(); i++) {
            if (i == 4) {
                builder.append('-');
            }
            builder.append(digitsOnly.charAt(i));
        }
        return builder.toString();
    }

    // ── DOB Formatter (DD/MM/YYYY) ─────────────────────────────────────────

    public static void attachDateFormatter(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormatting) return;

                String digitsOnly = editable.toString().replaceAll("\\D", "");
                if (digitsOnly.length() > 8) {
                    digitsOnly = digitsOnly.substring(0, 8);
                }

                String formatted = formatDob(digitsOnly);
                if (!formatted.equals(editable.toString())) {
                    isFormatting = true;
                    editText.setText(formatted);
                    editText.setSelection(formatted.length());
                    isFormatting = false;
                }
            }
        });
    }

    public static String formatDob(String digitsOnly) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digitsOnly.length(); i++) {
            if (i == 2 || i == 4) {
                builder.append('/');
            }
            builder.append(digitsOnly.charAt(i));
        }
        return builder.toString();
    }

    // ── Strict DOB Validation ──────────────────────────────────────────────

    /**
     * Validates a DOB string in DD/MM/YYYY format.
     * Rejects impossible dates (e.g., 30 Feb, 31 Apr, future dates, age > 120).
     * Returns null if valid, or a human-readable error message if invalid.
     */
    public static String validateDob(String dob) {
        if (dob == null || dob.isEmpty()) {
            return "DOB is required.";
        }

        if (!dob.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
            return "DOB must follow DD/MM/YYYY format.";
        }

        String[] parts = dob.split("/");
        int day, month, year;
        try {
            day = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
            year = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return "DOB contains invalid numbers.";
        }

        // Month range
        if (month < 1 || month > 12) {
            return "Invalid month. Must be 01–12.";
        }

        // Day range (basic)
        if (day < 1 || day > 31) {
            return "Invalid day. Must be 01–31.";
        }

        // Year sanity
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        if (year < 1900 || year > currentYear) {
            return "Year must be between 1900 and " + currentYear + ".";
        }

        // Days-per-month validation (including leap year)
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (isLeapYear(year)) {
            daysInMonth[1] = 29;
        }
        if (day > daysInMonth[month - 1]) {
            return "Invalid day for the given month. " + getMonthName(month) + " has " + daysInMonth[month - 1] + " days.";
        }

        // Cannot be a future date
        Calendar dob_cal = Calendar.getInstance();
        dob_cal.set(year, month - 1, day);
        Calendar today = Calendar.getInstance();
        if (dob_cal.after(today)) {
            return "DOB cannot be in the future.";
        }

        // Age sanity check (must be at least 13 and at most 120)
        int age = currentYear - year;
        if (age > 120) {
            return "Age cannot exceed 120 years.";
        }
        if (age < 13) {
            return "You must be at least 13 years old.";
        }

        return null; // Valid
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    private static String getMonthName(int month) {
        String[] names = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return names[month - 1];
    }
}
