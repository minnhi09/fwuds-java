# Kế hoạch FWUDS Java

- [x] Khởi tạo Maven/Java 21, model, parser và CLI.
- [x] Cài oracle vét cạn và FWUDS-CT.
- [x] Cài DSWUN-tree, WUNList và FWUDS-DWT.
- [x] Thêm dataset manifest, download script và tài liệu nghiên cứu.
- [x] Chạy `mvn verify` trên JDK 21 và sửa mọi sai khác với oracle.
- [x] Smoke-test CLI/benchmark và luồng tải/chuẩn hóa Retail, BMS-POS.
- [x] Tải, chuẩn hóa và xác minh đủ 5 dataset.
- [x] Pilot Retail/DWT và kiểm tra hash chéo CT/DWT trên dữ liệu thật.
- [x] Biên soạn lộ trình học, giải thích công thức/thuật toán/code và hướng dẫn chạy thực nghiệm.
- [x] Thêm learning demo tự động, standard CLI help và Javadoc cho public entry points.
- [ ] Chạy đủ benchmark paper-like dài hạn (chia theo dataset/threshold; xem `docs/validation.md`).
- [ ] Bổ sung `dFWUNL-stream` sau khi có tài liệu tham khảo [9] hoặc source code của tác giả.
