# Hướng dẫn cho agent và contributor

- Nguồn chuẩn của thuật toán là `docs/paper/fwuds-paper.pdf`. Khi mã và bài báo khác nhau, ghi rõ quyết định trong `docs/implementation_requirements.md`.
- Dự án dùng Java 21 và Maven Wrapper. Chạy toàn bộ kiểm tra bằng `./mvnw verify` hoặc `.\mvnw.cmd verify`.
- Không commit dataset gốc, kết quả benchmark, `target/` hoặc tệp sinh tạm.
- Mọi tối ưu hóa thuật toán phải tiếp tục vượt qua kiểm thử tương đương với `BruteForceMiner` trên từng sliding window.
- Giữ public API trong `org.fwuds.model` và `org.fwuds.mining` tương thích, trừ khi tài liệu kế hoạch nêu rõ migration.
- Tài liệu viết bằng tiếng Việt; giữ nguyên tên thuật toán, cấu trúc dữ liệu, class và thuật ngữ nghiên cứu bằng tiếng Anh.
- Không tuyên bố tái hiện số runtime/memory tuyệt đối của bài báo; xem `docs/reproducibility_limitations.md`.
