import urllib.request
import json

def test_agri_expert():
    url = "http://localhost:8083/api/v1/internal/ai/agricultural-expert"
    payload = {
        "userMessage": "Làm thế nào để trị bệnh đốm lá trên cây lúa?",
        "cropContext": "Lúa đang giai đoạn đẻ nhánh"
    }
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            print("Agri Expert Response:", response.status)
            print(response.read().decode('utf-8'))
    except Exception as e:
        print("Error:", e)

def test_buyer_expert():
    url = "http://localhost:8083/api/v1/internal/ai/buyer-expert"
    payload = {
        "userMessage": "Giá lúa Jasmine 85 hôm nay là bao nhiêu?",
        "buyerContext": "Đang tìm mua lúa tại Đồng bằng Sông Cửu Long"
    }
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            print("Buyer Expert Response:", response.status)
            print(response.read().decode('utf-8'))
    except Exception as e:
        print("Error:", e)

if __name__ == "__main__":
    print("Testing AI Service...")
    test_agri_expert()
    print("-" * 40)
    test_buyer_expert()
