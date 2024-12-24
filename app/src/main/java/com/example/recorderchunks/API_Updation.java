package com.example.recorderchunks;

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
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import nl.bryanderidder.themedtogglebuttongroup.ThemedButton;
import nl.bryanderidder.themedtogglebuttongroup.ThemedToggleButtonGroup;

public class API_Updation extends AppCompatActivity {

    private EditText etChatGptApi, etGeminiApi;
    private Button btnSave, btnUpdate, manage_prompt;
    private ThemedToggleButtonGroup toggleGroup;

    private static final String PREF_NAME = "ApiKeysPref";
    public static final String KEY_CHATGPT = "ChatGptApiKey";
    public static final String KEY_GEMINI = "GeminiApiKey";
    public static final String KEY_SELECTED_API = "SelectedApi";

    private SharedPreferences sharedPreferences;
    private Spinner languageSpinner;
    Switch api_switch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_updation);
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);
        api_switch=findViewById(R.id.api_switch);

        languageSpinner = findViewById(R.id.language_spinner);
        String[] languages = {"English", "Spanish", "French", "German", "Chinese"};

        // Create an ArrayAdapter using the language list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );

        // Set the layout for dropdown items
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Attach the adapter to the Spinner
        languageSpinner.setAdapter(adapter);

        // Set up a listener for item selection
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected language
                String selectedLanguage = languages[position];
                Toast.makeText(API_Updation.this, "Selected Language: " + selectedLanguage, Toast.LENGTH_SHORT).show();


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no selection is made (optional)
            }
        });



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
        manage_prompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i=new Intent(API_Updation.this, Manage_Prompt.class);
                startActivity(i);
            }
        });

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Load saved data if available
        loadSavedData();

        // Save button functionality
        btnSave.setOnClickListener(v -> saveData());

        // Update button functionality
        btnUpdate.setOnClickListener(v -> updateData());

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

    private void updateData() {
        saveData();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
