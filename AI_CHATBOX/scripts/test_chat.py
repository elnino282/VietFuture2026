import sys
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parents[1]
sys.path.append(str(ROOT_DIR))

from app.services.rag_service import RagService  # noqa: E402


if __name__ == "__main__":
    rag = RagService()
    print("FarmTrace AI Chatbox CLI. Type 'exit' to quit.")

    while True:
        question = input("\nBạn hỏi: ").strip()
        if question.lower() in {"exit", "quit"}:
            break

        result = rag.chat(question)
        print("\nAI trả lời:")
        print(result["answer"])

        if result["sources"]:
            print("\nNguồn tham khảo:")
            for source in result["sources"]:
                heading = f" > {source.heading}" if source.heading else ""
                page = f" - page {source.page}" if source.page else ""
                print(f"- {source.file_name}{heading}{page}")
