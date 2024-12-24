package com.example.recorderchunks.Background_Allow;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.recorderchunks.R;

import java.util.ArrayList;
import java.util.List;

public class Show_Add_notes_Activity extends AppCompatActivity {

    // Permissions needed for the app
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_add_notes);

        // Initialize the permissions launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    for (String permission : REQUIRED_PERMISSIONS) {
                        Boolean isGranted = result.get(permission);
                        if (isGranted == null || !isGranted) {
                            // Permission denied, you may show a dialog here explaining why it's needed
                           // finish(); // End the activity if critical permissions are denied
                        }
                    }
                }
        );

        // Check and request permissions
        checkAndRequestPermissions();

        // Set the default fragment
        setFragment(new Show_Notes_Fragment());
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    public void setFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_item_container, fragment)
                .commit();
    }
    @Override
    public void onBackPressed() {
        // Find the current fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_item_container);

        // Check if the current fragment is an instance of Show_Notes_Fragment (or any other fragment)
        if (currentFragment instanceof Show_Notes_Fragment) {
            new android.app.AlertDialog.Builder(this)
                    .setMessage("Do you want to close the app?")
                    .setCancelable(false) // Prevents the dialog from being canceled by touching outside
                    .setPositiveButton("Yes", (dialog, id) -> {
                        // If the user confirms, exit the app
                        finish(); // This will close the activity (app)
                    })
                    .setNegativeButton("No", (dialog, id) -> {
                        // If the user selects 'No', do nothing (dismiss the dialog)
                        dialog.dismiss();
                    })
                    .show();
        } else if (currentFragment instanceof Add_notes_Fragment) {
            setFragment(new Show_Notes_Fragment()); // Replace with your desired fragment
        } else {
            super.onBackPressed(); // Default back press behavior
        }
    }
}
