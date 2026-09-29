# Bối cảnh bài báo FWUDS

Nguồn: Nguyen Le và cộng sự, *Fast and scalable sliding-window-based algorithms for mining frequent weighted utility patterns over dynamic quantitative data streams*, Information Sciences 748 (2026) 123488, DOI `10.1016/j.ins.2026.123488`. Bản PDF dùng trong dự án nằm tại `docs/paper/fwuds-paper.pdf`.

## Bài toán

Một transaction định lượng `t_k` chứa quantity `x_kj` của từng item `i_j`. Mỗi weight batch có một weight `w_j` cho mỗi item. Dynamic stream được chia thành pane có `p` transaction; một cửa sổ gồm `w` pane nên có `|W| = w × p` transaction. Mỗi lần trượt loại pane cũ nhất và thêm đúng một pane mới.

Transaction weighted utility:

```text
twu(t_k) = sum(w_j × x_kj) / |t_k|
```

`|t_k|` là số item phân biệt, không phải tổng quantity. Với pattern `X` trong cửa sổ `W_i`:

```text
sumtwu(W_i) = sum(twu(t)) với mọi t thuộc W_i
wus(X,W_i) = sum(twu(t)) với mọi t chứa X / sumtwu(W_i)
```

`X` là FWUP khi `wus(X,W_i) >= minwus`.

## FWUDS-CT

- `lcTid` chạy vòng từ `1` đến `|W|`; CTset của pattern lưu các `lcTid` theo thứ tự thời gian.
- Khi trượt, thuật toán bỏ `p` lcTid cũ ở đầu các singleton CTset rồi thêm các transaction mới ở cuối.
- Hai CTset được giao tuyến tính bằng cách cộng `|W|` cho lcTid thuộc chu kỳ mới (`lcTid <= Curr_lcTid`) trước khi so sánh.
- Candidate được sinh bằng DFS theo lớp tương đương; numerator của `wus` lấy trực tiếp từ bảng `twu` theo lcTid.

## FWUDS-DWT

- DSWUN-tree giữ toàn bộ item, không bỏ singleton infrequent. Node chứa item, cumulative `twu`, parent, children, pre và post.
- TAILLIST giữ tail node của từng transaction theo thứ tự thời gian. Khi xóa, đi từ tail lên root và trừ `twu` của transaction.
- Các item trong transaction dùng một total order ổn định. Tree được đánh số pre/post để tạo WUNList.
- WUNList intersection dùng quan hệ ancestor (`pre_ancestor < pre_descendant` và `post_ancestor > post_descendant`), cộng weight cho cùng ancestor và prune khi phần weight còn lại không thể đạt `minwus × sumtwu`.

## Thực nghiệm trong bài báo

Bài báo sử dụng Retail, Accidents, BMS-POS, Kosarak và Susy; weights được sinh ngẫu nhiên trong `[1,10]`. Đánh giá gồm update time, total mining time, peak memory và scalability sau 50 lần trượt. Hai thuật toán chạy single-thread; baseline là bản sửa đổi `dFWUNL-stream` từ tài liệu tham khảo [9].
