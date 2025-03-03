package com.example.recorderchunks.Background_Allow;

import static com.example.recorderchunks.Activity.API_Updation.SELECTED_LANGUAGE;
import static com.example.recorderchunks.Encryption.AudioEncryptor.generateKeyPair;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.decrypt;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.decryptTextPrivateRSA;

import static com.example.recorderchunks.Encryption.RSAKeyGenerator.divideString;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.encrypt;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.generateAESKey_local;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.generateAndFormatRSAKeys;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.generateSHA256Hash;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.generateSHA256HashWithSalt;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.getPrivateKeyFromPEM;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.getPublicKeyFromPEM;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.getPublicKeyFromString;
import static com.example.recorderchunks.Encryption.RSAKeyGenerator.verifyDivision;


import android.Manifest;
import android.app.ProgressDialog;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.recorderchunks.Activity.API_Updation;
import com.example.recorderchunks.Activity.Manage_Prompt;
import com.example.recorderchunks.Adapter.AudioRecyclerAdapter;
import com.example.recorderchunks.Adapter.OnBackPressedListener;
import com.example.recorderchunks.Adapter.PromptAdapter;
import com.example.recorderchunks.AudioPlayer.AudioPlayerManager;
import com.example.recorderchunks.AudioPlayer.AudioPlayerViewModel;
import com.example.recorderchunks.Audio_Models.ModelDownloader;
import com.example.recorderchunks.Audio_Models.Vosk_Model;
import com.example.recorderchunks.Encryption.KeyPairPEM;
import com.example.recorderchunks.Helpeerclasses.Chunks_Database_Helper;
import com.example.recorderchunks.Helpeerclasses.LocaleHelper;
import com.example.recorderchunks.Helpeerclasses.Model_Database_Helper;
import com.example.recorderchunks.Helpeerclasses.Prompt_Database_Helper;
import com.example.recorderchunks.ManageLogs.AppLogger;
import com.example.recorderchunks.Model_Class.Prompt;
import com.example.recorderchunks.R;
import com.example.recorderchunks.utils.BuildUtils;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallRequest;


import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import android.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Show_Add_notes_Activity extends AppCompatActivity {

    // Permissions needed for the app
    public  static int reload=0;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE,              // For foreground services
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,
            Manifest.permission.WAKE_LOCK

    };
    public static final String SELECTED_APP_LANGUAGE = "SelectedappLanguage";
    public static final String IS_PROMPT_SAVED = "Is_Prompt_saved";
    public static final String SELECTED_LANGUAGE = "SelectedLanguage";
    private static final String PREF_NAME = "ApiKeysPref";
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<String[]> permissionLauncher;
    AppLogger logger ;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_add_notes);
        //////
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);




        if (isFirstLaunch) {


            // Mark first launch as completed
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isFirstLaunch", false);
            editor.apply();
        }
        //////
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedAppLanguage = sharedPreferences.getString(SELECTED_APP_LANGUAGE, "English");
        String isprompt_saved=sharedPreferences.getString(IS_PROMPT_SAVED, "no");
        String localeCode = getLocaleCode(savedAppLanguage);
        LocaleHelper.setLocale(Show_Add_notes_Activity.this, localeCode);
        logger= AppLogger.getInstance(Show_Add_notes_Activity.this);
        Chunks_Database_Helper chunks_database_helper=new Chunks_Database_Helper(this);
        chunks_database_helper.logAllChunks();

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

        //if default prompt not saved then save them else not
        if(isprompt_saved.contains("no"))
        {
            add_prompt("General Meeting Notes","Summarize the key points discussed during today's meeting, highlighting any important decisions made, assigned action items with responsible individuals, and the next steps to be taken");
            add_prompt("Lecture Notes","identify the key points, main arguments, and central theme presented by the lecturer, then rephrase them concisely in your own words, ensuring you capture the essential information without unnecessary details; this can be done by focusing on the most important concepts and supporting evidence discussed during the lecture");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(IS_PROMPT_SAVED, "yes");
            editor.apply();
            //Toast.makeText(this, "Notes Saved", Toast.LENGTH_SHORT).show();

        }
        else
        {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(IS_PROMPT_SAVED, "yes");
            editor.apply();
          //  Toast.makeText(this, "Notes not Saved", Toast.LENGTH_SHORT).show();


        }

        // Check and request permissions
        checkAndRequestPermissions();
        try {
            getUid();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Set the default fragment
        setFragment(new Show_Notes_Fragment());
        check_saved_model_download();
    }

    private void check_saved_model_download() {
        Model_Database_Helper modelDatabaseHelper=new Model_Database_Helper(Show_Add_notes_Activity.this);
        String savedModelLanguage = sharedPreferences.getString(SELECTED_LANGUAGE, "English");


        if(modelDatabaseHelper.checkModelDownloadedByLanguage(savedModelLanguage))
        {
           // Vosk_Model.initializeVoskModel(Show_Add_notes_Activity.this,modelDatabaseHelper.getModelNameByLanguage(savedModelLanguage));



        }
        else
        {
            if(!ModelDownloader.isModelDownloading(savedModelLanguage))
            {
                startModelDownload(savedModelLanguage,modelDatabaseHelper);


            }

        }

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

    private void getUid() throws Exception {
        // Get the shared preferences
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);

        // Check if uuid and signature already exist in shared preferences
        String storedUuid = prefs.getString("uuid", null);
        String stored_server_public_key= prefs.getString("server_public_key", null);
        String stored_client_private_key= prefs.getString("client_private_key", null);
        String stored_client_public_key= prefs.getString("client_public_key", null);
        String stored_AES_key= prefs.getString("client_AES_key", null);


        if (storedUuid != null && stored_server_public_key != null &&stored_client_private_key!=null && stored_client_public_key!=null) {
            Log.v("encryption",storedUuid+" \n : "+stored_server_public_key+" \n"+stored_client_private_key+" \n"+stored_client_public_key+"\n"+stored_AES_key);
           // Log.v("encryption", decryptTextPrivateRSA("cNwvH3L51/ckk2Lz9wQufONXeyrrQT9i49mMsrntceaQ55qBy2fmsUnMWDqvC9yUJTIxZdpac0hwovRCLu2uIuaWSZcYmjkCVzGDJcze6fbskiVb/G6XiuK3H0gyIYN57kVRxWs7doBZETUBIjVMnaghDlmbgkQGNHewbGtkxqju3dKLYf9Dg/HkWexee2MiKHXXJpAAgIThn7uFHK3S8l9oqM0+jMmHbtv5s5NSEHREso12smDzrU2jg5+A7Km/WmUYPRWO3605NNfKa2Wh03DanYMtoVXVhOCWh967QdyFW98WLZj/qQwdsvPVhCWkbDHPLXjrawqXYH3jDNtAyg==",Show_Add_notes_Activity.this));

         //   decryptText_private_rsa(,Show_Add_notes_Activity.this);
            return; // Exit the method if values already exist
        }


        // Get unique device details and hash them using SHA-1
        String product = BuildUtils.getSha1Hex(Build.PRODUCT);
        String build_id = BuildUtils.getSha1Hex(Build.ID);
        String build_display = BuildUtils.getSha1Hex(Build.DISPLAY);
        String ip_address = BuildUtils.getSha1Hex(BuildUtils.getIPAddress(true));
        String epoch_time = BuildUtils.getSha1Hex(String.valueOf(System.currentTimeMillis()));

        // Server URL for registration
        String URL = "https://notetakers.vipresearch.ca/App_Script/Full_final_Register.php";
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



        KeyPairPEM rsaKeys = generateAndFormatRSAKeys();
        String client_public_RSA_Key = rsaKeys.getPublicKey();
        String client_private_RSA_Key = rsaKeys.getPrivateKey();

        // Create the JsonObjectRequest for sending data to the server
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Log.v("encryption",response.toString());
                            String uuid = response.getString("uuid");
                            String server_public_key = response.getString("server_public_key");
                            Log.v("encryption",uuid+" \n : "+server_public_key+" \n"+client_public_RSA_Key+" \n"+client_private_RSA_Key);
                            SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE).edit();
                            SecretKey aeskey=generateAESKey_local(server_public_key,client_public_RSA_Key,uuid);
                            String USER_AES_KEY= Base64.encodeToString(aeskey.getEncoded(), Base64.DEFAULT);

                           // String[] client_key_public = divideString(client_public_RSA_Key);
                            ////// send public key
                            JSONObject jsonPayload = new JSONObject();
                            editor.putString("uuid", uuid);
                            editor.putString("server_public_key", server_public_key);
                            editor.putString("client_public_key", String.valueOf(client_public_RSA_Key));
                            editor.putString("client_private_key", String.valueOf(client_private_RSA_Key));
                            editor.putString("client_AES_key", String.valueOf(USER_AES_KEY));
                            editor.apply();
                            try {
                                jsonPayload.put("uuid", uuid);
                                jsonPayload.put("client_public_key", client_public_RSA_Key);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            // Initialize Volley RequestQueue
                            RequestQueue requestQueue2 = Volley.newRequestQueue(Show_Add_notes_Activity.this);

                            // Create JSON Request
                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                                    Request.Method.POST,
                                    "https://notetakers.vipresearch.ca/App_Script/Unenc_send_client_public.php",
                                    jsonPayload,
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {

                                            try {
                                                // Convert encrypted AES key to Base64
                                                PublicKey pk_server=getPublicKeyFromString(server_public_key) ;

                                                String encryptedKeyBase64 =generateSHA256HashWithSalt(USER_AES_KEY,client_public_RSA_Key);

                                                Log.v("encryption","hash of AES key : "+response.toString());
                                                // Create JSON payload
                                                JSONObject payload = new JSONObject();
                                                payload.put("uuid", uuid);
                                                payload.put("encrypted_aes_key", encryptedKeyBase64);

                                                // Initialize Volley request queue
                                                RequestQueue requestQueue = Volley.newRequestQueue(Show_Add_notes_Activity.this);

                                                // Create POST request
                                                JsonObjectRequest jsonRequest = new JsonObjectRequest(
                                                        Request.Method.POST,
                                                        "https://notetakers.vipresearch.ca/App_Script/send_aes_key.php",
                                                        payload,
                                                        new Response.Listener<JSONObject>() {
                                                            @Override
                                                            public void onResponse(JSONObject response) {
                                                                editor.putString("uuid", uuid);
                                                                editor.putString("server_public_key", server_public_key);
                                                                editor.putString("client_public_key", String.valueOf(client_public_RSA_Key));
                                                                editor.putString("client_private_key", String.valueOf(client_private_RSA_Key));
                                                                editor.putString("client_AES_key", String.valueOf(USER_AES_KEY));
                                                                editor.apply();
                                                                Log.v("encryption","response send aes : "+response.toString());
                                                               // Toast.makeText(Show_Add_notes_Activity.this,"response : "+response.toString(),Toast.LENGTH_LONG).show();

                                                            }
                                                        },
                                                        new Response.ErrorListener() {
                                                            @Override
                                                            public void onErrorResponse(VolleyError error) {
                                                                //Toast.makeText(Show_Add_notes_Activity.this,"Sending Hash of AES key API not working "+ error.toString(), Toast.LENGTH_SHORT).show();

                                                                Log.v("encryption","response send aes : "+error.getMessage());
                                                               // Toast.makeText(Show_Add_notes_Activity.this,"error :"+error.getMessage(),Toast.LENGTH_LONG).show();
                                                            }
                                                        }
                                                );

                                                // Add request to queue
                                                requestQueue.add(jsonRequest);

                                            } catch (Exception e) {
                                                Log.v("encryption","response send aes : "+"error 2 :"+e.getMessage());
                                               // Toast.makeText(Show_Add_notes_Activity.this,"error 2 :"+e.getMessage(),Toast.LENGTH_LONG).show();

                                            }
                                            // save
                                            editor.putString("uuid", uuid);
                                            editor.putString("server_public_key", server_public_key);
                                            editor.putString("client_public_key", String.valueOf(client_public_RSA_Key));
                                            editor.putString("client_private_key", String.valueOf(client_private_RSA_Key));
                                            editor.putString("client_AES_key", String.valueOf(USER_AES_KEY));
                                            editor.apply();
                                            Log.v("encryption","response send public key"+response.toString());

                                            Log.v("encryption",uuid+" \n : "+server_public_key+" \n"+client_public_RSA_Key+" \n"+client_private_RSA_Key+"\n"+USER_AES_KEY);

                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            // Handle the error response
                                            Toast.makeText(Show_Add_notes_Activity.this,"Sending public RSA key API not working "+ error.toString(), Toast.LENGTH_SHORT).show();
                                            Log.e("encryption", "Error hello : " + error.toString());
                                        }
                                    }
                            );

                            // Add the request to the queue
                            requestQueue2.add(jsonObjectRequest);

                            //Toast.makeText(Show_Add_notes_Activity.this,"uuid and encryption information setup Complete ",Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            // Handle any errors that occur during response processing

                            //Toast.makeText(Show_Add_notes_Activity.this, "Error processing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            //    Toast.makeText(Show_Add_notes_Activity.this, "ERROR:"+error.getMessage(), Toast.LENGTH_SHORT).show();
                // Handle errors that occur during the network request
                Log.w("_DEBUG_ error", error.getCause());
                //Toast.makeText(Show_Add_notes_Activity.this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Add the request to the queue
        requestQueue.add(jsonObjectRequest);
    }
    public static String encodeBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.DEFAULT); // Works on API 24+
    }
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // Generate a 128-bit AES key
        return keyGen.generateKey();
    }

    public void setFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_item_container, fragment)
                .commit();
    }

    @Override
    protected void onPause() {


        super.onPause();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
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
    public void add_prompt(String Title,String Text)
    {


        if (!Title.isEmpty() && !Text.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy 'at' HH:mm");
            String date= sdf.format(new Date());
            Prompt_Database_Helper pdh=new Prompt_Database_Helper(Show_Add_notes_Activity.this);
            pdh.addPrompt(Title,Text,date);



        } else {
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
    private void startModelDownload(String selectedLanguage,Model_Database_Helper modelDatabaseHelper) {
        // Start downloading the model in the background
        new Thread(() -> {
            try {
                // Call the download method for the selected language
                ModelDownloader.downloadModelFast(Show_Add_notes_Activity.this, selectedLanguage,modelDatabaseHelper.getModelDownloadLinkByLanguage(selectedLanguage));

                // Once the model is downloaded, set up the model (assuming the model setup happens here)
                //setupModelForLanguage(selectedLanguage);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(Show_Add_notes_Activity.this, "Error downloading model"+e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void showProgressDialog() {
        // Create and show the progress dialog (you can also use a `ProgressBar` or a custom dialog)
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Downloading FFmpeg module...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }



}
