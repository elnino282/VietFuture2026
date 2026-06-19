# FarmTrace AI Chatbox Local

Local RAG AI Chatbox using FastAPI, LangChain, ChromaDB and Ollama.

## 1. Install Ollama models

```bash
ollama pull qwen3.5:2b
ollama pull nomic-embed-text
```

## 2. Create virtual environment

```bash
python -m venv .venv
.venv\Scripts\activate
```

On macOS/Linux:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

## 3. Install dependencies

```bash
pip install -r requirements.txt
```

## 4. Add documents

Put your files into:

```txt
data/vietgap/
data/farmtrace/
data/faq/
```

Supported formats: `.pdf`, `.txt`, `.md`.

## 5. Ingest documents

```bash
python scripts/ingest_documents.py
```

Or call API:

```bash
curl -X POST http://localhost:8000/api/v1/ai/ingest ^
  -H "Content-Type: application/json" ^
  -d "{\"reset\": true}"
```

## 6. Run API

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Open:

```txt
http://localhost:8000/docs
```

## 7. Chat API

```bash
curl -X POST http://localhost:8000/api/v1/ai/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"message\": \"Tiêu chuẩn VietGAP về nguồn nước là gì?\", \"top_k\": 4}"
```

Response:

```json
{
  "answer": "...",
  "sources": [
    {
      "file_name": "vietgap.pdf",
      "heading": "4.2 Nguồn nước",
      "page": 3,
      "snippet": "Nguồn nước tưới cần được kiểm soát..."
    }
  ]
}
```
