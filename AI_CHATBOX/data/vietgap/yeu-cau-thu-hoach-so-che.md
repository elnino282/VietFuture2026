---
doc_id: vietgap__thu-hoach-so-che
title: Yêu cầu về thu hoạch, sơ chế và bảo quản trong VietGAP
category: vietgap
audience: farmer
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [vietgap, thu-hoach, so-che, bao-quan, nhap-kho, lo-hang, batch]
source_refs: [src_tcvn_11892_vsqi, src_tcvn_11892_tcvn]
---

# Yêu cầu về thu hoạch, sơ chế và bảo quản trong VietGAP

## Mục đích
Tài liệu này hướng dẫn kiểm soát các bước sau sản xuất: thu hoạch, sơ chế, bảo quản, nhập kho và vận chuyển.

Mục tiêu là giảm nguy cơ nhiễm bẩn, nhiễm chéo và mất khả năng truy xuất nguồn gốc sau khi nông sản rời khỏi đồng ruộng.

## Đối tượng sử dụng
- Nông dân và nhân công thu hoạch.
- Nhân viên sơ chế, đóng gói, nhập kho.
- Chủ nông trại hoặc hợp tác xã.
- Quản trị viên FarmTrace quản lý lô hàng và QR truy xuất.

## Yêu cầu chính khi thu hoạch
Nông sản sau thu hoạch cần được bảo vệ khỏi đất bẩn, nước bẩn, hóa chất và nguồn nhiễm chéo.

Nông dân cần:

- Chỉ thu hoạch khi đã kiểm tra nhật ký sử dụng thuốc bảo vệ thực vật và thời gian cách ly.
- Dùng dao, kéo, sọt, khay hoặc dụng cụ sạch.
- Không đặt nông sản trực tiếp xuống nền đất bẩn.
- Không dùng bao bì từng chứa phân bón, thuốc BVTV hoặc hóa chất để đựng nông sản.
- Nhân công cần rửa tay và giữ vệ sinh cá nhân khi thu hoạch.
- Ghi ngày thu hoạch, lô đất, mùa vụ và sản lượng thực tế.

## Yêu cầu chính khi sơ chế
Sơ chế gồm các hoạt động như phân loại, cắt tỉa, rửa, làm ráo, đóng gói hoặc chuẩn bị nhập kho.

Khu sơ chế nên đảm bảo:

- Có khu vực tương đối sạch, dễ vệ sinh.
- Có dụng cụ sơ chế riêng cho nông sản.
- Có nguồn nước phù hợp cho việc rửa nông sản nếu có bước rửa.
- Tách biệt hàng thô, hàng đã làm sạch và rác thải.
- Hạn chế vật nuôi, côn trùng và bụi bẩn xâm nhập.
- Không sử dụng hóa chất bảo quản hoặc chất xử lý không rõ nguồn gốc.

## Yêu cầu chính khi bảo quản và vận chuyển
Nông sản cần được bảo quản và vận chuyển trong điều kiện phù hợp với từng loại sản phẩm.

Nên thực hiện:

- Xếp nông sản lên kệ, pallet, khay hoặc thùng sạch.
- Tránh để nông sản tiếp xúc trực tiếp với nền bẩn hoặc nơi ẩm đọng.
- Không vận chuyển chung nông sản với thuốc BVTV, phân bón, hóa chất hoặc vật nuôi.
- Vệ sinh thùng xe hoặc dụng cụ chứa trước khi vận chuyển.
- Ghi nhận ngày nhập kho, số lượng, trạng thái và người phụ trách.

## Luồng dữ liệu khuyến nghị trong FarmTrace
1. Kiểm tra mùa vụ và lô đất trước thu hoạch.
2. Kiểm tra nhật ký thuốc BVTV và thời gian cách ly.
3. Tạo nhật ký thu hoạch.
4. Ghi sản lượng thu hoạch ban đầu.
5. Tạo lô hàng sau thu hoạch hoặc Batch ID.
6. Ghi hoạt động sơ chế nếu có.
7. Nhập kho thành phẩm hoặc bán trực tiếp.
8. Liên kết Batch ID với mã QR truy xuất.

## Dữ liệu cần nhập
| Trường dữ liệu | Mô tả | Bắt buộc khuyến nghị |
|---|---|---|
| Ngày thu hoạch | Ngày thực hiện thu hoạch | Có |
| Lô đất/mùa vụ | Nơi sản phẩm được thu hoạch | Có |
| Sản phẩm/cây trồng | Loại nông sản | Có |
| Sản lượng thu hoạch | Khối lượng hoặc số lượng | Có |
| Người thu hoạch | Người hoặc nhóm thực hiện | Nên có |
| Trạng thái sản phẩm | Tươi, hư hỏng, loại 1, loại 2 hoặc ghi chú khác | Nên có |
| Hoạt động sơ chế | Rửa, cắt tỉa, phân loại, đóng gói | Nên có nếu có sơ chế |
| Sản lượng nhập kho | Khối lượng sau sơ chế hoặc sau phân loại | Có nếu nhập kho |
| Batch ID | Mã lô hàng sau thu hoạch | Có |

## Hồ sơ hoặc nhật ký cần chuẩn bị
- Nhật ký thu hoạch.
- Nhật ký sơ chế nếu có.
- Phiếu nhập kho hoặc dữ liệu nhập kho.
- Nhật ký vệ sinh dụng cụ/khu sơ chế nếu có.
- Phiếu xuất kho hoặc biên bản giao hàng nếu sản phẩm được bán.
- Dữ liệu QR liên kết với lô hàng.

## Lỗi thường gặp
- Thu hoạch nhưng không kiểm tra thời gian cách ly.
- Gộp sản phẩm từ nhiều lô đất nhưng không ghi rõ nguồn gốc từng phần.
- Sử dụng sọt, bao bì hoặc xe chở không sạch.
- Để nông sản tiếp xúc trực tiếp với nền đất.
- Không ghi sản lượng hao hụt sau sơ chế.
- Nhập kho chung hàng đạt và hàng lỗi mà không phân loại.

## Checklist nhanh
- [ ] Đã kiểm tra nhật ký thuốc BVTV trước thu hoạch.
- [ ] Dụng cụ thu hoạch sạch và không từng chứa hóa chất.
- [ ] Nông sản không đặt trực tiếp xuống nền đất bẩn.
- [ ] Có nhật ký thu hoạch theo lô/mùa vụ.
- [ ] Có Batch ID cho lô hàng sau thu hoạch.
- [ ] Có ghi sản lượng trước và sau sơ chế nếu có hao hụt.
- [ ] Có phiếu nhập kho hoặc dữ liệu nhập kho.
- [ ] QR truy xuất liên kết đúng với lô hàng.

## Câu hỏi thường gặp

### Câu hỏi: Sau thu hoạch có cần nhập kho không?
Trả lời: Nên nhập kho nếu sản phẩm chưa bán ngay. Nhập kho giúp hệ thống ghi nhận sản lượng, trạng thái lô hàng và phục vụ truy xuất nguồn gốc.

### Câu hỏi: Có thể trộn sản phẩm từ nhiều lô đất vào một lô hàng không?
Trả lời: Có thể nếu hệ thống ghi rõ nguồn gốc từng lô và tỷ lệ hoặc sản lượng tương ứng. Nếu không ghi, khả năng truy xuất sẽ bị yếu.

### Câu hỏi: Sơ chế có bắt buộc phải có nhà xưởng lớn không?
Trả lời: Không nên khẳng định chung. Điều quan trọng là khu sơ chế phải đủ điều kiện vệ sinh, tránh nhiễm chéo và phù hợp quy mô sản xuất.

## Giới hạn áp dụng
Tài liệu này là hướng dẫn tổng hợp cho FarmTrace RAG, không thay thế yêu cầu cụ thể của tổ chức chứng nhận hoặc quy định kỹ thuật áp dụng cho từng sản phẩm.
