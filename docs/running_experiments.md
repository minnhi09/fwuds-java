# Chạy chương trình và thực nghiệm

Đây là tài liệu thao tác. Đọc `START_HERE.md` và chạy learning demo trước khi dùng dataset lớn.

## 1. Kiểm tra môi trường

Project dùng JDK 21 và Maven Wrapper, nên không cần cài Maven toàn hệ thống.

```powershell
java -version
$env:JAVA_HOME
.\mvnw.cmd -version
```

Nếu `java -version` không phải 21, sửa `JAVA_HOME`/PATH rồi mở PowerShell mới. Build và test:

```powershell
.\mvnw.cmd verify
```

Lệnh này compile code, chạy JUnit, tạo shaded JAR tại:

```text
target/fwuds-java-1.0.0-SNAPSHOT-all.jar
```

## 2. Learning demo

Lệnh khuyến nghị đầu tiên:

```powershell
.\scripts\run-learning-demo.ps1
```

Script dùng sample sáu transaction, chạy cả `oracle`, `ct`, `dwt`, tạo CSV trong `results/learning-demo/` và so sánh output. Dùng `-SkipBuild` nếu vừa chạy `verify`:

```powershell
.\scripts\run-learning-demo.ps1 -SkipBuild
```

Không dùng learning demo để đo hiệu năng. Nó tồn tại để chứng minh luồng dữ liệu và correctness.

## 3. Chạy `mine` thủ công

Ví dụ sample với cửa sổ gồm 2 pane, mỗi pane 2 transaction:

```powershell
java -jar target\fwuds-java-1.0.0-SNAPSHOT-all.jar mine `
  --algorithm ct `
  --input data\sample\fwuds-example.txt `
  --window-panes 2 `
  --pane-size 2 `
  --weight-batch-size 3 `
  --min-wus 0.5 `
  --seed 42 `
  --max-slides 1 `
  --output results\manual-ct.csv
```

Đổi `--algorithm ct` thành `dwt` hoặc `oracle`. `oracle` chỉ dùng cho input nhỏ.

Các tham số có nghĩa:

| Option | Ý nghĩa |
|---|---|
| `--algorithm` | `ct`, `dwt` hoặc `oracle` |
| `--input` | file transaction FIMI/SPMF |
| `--window-panes` | số pane trong một window |
| `--pane-size` | số transaction mỗi pane |
| `--weight-batch-size` | số sequence dùng chung weight batch |
| `--min-wus` | threshold dạng fraction, ví dụ `0.5` = 50% |
| `--seed` | seed deterministic, mặc định `42` |
| `--max-slides` | số slide sau window đầu; `-1` đọc đến EOF |
| `--output` | CSV pattern output |

CSV của `mine` có một dòng cho mỗi pattern trong mỗi window:

```text
dataset,algorithm,seed,window,pattern,wus,window_runtime_nanos,jvm
```

`window_runtime_nanos` chỉ là thời gian xử lý window đó; đọc input và tạo weight không nằm trong cột này.

## 4. Dataset lớn

Tải dataset theo manifest:

```powershell
.\scripts\download-datasets.ps1
java -jar target\fwuds-java-1.0.0-SNAPSHOT-all.jar datasets verify
```

Có thể tải riêng:

```powershell
.\scripts\download-datasets.ps1 -Name Retail,Kosarak
```

`datasets verify` kiểm tra transaction count, item count và SHA-256. Dataset nằm trong `data/raw/` và bị Git ignore.

Lưu ý về Susy: mirror có ID zero-based và item lặp trong một dòng. Script dịch ID sang số dương và gộp lặp thành `item:quantity`, nên manifest của project ghi số item quan sát được sau chuẩn hóa. Đây là quyết định dữ liệu, không phải thay đổi âm thầm trong Java miner.

Không mở Susy/Retail bằng editor thông thường; hãy dùng `datasets verify` hoặc các script thống kê.

## 5. Chạy một dataset thật có giới hạn

Luôn thử một threshold và ít repetition trước:

```powershell
$env:JAVA_TOOL_OPTIONS='-Xms1g -Xmx8g'
.\scripts\run-paper-benchmarks.ps1 `
  -Dataset Retail `
  -Algorithm dwt `
  -MinWus 0.003 `
  -Warmups 1 `
  -Repetitions 1 `
  -TimeoutMinutes 60
```

Các tham số thêm của script:

- `-Dataset Retail`: chỉ chạy một dataset.
- `-MinWus 0.003,0.002`: chỉ chạy threshold được chọn.
- `-Algorithm ct,dwt`: chạy cả hai implementation.
- `-Warmups 1 -Repetitions 5`: cấu hình phù hợp cho số liệu chính thức.
- `-TimeoutMinutes 60`: dừng một JVM nếu vượt ngân sách thời gian.
- `-SkipExisting`: bỏ qua CSV đã có đủ số repetition yêu cầu.

Chạy từng threshold giúp bạn dừng một cấu hình khó mà không mất các cấu hình đã hoàn tất. Kết quả nằm trong `results/` và không commit.

## 6. Đọc benchmark CSV

Benchmark có header:

```text
dataset,algorithm,seed,run,windows,patterns,
runtime_nanos,peak_heap_bytes,result_sha256,jvm,os
```

- `windows`: phải là 51 khi preset dùng window đầu + 50 slide.
- `patterns`: tổng số pattern trên mọi window, không phải số pattern unique toàn dataset.
- `runtime_nanos`: tổng thời gian initialize/slide trong logical run.
- `peak_heap_bytes`: peak Java heap được MXBean quan sát.
- `result_sha256`: hash canonical của `(window, itemset)`, dùng kiểm tra các run/algorithm trả cùng tập pattern; không phải hash của bit raw `double`.

Với repetition, kiểm tra trước:

1. Mọi run cùng seed có cùng `result_sha256`.
2. CT và DWT cùng hash khi cùng input/config.
3. Sau đó mới tính median và min–max runtime.

Không so sánh trực tiếp số runtime/memory với biểu đồ paper như một reproduction tuyệt đối: paper dùng môi trường, random weight và baseline khác.

## 7. Quy trình thực nghiệm được khuyến nghị

```text
sample + oracle
      │
      ▼
sample + ct/dwt equivalence
      │
      ▼
dataset verify
      │
      ▼
one dataset + one threshold + one repetition
      │
      ▼
one dataset + all thresholds + 5 repetitions
      │
      ▼
dataset tiếp theo
```

Không bắt đầu bằng toàn bộ 5 dataset × 5 threshold × 2 algorithm × 5 repetition. Pilot Retail cho thấy threshold thấp có thể tăng runtime rất mạnh.

## 8. Lỗi thường gặp

### `mvnw.cmd` bị chặn bởi ExecutionPolicy

Dùng `mvnw.cmd` thay vì gọi script PowerShell wrapper, hoặc mở PowerShell với policy phù hợp theo chính sách máy của bạn. Không cần cài Maven mới.

### Thiếu JAR

Chạy:

```powershell
.\mvnw.cmd package
```

### Thiếu file dataset

Kiểm tra `data/raw` và chạy `download-datasets.ps1`, sau đó `datasets verify`.

### OOM/chạy quá lâu

Giảm `--max-slides`, tăng `--min-wus`, dùng dataset nhỏ hơn, giảm heap nếu cần cho hệ điều hành, hoặc dùng `-TimeoutMinutes`. Ghi nhận cấu hình bị timeout; không tự đổi tham số paper rồi gọi đó là kết quả tương đương.

### CT/DWT khác hash

Không đoán ngay rằng một thuật toán sai. Kiểm tra cùng seed, cùng input, cùng pane/window/batch/threshold; chạy `mvnw verify` và thử lại trên sample. Nếu pattern key khác, đó là lỗi correctness cần điều tra bằng oracle.

## 9. Kết quả có thể kết luận

Bạn có thể kết luận implementation trả cùng FWUP và `wus` với oracle trên các test window đã kiểm tra. Bạn có thể so sánh xu hướng CT/DWT trong cùng máy Java. Bạn không được kết luận Java đã tái tạo chính xác runtime/memory tuyệt đối của paper nếu chưa có weight matrix, source baseline và môi trường gốc.
