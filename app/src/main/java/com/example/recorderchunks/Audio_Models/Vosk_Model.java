package com.example.recorderchunks.Audio_Models;

import android.content.Context;
import android.widget.Toast;

import com.example.recorderchunks.Helpeerclasses.Model_Database_Helper;
import com.example.recorderchunks.utils.ZipUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Vosk_Model {

    public static Model voskModel;


    //Setup vosk Model -> removing from assets folder and unzipping
    public static void setupModel(Context context) {
        File modelDir = new File(context.getExternalFilesDir(null), "vosk-model");
        if (!modelDir.exists()) {

            modelDir.mkdirs();
            try (InputStream zipInputStream = context.getAssets().open("model.zip")) {
                ZipUtils.extractZip(zipInputStream, modelDir);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "error in extracting zip", Toast.LENGTH_SHORT).show();
            }

        }
    }

    //initialize vosk model -> ensure that model is already extracted in folder -> load vosk model
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void initializeVoskModel(Context context, String modelName, Runnable onComplete) {
        executorService.execute(() -> {
            try {
                File modelDir = new File(context.getExternalFilesDir(null), "models/" + modelName);
                voskModel = new Model(modelDir.getPath());

                if (onComplete != null) {
                    onComplete.run(); // Notify success
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Failed to load Vosk model: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Recognizes speech from a WAV file after ensuring the Vosk model is loaded.
     * This method waits for the model initialization before proceeding.
     */
    public static String recognizeSpeech(Context context, String wavFilePath, String language) {

        Model_Database_Helper modelDatabaseHelper = new Model_Database_Helper(context);
        String modelName = modelDatabaseHelper.getModelNameByLanguage(language);
        if(modelDatabaseHelper.checkModelDownloadedByLanguage(language))
        {
            CountDownLatch latch = new CountDownLatch(1);

            initializeVoskModel(context, modelName, latch::countDown);

            try {
                latch.await(); // Wait for model initialization

                if (voskModel == null) {
                    return "Model initialization failed.";
                }

                try (FileInputStream fis = new FileInputStream(wavFilePath)) {
                    Recognizer recognizer = new Recognizer(voskModel, 16000);
                    byte[] buffer = new byte[4000];
                    int bytesRead;

                    while ((bytesRead = fis.read(buffer)) != -1) {
                        recognizer.acceptWaveForm(buffer, bytesRead);
                    }

                    return extractTextFromResult(recognizer.getFinalResult());
                } catch (IOException e) {
                    e.printStackTrace();
                    return "Error during speech recognition.";
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return "Model initialization was interrupted.";
            }
        }
        else
        {
            return "Local Model for "+language+" is not available";
        }


    }

    /**
     * Extracts text from the JSON result of the Vosk recognizer.
     */
    public static String extractTextFromResult(String jsonResult) {
        try {
            JSONObject result = new JSONObject(jsonResult);
            return result.optString("text", ""); // Extract the "text" field
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }
}