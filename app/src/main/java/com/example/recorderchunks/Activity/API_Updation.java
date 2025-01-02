package com.example.recorderchunks.Activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.example.recorderchunks.Audio_Models.ModelMetadata;
import com.example.recorderchunks.Background_Allow.Show_Add_notes_Activity;
import com.example.recorderchunks.Helpeerclasses.LocaleHelper;
import com.example.recorderchunks.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import nl.bryanderidder.themedtogglebuttongroup.ThemedToggleButtonGroup;

public class API_Updation extends AppCompatActivity {

    private EditText etChatGptApi, etGeminiApi;
    private Button btnSave, btnUpdate, manage_prompt;
    private ThemedToggleButtonGroup toggleGroup;

    private static final String PREF_NAME = "ApiKeysPref";
    public static final String KEY_CHATGPT = "ChatGptApiKey";
    public static final String KEY_GEMINI = "GeminiApiKey";
    public static final String SELECTED_LANGUAGE = "SelectedLanguage";
    public static final String SELECTED_APP_LANGUAGE = "SelectedappLanguage";


    public static final String KEY_SELECTED_API = "SelectedApi";

    private SharedPreferences sharedPreferences;
    private Spinner languageSpinner,applanguagespinner;
    Switch api_switch;

    TextView uuid_text,signature_text;
    ImageView uuid_image, signature_image;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_updation);
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);
        api_switch=findViewById(R.id.api_switch);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        languageSpinner = findViewById(R.id.language_spinner);
        applanguagespinner=findViewById(R.id.app_language_spinner);
        String[] languages = {
                "English",
                "French",
                "Chinese",
                "Hindi",
                "Spanish"
        };

        // Create an ArrayAdapter using the language list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );
        String savedLanguage = sharedPreferences.getString(SELECTED_LANGUAGE, "English");  // Default value is null
        String savedAppLanguage = sharedPreferences.getString(SELECTED_APP_LANGUAGE, "English");


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
                String selectedLanguage = languages[position];
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(SELECTED_LANGUAGE, selectedLanguage);
                editor.apply();
                startModelDownload(selectedLanguage);


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
                if(Show_Add_notes_Activity.reload<=0)
                {
                    recreate();
                    Show_Add_notes_Activity.reload=10;
                }


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
        Show_Add_notes_Activity.reload=10;

        // Enable back button in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Initialize UI components
        etChatGptApi = findViewById(R.id.et_chatgpt_api);
        etGeminiApi = findViewById(R.id.et_gemini_api);
        btnSave = findViewById(R.id.btn_save);
        btnUpdate = findViewById(R.id.btn_update);
        toggleGroup = findViewById(R.id.time);
        manage_prompt=findViewById(R.id.manage_prompt);
        uuid_image=findViewById(R.id.uuid_copy);
        signature_image=findViewById(R.id.signature_copy);

        uuid_text=findViewById(R.id.uuid_text);
        signature_text=findViewById(R.id.signature_text);
        manage_prompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
            } else {
                selectedApi = "use Gemini Ai"; // API when the switch is OFF
            }

            // Save the selected API to SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SELECTED_API, selectedApi);
            editor.apply();

            // Optional: Show a Toast message
            Toast.makeText(this, "Selected API: " + selectedApi, Toast.LENGTH_SHORT).show();
        });
    }
    private void startModelDownload(String selectedLanguage) {
        // Start downloading the model in the background
        new Thread(() -> {
            try {
                // Call the download method for the selected language
                //ModelDownloader.downloadModel(API_Updation.this, selectedLanguage);

                // Once the model is downloaded, set up the model (assuming the model setup happens here)
                //setupModelForLanguage(selectedLanguage);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(API_Updation.this, "Error downloading model", Toast.LENGTH_SHORT).show());
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
        String selectedApi = sharedPreferences.getString(KEY_SELECTED_API, "Use ChatGPT"); // Default to "Use ChatGPT"

        if (!TextUtils.isEmpty(chatGptApi)) etChatGptApi.setText(chatGptApi);
        if (!TextUtils.isEmpty(geminiApi)) etGeminiApi.setText(geminiApi);

        // Set the toggle button state based on the saved API selection
        if ("use ChatGpt".equalsIgnoreCase(selectedApi)) {
           api_switch.setChecked(true); // Assuming btn1 is for ChatGPT
        } else if ("use Gemini Ai".equalsIgnoreCase(selectedApi)) {
            api_switch.setChecked(false); // Assuming btn1 is for ChatGPT
        }
        else
        {
            api_switch.setChecked(false); // Assuming btn1 is for ChatGPT

        }

       // Toast.makeText(this, "Loaded saved data", Toast.LENGTH_SHORT).show();
    }

    private void saveData() {
        String chatGptApi = etChatGptApi.getText().toString().trim();
        String geminiApi = etGeminiApi.getText().toString().trim();
       // String selected_api= etPrompt.getText().toString().trim();

        if (TextUtils.isEmpty(chatGptApi) || TextUtils.isEmpty(geminiApi) ) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

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
        Intent i=new Intent(API_Updation.this,Show_Add_notes_Activity.class);
        startActivity(i);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
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
}
