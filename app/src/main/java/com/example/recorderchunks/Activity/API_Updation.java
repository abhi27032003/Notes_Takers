package com.example.recorderchunks.Activity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recorderchunks.Adapter.LanguageAdapter;
import com.example.recorderchunks.Audio_Models.ModelDownloader;
import com.example.recorderchunks.Audio_Models.ModelMetadata;
import com.example.recorderchunks.Background_Allow.Show_Add_notes_Activity;
import com.example.recorderchunks.Helpeerclasses.LocaleHelper;
import com.example.recorderchunks.Helpeerclasses.Model_Database_Helper;
import com.example.recorderchunks.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class API_Updation extends AppCompatActivity {

    private EditText etChatGptApi, etGeminiApi;
    private Button btnSave, btnUpdate, manage_prompt;


    private static final String PREF_NAME = "ApiKeysPref";
    public static final String KEY_CHATGPT = "ChatGptApiKey";
    public static final String KEY_GEMINI = "GeminiApiKey";
    public static final String SELECTED_LANGUAGE = "SelectedLanguage";
    public static final String SELECTED_APP_LANGUAGE = "SelectedappLanguage";
    TextView Gemini_t,Chatgpt_t,Local_t,Server_t,selected_api;
    private RecyclerView recyclerView;
    private LanguageAdapter adapter;

    public static final String KEY_SELECTED_API = "SelectedApi";
    public static final String SELECTED_TRANSCRIPTION_METHOD = "SelectedTranscriptionMethod";

    private SharedPreferences sharedPreferences;
    private Spinner languageSpinner,applanguagespinner;
    Switch api_switch,model_switch;

    TextView uuid_text,signature_text,currently_downloading_model,no_models_downloaded;
    ImageView uuid_image, signature_image;
    Model_Database_Helper modelDatabaseHelper;

    int i=0;

    private Handler handler ;
    private Runnable updateTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_updation);
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);


        // Recycler view setup for downloaded models
        handler = new Handler();
        no_models_downloaded=findViewById(R.id.no_models_downloaded);
        currently_downloading_model=findViewById(R.id.currently_downloading_model);
        no_models_downloaded.setVisibility(View.GONE);
        recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        Model_Database_Helper dbHelper = new Model_Database_Helper(this);

        updateTask = new Runnable() {
            @Override
            public void run() {
                List<String> downloadedLanguages = dbHelper.getDownloadedLanguages();
                adapter = new LanguageAdapter(downloadedLanguages);
                recyclerView.setAdapter(adapter);

                if (downloadedLanguages.size() <= 0) {
                    no_models_downloaded.setVisibility(View.VISIBLE);
                } else {
                    no_models_downloaded.setVisibility(View.GONE);
                }

                String downloadingModels = ModelDownloader.getCurrentlyDownloadingModelsAsString();
                if (!downloadingModels.isEmpty()) {
                    currently_downloading_model.setText("Currently downloading models: " + downloadingModels);
                } else {
                    currently_downloading_model.setText("No models are currently being downloaded.");
                }

                // Schedule the next execution after 5 seconds
                handler.postDelayed(this, 1000);
            }
        };

        ///////////////////////////////
        api_switch=findViewById(R.id.api_switch);
        Chatgpt_t=findViewById(R.id.Chat);
        Gemini_t=findViewById(R.id.Gem);
        Local_t=findViewById(R.id.Loc);
        Server_t=findViewById(R.id.Ser);
        selected_api=findViewById(R.id.selected_api);


        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        modelDatabaseHelper=new Model_Database_Helper(API_Updation.this);

        languageSpinner = findViewById(R.id.language_spinner);
        applanguagespinner=findViewById(R.id.app_language_spinner);
        String[] languages = {
                "English",
                "French",
                "Chinese",
                "Hindi",
                "Spanish"
        };
        model_switch=findViewById(R.id.transcription_switch);
        // Create an ArrayAdapter using the language list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );
        String savedLanguage = sharedPreferences.getString(SELECTED_LANGUAGE, "English");  // Default value is null
        String savedAppLanguage = sharedPreferences.getString(SELECTED_APP_LANGUAGE, "English");
        String savedTranscriptionMethod = sharedPreferences.getString(SELECTED_TRANSCRIPTION_METHOD, "Local");


        // Set the layout for dropdown items
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Attach the adapter to the Spinner
        languageSpinner.setAdapter(adapter);
        applanguagespinner.setAdapter(adapter);


        // Set up a listener for item selection
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected language
                if(i!=0)
                {
                    String selectedLanguage = languages[position];
                    if(false)
                    {
                        new AlertDialog.Builder(API_Updation.this)
                                .setTitle("Change Language")
                                .setMessage("A Language is currently downloading. If you change the language, the download will stop. Do you want to continue?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                   // ModelDownloader.cancelDownload(selectedLanguage);
                                    if(modelDatabaseHelper.checkModelDownloadedByLanguage(selectedLanguage))
                                    {
                                       // Toast.makeText(API_Updation.this, "Model Downloaded", Toast.LENGTH_SHORT).show();
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString(SELECTED_LANGUAGE, selectedLanguage);
                                        editor.apply();

                                    }
                                    else
                                    {
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString(SELECTED_LANGUAGE, selectedLanguage);
                                        editor.apply();
                                        startModelDownload(selectedLanguage);

                                    }

                                })
                                .setNegativeButton("No", (dialog, which) ->
                                        {
                                            String sl = sharedPreferences.getString(SELECTED_LANGUAGE, "English");  // Default value is null
                                            if (sl != null) {
                                                for (int i = 0; i < languages.length; i++) {
                                                    if (languages[i].equals(savedLanguage)) {
                                                        languageSpinner.setSelection(i);
                                                        break;
                                                    }
                                                }
                                            }
                                            i=0;
                                            dialog.dismiss();
                                        }

                                )


                                .show();
                      //  Toast.makeText(API_Updation.this, "One model is already getting downloaded", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        if(modelDatabaseHelper.checkModelDownloadedByLanguage(selectedLanguage))
                        {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(SELECTED_LANGUAGE, selectedLanguage);
                            editor.apply();
                          //  Toast.makeText(API_Updation.this, "Model Downloaded", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(SELECTED_LANGUAGE, selectedLanguage);
                            editor.apply();
                            startModelDownload(selectedLanguage);
                        }
                    }
                }
                else
                {
                    i=1;
                }




            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no selection is made (optional)
            }
        });
        applanguagespinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected app language
                String selectedAppLanguage = languages[position];
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(SELECTED_APP_LANGUAGE, selectedAppLanguage);
                editor.apply();
                String localeCode = getLocaleCode(selectedAppLanguage);
                LocaleHelper.setLocale(API_Updation.this, localeCode);

                // Handle app language selection (you can add functionality here)
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no selection is made (optional)
            }
        });

        if (savedLanguage != null) {
            // Find the index of the saved language in the languages array
            for (int i = 0; i < languages.length; i++) {
                if (languages[i].equals(savedLanguage)) {
                    // Set the spinner to the saved language
                    languageSpinner.setSelection(i);
                    break;
                }
            }
        }

        if (savedAppLanguage != null) {
            for (int i = 0; i < languages.length; i++) {
                if (languages[i].equals(savedAppLanguage)) {
                    applanguagespinner.setSelection(i);
                    break;
                }
            }
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Initialize UI components
        etChatGptApi = findViewById(R.id.et_chatgpt_api);
        etGeminiApi = findViewById(R.id.et_gemini_api);
        btnSave = findViewById(R.id.btn_save);
        btnUpdate = findViewById(R.id.btn_update);
        manage_prompt=findViewById(R.id.manage_prompt);
        uuid_image=findViewById(R.id.uuid_copy);
        signature_image=findViewById(R.id.signature_copy);

        uuid_text=findViewById(R.id.uuid_text);
        signature_text=findViewById(R.id.signature_text);
        manage_prompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();

                Intent i=new Intent(API_Updation.this, Manage_Prompt.class);
                startActivity(i);
            }
        });

        //accordian feature for ui
        LinearLayout accordionContent = findViewById(R.id.accordion_content);
        TextView accordionToggle = findViewById(R.id.accordion_toggle);
        ImageView expandIcon = findViewById(R.id.expand_icon);
        expandIcon.setOnClickListener(v -> {
            if (accordionContent.getVisibility() == View.GONE) {
                accordionContent.setVisibility(View.VISIBLE);
                accordionToggle.setText(getString(R.string.hide_user_details));
                expandIcon.setImageResource(R.mipmap.collapse); // Update icon for collapse
            } else {
                accordionContent.setVisibility(View.GONE);
                accordionToggle.setText(getString(R.string.show_user_details));
                expandIcon.setImageResource(R.mipmap.expand); // Update icon for expand
            }
        });
        accordionToggle.setOnClickListener(v -> {
            if (accordionContent.getVisibility() == View.GONE) {
                accordionContent.setVisibility(View.VISIBLE);
                accordionToggle.setText(getString(R.string.hide_user_details));
                expandIcon.setImageResource(R.mipmap.collapse); // Update icon for collapse
            } else {
                accordionContent.setVisibility(View.GONE);
                accordionToggle.setText(getString(R.string.show_user_details));
                expandIcon.setImageResource(R.mipmap.expand); // Update icon for expand
            }
        });

        //accordian feature for api keys
        LinearLayout accordionContent_api = findViewById(R.id.accordion_content_api);
        TextView accordionToggle_api = findViewById(R.id.accordion_toggle_api);
        ImageView expandIcon_api = findViewById(R.id.expand_icon_api);
        expandIcon_api.setOnClickListener(v -> {
            if (accordionContent_api.getVisibility() == View.GONE) {
                accordionContent_api.setVisibility(View.VISIBLE);
                accordionToggle_api.setText(getString(R.string.hide_api_details));
                expandIcon_api.setImageResource(R.mipmap.collapse); // Update icon for collapse
            } else {
                accordionContent_api.setVisibility(View.GONE);
                accordionToggle_api.setText(getString(R.string.show_api_details));
                expandIcon_api.setImageResource(R.mipmap.expand); // Update icon for expand
            }
        });
        accordionToggle_api.setOnClickListener(v -> {

            if (accordionContent_api.getVisibility() == View.GONE) {
                accordionContent_api.setVisibility(View.VISIBLE);
                accordionToggle_api.setText(getString(R.string.hide_api_details));
                expandIcon_api.setImageResource(R.mipmap.collapse); // Update icon for collapse
            } else {
                accordionContent_api.setVisibility(View.GONE);
                accordionToggle_api.setText(getString(R.string.show_api_details));
                expandIcon_api.setImageResource(R.mipmap.expand); // Update icon for expand
            }
        });




        // Load saved data if available
        loadSavedData();
        displayUuidAndSignature();

        // Save button functionality
        btnSave.setOnClickListener(v -> saveData());

        // Update button functionality
        btnUpdate.setOnClickListener(v -> updateData());

        // UUID copy
        uuid_image.setOnClickListener(v -> copyTextToClipboard("UUID",uuid_text.getText().toString()));

        // Signature copy
        signature_image.setOnClickListener(v -> copyTextToClipboard("Signature",signature_text.getText().toString()));
        // Handle toggle button selection

        api_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String selectedApi;

            // Use a switch statement to handle the state
            if (isChecked) {
                selectedApi = "use ChatGpt"; // API when the switch is ON
                chatgptselected();
            } else {
                selectedApi = "use Gemini Ai"; // API when the switch is OFF
                geminiselected();
            }

            // Save the selected API to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SELECTED_API, selectedApi);
            editor.apply();

            // Optional: Show a Toast message
            Toast.makeText(this, "Selected API: " + selectedApi, Toast.LENGTH_SHORT).show();
        });
        model_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String selectedmodel="";

            // Use a switch statement to handle the state
            if (isChecked) {
                selectedmodel = "Server"; // API when the switch is ON
                Serversideselected();
            } else {
                selectedmodel = "Local"; // API when the switch is OFF
                LocalSideSelected();
            }

            // Save the selected API to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(SELECTED_TRANSCRIPTION_METHOD, selectedmodel);
            editor.apply();

            // Optional: Show a Toast message
            Toast.makeText(this, "Selected Model: " + selectedmodel, Toast.LENGTH_SHORT).show();
        });

    }
    private void startModelDownload(String selectedLanguage) {
        // Start downloading the model in the background
        new Thread(() -> {
            try {
                // Call the download method for the selected language
                ModelDownloader.downloadModelFast(API_Updation.this, selectedLanguage,modelDatabaseHelper.getModelDownloadLinkByLanguage(selectedLanguage));

                // Once the model is downloaded, set up the model (assuming the model setup happens here)
                //setupModelForLanguage(selectedLanguage);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(API_Updation.this, "Error downloading model"+e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    private void copyTextToClipboard(String label,String textToCopy) {
        // Get the system ClipboardManager
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // Create a ClipData object containing the text to copy
        ClipData clip = ClipData.newPlainText(label, textToCopy);

        // Set the ClipData to the clipboard
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            // Optionally, show a toast message to notify the user
            Toast.makeText(this, label+" copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayUuidAndSignature() {
        // Get the shared preferences
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);

        // Get stored uuid and signature
        String storedUuid = prefs.getString("uuid", null);
        String storedSignature = prefs.getString("signature", null);


        // Check if uuid and signature are present and set the text accordingly
        if (storedUuid != null && storedSignature != null) {
            // If both uuid and signature are found, display them
            uuid_text.setText(storedUuid);
            signature_text.setText(storedSignature);
            uuid_image.setVisibility(View.VISIBLE);
            signature_image.setVisibility(View.VISIBLE);
        } else {
            // If not found, display "Not available"
            uuid_text.setText("Not available");
            signature_text.setText("Not available");
            uuid_image.setVisibility(View.GONE);
            signature_image.setVisibility(View.GONE);
        }
    }

    private void loadSavedData() {
        String chatGptApi = sharedPreferences.getString(KEY_CHATGPT, "");
        String geminiApi = sharedPreferences.getString(KEY_GEMINI, "");
        String selectedApi = sharedPreferences.getString(KEY_SELECTED_API, "use Gemini Ai"); // Default to "Use ChatGPT"
        String selectedModel = sharedPreferences.getString(SELECTED_TRANSCRIPTION_METHOD, "Local"); // Default to "Use ChatGPT"

        if (!TextUtils.isEmpty(chatGptApi)) etChatGptApi.setText(chatGptApi);
        if (!TextUtils.isEmpty(geminiApi)) etGeminiApi.setText(geminiApi);

        // Set the toggle button state based on the saved API selection
        if ("use ChatGpt".equalsIgnoreCase(selectedApi)) {
           api_switch.setChecked(true);
           chatgptselected();

        } else if ("use Gemini Ai".equalsIgnoreCase(selectedApi)) {
            api_switch.setChecked(false); // Assuming btn1 is for ChatGPT
            geminiselected();
        }
        else
        {
            api_switch.setChecked(false); // Assuming btn1 is for ChatGPT
            geminiselected();

        }
        if ("Server".equalsIgnoreCase(selectedModel)) {
            model_switch.setChecked(true); // Assuming btn1 is for ChatGPT
            Serversideselected();
        } else if ("Local".equalsIgnoreCase(selectedModel)) {
            model_switch.setChecked(false); // Assuming btn1 is for ChatGPT
            LocalSideSelected();
        }
        else
        {
            model_switch.setChecked(false); // Assuming btn1 is for ChatGPT

        }


        // Toast.makeText(this, "Loaded saved data", Toast.LENGTH_SHORT).show();
    }
    private void chatgptselected()
    {
        Chatgpt_t.setTypeface(null, Typeface.BOLD);
        Gemini_t.setTypeface(null, Typeface.NORMAL);
        selected_api.setText("ChatGpt");

    }
    private void geminiselected()
    {
        Chatgpt_t.setTypeface(null, Typeface.NORMAL);
        Gemini_t.setTypeface(null, Typeface.BOLD);
        selected_api.setText("Gemini");

    }
    private void Serversideselected()
    {
        Local_t.setTypeface(null, Typeface.NORMAL);
        Server_t.setTypeface(null, Typeface.BOLD);
    }
    private  void LocalSideSelected()
    {
        Server_t.setTypeface(null, Typeface.NORMAL);
        Local_t.setTypeface(null, Typeface.BOLD);
    }

    private void saveData() {
        String chatGptApi = etChatGptApi.getText().toString().trim();
        String geminiApi = etGeminiApi.getText().toString().trim();
       // String selected_api= etPrompt.getText().toString().trim();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CHATGPT, chatGptApi);
        editor.putString(KEY_GEMINI, geminiApi);
        editor.apply();

        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
    }
    public static String[] getLanguagesFromMetadata(Context context) {
        try {
            // Fetch all models from metadata
            JSONArray models = ModelMetadata.getModelMetadata();
            List<String> languageList = new ArrayList<>();

            // Iterate through models and extract unique languages
            for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.getJSONObject(i);
                String language = model.getString("language");

                // Add language to list if not already added
                if (!languageList.contains(language)) {
                    languageList.add(language);
                }
            }

            // Convert List to String[] and return
            return languageList.toArray(new String[0]);

        } catch (Exception e) {
            e.printStackTrace();
            return new String[0]; // Return empty array in case of error
        }
    }

    private void updateData() {
        saveData();
    }

    @Override
    public void onBackPressed() {
        saveData();
        Intent i=new Intent(API_Updation.this,Show_Add_notes_Activity.class);
        startActivity(i);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveData();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        super.onResume();
        // Start the periodic update when the activity/fragment is resumed
        handler.post(updateTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop updates when the activity/fragment is paused to avoid memory leaks
        handler.removeCallbacks(updateTask);
    }
}
