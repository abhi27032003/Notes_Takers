package com.example.recorderchunks.utils;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class AudioUtils {

    public static String getAudioDuration(String audioPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(audioPath); // Pass the resolved file path
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();

            if (duration != null) {
                long durationMs = Long.parseLong(duration);
                return String.valueOf(durationMs / 1000); // Convert to seconds
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "0"; // Default if duration is unavailable
    }
    public static String getFileExtension(String filePath) {
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }
    public static String getFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }
    public static String processAudioFiletoflac(String wavFilePath , Context context) {
        // Define the FLAC file path
        File wavFile = new File(wavFilePath);
        if (!wavFile.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show();
            return null;
        }
        String flacFilePath = replaceWavWithFlac(wavFilePath);

        // Convert WAV to FLAC using FFmpeg
        String command = "-i " + wavFilePath + " -f flac -y " + flacFilePath;
        int resultCode = FFmpeg.execute(command);

        if (resultCode == 0) {

            Toast.makeText(context, "File successfully converted to FLAC: ", Toast.LENGTH_SHORT).show();
            return flacFilePath; // Return the FLAC file path
        } else {

            Toast.makeText(context, "FFmpeg conversion failed"+resultCode, Toast.LENGTH_LONG).show();
            return null; // Indicate failure
        }
    }
    public static String replaceWavWithFlac(String filePath) {
        if (filePath == null || !filePath.endsWith(".wav")) {
            throw new IllegalArgumentException("Invalid .wav file path");
        }
        return filePath.substring(0, filePath.lastIndexOf(".")) + ".flac";
    }

    public static String convertToWavFilePath(String audioFilePath) {
        return audioFilePath.substring(0, audioFilePath.lastIndexOf('.')) + ".wav";
    }
    public static  boolean isWavFormatValid(String inputPath ,Context context) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(inputPath);
            MediaFormat format = extractor.getTrackFormat(0);

            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            // Desired parameters: sample rate = 16000 Hz, channel count = 1
            return sampleRate == 16000 && channelCount == 1;
        } catch (IOException e) {
            Toast.makeText(context, "Error reading file format", Toast.LENGTH_SHORT).show();
            return false;
        } finally {
            extractor.release();
        }
    }

    public static String performConversion(String inputPath, String outputPath,Context context) {
        File outputFile = new File(outputPath);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        // Construct the FFmpeg command for conversion
        String command = String.format(
                "-i \"%s\" -ar 16000 -ac 1 -c:a pcm_s16le -y \"%s\"",
                inputPath, outputPath
        );

        int rc = FFmpeg.execute(command);
        if (rc == 0) {
            //Toast.makeText(context, "Conversion to WAV successful ", Toast.LENGTH_SHORT).show();
            return outputPath;
        } else {
            //Toast.makeText(context, "Conversion failed: " + rc, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public static String adjustSampleRate(String inputPath) {
        // Generate a new output file name with '_adjusted_sample_rate' appended
        File inputFile = new File(inputPath);
        String inputFileName = inputFile.getName();
        String parentDirectory = inputFile.getParent();
        String newFileName = inputFileName.replace(".wav", "_adjusted_sample_rate.wav");
        String outputPath = new File(parentDirectory, newFileName).getAbsolutePath();

        // Construct the FFmpeg command to adjust the sample rate
        String command = String.format(
                "-i \"%s\" -ar 16000 -ac 1 -c:a pcm_s16le -y \"%s\"",
                inputPath, outputPath
        );

        int rc = FFmpeg.execute(command);


        // String ffmpegLogs = FFmpeg.getLastCommandOutput();
        if (rc == 0) {

            return outputPath;
        } else {

            return null;
        }
    }

    public static String convertToWav(String inputPath,Context context) {
//        recording_animation_card.setVisibility(View.GONE);
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {

            return null;
        }

        // Validate the input file format
        String fileExtension = getFileExtension(inputPath).toLowerCase();
        String[] supportedFormats = {"wav", "3gp"};
        if (!Arrays.asList(supportedFormats).contains(fileExtension)) {

            return "wrong format";
        }

        // Check if input WAV file needs conversion
        if (fileExtension.equals("wav")) {
            if (isWavFormatValid(inputPath,context)) {
                return inputPath; // No conversion needed
            } else {
                // Adjust the sample rate of the WAV file
                return adjustSampleRate(inputPath);
            }
        }

        // For non-WAV files, convert them to WAV
        String outputPath = AudioUtils.convertToWavFilePath(inputPath);
        return performConversion(inputPath, outputPath,context);
    }






}
