# FWUDS Java

Bản triển khai Java 21 của hai thuật toán khai phá Frequent Weighted Utility Patterns trên dynamic quantitative data streams:

- `FWUDS-CT`: Circular Tidset.
- `FWUDS-DWT`: DSWUN-tree và WUNList.

## Yêu cầu

1. Cài JDK 21 (khuyến nghị Eclipse Temurin 21).
2. Đặt `JAVA_HOME` trỏ tới JDK và kiểm tra `java -version`.
3. Maven không cần cài toàn hệ thống; wrapper sẽ tải Maven 3.9.9 và kiểm tra checksum ở lần chạy đầu.

```powershell
.\mvnw.cmd verify
.\mvnw.cmd package
```

## Chưa biết bắt đầu từ đâu?

Đọc [docs/START_HERE.md](docs/START_HERE.md). Lộ trình này bắt đầu bằng ví dụ sáu transaction, sau đó giải thích công thức, code và dataset lớn theo từng bước.

Chạy learning demo:

```powershell
.\scripts\run-learning-demo.ps1
```

Demo chạy `oracle`, `ct` và `dwt` trên sample, so sánh kết quả và ghi CSV vào `results/learning-demo/`.

## Chạy ví dụ

`min-wus` là tỷ lệ trong `[0,1]`, không phải phần trăm.

```powershell
java -jar target/fwuds-java-1.0.0-SNAPSHOT-all.jar mine `
  --algorithm ct `
  --input data/sample/fwuds-example.txt `
  --window-panes 2 `
  --pane-size 2 `
  --weight-batch-size 3 `
  --min-wus 0.5 `
  --seed 42 `
  --output results/example-ct.csv
```

Đổi `--algorithm dwt` để chạy FWUDS-DWT. Dùng `benchmark` với cùng các tham số và thêm `--warmups`, `--repetitions` để tạo CSV đo hiệu năng.

`--max-slides 50` giới hạn đúng 50 lần trượt như bài báo. Sau khi tải dữ liệu, chạy toàn bộ preset paper-like bằng `scripts/run-paper-benchmarks.ps1`; mỗi cấu hình/thuật toán được khởi chạy trong một JVM riêng.

Nên chia lượt chạy dài theo dataset/threshold và dùng timeout. `-SkipExisting` chỉ bỏ qua CSV đã có ít nhất số repetition yêu cầu:

```powershell
$env:JAVA_TOOL_OPTIONS='-Xms1g -Xmx8g'
.\scripts\run-paper-benchmarks.ps1 -Dataset Retail -Algorithm dwt `
  -MinWus 0.003 -Warmups 1 -Repetitions 5 -TimeoutMinutes 60 -SkipExisting
```

## Dataset

```powershell
.\scripts\download-datasets.ps1
java -jar target/fwuds-java-1.0.0-SNAPSHOT-all.jar datasets verify
```

Có thể chỉ tải một phần: `-Name Retail,Kosarak`. Dữ liệu được lưu trong `data/raw/` và không được commit. Do chứng chỉ của FIMI repository đã hết hạn tại thời điểm kiểm tra, script dùng mirror SPMF chính thức cho Retail/Accidents/Kosarak và mirror benchmark Apache-2.0 cho BMS-POS; script chuẩn hóa BMS-POS về một transaction mỗi dòng. Susy được dịch từ ID zero-based sang ID dương và item lặp trong một dòng được gộp thành `item:quantity`; xem quyết định chi tiết trong `docs/implementation_requirements.md`.

Đọc [bối cảnh bài báo](docs/paper_context.md), [yêu cầu triển khai](docs/implementation_requirements.md), [quy trình validation](docs/validation.md) và [giới hạn tái hiện](docs/reproducibility_limitations.md) trước khi diễn giải kết quả.

Để hiểu code theo thứ tự, đọc thêm [code walkthrough](docs/code_walkthrough.md), [giải thích thuật toán](docs/algorithm_guide.md) và [hướng dẫn chạy thực nghiệm](docs/running_experiments.md).
