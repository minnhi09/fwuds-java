# Bắt đầu với FWUDS Java

Tài liệu này dành cho người biết Java cơ bản nhưng chưa quen đọc một project nghiên cứu. Mục tiêu đầu tiên không phải là chạy ngay dataset 5 triệu transaction. Mục tiêu là tạo được một kết quả nhỏ, hiểu kết quả đó đến từ đâu, rồi mới tăng kích thước dữ liệu.

## Nếu chỉ có 10 phút

Mở PowerShell tại thư mục `fwuds-java` và chạy:

```powershell
.\mvnw.cmd verify
.\scripts\run-learning-demo.ps1 -SkipBuild
```

Nếu cuối cùng thấy `LEARNING DEMO: PASS`, code, `BruteForceMiner`, `FWUDS-CT` và `FWUDS-DWT` đã cùng trả về một tập pattern trên ví dụ nhỏ. Các CSV sinh ra nằm trong `results/learning-demo/`; đây là kết quả tạm thời và không commit.

Sau đó đọc theo thứ tự:

1. Phần **Luồng chạy** dưới đây.
2. [algorithm_guide.md](algorithm_guide.md).
3. [code_walkthrough.md](code_walkthrough.md).
4. [running_experiments.md](running_experiments.md).

## Repository này dùng để làm gì?

Đây là bản triển khai Java 21 của hai thuật toán khai phá `Frequent Weighted Utility Patterns` trên `dynamic quantitative data streams`:

- `FWUDS-CT`: dùng `Circular Tidset`.
- `FWUDS-DWT`: dùng `DSWUN-tree` và `WUNList`.
- `BruteForceMiner`: oracle vét cạn dùng để kiểm tra tính đúng trên stream nhỏ.

Một dòng input là một transaction. Một token như `3` có quantity bằng 1; `3:2` nghĩa là item 3 có quantity 2. Stream được gom thành `pane`; một cửa sổ chứa nhiều pane. Sau mỗi lần trượt, pane cũ nhất bị bỏ và pane mới được thêm vào.

## Bản đồ thư mục

| Đường dẫn | Vai trò |
|---|---|
| `src/main/java/org/fwuds/model` | Dữ liệu bất biến: `Itemset`, transaction, `Pane`, kết quả và cấu hình |
| `src/main/java/org/fwuds/io` | Đọc transaction và gom transaction thành pane |
| `src/main/java/org/fwuds/util` | Sinh dynamic weight và tính `twu` |
| `src/main/java/org/fwuds/mining` | Interface miner, oracle và hai thuật toán FWUDS |
| `src/main/java/org/fwuds/cli` | Các lệnh `mine`, `benchmark`, `datasets` |
| `src/test/java` | Test công thức, paper example, oracle equivalence và CLI |
| `data/sample` | Ví dụ nhỏ để học; có thể mở bằng editor |
| `data/raw` | Dataset lớn, chỉ đọc và không commit |
| `results` | CSV output, không commit |
| `docs/paper` | PDF nguồn chuẩn của nghiên cứu |

## Luồng chạy tổng quát

```text
FIMI/SPMF file
    │
    ▼
TransactionFileReader
    │  QuantitativeTransaction
    ▼
TransactionWeigher + DynamicWeightGenerator
    │  WeightedTransaction (có twu)
    ▼
PaneReader
    │  Pane
    ▼
MiningRun ──► MinerFactory ──► oracle / ct / dwt
                                │
                                ▼
                         WindowResult
                                │
                                ▼
                         pattern CSV + summary
```

Điểm cần nhớ: thời gian đọc file không nằm trong thời gian mining. `MiningRun` đọc và weigh input trước, sau đó đo `initialize` và từng `slide` riêng.

## Lộ trình học đề xuất

### Buổi 1 — Chạy và quan sát

- Chạy `mvnw verify`.
- Chạy learning demo.
- Mở ba CSV trong `results/learning-demo/`.
- So sánh cột `window`, `pattern`, `wus`.

### Buổi 2 — Hiểu dữ liệu và công thức

- Đọc [algorithm_guide.md](algorithm_guide.md), phần notation.
- Mở `data/sample/fwuds-example.txt`.
- Tự tính một transaction `twu`, một `sumtwu` và một `wus`.
- Đối chiếu fixture `PaperExample` trong test.

### Buổi 3 — Lần theo code

- Đọc [code_walkthrough.md](code_walkthrough.md).
- Đặt breakpoint tại `MiningRun.execute`.
- Đi qua `TransactionFileReader.read`, `TransactionWeigher.weigh`, `PaneReader.read`.
- Quan sát `WindowResult` sau `initialize` và sau một `slide`.

### Buổi 4 — FWUDS-CT

- Đọc phần `Circular Tidset`.
- Theo dõi `currentLcTid`, `singletonCtsets` và `intersect`.
- Chạy test `circularTidsetsMatchPaperAcrossSlide`.

### Buổi 5 — FWUDS-DWT

- Đọc phần `DSWUN-tree` và `WUNList`.
- Theo dõi `rank`, `pre/post`, insert, eviction và intersection.
- Chạy test tương đương với oracle.

### Buổi 6 — Dataset và thực nghiệm

- Chạy `datasets verify`.
- Chạy `mine` trên một phần nhỏ hoặc giới hạn `--max-slides`.
- Đọc [running_experiments.md](running_experiments.md) trước khi chạy benchmark.
- Không diễn giải runtime/memory Java như bản sao tuyệt đối của paper.

## Ba khái niệm hay bị nhầm

- `pane-size`: số transaction trong một pane.
- `window-panes`: số pane trong một cửa sổ, nên cửa sổ có `window-panes × pane-size` transaction.
- `max-slides`: số lần trượt sau cửa sổ đầu. `--max-slides 1` tạo hai window result: window đầu và một window sau đó.

## Khi chương trình báo lỗi

- `Input does not contain a complete initial window`: file không đủ `window-panes × pane-size` transaction.
- `Duplicate item`: một transaction có cùng item hai lần nhưng chưa được biểu diễn thành quantity.
- `Expected pane size`: pane cuối không đủ transaction; pane thiếu bị bỏ qua có chủ ý.
- Chạy chậm hoặc hết heap: quay lại sample, tăng `min-wus`, giảm `window-panes`, giảm `max-slides`, hoặc chia benchmark theo từng threshold.

## Sau khi hoàn tất lộ trình

Bạn nên có khả năng trả lời bốn câu hỏi:

1. `wus` được tính từ những transaction nào?
2. CTset và WUNList lưu thông tin gì để tránh quét lại toàn bộ stream?
3. Vì sao `BruteForceMiner` là oracle chứ không phải miner để benchmark dataset lớn?
4. Một dòng CSV benchmark chứng minh điều gì, và không chứng minh điều gì?

Nếu chưa trả lời được, quay lại buổi tương ứng thay vì chạy dataset lớn hơn.
