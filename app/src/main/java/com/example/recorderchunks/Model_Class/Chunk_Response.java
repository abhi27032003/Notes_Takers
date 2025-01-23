package com.example.recorderchunks.Model_Class;

public class Chunk_Response {
    private String chunkId;
    private String status;
    private String transcription;


    public Chunk_Response(String chunkId, String status, String transcription) {
            this.chunkId = chunkId;
            this.status = status;
            this.transcription = transcription;
        }

        // Getters
        public String getChunkId() {
            return chunkId;
        }

        public String getStatus() {
            return status;
        }

        public String getTranscription() {
            return transcription;
        }

        // Setters
        public void setChunkId(String chunkId) {
            this.chunkId = chunkId;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setTranscription(String transcription) {
            this.transcription = transcription;
        }

        @Override
        public String toString() {
            return "ChunkResponse{" +
                    "chunkId='" + chunkId + '\'' +
                    ", status='" + status + '\'' +
                    ", transcription='" + transcription + '\'' +
                    '}';
        }


}
