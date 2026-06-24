from pathlib import Path
from dotenv import load_dotenv
import os


ROOT_DIR = Path(__file__).resolve().parents[1]
load_dotenv(ROOT_DIR / ".env")


def _resolve_path(value: str) -> Path:
    path = Path(value)
    if path.is_absolute():
        return path
    return ROOT_DIR / path


def _optional_float(value: str | None) -> float | None:
    if value is None or value.strip() == "":
        return None
    return float(value)


class Settings:
    ROOT_DIR: Path = ROOT_DIR

    APP_NAME: str = os.getenv("APP_NAME", "ACM AI Chatbox")
    API_PREFIX: str = os.getenv("API_PREFIX", "/api/v1/ai")

    OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    OLLAMA_MODEL: str = os.getenv("OLLAMA_MODEL", "qwen3.5:2b")
    EMBEDDING_MODEL: str = os.getenv("EMBEDDING_MODEL", "nomic-embed-text")

    OLLAMA_THINK: bool = os.getenv("OLLAMA_THINK", "false").lower() in ("1", "true", "yes")

    CHROMA_DIR: Path = _resolve_path(os.getenv("CHROMA_DIR", "./chroma_db"))
    DATA_DIR: Path = _resolve_path(os.getenv("DATA_DIR", "./data"))
    COLLECTION_NAME: str = os.getenv("COLLECTION_NAME", "acm_knowledge")

    CHUNK_SIZE: int = int(os.getenv("CHUNK_SIZE", "650"))
    CHUNK_OVERLAP: int = int(os.getenv("CHUNK_OVERLAP", "100"))
    DEFAULT_TOP_K: int = int(os.getenv("DEFAULT_TOP_K", "4"))
    MAX_CONTEXT_CHARS: int = int(os.getenv("MAX_CONTEXT_CHARS", "2000"))
    MAX_DISTANCE_THRESHOLD: float | None = _optional_float(os.getenv("MAX_DISTANCE_THRESHOLD", "0.42"))
    MIN_RETRIEVAL_SCORE: float | None = _optional_float(os.getenv("MIN_RETRIEVAL_SCORE", ""))
    DEBUG_RAG: bool = os.getenv("DEBUG_RAG", "false").lower() in ("1", "true", "yes")

    NUM_PREDICT: int = int(os.getenv("NUM_PREDICT", "384"))
    NUM_CTX: int = int(os.getenv("NUM_CTX", "4096"))

    TEMPERATURE: float = float(os.getenv("TEMPERATURE", "0.05"))
    TOP_K: int = int(os.getenv("TOP_K", "10"))
    TOP_P: float = float(os.getenv("TOP_P", "0.7"))
    REPEAT_PENALTY: float = float(os.getenv("REPEAT_PENALTY", "1.1"))

    KEEP_ALIVE: str = os.getenv("KEEP_ALIVE", "30m")


settings = Settings()
settings.CHROMA_DIR.mkdir(parents=True, exist_ok=True)
settings.DATA_DIR.mkdir(parents=True, exist_ok=True)
