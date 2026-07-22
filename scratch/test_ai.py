import requests
import json
import base64

def test_agri_expert():
    url = "http://localhost:8083/api/v1/internal/ai/agricultural-expert"
    headers = {"Content-Type": "application/json"}
    payload = {
        "userMessage": "Làm thế nào để trị bệnh đốm lá trên cây lúa?",
        "cropContext": "Lúa đang giai đoạn đẻ nhánh"
    }
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        print("Agri Expert Response:", response.status_code)
        print(response.text)
    except Exception as e:
        print("Error:", e)

def test_buyer_expert():
    url = "http://localhost:8083/api/v1/internal/ai/buyer-expert"
    headers = {"Content-Type": "application/json"}
    payload = {
        "userMessage": "Giá lúa Jasmine 85 hôm nay là bao nhiêu?",
        "buyerContext": "Đang tìm mua lúa tại Đồng bằng Sông Cửu Long"
    }
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        print("Buyer Expert Response:", response.status_code)
        print(response.text)
    except Exception as e:
        print("Error:", e)

if __name__ == "__main__":
    print("Testing AI Service...")
    test_agri_expert()
    print("-" * 40)
    test_buyer_expert()
