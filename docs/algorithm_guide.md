# Giải thích bài toán và thuật toán FWUDS

Đây là chương toán và thuật toán. Nếu các ký hiệu còn mới, hãy đọc `START_HERE.md` trước và chạy learning demo.

## 1. Mô hình dữ liệu

Một transaction định lượng có dạng:

```text
item:quantity item:quantity ...
```

Ví dụ:

```text
1:2 3:1 5:1
```

nghĩa là transaction chứa item 1 với quantity 2, item 3 với quantity 1 và item 5 với quantity 1. Item ID phải dương và không được lặp trong một transaction; nếu cần biểu diễn nhiều lần xuất hiện, dùng quantity.

### Dynamic weight

Mỗi item trong mỗi weight batch có một weight dương. Với transaction `t_k`, implementation tính:

```text
twu(t_k) = sum_j(weight(batch(k), item_j) × quantity_j) / |t_k|
```

`|t_k|` là số item phân biệt, không phải tổng quantity. `DynamicWeightGenerator` sinh weight nguyên trong `[1,10]` từ `(seed, batch, item)` bằng SplitMix64-like mixing. Vì paper không công bố seed/PRNG/thứ tự sinh chính xác, đây là quyết định tái lập của project, không phải tuyên bố khớp weight matrix của tác giả.

### Pane và sliding window

Stream được chia tuần tự:

```text
pane 0       pane 1       pane 2       pane 3
[p tx]       [p tx]       [p tx]       [p tx]
└──────────── window 0 ────────────┘
             └────────── window 1 ──────────┘
```

Nếu `pane-size = p` và `window-panes = w`, mỗi window có `w × p` transaction. Một lần `slide` thêm đúng một pane mới và loại đúng pane cũ nhất. Window đầu tiên cũng là một kết quả, vì vậy `--max-slides 1` tạo hai window result.

## 2. Weighted utility của một pattern

Với pattern/itemset `X` và window `W_i`:

```text
sumtwu(W_i) = sum(twu(t)) với mọi transaction t trong W_i

numerator(X, W_i) = sum(twu(t)) với mọi t chứa toàn bộ X

wus(X, W_i) = numerator(X, W_i) / sumtwu(W_i)
```

`X` là `FWUP` nếu:

```text
wus(X, W_i) >= minwus
```

Trong code, so sánh được thực hiện trên numerator với `minwus × sumtwu`, cộng một epsilon nhỏ để tránh sai khác biên do floating point.

## 3. Ví dụ số từ paper fixture

`src/test/java/org/fwuds/support/PaperExample.java` cố định `twu` của sáu transaction để test đúng hình minh họa trong paper:

| Transaction | Items | `twu` |
|---|---|---:|
| `t1` | `1,3,4,5` | `0.48` |
| `t2` | `2,3,4,5` | `0.70` |
| `t3` | `1,3,5` | `0.37` |
| `t4` | `1,3,4` | `0.97` |
| `t5` | `2,3,4` | `0.43` |
| `t6` | `1,3,4,5` | `1.05` |

Window đầu `W1 = {t1,t2,t3,t4}` có:

```text
sumtwu(W1) = 0.48 + 0.70 + 0.37 + 0.97 = 2.52
```

Itemset `{1}` xuất hiện trong `t1,t3,t4`, nên:

```text
wus({1}, W1) = (0.48 + 0.37 + 0.97) / 2.52
              = 1.82 / 2.52
              ≈ 0.722222
```

Sau một slide, `W2 = {t3,t4,t5,t6}`:

```text
sumtwu(W2) = 0.37 + 0.97 + 0.43 + 1.05 = 2.82
wus({1}, W2) = (0.37 + 0.97 + 1.05) / 2.82
              = 2.39 / 2.82
```

Đây là lý do test kiểm tra giá trị `2.39 / 2.82`. Hãy đọc test này trước khi đọc implementation tối ưu.

## 4. FWUDS-CT: Circular Tidset

### Ý tưởng

`FWUDS-CT` lưu với mỗi singleton item một danh sách các local transaction ID (`lcTid`) mà item xuất hiện. `lcTid` chạy vòng trong `[1, windowTransactions]`.

```text
window capacity = 4
transaction mới: 1  2  3  4  1  2 ...
lcTid:           1  2  3  4  1  2 ...
```

`twuByLcTid[lcTid]` giữ `twu` của transaction hiện tại đang chiếm vị trí đó. Vì ID bị wrap, thứ tự số không luôn là thứ tự thời gian.

### Các bước initialize

1. Đọc đủ các pane đầu tiên.
2. Gán `lcTid` tuần tự cho từng transaction.
3. Thêm `lcTid` vào CTset của mọi item trong transaction.
4. Tính singleton candidate.
5. Giao các CTset để sinh itemset lớn hơn.

### Các bước slide

1. Xác định `p` local ID cũ nhất.
2. Xóa các ID đó khỏi mọi singleton CTset.
3. Xóa `twuByLcTid` tương ứng.
4. Chèn pane mới và để `lcTid` tiếp tục chạy vòng.
5. Mine lại window hiện tại.

### Intersection khi wrap

Khi so sánh hai CTset, nếu `lcTid <= currentLcTid`, implementation tạm cộng `capacity` để đưa ID về trục thời gian tăng dần. Kết quả vẫn trả về ID gốc, không sắp xếp lại CTset.

Ví dụ trong test:

```text
W1: CTset(1) = [1, 3, 4]
slide
W2: CTset(1) = [3, 4, 2]
```

Đọc `FwudsCtMiner.intersect` cùng test `circularTidsetsMatchPaperAcrossSlide` để thấy chính xác phần wrap.

### Candidate generation

Các candidate được chuẩn hóa bằng `Itemset`, giao danh sách transaction và cộng `twu` tại các ID giao nhau. Candidate không đạt threshold bị loại sớm. Đây là nơi CTset tránh phải quét lại toàn bộ transaction cho mỗi pattern.

## 5. FWUDS-DWT: DSWUN-tree và WUNList

### Ý tưởng

`FWUDS-DWT` xây một prefix tree theo thứ tự item ổn định. Mỗi transaction đi theo một path; các node giữ cumulative `twu`. `TAILLIST` giữ thông tin để xóa các transaction cũ khi cửa sổ trượt.

Pipeline khái niệm:

```text
WeightedTransaction
        │ insert theo rank
        ▼
DSWUN-tree (parent/children, pre/post, cumulative twu)
        │ generate
        ▼
WUNList (WUN-code: pre, post, weight)
        │ ancestor intersection + pruning
        ▼
PatternResult
```

### Item order

Trong initial window, item được xếp theo singleton `wus`/weight giảm dần; nếu hòa thì item ID tăng dần. Item xuất hiện lần đầu sau đó được nối ở cuối rank theo item ID trong pane. Quy tắc late item này là quyết định bổ sung vì paper không mô tả đầy đủ trường hợp đó.

### Insert và eviction

- Insert pane mới trước, sau đó evict đúng `pane-size` transaction cũ nhất theo Algorithm 5.
- Khi evict, đi từ tail node về root và trừ `twu` của transaction.
- DSWUN-tree không loại singleton infrequent ngay khi xây cây; việc lọc threshold diễn ra trong giai đoạn mining.

### WUN-code và intersection

Một `WunCode(pre, post, weight)` đại diện cho một node và trọng số của phần transaction tương ứng. Quan hệ ancestor là:

```text
ancestor.pre  < descendant.pre
ancestor.post > descendant.post
```

Khi hai WUNList giao nhau, code ancestor phù hợp được cộng weight. `FwudsDwtMiner.intersect` còn tính upper bound của phần weight chưa xử lý; nếu phần còn lại chắc chắn không đạt `minwus × sumtwu`, candidate bị prune.

## 6. Vì sao cần BruteForceMiner?

`BruteForceMiner` lấy toàn bộ item trong window, sinh mọi itemset có thể, rồi tính trực tiếp bằng `WeightedTransaction.contains`. Nó chậm theo cấp số mũ và chỉ phù hợp stream nhỏ, nhưng độc lập hơn với CTset/tree.

Mọi tối ưu hóa phải thỏa:

```text
patterns(optimized, window) = patterns(oracle, window)
wus(optimized, pattern) ≈ wus(oracle, pattern) trong 1e-12
```

`FwudsAlgorithmsTest` kiểm tra paper example; `RandomizedEquivalenceTest` kiểm tra 20 seed và nhiều lần slide.

## 7. Bản đồ paper → code

| Khái niệm | Class/method nên đọc |
|---|---|
| Input transaction | `TransactionFileReader.read` |
| Dynamic weight | `DynamicWeightGenerator.weight` |
| Transaction `twu` | `TransactionWeigher.weigh` |
| Cấu hình window | `MiningConfig` |
| FWUDS interface | `FwudsMiner` |
| Oracle | `BruteForceMiner` |
| CTset, circular ID | `FwudsCtMiner.insertPane`, `removeOldestPane`, `intersect` |
| DSWUN-tree | `DswunTree` |
| WUNList intersection | `WunList`, `FwudsDwtMiner.intersect` |
| Output window | `WindowResult`, `PatternResult` |

## 8. Những gì implementation không thể khẳng định về paper

Seed, PRNG, weight matrix, bản sửa đổi `dFWUNL-stream`, môi trường runtime và một số tie-break không được công bố đầy đủ. Vì vậy project tái lập định nghĩa và kiểm tra correctness; không tuyên bố tái lập tuyệt đối runtime/memory của paper. Chi tiết nằm trong [reproducibility_limitations.md](reproducibility_limitations.md).
