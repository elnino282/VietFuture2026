# 1. Project Overview

## Mục tiêu dự án

Project `AI_CHATBOX` là backend RAG local cho chatbot "FarmTrace AI Chatbox"/ACM. Mục tiêu là dùng tài liệu nội bộ trong thư mục `data/` để trả lời các câu hỏi về:

* Hệ thống ACM (Agricultural Crop Management).
* VietGAP.
* Truy xuất nguồn gốc nông sản bằng QR.
* Quy trình mùa vụ, nhật ký sản xuất, thu hoạch, nhập kho, đăng bán.
* FAQ cho nông dân, người mua, admin.

Theo `README.md`, project là: "Local RAG AI Chatbox using FastAPI, LangChain, ChromaDB and Ollama."

## Chức năng chatbot

* API chat nhận câu hỏi người dùng, retrieve các chunk từ Chroma, build prompt và gọi Ollama để sinh câu trả lời.
* Trả lời bằng tiếng Việt, ưu tiên ngắn gọn, dựa trên dữ liệu retrieve.
* Nếu không đủ dữ liệu, trả về: `Tôi chưa có đủ dữ liệu trong tài liệu hiện tại.`
* Trả về danh sách nguồn riêng gồm `file_name`, `heading`, `page`, `snippet`.
* API ingest nạp dữ liệu từ thư mục `data/` vào Chroma.

## Công nghệ sử dụng

Từ `requirements.txt`:

* `fastapi`, `uvicorn[standard]`
* `python-dotenv`
* `langchain`, `langchain-community`
* `langchain-ollama`
* `langchain-chroma`
* `langchain-text-splitters`
* `chromadb`
* `pypdf`
* `requests`

Runtime/model:

* LLM: Ollama `qwen3.5:2b`
* Embedding: Ollama `nomic-embed-text`
* Vector database: ChromaDB persistent local tại `./chroma_db`
* Collection: `acm_knowledge`

## Luồng hoạt động tổng quát

1. Ingest: đọc file `.md/.txt/.pdf` từ `data/`.
2. Markdown chunking: tách theo heading, gắn metadata (`category`, `source`, `file_name`, `heading`, `chunk_id`).
3. Oversized chunk split: chunk dài hơn `CHUNK_SIZE * 1.5` được split bằng `RecursiveCharacterTextSplitter`.
4. Embedding: dùng `OllamaEmbeddings(model=nomic-embed-text)`.
5. Index: lưu documents vào Chroma với id ổn định từ `chunk_id`.
6. Chat: detect intent/category, expand query, retrieve Chroma bằng distance.
7. Selection: filter bằng `MAX_DISTANCE_THRESHOLD`, sort distance tăng dần, deduplicate theo `chunk_id` và content hash.
8. Prompt: build context có giới hạn `MAX_CONTEXT_CHARS`; definition query dùng context budget riêng.
9. Generation: gọi `ChatOllama`; nếu lỗi hoặc empty thì fallback sang Ollama `/api/chat`.
10. Postprocess: remove thinking tags, trim definition answer nếu quá dài, build public sources.

# 2. Project Structure

```text
AI_CHATBOX/
├─ .env
├─ .env.example
├─ README.md
├─ requirements.txt
├─ app/
│  ├─ main.py
│  ├─ config.py
│  ├─ constants.py
│  ├─ api/
│  │  └─ chat_api.py
│  ├─ prompts/
│  │  └─ system_prompt.py
│  ├─ schemas/
│  │  └─ chat_schema.py
│  ├─ services/
│  │  ├─ document_service.py
│  │  ├─ markdown_chunker.py
│  │  ├─ ollama_service.py
│  │  ├─ rag_retrieval.py
│  │  ├─ rag_service.py
│  │  └─ source_sanitizer.py
│  ├─ utils/
│  │  └─ file_loader.py
│  └─ vectorstore/
│     └─ chroma_store.py
├─ chroma_db/
├─ data/
│  ├─ acm/
│  ├─ crops/
│  │  ├─ ca-chua/
│  │  ├─ gao/
│  │  └─ rau-an-la/
│  ├─ faq/
│  ├─ templates/
│  ├─ traceability/
│  └─ vietgap/
├─ docs/
│  └─ superpowers/
├─ scripts/
│  ├─ ingest_documents.py
│  └─ test_chat.py
└─ tests/
   ├─ test_document_processing.py
   └─ test_rag_retrieval.py
```

# 3. Configuration

## `.env`

```dotenv
APP_NAME="FarmTrace AI Chatbox"
API_PREFIX=/api/v1/ai

OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3.5:2b
EMBEDDING_MODEL=nomic-embed-text

OLLAMA_THINK=false

CHROMA_DIR=./chroma_db
DATA_DIR=./data
COLLECTION_NAME=acm_knowledge

CHUNK_SIZE=650
CHUNK_OVERLAP=100

DEFAULT_TOP_K=5
MAX_CONTEXT_CHARS=2600
MAX_DISTANCE_THRESHOLD=0.5

NUM_PREDICT=512
NUM_CTX=4096

TEMPERATURE=0.1
TOP_K=20
TOP_P=0.8
REPEAT_PENALTY=1.1

KEEP_ALIVE=30m
```

## `app/config.py`

`config.py` load `.env` bằng `load_dotenv(ROOT_DIR / ".env")`, resolve path tương đối theo project root, và tạo thư mục Chroma/Data nếu chưa có.

Giá trị cấu hình runtime:

| Key | Value |
| --- | --- |
| `APP_NAME` | `FarmTrace AI Chatbox` |
| `API_PREFIX` | `/api/v1/ai` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `OLLAMA_MODEL` | `qwen3.5:2b` |
| `EMBEDDING_MODEL` | `nomic-embed-text` |
| `OLLAMA_THINK` | `false` |
| `CHROMA_DIR` | `./chroma_db` |
| `DATA_DIR` | `./data` |
| `COLLECTION_NAME` | `acm_knowledge` |
| `CHUNK_SIZE` | `650` |
| `CHUNK_OVERLAP` | `100` |
| `DEFAULT_TOP_K` | `5` |
| `MAX_CONTEXT_CHARS` | `2600` |
| `MAX_DISTANCE_THRESHOLD` | `0.5` |
| `MIN_RETRIEVAL_SCORE` | unset / `None` |
| `NUM_PREDICT` | `512` |
| `NUM_CTX` | `4096` |
| `TEMPERATURE` | `0.1` |
| `TOP_K` | `20` |
| `TOP_P` | `0.8` |
| `REPEAT_PENALTY` | `1.1` |
| `KEEP_ALIVE` | `30m` |

Các giá trị RAG/generation chính:

* Chunk size: `650`
* Chunk overlap: `100`
* Retrieval top_k mặc định: `5`
* Chat schema cho phép `top_k` từ `1` đến `10`
* Retrieval threshold: Chroma distance `<= 0.5`
* Model: `qwen3.5:2b`
* Embedding: `nomic-embed-text`
* Context length model: `NUM_CTX=4096`
* Prompt context budget: `MAX_CONTEXT_CHARS=2600`
* Definition-query context budget: `_DEFINITION_MAX_CONTEXT_CHARS=1000`
* Definition-query answer max chars: `_DEFINITION_MAX_ANSWER_CHARS=350`
* Generation: `temperature=0.1`, `num_predict=512`, `top_k=20`, `top_p=0.8`, `repeat_penalty=1.1`, `keep_alive=30m`, `think=false`

# 4. RAG Pipeline

## Ingest flow

Entry point CLI:

```python
# scripts/ingest_documents.py
service = DocumentService()
result = service.ingest(reset=True)
```

Core ingest:

```python
# app/services/document_service.py
documents, files_scanned = load_documents(target_dir)
documents = self._filter_excluded_documents(documents)

if not reset:
    self.chroma_store.delete_by_sources(loaded_sources)

heading_chunks = [doc for doc in documents if doc.metadata.get("chunk_id")]
fallback_documents = [doc for doc in documents if not doc.metadata.get("chunk_id")]
fallback_chunks = self.splitter.split_documents(fallback_documents)
fallback_chunks = self._ensure_chunk_metadata(fallback_chunks)

chunks = self._split_oversized_documents(heading_chunks + fallback_chunks)
self._check_chunk_id_collisions(chunks)
self.chroma_store.add_documents(chunks)
```

Supported file types:

```python
# app/utils/file_loader.py
SUPPORTED_EXTENSIONS = {".pdf", ".txt", ".md"}
```

Root admin files skipped before chunking:

```python
ROOT_ADMIN_FILES = {
    "README.md",
    "data_guide.md",
    "sources.jsonl",
}
```

## Chunking flow

Markdown files are split by headings:

```python
# app/services/markdown_chunker.py
_HEADING_RE = re.compile(r"^(#{1,6})\s+(.+?)\s*$")
```

Metadata category mapping:

```python
CATEGORY_ALIASES = {
    "acm": "acm",
    "farmtrace": "acm",
    "vietgap": "vietgap",
    "faq": "faq",
    "crops": "crop",
    "crop": "crop",
    "traceability": "traceability",
    "templates": "template",
    "template": "template",
}
```

Chunk id format for heading chunks:

```python
metadata = {
    **base_metadata,
    "heading": heading,
    "chunk_id": (
        f"{base_metadata['category']}:{base_metadata['source']}:"
        f"{heading_index}:{chunk_index}"
    ),
}
```

Oversized chunks:

```python
# app/services/document_service.py
max_unsplit_chars = int(settings.CHUNK_SIZE * 1.5)
if len(doc.page_content) > max_unsplit_chars:
    split_docs = self.splitter.split_documents([doc])
    metadata["parent_chunk_id"] = old_chunk_id
    metadata["chunk_id"] = f"{old_chunk_id}:part-{index}"
```

## Embedding flow

```python
# app/vectorstore/chroma_store.py
self.embeddings = OllamaEmbeddings(
    model=settings.EMBEDDING_MODEL,
    base_url=settings.OLLAMA_BASE_URL,
)
```

## Retrieval flow

Candidate search:

```python
# app/services/rag_service.py
results = self.chroma_store.similarity_search_with_score(
    query,
    k=fetch_k,
    filter=metadata_filter,
)
```

Definition query uses `fetch_k=max(top_k * 3, 10)`, selected `effective_k=3`. Non-definition uses `fetch_k=max(top_k * 4, 12)`, selected `effective_k=top_k`.

Intent routing:

```python
if intent.confidence == "high" and intent.category:
    filtered_candidates = self._search_candidates(queries, fetch_k, category=intent.category)
    candidates.extend(filtered_candidates)

all_candidates = self._search_candidates(queries, fetch_k, category=None)
candidates.extend(all_candidates)
```

Selection:

```python
# app/services/rag_retrieval.py
if max_distance_threshold is not None and candidate.score is not None and candidate.score > max_distance_threshold:
    continue

sorted_candidates = sorted(filtered, key=lambda item: float("inf") if item.score is None else item.score)
```

Distance semantics from Chroma wrapper:

```python
# app/vectorstore/chroma_store.py
"""Chroma/LangChain returns distance here, not normalized similarity:
lower score means a closer match."""
```

## Prompt flow

System prompt states that the answer must only use retrieved support content:

```python
# app/prompts/system_prompt.py
- Chỉ sử dụng thông tin nằm trong phần "NỘI DUNG HỖ TRỢ".
- Không sử dụng kiến thức bên ngoài.
- Không suy đoán, không tự bịa số liệu, quy định, tiêu chuẩn, quy trình hoặc chức năng.
- Nếu không có thông tin trực tiếp hoặc không đủ dữ liệu để trả lời trọng tâm câu hỏi, chỉ trả lời: "{INSUFFICIENT_DATA_MESSAGE}"
```

Context build:

```python
# app/services/rag_service.py
part = (
    f"[TÀI LIỆU {index}]\n"
    f"Tiêu đề: {heading}\n"
    f"{content}"
)
if total_chars + separator_chars + len(part) > char_budget:
    continue
```

Prompt template:

```python
RAG_PROMPT_TEMPLATE = """
/no_think

{system_prompt}

=== NỘI DUNG HỖ TRỢ ===
{context}

=== CÂU HỎI NGƯỜI DÙNG ===
{question}

=== YÊU CẦU ĐẦU RA ==={is_definition_hint}
...
=== TRẢ LỜI ===
"""
```

## Generation flow

ChatOllama config:

```python
# app/services/ollama_service.py
self.llm = ChatOllama(
    model=settings.OLLAMA_MODEL,
    base_url=settings.OLLAMA_BASE_URL,
    temperature=settings.TEMPERATURE,
    num_predict=settings.NUM_PREDICT,
    num_ctx=settings.NUM_CTX,
    top_k=settings.TOP_K,
    top_p=settings.TOP_P,
    repeat_penalty=settings.REPEAT_PENALTY,
    keep_alive=settings.KEEP_ALIVE,
    reasoning=False,
    extra_body={"think": settings.OLLAMA_THINK},
)
```

Fallback generation:

```python
url = f"{settings.OLLAMA_BASE_URL}/api/chat"
payload = {
    "model": settings.OLLAMA_MODEL,
    "messages": [{"role": "user", "content": prompt}],
    "stream": False,
    "think": settings.OLLAMA_THINK,
    "options": {
        "temperature": settings.TEMPERATURE,
        "num_predict": settings.NUM_PREDICT,
        "num_ctx": settings.NUM_CTX,
        "top_k": settings.TOP_K,
        "top_p": settings.TOP_P,
        "repeat_penalty": settings.REPEAT_PENALTY,
    },
    "keep_alive": settings.KEEP_ALIVE,
}
```

# 5. Source Code Summary

## `app/services/rag_service.py`

Mục đích:

* Orchestrator chính của RAG chat: retrieve, build context, build prompt, call Ollama, postprocess answer, build sources.

Luồng xử lý:

1. `chat(question, top_k)` nhận câu hỏi.
2. `_retrieve_contexts()` detect definition, detect intent, expand query, retrieve filtered/all, dedupe, rerank definition, select by distance.
3. `_build_context()` sanitize chunk content, giới hạn số chunk và context chars.
4. Format `RAG_PROMPT_TEMPLATE`.
5. `OllamaService.generate()` sinh câu trả lời.
6. Nếu answer insufficient/empty thì trả insufficient message và không trả sources.
7. `_postprocess_answer()` cleanup; `_build_sources()` tạo tối đa 3 source cards.

Hàm quan trọng:

* `_search_candidates(queries, fetch_k, category)`
* `_retrieve_contexts(question, top_k)`
* `_deduplicate_candidates_preserving_route_order(candidates)`
* `_build_context(contexts, max_chunks, is_definition)`
* `_build_sources(contexts)`
* `_postprocess_answer(answer, is_definition)`
* `chat(question, top_k)`

Tham số/constant quan trọng:

* `MAX_DISPLAY_SOURCES = 3`
* `_DEFINITION_MAX_CONTEXT_CHARS = 1000`
* `_DEFINITION_MAX_ANSWER_CHARS = 350`
* `settings.DEFAULT_TOP_K`
* `settings.MAX_CONTEXT_CHARS`
* `settings.MAX_DISTANCE_THRESHOLD`

## `app/services/document_service.py`

Mục đích:

* Quản lý ingest: load documents, filter excluded files, split fallback/oversized chunks, check duplicate chunk ids, add vào Chroma.

Luồng xử lý:

1. Resolve `data_dir`.
2. Nếu `reset=True`, reset Chroma directory.
3. `load_documents(target_dir)` lấy documents và số file scanned.
4. Filter `README.md`, `data_guide.md`, `sources.jsonl`.
5. Nếu không reset, delete old chunks theo source.
6. Tách `heading_chunks` và `fallback_documents`.
7. Fallback split bằng `RecursiveCharacterTextSplitter`.
8. Split oversized chunks.
9. Check collision.
10. Add documents vào Chroma.

Hàm quan trọng:

* `_filter_excluded_documents()`
* `_document_source()`
* `_ensure_chunk_metadata()`
* `_split_oversized_documents()`
* `_check_chunk_id_collisions()`
* `ingest(data_dir=None, reset=False)`

Tham số quan trọng:

* `CHUNK_SIZE=650`
* `CHUNK_OVERLAP=100`
* `max_unsplit_chars = int(CHUNK_SIZE * 1.5) = 975`
* `EXCLUDED_FILES = {"README.md", "data_guide.md", "sources.jsonl"}`

## `app/services/ollama_service.py`

Mục đích:

* Wrapper gọi Ollama LLM qua LangChain `ChatOllama`; xử lý fallback và clean thinking tags.

Luồng xử lý:

1. Khởi tạo `ChatOllama` với model/generation params từ settings.
2. `generate()` log prompt size/chunks, gọi `self.llm.invoke(prompt)`.
3. `_clean_thinking()` loại `<think>...</think>`, tag think, block/line "Thinking".
4. Nếu invoke lỗi hoặc content rỗng, `_fallback_generate()` gọi trực tiếp `/api/chat`.
5. Fallback lỗi thì trả insufficient message.

Hàm quan trọng:

* `_clean_thinking(text)`
* `_fallback_generate(prompt)`
* `generate(prompt, chunks_count=0)`

Tham số quan trọng:

* `OLLAMA_MODEL=qwen3.5:2b`
* `OLLAMA_BASE_URL=http://localhost:11434`
* `NUM_CTX=4096`
* `NUM_PREDICT=512`
* `TEMPERATURE=0.1`
* `TOP_K=20`
* `TOP_P=0.8`
* `REPEAT_PENALTY=1.1`
* `KEEP_ALIVE=30m`
* `OLLAMA_THINK=false`

## `app/vectorstore/chroma_store.py`

Mục đích:

* Wrapper ChromaDB + Ollama embeddings.

Luồng xử lý:

1. Khởi tạo `OllamaEmbeddings`.
2. `get_vectorstore()` mở Chroma collection persistent.
3. `add_documents()` add theo batch size 32, id lấy từ `chunk_id`.
4. `delete_by_sources()` xóa chunk cũ theo metadata `source`.
5. `reset()` xóa và tạo lại thư mục Chroma.
6. `similarity_search_with_score()` trả document + Chroma distance.

Hàm quan trọng:

* `get_vectorstore()`
* `_build_ids(documents)`
* `add_documents(documents)`
* `delete_by_metadata(where)`
* `delete_by_sources(sources)`
* `delete_by_files(file_names)`
* `reset()`
* `count()`
* `similarity_search_with_score(query, k, filter=None)`

Tham số quan trọng:

* `_ADD_DOCUMENTS_BATCH_SIZE = 32`
* `EMBEDDING_MODEL=nomic-embed-text`
* `COLLECTION_NAME=acm_knowledge`
* `CHROMA_DIR=./chroma_db`

## `app/prompts/system_prompt.py`

Mục đích:

* Định nghĩa guardrails và prompt template cho RAG.

Luồng xử lý:

* `SYSTEM_PROMPT` chứa vai trò, nguyên tắc bắt buộc, chống nhiễu chỉ dẫn, cách trả lời, hướng dẫn câu hỏi định nghĩa.
* `RAG_PROMPT_TEMPLATE` nhúng system prompt, context, question và yêu cầu đầu ra.

Thành phần quan trọng:

* Insufficient message lấy từ `app.constants.INSUFFICIENT_DATA_MESSAGE`.
* Prompt bắt đầu bằng `/no_think`.
* Context đặt trong `=== NỘI DUNG HỖ TRỢ ===`.
* Câu hỏi đặt trong `=== CÂU HỎI NGƯỜI DÙNG ===`.

## `scripts/ingest_documents.py`

Mục đích:

* CLI ingest toàn bộ tài liệu vào Chroma.

Luồng xử lý:

```python
ROOT_DIR = Path(__file__).resolve().parents[1]
sys.path.append(str(ROOT_DIR))
service = DocumentService()
result = service.ingest(reset=True)
```

Output:

* `Files loaded`
* `Files skipped`
* `Heading chunks`
* `Fallback chunks`
* `Chunks indexed`
* `Collection`

# 6. Data Summary

## Tổng hợp số lượng

Thời điểm audit: sau khi chạy `python scripts/ingest_documents.py`.

* Tổng số thư mục dưới `data/`: 9
* Thư mục cấp nhóm: 6 (`acm`, `crops`, `faq`, `templates`, `traceability`, `vietgap`)
* Subfolder crops: 3 (`ca-chua`, `gao`, `rau-an-la`)
* Tổng số file trong `data/`: 56
* File knowledge được ingest: 54
* Root admin/guide files: 2 (`data/README.md`, `data/data_guide.md`)

## Nhóm dữ liệu

| Nhóm | Số file | Ghi chú |
| --- | ---: | --- |
| `vietgap` | 9 | Tổng quan, checklist, yêu cầu đất/nước/giống, phân bón/BVTV, thu hoạch/sơ chế, chứng nhận, lỗi thường gặp |
| `crops` | 18 | 3 crop groups x 6 file: cà chua, gạo, rau ăn lá |
| `acm` / FarmTrace | 9 | Tổng quan ACM, mùa vụ, nhật ký, nông trại, kho, đăng bán, giỏ hàng |
| `traceability` | 5 | Tổng quan truy xuất, QR, thông tin hiển thị, bảo mật dữ liệu, lỗi truy xuất |
| `faq` | 6 | FAQ ACM, admin, người mua, nông dân, truy xuất, VietGAP |
| `template` / `templates` | 7 | Mẫu nhật ký, phiếu nhập kho, biên bản kiểm tra |
| root files | 2 | `README.md`, `data_guide.md`, không ingest |

Danh sách nhóm đã có:

* `vietgap`
* `crops`
* `farmtrace`: không có folder riêng hiện tại; alias `farmtrace` được normalize thành `acm` trong code.
* `traceability`
* `faq`
* `template`: folder thực tế là `templates`, metadata category là `template`.

# 7. Sample Documents

## 1 file VietGAP: `data/vietgap/tong-quan-vietgap.md`

```markdown
---
doc_id: vietgap__tong_quan_vietgap
title: Tổng quan VietGAP
category: vietgap
audience: all
language: vi
version: 1.0
updated_at: 2026-06-24
tags: [vietgap, tong-quan, thuc-hanh-nong-nghiep-tot, chung-nhan, tieu-chuan]
source_type: internal_demo
---

# Tổng quan VietGAP

## VietGAP là gì?

VietGAP là viết tắt của Vietnamese Good Agricultural Practices — Thực hành nông nghiệp tốt tại Việt Nam.

Trong lĩnh vực trồng trọt, VietGAP hướng dẫn cách sản xuất và sơ chế nông sản an toàn, có kiểm soát và có thể truy xuất nguồn gốc.

VietGAP không phải là tiêu chuẩn hữu cơ. VietGAP cho phép sử dụng phân bón hóa học và thuốc bảo vệ thực vật, nhưng phải đúng quy định và có ghi chép đầy đủ.
```

## 1 file FarmTrace/ACM: `data/acm/tong-quan-he-thong.md`

```markdown
---
doc_id: acm__tong_quan_he_thong
title: Tổng quan hệ thống ACM
category: acm
audience: all
language: vi
version: 1.0
updated_at: 2026-06-24
tags: [acm, he-thong, tong-quan, quan-ly-nong-nghiep, agricultural-crop-management]
source_type: internal_demo
---

# Tổng quan hệ thống ACM

## ACM là gì?

ACM (Agricultural Crop Management) là hệ thống quản lý sản xuất nông nghiệp. ACM hỗ trợ nông dân quản lý nông trại, mùa vụ, nhật ký sản xuất, kho hàng, đăng bán sản phẩm và truy xuất nguồn gốc.

ACM không phải là đơn vị chứng nhận VietGAP. ACM là công cụ hỗ trợ ghi chép, quản lý và truy xuất.
```

## 1 file FAQ: `data/faq/faq-nong-dan.md`

```markdown
---
doc_id: faq__faq_nong_dan_mo_rong
title: FAQ nông dân mở rộng
category: faq
audience: farmer
language: vi
version: 1.1
updated_at: 2026-06-20
tags: [faq, nong-dan, ACM, mua-vu, nhat-ky, truy-xuat]
source_type: internal_faq
review_required: true
---

# FAQ nông dân mở rộng

## Mục đích

Tài liệu này mở rộng bộ câu hỏi thường gặp cho chatbot ACM, giúp trả lời nhất quán, ngắn gọn và đúng phạm vi dữ liệu.

## Nguyên tắc trả lời

- Trả lời ngắn, trực tiếp và dễ hiểu.
- Không suy đoán khi thiếu dữ liệu.
- Không khẳng định đạt VietGAP nếu chưa có chứng nhận hợp lệ.
```

## 1 file Traceability: `data/traceability/thong-tin-hien-thi-khi-quet-qr.md`

```markdown
---
doc_id: traceability__thong_tin_hien_thi_khi_quet_qr
title: Thông tin hiển thị khi quét QR
category: traceability
audience: buyer
language: vi
version: 1.0
updated_at: 2026-06-24
tags: [qr, quet-qr, nguoi-mua, thong-tin, hien-thi, trang-truy-xuat, ma-qr]
source_type: internal_demo
---

# Thông tin hiển thị khi quét QR

## Người mua quét QR thấy gì?

Khi quét QR, người mua sẽ được chuyển đến trang truy xuất công khai. Trang này có thể hiển thị:

- Tên sản phẩm.
- Tên nông trại sản xuất.
- Khu vực sản xuất.
- Ngày thu hoạch.
- Thông tin lô hàng.
- Mùa vụ liên quan.
- Nhật ký sản xuất đã được công khai.
- Trạng thái chứng nhận VietGAP nếu có.
```

# 8. Ingest Statistics

Command đã chạy:

```powershell
.\.venv\Scripts\python.exe scripts\ingest_documents.py
```

Output:

```text
Ingest completed
Files loaded   : 54
Files skipped  : 0
Heading chunks : 747
Fallback chunks: 0
Chunks indexed : 747
Collection     : acm_knowledge
```

Collection count sau ingest:

```text
747
```

Tổng hợp:

| Metric | Value |
| --- | ---: |
| Files loaded | 54 |
| Files skipped/excluded | 0 |
| Heading chunks | 747 |
| Fallback chunks | 0 |
| Chunks created/indexed | 747 |
| Collection size | 747 |
| Collection name | `acm_knowledge` |

# 9. Test Cases

Các câu hỏi dưới đây được chạy bằng `RagService.chat(question, top_k=5)` sau khi ingest lại collection.

| # | Câu hỏi | Câu trả lời hiện tại |
| ---: | --- | --- |
| 1 | VietGAP là gì? | VietGAP là viết tắt của "Viện Good Agricultural Practices" (Thực hành nông nghiệp tốt). Đây là bộ tiêu chuẩn thực hành nông nghiệp tại Việt Nam, hướng dẫn cách sản xuất và sơ chế nông sản an toàn với kiểm soát chặt chẽ. |
| 2 | Nguồn nước trong VietGAP cần đáp ứng yêu cầu gì? | Nguồn nước trong VietGAP cần đáp ứng các yêu cầu cơ bản sau: 1. Nước phải sạch hoặc đã qua xử lý phù hợp để đảm bảo an toàn cho cây trồng. 2. Nguồn nước được sử dụng phải có khả năng cung cấp đủ lượng và chất lượng theo quy định của từng loại sản phẩm cụ thể. Chưa có dữ liệu về: Yêu cầu chi tiết về nguồn nước, phân bón và thuốc bảo vệ thực vật (bao gồm cả thu hoạch và sơ chế). |
| 3 | Làm sao tạo mùa vụ trong hệ thống ACM? | Để tạo mùa vụ trong hệ thống ACM: 1. Truy cập vào trang quản lý nông trại của bạn để xác định vị trí chính xác. 2. Chọn mục "Tạo mới" hoặc "Thêm mùa vụ". 3. Nhập tên cho mùa vụ và mô tả ngắn gọn về sản phẩm cần trồng. 4. Xác định diện tích đất phù hợp với loại cây trồng đã chọn. 5. Lưu thông tin để hệ thống tự động phân bổ các giai đoạn chăm sóc tương ứng. |
| 4 | Khi thu hoạch xong thì nhập kho trong ACM như thế nào? | Khi thu hoạch xong thì nhập kho trong ACM như thế nào? Sau khi hoàn tất việc thu hoạch sản phẩm, nông dân cần thực hiện các bước sau để tiến hành nhập kho trên hệ thống: 1. Thực hiện thao tác "Thu hoạch" trong phần quản lý cây trồng hoặc lô hàng của bạn. 2. Sau đó chuyển sang mục "Nhập kho"... |
| 5 | Mã QR truy xuất nguồn gốc hiển thị những thông tin nào? | Mã QR truy xuất nguồn gốc hiển thị những thông tin sau: 1. Thông tin sản phẩm cụ thể (loại cây trồng). 2. Thông tin nông trại liên quan đến lô hàng hoặc sản phẩm đó. 3. Ngày thu hoạch của sản phẩm. 4. Nhật ký công khai ghi chép quá trình chăm sóc và quản lý. 5. Trạng thái chứng nhận nếu có sẵn trên hệ thống ACM. |
| 6 | Người mua liên kết giỏ hàng với nông trại như thế nào? | Người mua liên kết giỏ hàng với nông trại như sau: 1. Người mua thêm sản phẩm từ nhiều nông trại vào giỏ hàng trước khi xác nhận đặt hàng trên hệ thống ACM. 2. Sau khi người bán giao hàng, thông tin lô hàng và dữ liệu nền của từng nông trại vẫn được giữ nguyên trong hồ sơ truy xuất. 3. Khi người mua cần kiểm tra nguồn gốc hoặc yêu cầu hỗ trợ cụ thể về sản phẩm đã mua, họ quét mã QR trên bao bì để xem lại lịch sử liên kết với các đơn vị cung cấp. |
| 7 | Nhật ký sản xuất cần ghi những nội dung gì? | Nhật ký sản xuất cần ghi những nội dung gì? 1. Thông tin đất ban đầu (đất đai, nước tưới). 2. Dữ liệu giống cây trồng được sử dụng. 3. Lịch sử chăm sóc và các hoạt động canh tác trước khi bắt đầu mùa vụ mới. |
| 8 | Cà chua thường gặp sâu bệnh nào? | Cà chua thường gặp sâu bệnh là: 1. Sâu đục thân cà chua (SBD) gây hại nghiêm trọng nhất trên cây. 2. Rầy nâu, rầy đồng và bọ chét ăn lá làm giảm năng suất. 3. Bệnh thối rễ do nấm hoặc vi khuẩn tấn công gốc cây. Chưa có dữ liệu về: Sâu bệnh thường gặp trên cà chua |
| 9 | Nông dân quên ghi nhật ký thì sao? | Nông dân nên ghi nhật ký ngay sau khi thực hiện hoạt động để tránh thiếu hoặc sai thông tin. Việc ghi muộn vẫn được phép nhưng ngày tháng phải chính xác theo thời gian thực của việc làm. |
| 10 | Hệ thống có hỗ trợ thanh toán blockchain không? | Tôi chưa có đủ dữ liệu trong tài liệu hiện tại. |

# 10. Retrieval Examples

Ghi chú: `score_distance` là Chroma distance; thấp hơn là gần hơn.

## 1. VietGAP là gì?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.367129 | `data/vietgap/phan-biet-vietgap-huu-co-an-toan.md`, heading `VietGAP là gì?` |
| 2 | 0.378740 | `data/vietgap/tong-quan-vietgap.md`, heading `Tổng quan VietGAP` |
| 3 | 0.392849 | `data/vietgap/tong-quan-vietgap.md`, heading `VietGAP là gì?` |

## 2. Nguồn nước trong VietGAP cần đáp ứng yêu cầu gì?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.225340 | `data/vietgap/yeu-cau-dat-nuoc-giong.md`, heading `Yêu cầu VietGAP về đất, nước và giống` |
| 2 | 0.344371 | `data/vietgap/yeu-cau-dat-nuoc-giong.md`, heading `Mục đích` |
| 3 | 0.355640 | `data/vietgap/tong-quan-vietgap.md`, heading `VietGAP gồm những nội dung chính nào?` |
| 4 | 0.359192 | `data/vietgap/yeu-cau-phan-bon-thuoc-bvtv.md`, heading `Yêu cầu VietGAP về phân bón và thuốc bảo vệ thực vật` |
| 5 | 0.381079 | `data/vietgap/yeu-cau-thu-hoach-so-che.md`, heading `Yêu cầu VietGAP về thu hoạch và sơ chế` |

## 3. Làm sao tạo mùa vụ trong hệ thống ACM?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.230907 | `data/acm/tong-quan-he-thong.md`, heading `Tổng quan hệ thống ACM` |
| 2 | 0.259029 | `data/acm/tao-mua-vu.md`, heading `Tạo mùa vụ trên ACM` |
| 3 | 0.291218 | `data/acm/quan-ly-nong-trai.md`, heading `Nông trại là gì trong ACM?` |
| 4 | 0.313914 | `data/acm/ghi-nhat-ky-san-xuat.md`, heading `Ghi nhật ký sản xuất trên ACM` |
| 5 | 0.332810 | `data/acm/thu-hoach-va-nhap-kho.md`, heading `Thu hoạch và nhập kho trên ACM` |

## 4. Khi thu hoạch xong thì nhập kho trong ACM như thế nào?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.287608 | `data/acm/tao-mua-vu.md`, heading `Mùa vụ kết thúc khi nào?` |
| 2 | 0.301266 | `data/acm/thu-hoach-va-nhap-kho.md`, heading `Thu hoạch và nhập kho trên ACM` |
| 3 | 0.318185 | `data/acm/thu-hoach-va-nhap-kho.md`, heading `Nhập kho sau thu hoạch` |
| 4 | 0.321904 | `data/acm/thu-hoach-va-nhap-kho.md`, heading `Mục đích` |
| 5 | 0.323239 | `data/templates/mau-phieu-nhap-kho.md`, heading `Mục đích` |

## 5. Mã QR truy xuất nguồn gốc hiển thị những thông tin nào?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.261764 | `data/faq/faq-admin.md`, heading `Câu hỏi: QR nên hiển thị những thông tin nào?` |
| 2 | 0.278703 | `data/traceability/thong-tin-hien-thi-khi-quet-qr.md`, heading `Mục đích` |
| 3 | 0.299764 | `data/traceability/tong-quan-truy-xuat.md`, heading `Truy xuất nguồn gốc trên ACM` |
| 4 | 0.311600 | `data/traceability/quy-trinh-tao-ma-qr.md`, heading `QR truy xuất dùng để làm gì?` |
| 5 | 0.311891 | `data/traceability/thong-tin-hien-thi-khi-quet-qr.md`, heading `Thông tin hiển thị khi quét QR` |

## 6. Người mua liên kết giỏ hàng với nông trại như thế nào?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.412252 | `data/vietgap/loi-thuong-gap-khi-lam-vietgap.md`, heading `Lỗi về hồ sơ và truy xuất` |
| 2 | 0.415213 | `data/acm/lien-ket-gio-hang-nong-trai.md`, heading `Truy xuất từ giỏ hàng` |
| 3 | 0.437800 | `data/acm/lien-ket-gio-hang-nong-trai.md`, heading `Giỏ hàng trên ACM` |
| 4 | 0.455048 | `data/faq/faq-nguoi-mua.md`, heading `Câu hỏi: Khi nào tôi nên liên hệ người bán?` |
| 5 | 0.474436 | `data/faq/faq-nguoi-mua.md`, heading `Câu hỏi: Tôi không nhận được hàng thì làm sao?` |

## 7. Nhật ký sản xuất cần ghi những nội dung gì?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.320815 | `data/acm/ghi-nhat-ky-san-xuat.md`, heading `Khi nào nên ghi nhật ký?` |
| 2 | 0.353937 | `data/faq/faq-nong-dan.md`, heading `Câu hỏi: Nếu không sử dụng thuốc BVTV thì có cần ghi không?` |
| 3 | 0.378277 | `data/acm/quan-ly-phan-bon-thuoc-bvtv.md`, heading `Ghi nhật ký bón phân` |
| 4 | 0.378894 | `data/acm/tao-mua-vu.md`, heading `Sau khi tạo mùa vụ cần làm gì?` |
| 5 | 0.381202 | `data/acm/ghi-nhat-ky-san-xuat.md`, heading `Nhật ký sản xuất là gì?` |

## 8. Cà chua thường gặp sâu bệnh nào?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.259842 | `data/crops/ca-chua/sau-benh-thuong-gap.md`, heading `Sâu bệnh thường gặp trên cà chua` |
| 2 | 0.331742 | `data/crops/rau-an-la/sau-benh-thuong-gap.md`, heading `Sâu bệnh thường gặp trên rau ăn lá` |
| 3 | 0.355207 | `data/crops/gao/sau-benh-thuong-gap.md`, heading `Sâu bệnh thường gặp trên lúa` |
| 4 | 0.369872 | `data/vietgap/phan-biet-vietgap-huu-co-an-toan.md`, heading `Câu hỏi thường gặp` |
| 5 | 0.399033 | `data/faq/faq-admin.md`, heading `Câu hỏi thường gặp về nhật ký sản xuất` |

## 9. Nông dân quên ghi nhật ký thì sao?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.269134 | `data/acm/ghi-nhat-ky-san-xuat.md`, heading `Khi nào nên ghi nhật ký?` |
| 2 | 0.289836 | `data/acm/quy-trinh-mvp.md`, heading `Nông dân` |
| 3 | 0.298393 | `data/faq/faq-nong-dan.md`, heading `Câu hỏi: Nông dân nên ghi nhật ký khi nào?` |
| 4 | 0.322388 | `data/acm/quy-trinh-mvp.md`, heading `Sau khi tạo mùa vụ tôi cần làm gì tiếp theo?` |
| 5 | 0.322902 | `data/acm/tao-mua-vu.md`, heading `Một nông trại có nhiều mùa vụ không?` |

## 10. Hệ thống có hỗ trợ thanh toán blockchain không?

| Rank | score_distance | chunk |
| ---: | ---: | --- |
| 1 | 0.426342 | `data/faq/faq-nguoi-mua.md`, heading `Câu hỏi: Tôi có thể thanh toán khi nhận hàng không?` |
| 2 | 0.442498 | `data/faq/faq-nguoi-mua.md`, heading `Câu hỏi: Sản phẩm này có an toàn không?` |
| 3 | 0.456381 | `data/faq/faq-nguoi-mua.md`, heading `Câu hỏi: Người mua có xem được thông tin cá nhân của nông dân không?` |
| 4 | 0.460619 | `data/faq/faq-nong-dan.md`, heading `Câu hỏi: Một tài khoản có thể quản lý nhiều nông trại không?` |
| 5 | 0.469304 | `data/faq/faq-nguoi-mua.md`, heading `Câu hỏi: Tôi có thể so sánh các sản phẩm theo nông trại không?` |

# 11. Known Issues

Các vấn đề dưới đây là phát hiện hiện trạng từ code và test run. Không bao gồm đề xuất xử lý.

1. Một số câu trả lời có nội dung không đúng với dữ liệu retrieve. Ví dụ: câu "VietGAP là gì?" trả lời `Viện Good Agricultural Practices`, trong khi tài liệu ghi `Vietnamese Good Agricultural Practices`.

2. Một số câu trả lời có dấu hiệu suy diễn/hallucination thao tác UI không xuất hiện trực tiếp trong retrieved chunks. Ví dụ câu tạo mùa vụ trả về các bước như "Chọn mục Tạo mới", "Lưu thông tin để hệ thống tự động phân bổ..." trong khi top retrieved chunks chủ yếu là heading/tổng quan.

3. Một số câu trả lời nhắc lại nguyên câu hỏi ở đầu answer. Ví dụ câu nhập kho và câu nhật ký sản xuất.

4. Một số câu trả lời tự thêm "Chưa có dữ liệu về..." dù đã trả lời bằng nội dung có vẻ không khớp hoàn toàn với retrieved chunk. Ví dụ câu sâu bệnh cà chua trả lời danh sách sâu bệnh rồi lại ghi chưa có dữ liệu về sâu bệnh cà chua.

5. Một số retrieval top rank là heading-only chunk hoặc chunk rất ngắn, ví dụ `# Tổng quan VietGAP`, `# Tạo mùa vụ trên ACM`, `# Thu hoạch và nhập kho trên ACM`. Các chunk này được đưa vào prompt và sources.

6. Source cards có thể hiển thị snippet chỉ là heading, ví dụ source `tong-quan-vietgap.md` heading `Tổng quan VietGAP` có snippet `Tổng quan VietGAP`.

7. Query "Người mua liên kết giỏ hàng với nông trại như thế nào?" retrieve rank 1 từ `data/vietgap/loi-thuong-gap-khi-lam-vietgap.md` heading `Lỗi về hồ sơ và truy xuất`, không phải tài liệu ACM giỏ hàng.

8. Query "Cà chua thường gặp sâu bệnh nào?" retrieve thêm chunk sâu bệnh rau ăn lá và lúa trong top 3, ngoài crop cà chua.

9. Query "Hệ thống có hỗ trợ thanh toán blockchain không?" vẫn retrieve 5 chunk dưới threshold, nhưng model trả insufficient. Retrieval không empty; answer empty-source do `is_insufficient_answer()` xóa sources khi answer insufficient.

10. Prompt cấm nhắc "tài liệu", "context", nguồn trong answer, nhưng test run trước đó với input bị lỗi encoding đã có output nhắc "Theo tài liệu" và "tài liệu đã cung cấp". Trong run UTF-8 chính thức, vẫn có answer không hoàn toàn tuân thủ phạm vi dữ liệu.

11. `README.md` hướng dẫn đặt tài liệu vào `data/farmtrace/`, nhưng cấu trúc hiện tại dùng `data/acm/`. Code có alias `farmtrace -> acm`, nhưng repo hiện không có folder `data/farmtrace/`.

12. `MIN_RETRIEVAL_SCORE` vẫn tồn tại trong `config.py` nhưng nếu set thì code log là ignored vì Chroma trả distance, không phải similarity score.

13. Retrieval examples cho thấy `MAX_DISTANCE_THRESHOLD=0.5` cho phép nhiều chunk xa/chung chung vào prompt, ví dụ distances quanh `0.45-0.47` ở câu giỏ hàng và blockchain.

14. `source_sanitizer.py` sanitize metadata/score/chunk_id khỏi public output, nhưng sources vẫn có thể chứa heading/snippet quá ít thông tin nếu chunk gốc là heading-only.

15. `DocumentService._split_oversized_documents()` chỉ split khi `len(page_content) > CHUNK_SIZE * 1.5`; heading chunks ngắn/chỉ heading vẫn được giữ nguyên và index.

16. `ChromaStore.delete_by_files()` còn tồn tại và comment ghi có thể delete quá rộng nếu trùng file name ở nhiều folder; ingest hiện dùng `delete_by_sources()`.

17. Output PowerShell có thể bị mojibake nếu không set `PYTHONIOENCODING=utf-8` hoặc dùng Unicode escape cho inline Python. Lần test đầu đã làm câu hỏi tiếng Việt thành dạng `VietGAP l? g??`, gây retrieval nhiễu.

18. Các file data là `source_type: internal_demo` hoặc `internal_faq`; nhiều nội dung là dữ liệu demo/nội bộ, không phải tiêu chuẩn pháp lý chính thức.

19. Không có test tự động trong lần audit này cho toàn bộ 10 câu hỏi ở dạng snapshot/assertion; các câu hỏi được chạy thủ công bằng script inline qua `RagService`.

20. `RagService._postprocess_answer()` chỉ trim answer definition nếu tìm được sentence boundary; nếu không có boundary thì giữ full answer dù vượt `_DEFINITION_MAX_ANSWER_CHARS`.
