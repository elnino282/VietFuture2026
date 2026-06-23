# AI Chatbox Integration

This frontend integrates the local `AI_CHATBOX` FastAPI service as a separate microservice for the existing farmer and buyer AI assistant surfaces. The Spring Boot backend stays on the existing `/api` proxy, while the AI service uses `/ai-api`.

## Required Ports

- Spring Boot backend: `http://localhost:8080`
- AI Chatbox FastAPI service: `http://localhost:8000`
- Vite frontend: `http://localhost:3000` by default

## Run AI Service

From the repository root:

```bash
cd AI_CHATBOX
pip install -r requirements.txt
python scripts/ingest_documents.py
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Run Frontend

From the repository root:

```bash
cd agricultural-crop-management-frontend
npm install
npm run dev
```

## Frontend Endpoint

The existing AI assistant drawers/pages call:

```txt
POST /ai-api/v1/ai/chat
```

Vite rewrites that request to:

```txt
http://localhost:8000/api/v1/ai/chat
```

Configure the proxy target with:

```env
VITE_AI_API_PROXY_TARGET=http://localhost:8000
```

## Manual API Test

```bash
curl -X POST http://localhost:8000/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"Tiêu chuẩn VietGAP về nguồn nước là gì?\",\"top_k\":4}"
```

## Manual Assistant Questions

- `VietGAP yêu cầu nguồn nước như thế nào?`
- `Sau khi tạo mùa vụ tôi cần làm gì tiếp theo?`
- `Làm sao để truy xuất nguồn gốc bằng mã QR?`

## Troubleshooting

- If an AI assistant shows an offline message, confirm `AI_CHATBOX` is running on port `8000`.
- If the frontend cannot reach the service, confirm `VITE_AI_API_PROXY_TARGET` points to `http://localhost:8000`.
- If answers have no sources, confirm documents were ingested with `python scripts/ingest_documents.py`.
- Keep Spring Boot calls on `/api`; do not point `/api` at the AI service.
