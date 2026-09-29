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
- So sánh `result_sha256` giữa repetition và giữa hai thuật toán trước khi diễn giải runtime.
- Chạy từng algorithm trong JVM riêng khi lập bảng báo cáo chính thức để peak heap không bị ảnh hưởng bởi lần chạy trước.
- Preset `config/paper-benchmarks.csv` dùng `--max-slides 50`, tức cửa sổ đầu cộng đúng 50 cửa sổ sau khi trượt.
- Báo median và khoảng min–max; không dùng một lần chạy đơn lẻ.

## Tiêu chí chấp nhận

- `mvn verify` thành công.
- Hai thuật toán bằng oracle trên mọi cửa sổ test.
- CLI tạo CSV hợp lệ và hai lần chạy cùng seed cho cùng result hash.
- `datasets verify` khớp số transaction/item trong `data/datasets.csv` và checksum cục bộ tạo lúc tải.
