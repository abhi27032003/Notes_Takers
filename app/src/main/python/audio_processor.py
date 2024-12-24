import whisper
import os

def transcribe_audio(audio_file_path, model_path):
    # Load the model from the .pt file located at the provided model path
    model = whisper.load_model(model_path)

    # Transcribe the audio file
    result = model.transcribe(audio_file_path)

    return result["text"]
