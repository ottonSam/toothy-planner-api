import base64
import os
import tempfile
from pathlib import Path

from fastapi import FastAPI, HTTPException
from faster_whisper import WhisperModel
from pydantic import BaseModel


SUPPORTED_EXTENSIONS = {
    "audio/webm": ".webm",
    "audio/ogg": ".ogg",
    "audio/wav": ".wav",
    "audio/mpeg": ".mp3",
    "audio/mp4": ".mp4",
}

app = FastAPI()
model = None


class TranscriptionRequest(BaseModel):
    audioBase64: str
    contentType: str


class TranscriptionResponse(BaseModel):
    text: str


def get_model():
    global model
    if model is None:
        model = WhisperModel(
            os.getenv("FASTER_WHISPER_MODEL", "small"),
            device=os.getenv("FASTER_WHISPER_DEVICE", "cpu"),
            compute_type=os.getenv("FASTER_WHISPER_COMPUTE_TYPE", "int8"),
        )
    return model


def content_type_extension(content_type: str) -> str:
    normalized = content_type.split(";", 1)[0].strip().lower()
    extension = SUPPORTED_EXTENSIONS.get(normalized)
    if extension is None:
        raise HTTPException(status_code=400, detail="Audio content type is not supported")
    return extension


@app.post("/transcribe", response_model=TranscriptionResponse)
def transcribe(request: TranscriptionRequest):
    extension = content_type_extension(request.contentType)
    try:
        audio_content = base64.b64decode(request.audioBase64, validate=True)
    except Exception as exception:
        raise HTTPException(status_code=400, detail="Audio content must be valid Base64") from exception

    temp_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=extension) as temp_file:
            temp_file.write(audio_content)
            temp_path = Path(temp_file.name)

        language = os.getenv("FASTER_WHISPER_LANGUAGE", "pt").strip() or None
        segments, _ = get_model().transcribe(str(temp_path), language=language, vad_filter=True)
        text = " ".join(segment.text.strip() for segment in segments if segment.text.strip()).strip()
        return TranscriptionResponse(text=text)
    finally:
        if temp_path is not None:
            temp_path.unlink(missing_ok=True)
