# Code walkthrough

Chương này trả lời câu hỏi: khi chạy một lệnh, Java đi qua những class nào và state của thuật toán nằm ở đâu?

## 1. Bắt đầu từ CLI

Entry point là `org.fwuds.cli.FwudsApplication`:

```text
FwudsApplication
 ├─ mine      → MineCommand
 ├─ benchmark → BenchmarkCommand
 └─ datasets  → DatasetVerifyCommand
```

`MineCommand` ghi từng `PatternResult` ra CSV. `BenchmarkCommand` chỉ ghi summary mỗi repetition để không làm benchmark chậm vì I/O pattern. Cả hai đều dùng `MiningRun.execute`.

### `MiningRun.execute` theo thứ tự

1. Reset peak heap measurement.
2. Tạo `TransactionFileReader`.
3. Tạo `PaneReader` với `TransactionWeigher` và `DynamicWeightGenerator`.
4. Đọc đủ `window-panes` pane đầu tiên.
5. Dùng `MinerFactory` tạo `oracle`, `ct` hoặc `dwt`.
6. Đo `miner.initialize(initialWindow)`.
7. Đọc pane mới từng bước và đo `miner.slide(newestPane)`.
8. Gửi từng `WindowResult` cho command đang gọi.
9. Trả về số window, số pattern, runtime, peak heap và hash tập itemset.

Đọc file và tạo weight nằm ngoài vùng runtime mining. Đây là chủ ý để các thuật toán được so sánh trên cùng input và cùng seed.

## 2. Model layer

### `QuantitativeTransaction`

Đại diện input trước khi gắn `twu`. Constructor clone array, sort item tăng dần và từ chối item/quantity không dương hoặc item trùng.

### `WeightedTransaction`

Đại diện transaction sau khi `TransactionWeigher` tính `twu`. Method `contains(Itemset)` dùng hai con trỏ vì cả transaction và `Itemset` đã được canonicalize tăng dần.

### `Itemset`

Là value object immutable. Nó sort item, từ chối duplicate, cung cấp `union`, `equals`, `hashCode`, `compareTo`. Mọi thuật toán đều dựa vào invariant này để candidate không phụ thuộc thứ tự sinh.

### `Pane`, `MiningConfig`, `WindowResult`, `PatternResult`

- `Pane`: danh sách transaction có kích thước chính xác bằng `pane-size`.
- `MiningConfig`: window, pane, batch, threshold và seed.
- `WindowResult`: một window index, danh sách pattern và `sumTwu`.
- `PatternResult`: itemset và `wus`.

## 3. Input layer

```text
TransactionFileReader.read()
    → QuantitativeTransaction
PaneReader.read()
    → List<WeightedTransaction>
    → Pane
```

`TransactionFileReader` bỏ qua dòng trống và comment `#`, `%`, `@`. Token không có `:` nhận quantity 1. Token `id:quantity` nhận quantity đã ghi.

`PaneReader` trả `null` nếu EOF xảy ra trước khi đủ pane. Do đó final pane thiếu transaction bị bỏ qua có chủ ý, giữ invariant rằng mọi miner luôn nhận pane đầy đủ.

## 4. Weight layer

`DynamicWeightGenerator` xác định weight từ:

```text
batch = transactionSequence / weightBatchSize
weight = f(seed, batch, item) ∈ [1,10]
```

Điểm quan trọng: weight phụ thuộc batch và item, không phụ thuộc thứ tự iteration của collection. `TransactionWeigher` tính:

```text
twu = Σ(weight(item) × quantity(item)) / số item phân biệt
```

Muốn xem điều này bằng test, đọc `TransactionWeigherTest.usesItemCardinalityRatherThanQuantitySum`.

## 5. Mining interface

`FwudsMiner` có ba operation:

```java
WindowResult initialize(List<Pane> initialWindow);
WindowResult slide(Pane newestPane);
WindowResult current();
```

`initialize` chỉ được gọi một lần và phải nhận đúng số pane. `slide` nhận đúng một pane đầy đủ. `current` trả kết quả hiện tại sau khi miner đã initialize.

`MinerFactory` là nơi biến string CLI thành implementation:

```text
oracle → BruteForceMiner
ct     → FwudsCtMiner
dwt    → FwudsDwtMiner
```

## 6. Đọc `FwudsCtMiner`

State chính:

| Field | Ý nghĩa |
|---|---|
| `capacity` | số transaction của một window |
| `singletonCtsets` | item → các `lcTid` chứa item |
| `twuByLcTid` | `twu` tại local ID hiện tại |
| `currentLcTid` | vị trí cuối cùng đã insert |
| `windowIndex` | số lần slide |
| `current` | `WindowResult` gần nhất |

Đọc theo thứ tự này:

1. `initialize` → `insertPane`.
2. `slide` → `removeOldestPane` → `insertPane` → `mine`.
3. `mine` → singleton candidates → `mineLevel`.
4. `intersect` để xử lý thứ tự thời gian khi ID wrap.

Breakpoint hữu ích:

```text
FwudsCtMiner.insertPane
FwudsCtMiner.removeOldestPane
FwudsCtMiner.intersect
FwudsCtMiner.mineLevel
```

## 7. Đọc `FwudsDwtMiner`

State chính:

| Field | Ý nghĩa |
|---|---|
| `rank` | thứ tự prefix của item |
| `tree` | DSWUN-tree hiện tại |
| `windowIndex` | số lần slide |
| `current` | kết quả gần nhất |

Đọc theo thứ tự:

1. `establishInitialOrder` tính rank từ initial window.
2. `DswunTree.insert` xây path cho từng transaction.
3. `FwudsDwtMiner.slide` append rank cho late item, insert pane mới, rồi evict cũ.
4. `tree.generateWunLists` tạo candidate representation.
5. `intersect` giao descendant/ancestor WUNList và prune bằng remaining-weight bound.

Breakpoint hữu ích:

```text
FwudsDwtMiner.establishInitialOrder
FwudsDwtMiner.slide
DswunTree.insert
DswunTree.evictOldest
DswunTree.generateWunLists
FwudsDwtMiner.intersect
```

## 8. Đọc test trước khi sửa code

| Test | Điều cần học |
|---|---|
| `TransactionFileReaderTest` | format input và validation |
| `TransactionWeigherTest` | công thức `twu` và tính deterministic |
| `FwudsAlgorithmsTest` | paper example và CTset sau slide |
| `RandomizedEquivalenceTest` | CT/DWT phải khớp oracle trên từng window |
| `MiningRunTest` | hash output ổn định giữa CT và DWT |
| `FwudsApplicationTest` | CLI tạo CSV đúng header |

Khi thay đổi miner, chạy `.\mvnw.cmd verify` trước. Nếu test randomized fail, không nên tin benchmark dù chương trình vẫn chạy.

## 9. Bài tập đọc code

1. Thay `--min-wus` trên sample và quan sát số pattern.
2. Đổi `--max-slides 0` thành `1`; đếm số window trong CSV.
3. Đổi seed từ `42` sang `43`; kiểm tra `twu` và pattern có thể thay đổi.
4. Đặt breakpoint vào `FwudsCtMiner.intersect` và ghi lại CTset trước/sau wrap.
5. So sánh số lần gọi `slide` với số pane được đọc.

## 10. Quy tắc an toàn khi chỉnh sửa

- Giữ public API trong `org.fwuds.model` và `org.fwuds.mining`.
- Mọi tối ưu hóa phải so với `BruteForceMiner` trên từng window.
- Không đưa raw dataset, CSV benchmark hoặc `target/` vào Git.
- Nếu paper không định nghĩa một edge case, ghi quyết định trong `docs/implementation_requirements.md`.
