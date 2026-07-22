# ACM RAG Data

Thư mục này chứa toàn bộ tài liệu dùng để nạp vào hệ thống RAG chatbot ACM.

Chatbot ACM hỗ trợ nông dân, người mua và quản trị viên trong các chủ đề:

* VietGAP
* Quy trình sản xuất nông nghiệp
* Nhật ký sản xuất
* Truy xuất nguồn gốc
* Quản lý nông trại, mùa vụ, kho và sản phẩm
* Hướng dẫn sử dụng hệ thống ACM
* Câu hỏi thường gặp

## Cấu trúc thư mục

```txt
data/
├── README.md
├── sources.jsonl
├── data_guide.md
│
├── vietgap/
├── crops/
├── ACM/
├── traceability/
├── templates/
└── faq/
```

## Ý nghĩa từng thư mục

| Thư mục         | Mục đích                                                                      |
| --------------- | ----------------------------------------------------------------------------- |
| `vietgap/`      | Lưu tài liệu tổng quan, yêu cầu, checklist và hướng dẫn liên quan đến VietGAP |
| `crops/`        | Lưu tài liệu theo từng loại cây trồng như rau ăn lá, cà chua, dưa leo, xoài   |
| `ACM/`    | Lưu hướng dẫn sử dụng các chức năng của hệ thống ACM                    |
| `traceability/` | Lưu tài liệu về truy xuất nguồn gốc, QR, dữ liệu hiển thị và bảo mật QR       |
| `templates/`    | Lưu các mẫu nhật ký sản xuất, phiếu nhập kho, biên bản kiểm tra               |
| `faq/`          | Lưu câu hỏi thường gặp theo từng nhóm người dùng và chủ đề                    |

## Nguyên tắc quản lý dữ liệu

* Mỗi file tài liệu nên dùng định dạng Markdown `.md`.
* Tên file viết không dấu, chữ thường, dùng dấu gạch ngang `-`.
* Mỗi file cần có tiêu đề rõ ràng bằng heading `#`.
* Nội dung nên chia bằng heading `##` và `###`.
* Không để nội dung quá dài trong một đoạn.
* Không đưa thông tin nhạy cảm, mật khẩu, token hoặc dữ liệu cá nhân vào tài liệu.
* Không trộn metadata kỹ thuật như đường dẫn file, chunk ID, vector ID vào nội dung trả lời cho chatbot.
* Nếu tài liệu được tổng hợp nội bộ, cần ghi rõ là tài liệu tổng hợp nội bộ.
* Nếu tài liệu dựa trên nguồn bên ngoài, cần ghi nguồn trong `sources.jsonl`.

## Quy ước đặt tên file

Mẫu đặt tên:

```txt
<chu-de>-<noi-dung-chinh>.md
```

Ví dụ:

```txt
tong-quan-vietgap.md
yeu-cau-dat-nuoc-giong.md
tao-mua-vu.md
ghi-nhat-ky-san-xuat.md
quy-trinh-truy-xuat.md
faq-nong-dan.md
```

## Quy ước nội dung

Mỗi file nên có cấu trúc cơ bản:

```md
# Tiêu đề tài liệu

## Mục đích

## Đối tượng sử dụng

## Nội dung chính

## Các bước thực hiện

## Lưu ý

## Câu hỏi thường gặp
```

Không bắt buộc file nào cũng có đủ tất cả mục trên, nhưng cần đảm bảo nội dung rõ ràng, dễ tách chunk và dễ truy xuất.

## Đối tượng sử dụng

Các giá trị đối tượng nên dùng thống nhất:

| Giá trị  | Ý nghĩa                        |
| -------- | ------------------------------ |
| `farmer` | Nông dân / chủ nông trại       |
| `buyer`  | Người mua / người quét QR      |
| `admin`  | Quản trị viên hệ thống         |
| `all`    | Dùng chung cho nhiều đối tượng |

## Nhóm tài liệu chính

| Nhóm           | Mô tả                                |
| -------------- | ------------------------------------ |
| `vietgap`      | Kiến thức và checklist VietGAP       |
| `crop`         | Quy trình theo cây trồng             |
| `ACM`    | Hướng dẫn sử dụng hệ thống ACM |
| `traceability` | Truy xuất nguồn gốc và QR            |
| `template`     | Mẫu biểu, nhật ký, phiếu ghi nhận    |
| `faq`          | Câu hỏi thường gặp                   |

## Quy trình cập nhật dữ liệu

Khi thêm tài liệu mới:

1. Tạo file `.md` đúng thư mục.
2. Đặt tên file không dấu, rõ nghĩa.
3. Viết nội dung theo chuẩn trong `data_guide.md`.
4. Thêm một dòng metadata tương ứng vào `sources.jsonl`.
5. Kiểm tra nội dung không trùng lặp với file đã có.
6. Chạy lại pipeline ingest để cập nhật vector database.

## Lưu ý khi dùng cho RAG

* Ưu tiên viết câu ngắn, rõ nghĩa.
* Mỗi heading nên chứa một nhóm ý hoàn chỉnh.
* FAQ nên viết theo dạng câu hỏi - trả lời.
* Quy trình nên viết theo từng bước.
* Checklist nên dùng gạch đầu dòng hoặc bảng.
* Không nên copy nguyên văn văn bản dài mà không chia mục.
* Không nên để nhiều chủ đề khác nhau trong cùng một file.
