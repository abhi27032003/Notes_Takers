package com.example.recorderchunks.Background_Allow;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.recorderchunks.Activity.API_Updation;
import com.example.recorderchunks.Adapter.OnBackPressedListener;
import com.example.recorderchunks.Helpeerclasses.LocaleHelper;
import com.example.recorderchunks.R;
import com.example.recorderchunks.utils.BuildUtils;
import com.yariksoffice.lingver.Lingver;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Show_Add_notes_Activity extends AppCompatActivity {

    // Permissions needed for the app
    public  static int reload=0;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE,              // For foreground services
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION

    };
    public static final String SELECTED_APP_LANGUAGE = "SelectedappLanguage";
    private static final String PREF_NAME = "ApiKeysPref";
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_add_notes);
        //////
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedAppLanguage = sharedPreferences.getString(SELECTED_APP_LANGUAGE, "English");
        String localeCode = getLocaleCode(savedAppLanguage);
        LocaleHelper.setLocale(Show_Add_notes_Activity.this, localeCode);

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
        getUid();

        // Set the default fragment
        setFragment(new Show_Notes_Fragment());
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Iterate through the required permissions
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // Check if the list is not empty, then request permissions
        if (!permissionsToRequest.isEmpty()) {
            // Use the permission launcher for requesting multiple permissions
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            // Optional: Handle the case where all permissions are already granted
            Log.d("Permissions", "All required permissions are granted.");
        }
    }

    private void getUid() {
        // Get the shared preferences
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);

        // Check if uuid and signature already exist in shared preferences
        String storedUuid = prefs.getString("uuid", null);
        String storedSignature = prefs.getString("signature", null);

        if (storedUuid != null && storedSignature != null) {
            // If both uuid and signature are found, skip the network call
            Toast.makeText(Show_Add_notes_Activity.this, "UUID and Signature already saved", Toast.LENGTH_SHORT).show();
            return; // Exit the method if values already exist
        }

        // Get unique device details and hash them using SHA-1
        String product = BuildUtils.getSha1Hex(Build.PRODUCT);
        String build_id = BuildUtils.getSha1Hex(Build.ID);
        String build_display = BuildUtils.getSha1Hex(Build.DISPLAY);
        String ip_address = BuildUtils.getSha1Hex(BuildUtils.getIPAddress(true));
        String epoch_time = BuildUtils.getSha1Hex(String.valueOf(System.currentTimeMillis()));

        // Server URL for registration
        String URL = "https://nextstop.vipresearch.ca/App_Scripts//register.php";
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        // Prepare the API parameters to send in the request
        JSONObject params = new JSONObject();
        try {
            params.put("product", product);
            params.put("build_id", build_id);
            params.put("build_display", build_display);
            params.put("ip_address", ip_address);
            params.put("epoch_time", epoch_time);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Create the JsonObjectRequest for sending data to the server
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Extract UUID and CRC from the server response
                            String uuid = response.getString("uuid");
                            String crc = response.getString("crc");

                            // Calculate CRC on the client side using the received UUID and the data sent
                            String crc2 = BuildUtils.getSha1Hex(uuid + product + build_id + build_display + ip_address + epoch_time);
                            String signature = BuildUtils.getSha1Hex(product + build_id + build_display + ip_address + epoch_time);

                            // Compare the calculated CRC with the server's CRC
                            if (crc2.equals(crc)) {
                                // If CRCs match, show the UUID and signature in a Toast message
                                Toast.makeText(Show_Add_notes_Activity.this, "UUID: " + uuid + "\nSignature: " + signature, Toast.LENGTH_SHORT).show();

                                // Store the UUID and signature for later use if needed
                                SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE).edit();
                                editor.putString("uuid", uuid);
                                editor.putString("signature", signature);
                                editor.apply();

                            } else {
                                // If CRCs don't match, you can handle the error here
                                Toast.makeText(Show_Add_notes_Activity.this, "CRC mismatch, retrying...", Toast.LENGTH_SHORT).show();
                                getUid();  // Optionally, retry if CRCs don't match
                            }
                        } catch (Exception e) {
                            // Handle any errors that occur during response processing
                            e.printStackTrace();
                            Toast.makeText(Show_Add_notes_Activity.this, "Error processing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Handle errors that occur during the network request
                Log.w("_DEBUG_ error", error.getCause());
                Toast.makeText(Show_Add_notes_Activity.this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Add the request to the queue
        requestQueue.add(jsonObjectRequest);
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
            if (currentFragment instanceof OnBackPressedListener) {
                // Let the fragment handle the back press
                if (((OnBackPressedListener) currentFragment).onBackPressed()) {
                    return; // If fragment consumed the back press, exit here
                }
            }
            else
            {
                setFragment( new Show_Notes_Fragment());
            }
             // Replace with your desired fragment
        } else {
            super.onBackPressed(); // Default back press behavior
        }
    }
    private String getLocaleCode(String language) {
        switch (language) {
            case "English":
                return "en"; // English
            case "French":
                return "fr"; // French
            case "Chinese":
                return "zh"; // Chinese
            case "Hindi":
                return "hi"; // Hindi
            case "Spanish":
                return "es"; // Spanish
            default:
                return "en"; // Default to English if no match
        }
    }

    @Override
    protected void onResume() {
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedAppLanguage = sharedPreferences.getString(SELECTED_APP_LANGUAGE, "English");
        String localeCode = getLocaleCode(savedAppLanguage);
        LocaleHelper.setLocale(Show_Add_notes_Activity.this, localeCode);
        super.onResume();
    }

    @Override
    protected void onRestart() {
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedAppLanguage = sharedPreferences.getString(SELECTED_APP_LANGUAGE, "English");
        String localeCode = getLocaleCode(savedAppLanguage);
        LocaleHelper.setLocale(Show_Add_notes_Activity.this, localeCode);
        super.onRestart();
    }
}
