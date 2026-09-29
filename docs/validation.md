# Validation

## Tính đúng

`BruteForceMiner` liệt kê mọi itemset trên universe tối đa 24 item và tính `wus` trực tiếp từ transactions. Đây là oracle độc lập với CTset và WUNList.

Các kiểm thử bắt buộc:

1. Ví dụ bài báo: W1 có CTset(A) `{1,3,4}`, sau khi trượt W2 có `{3,4,2}`; CTset(C) của W2 là `{3,4,1,2}`; `wus(A,W2) ≈ 0.85`.
2. Với W1 và W2, tập pattern và `wus` của FWUDS-CT/FWUDS-DWT bằng oracle trong `1e-12`.
3. Nhiều stream nhỏ sinh từ seed cố định kiểm tra wrap-around nhiều lần, thay weight batch và item xuất hiện muộn.
4. Parser từ chối transaction rỗng, item trùng, item/quantity không dương và pane không đủ kích thước.

Chạy:

```powershell
.\mvnw.cmd verify
```

## Benchmark

- Chạy một warm-up và tối thiểu năm repetition đo.
- Giữ cố định dataset, seed, minwus, window panes, pane size, weight-batch size, JDK và heap options.
- So sánh `result_sha256` giữa repetition và giữa hai thuật toán trước khi diễn giải runtime. Hash chuẩn hóa theo `(window, itemset)` và không chứa bit thô của `double`, vì hai thứ tự cộng hợp lệ có thể tạo sai số nhỏ hơn tolerance dù cùng tập pattern.
- Chạy từng algorithm trong JVM riêng khi lập bảng báo cáo chính thức để peak heap không bị ảnh hưởng bởi lần chạy trước.
- Preset `config/paper-benchmarks.csv` dùng `--max-slides 50`, tức cửa sổ đầu cộng đúng 50 cửa sổ sau khi trượt.
- Báo median và khoảng min–max; không dùng một lần chạy đơn lẻ.

## Tiêu chí chấp nhận

- `mvn verify` thành công.
- Hai thuật toán bằng oracle trên mọi cửa sổ test.
- CLI tạo CSV hợp lệ và hai lần chạy cùng seed cho cùng result hash.
- `datasets verify` khớp số transaction/item trong `data/datasets.csv` và checksum cục bộ tạo lúc tải.

## Nhật ký xác nhận cục bộ 2026-09-30

- JDK Eclipse Temurin `21.0.12.1`, Windows 11, heap benchmark `-Xms1g -Xmx8g`.
- `mvn verify`: 10 test, không có failure/error.
- `datasets verify`: Retail 88.162/16.470, Accidents 340.183/468, BMS-POS 515.597/1.657, Kosarak 990.002/41.270 và Susy 5.000.000/179 đều `OK` cùng checksum trong manifest.
- Pilot Retail/DWT, 51 cửa sổ, một warm-up và một repetition: `minwus=0.003` khoảng 22,6 s; `0.002` khoảng 56,2 s; `0.001` khoảng 276,0 s. Hai ngưỡng thấp hơn chưa hoàn tất trong pilot vì thời gian tăng nhanh.
- Kiểm tra chéo Retail tại `minwus=0.003`, không warm-up: CT và DWT cùng 71.688 pattern và cùng hash `3abbdc7005b46a0bfebbedbb2091e7c2a4fed08abe86e62b1073518dfb760f2a` trên 51 cửa sổ. Runtime đơn lẻ chỉ dùng để kiểm tra luồng chạy, không phải kết quả benchmark chính thức.
- Ma trận 5 dataset × 5 threshold × 2 thuật toán × 5 repetition chưa được tuyên bố hoàn tất. Chạy theo từng threshold với `-TimeoutMinutes` và `-SkipExisting`; không commit CSV trong `results/`.
