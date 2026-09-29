# Yêu cầu và quyết định triển khai

## Nền tảng

- Java 21, Maven 3.9.9 qua wrapper, UTF-8 và single-thread.
- Public model nằm trong `org.fwuds.model`; thuật toán qua interface `FwudsMiner`.
- `initialize` nhận đúng số pane của cửa sổ đầu; `slide` nhận đúng một pane đầy đủ. Final pane thiếu transaction bị bỏ qua để giữ invariant kích thước cửa sổ.
- Item ID và quantity phải là số nguyên dương; transaction rỗng hoặc item trùng bị từ chối.
- Dùng `double`. So threshold theo numerator: `numerator + 1e-12 >= minwus × sumtwu`, tránh chia và giảm lỗi sát biên.

## Trọng số động và input

- File FIMI/SPMF: một dòng là một transaction; token `id` có quantity 1, token `id:quantity` biểu diễn dữ liệu định lượng.
- Bản Susy từ mirror SPMF dùng item ID từ 0 và chứa item trùng trong cùng transaction. Script tải dịch ID thêm 1 và gộp các lần xuất hiện thành `item:quantity`, nhờ đó bảo toàn utility của 19 token gốc. File quan sát được có 179 item ID phân biệt, khác con số 190 được báo cáo trong bài báo; manifest dùng 179 để kiểm tra dữ liệu thực tế.
- Weight batch độc lập với pane. Batch index của transaction là `sequence / weightBatchSize`.
- Weight mặc định là số nguyên `[1,10]`, sinh từ SplitMix64 tự cài đặt với tuple `(seed,batch,item)`. Cách này không phụ thuộc thứ tự duyệt collection hay phiên bản JDK.
- Seed mặc định cho thí nghiệm là `42`; mọi CSV phải ghi seed và cấu hình.

## Invariants FWUDS-CT

- `capacity = windowPanes × paneSize`; `lcTid` luôn thuộc `[1,capacity]`.
- CTset singleton là deque theo thứ tự transaction từ cũ đến mới, kể cả khi giá trị số đã wrap.
- `twuByLcTid` luôn đại diện transaction hiện có tại vị trí vòng đó.
- Candidate itemset được chuẩn hóa tăng dần theo item ID; CTset intersection không sắp xếp lại lcTid.

## Invariants FWUDS-DWT

- Thứ tự ban đầu là singleton `wus` giảm dần trong cửa sổ đầu, hòa thì item ID tăng dần. Thứ tự này được cố định để tree có một prefix order nhất quán.
- Item lần đầu xuất hiện sau cửa sổ đầu được nối vào cuối rank theo item ID trong pane đó. Đây là quy tắc bổ sung vì bài báo không mô tả late item.
- Mỗi TailEntry giữ `leaf`, `twu` và sequence. Bài báo gọi `twu(L.tid)` nhưng node không lưu Tid; giữ `twu` tại entry loại bỏ nhu cầu rescan.
- Theo Algorithm 5, pane mới được insert trước khi xóa `paneSize` TailEntry cũ nhất.
- Node có weight xấp xỉ 0 trong epsilon mới được xóa. WUN-code được sinh lại sau mỗi update vì pre/post phụ thuộc hình dạng tree.

## CLI và output

- `mine` xuất một dòng CSV cho mỗi pattern/cửa sổ và tóm tắt trên console.
- `benchmark` tạo miner mới cho từng logical run trong cùng JVM, không ghi từng pattern; hash kết quả chuẩn hóa theo window và itemset để chứng minh các repetition/hai thuật toán trả cùng tập pattern mà không phụ thuộc bit làm tròn `double`. Script paper-like gọi mỗi cấu hình trong một JVM riêng.
- Runtime chỉ bao quanh initialize/mine/slide, không tính đọc file. Peak heap lấy từ `MemoryPoolMXBean`; CSV ghi JVM và OS để tránh so sánh sai môi trường.
