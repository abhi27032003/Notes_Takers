package com.example.recorderchunks.Model_Class;

public class Chunk_Response {
    private String chunkId;
    private String status;
    private String transcription;

    private String chunk_name;


    public Chunk_Response(String chunkId, String status, String transcription, String  chunk_name) {
            this.chunkId = chunkId;
            this.status = status;
            this.transcription = transcription;
            this.chunk_name=chunk_name;
        }

    public String getChunk_name() {
        return chunk_name;
    }

    public void setChunk_name(String chunk_name) {
        this.chunk_name = chunk_name;
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
