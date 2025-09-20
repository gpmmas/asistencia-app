package com.checador;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private CircleImageView profileImageView;
    private TextView userNameTextView;
    private TextView dateTimeTextView;
    private TextView statusTextView;
    private Button attendanceButton;
    private Button logoutButton;

    private boolean isCheckedIn = false; // To track the current attendance state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profileImageView = findViewById(R.id.profileImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        dateTimeTextView = findViewById(R.id.dateTimeTextView);
        statusTextView = findViewById(R.id.statusTextView);
        attendanceButton = findViewById(R.id.attendanceButton);
        logoutButton = findViewById(R.id.logoutButton);

        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Load user data and check attendance status on startup
            loadUserData();
            checkAttendanceStatus();
            updateDateTime();
        }

        attendanceButton.setOnClickListener(v -> {
            if (isCheckedIn) {
                recordAttendance("Check Out");
            } else {
                recordAttendance("Check In");
            }
        });



        if (currentUser != null) {
            loadUserData();
            checkAttendanceStatus();
            updateDateTime();
        }

        // Set a click listener for the logout button
        logoutButton.setOnClickListener(v -> {
            // Sign out the current user
            mAuth.signOut();

            // Redirect to the login screen
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close the main activity
        });
    }
    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy \n h:mm a", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        dateTimeTextView.setText(currentDateAndTime);
    }

    private void checkAttendanceStatus() {
        if (currentUser == null) return;

        // Query Firestore for the most recent attendance record for the current user
        db.collection("attendanceLogs")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String lastType = queryDocumentSnapshots.getDocuments().get(0).getString("type");
                        if ("Check In".equals(lastType)) {
                            // Last action was Check In, so the user is currently checked in
                            isCheckedIn = true;
                            statusTextView.setText("Entrada");
                            attendanceButton.setText("Check Out");
                        } else {
                            // Last action was Check Out, or no records exist
                            isCheckedIn = false;
                            statusTextView.setText("Salida");
                            attendanceButton.setText("Check In");
                        }
                    } else {
                        // No attendance records found, user is not checked in
                        isCheckedIn = false;
                        statusTextView.setText("Salida");
                        attendanceButton.setText("Check In");
                    }
                });
    }

    private void recordAttendance(String type) {
        if (currentUser == null) return;

        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("userId", currentUser.getUid());
        attendanceData.put("email", currentUser.getEmail());
        attendanceData.put("timestamp", new Date());
        attendanceData.put("type", type);

        db.collection("attendanceLogs")
                .add(attendanceData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, type + " recorded successfully!", Toast.LENGTH_SHORT).show();
                    // Update UI immediately after successful record
                    isCheckedIn = "Check In".equals(type);
                    statusTextView.setText(isCheckedIn ? "Entrada" : "Salida");
                    attendanceButton.setText(isCheckedIn ? "Check Out" : "Check In");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error recording " + type + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserData() {
        if (currentUser == null) return;

        // Fetch user document from Firestore
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null) {
                            userNameTextView.setText(name);
                        }
                        // You can load the profile image here if the URL is stored in Firestore
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                });
    }

}