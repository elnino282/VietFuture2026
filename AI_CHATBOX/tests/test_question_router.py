import unittest

from app.services.question_router import QuestionRouter


class QuestionRouterTests(unittest.TestCase):
    def setUp(self):
        self.router = QuestionRouter()

    def test_routes_strict_rag_questions(self):
        cases = [
            "VietGAP là gì?",
            "Làm sao tạo mùa vụ trong ACM?",
            "Mã QR hiển thị thông tin gì?",
            "VietGAP yêu cầu pH đất chính xác bao nhiêu?",
            "Hồ sơ chứng nhận VietGAP cần gì?",
            "ACM có hỗ trợ blockchain không?",
        ]

        for question in cases:
            with self.subTest(question=question):
                route = self.router.route(question)

                self.assertEqual(route.mode, "strict_rag")
                self.assertEqual(route.confidence, "high")

    def test_routes_crop_document_questions_to_rag_first(self):
        cases = [
            "Ca chua thuong gap sau benh nao?",
            "Rau an la can cham soc nhu the nao?",
            "Lua thuong gap sau benh nao?",
            "Gao can thu hoach bao quan the nao?",
        ]

        for question in cases:
            with self.subTest(question=question):
                route = self.router.route(question)

                self.assertEqual(route.mode, "rag_first")
                self.assertEqual(route.category, "crop")

    def test_routes_undocumented_crop_questions_to_general_llm(self):
        cases = [
            "Ca phe can dieu kien khi hau gi?",
            "Ho tieu bi vang la do dau?",
            "Sau rieng nen tuoi nuoc nhu the nao?",
            "Ngo can mat do trong bao nhieu?",
            "Bap can thoi gian sinh truong bao lau?",
            "Thanh long bi nam thi nen lam gi?",
            "Chuoi can bon phan nhu the nao?",
        ]

        for question in cases:
            with self.subTest(question=question):
                route = self.router.route(question)

                self.assertEqual(route.mode, "general_agriculture_llm")
                self.assertEqual(route.category, "general_agriculture")

    def test_undocumented_crop_with_official_signal_routes_strict_rag(self):
        cases = [
            "Ca phe theo VietGAP can ho so gi?",
            "Ho tieu co can QR truy xuat khong?",
            "Sau rieng tren ACM tao mua vu the nao?",
            "Thanh long co tieu chuan chung nhan nao bat buoc?",
            "Chuoi can checklist phap ly gi?",
        ]

        for question in cases:
            with self.subTest(question=question):
                route = self.router.route(question)

                self.assertEqual(route.mode, "strict_rag")

    def test_exact_quantity_words_alone_do_not_force_strict_rag(self):
        cases = [
            "Ngo can mat do trong bao nhieu?",
            "Ca phe can lieu luong phan bon uoc tinh bao nhieu?",
            "Chuoi can thoi gian tuoi nuoc bao lau?",
        ]

        for question in cases:
            with self.subTest(question=question):
                route = self.router.route(question)

                self.assertEqual(route.mode, "general_agriculture_llm")

    def test_routes_general_agriculture_questions_to_general_llm(self):
        cases = [
            "Cà phê bị vàng lá do đâu?",
            "Sâu xanh ăn lá là gì?",
            "Cây thiếu kali có biểu hiện gì?",
            "Đất bị chai cứng thì nên cải tạo như thế nào?",
            "Tưới nước cho rau nên lưu ý gì?",
        ]

        for question in cases:
            with self.subTest(question=question):
                route = self.router.route(question)

                self.assertEqual(route.mode, "general_agriculture_llm")
                self.assertEqual(route.category, "general_agriculture")

    def test_routes_non_agriculture_questions_off_topic(self):
        cases = [
            "Viết bài thơ về tình yêu",
            "Bitcoin hôm nay giá bao nhiêu?",
            "Lập trình React là gì?",
        ]

        for question in cases:
            with self.subTest(question=question):
                route = self.router.route(question)

                self.assertEqual(route.mode, "off_topic")


if __name__ == "__main__":
    unittest.main()
