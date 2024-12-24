package com.example.recorderchunks.Audio_Models;

import android.content.Context;
import android.widget.Toast;

import com.example.recorderchunks.utils.ZipUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
    public static void initializeVoskModel(Context context) {
        try {
            setupModel(context);
            try {

                File modelDir = new File(context.getExternalFilesDir(null), "vosk-model/vosk-model-small-en-in-0.4");
                voskModel = new Model(modelDir.getPath());
            }
            catch (Exception e)
            {
                Toast.makeText(context,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Vosk model failed to load", Toast.LENGTH_SHORT).show();
        }
    }

    //recognize speech from wav file
    public static String recognizeSpeech(String wavFilePath) {

        try (FileInputStream fis = new FileInputStream(wavFilePath)) {
            Recognizer recognizer = new Recognizer(voskModel, 16000);
            byte[] buffer = new byte[4000]; // Try doubling the size
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {

                } else {
                }
            }
            String transcription=extractTextFromResult(recognizer.getFinalResult());




            return transcription;

        } catch (IOException e) {
            e.printStackTrace();

        }
        return "Unable to save Transcription";
    }

    //extract text from vosk transcription json
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
