package com.example.recorderchunks.AI_Transcription;
import android.util.Log;
import android.media.MediaPlayer;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioChunkHelper {

    // Splits the audio file into chunks and returns the list of chunk file paths
    public static List<String> splitAudioIntoChunks(String filePath, int chunkSizeMs) {
        List<String> chunkPaths = new ArrayList<>();
        try {
            File audioFile = new File(filePath);
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepare();
            int duration = mediaPlayer.getDuration(); // Total duration in milliseconds

            // Create a folder for chunks based on the original filename
            String folderName = audioFile.getName().replaceAll("\\.[^.]+$", ""); // Remove extension
            File chunkFolder = new File(audioFile.getParent(), folderName);
            if (!chunkFolder.exists()) {
                chunkFolder.mkdirs(); // Create folder if it doesn't exist
            }


            // Generate chunks
            for (int startMs = 0; startMs < duration; startMs += chunkSizeMs) {
                int chunkNumber = startMs / chunkSizeMs + 1;
                String fileExtension = getFileExtension(audioFile);
                File chunkFile = new File(chunkFolder, "chunk" + chunkNumber + fileExtension);
                if(chunkFile.exists()) {
                    String command = String.format(
                            "-i \"%s\" -ss %.2f -t %.2f \"%s\"",
                            audioFile.getAbsolutePath(), // Input audio file path
                            startMs / 1000.0,            // Start time in seconds
                            chunkSizeMs / 1000.0,        // Chunk size in seconds
                            chunkFile.getAbsolutePath()  // Output chunk file path
                    );

                    int rc = FFmpeg.execute(command);


                    // String ffmpegLogs = FFmpeg.getLastCommandOutput();
                    if (rc == 0) {
                        chunkPaths.add(chunkFile.getAbsolutePath());
                        Log.e("chunk_path",chunkFile.getAbsolutePath());


                    }
                    else
                    {
                        chunkPaths.add("unable to chunkify");
                    }
                }
                else {
                    Log.e("chunk_path",chunkFile.getAbsolutePath());
                    chunkPaths.add(chunkFile.getAbsolutePath());
                }



            }
        } catch (Exception e) {
            Log.e("AudioChunkHelper", "Error splitting audio: " + e.getMessage());
        }
        return chunkPaths; // Return the list of chunk file paths
    }

    // Helper method to get file extension
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex != -1) {
            return fileName.substring(dotIndex); // Get the file extension
        }
        return ""; // No extension found
    }
}
