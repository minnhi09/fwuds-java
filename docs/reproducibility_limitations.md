# Vì sao không thể khớp số liệu tuyệt đối trong bài báo

## Kết luận ngắn

Dự án có thể tái hiện định nghĩa, tập FWUP, cơ chế sliding window và xu hướng hiệu năng. Chỉ từ PDF hiện có, không thể cam kết tạo đúng từng số runtime/memory trong hình của bài báo.

## Thông tin không được công bố

1. Bài báo nói item weights của mỗi batch được lấy ngẫu nhiên trong `[1,10]`, nhưng không cung cấp random seed, PRNG, thứ tự sinh hoặc weight matrix đã dùng. Thay weight làm thay `twu`, `wus`, số pattern và toàn bộ chi phí khai phá.
2. Mục Data availability ghi dữ liệu đã sử dụng là confidential. Các transaction gốc có thể tải từ FIMI/SPMF, nhưng phần biến đổi động và weights chính xác của nhóm tác giả không có.
3. Baseline `dFWUNL-stream` là bản sửa đổi của `dFWUNL`, nhưng bài báo không công bố đầy đủ mã hoặc giả mã của phần sửa đổi. Vì vậy không thể bảo đảm một bản tự viết có cùng hằng số thời gian và memory layout.
4. Một số chi tiết triển khai không được định nghĩa: PRNG, tie-break khi hai item có cùng `wus`, xử lý item xuất hiện muộn, chế độ GC, JVM/.NET memory measurement và số lần warm-up.

## Runtime và memory không phải hằng số thuật toán

Bài báo dùng C# 2022, .NET Framework 4.7.2, Windows 11, Intel Core i5-9400F và 32 GB RAM. Dự án này dùng Java 21. JIT, GC, object layout, collection implementation, CPU, nhiệt độ, tiến trình nền và filesystem cache đều làm thay số đo. Peak memory của .NET cũng không tương đương trực tiếp với tổng peak heap do Java MXBeans báo cáo.

Các biểu đồ còn làm tròn và không kèm raw measurement, nên không thể kiểm tra sai số ngay cả khi cấu hình gần giống.

## Cách dự án tạo kết quả có thể tái lập

- Tự định nghĩa bộ sinh weight ổn định theo `(seed,batch,item)`; seed mặc định `42`.
- Ghi toàn bộ tham số, JVM, OS, runtime, peak heap và hash tập kết quả vào CSV.
- Dùng cùng input và seed cho cả hai thuật toán.
- Kiểm tra kết quả bằng oracle vét cạn trước khi đo hiệu năng.
- Phân biệt rõ hai tuyên bố trong báo cáo:
  - **Correctness reproduction:** thuật toán trả đúng FWUP theo công thức và input đã công bố.
  - **Performance trend reproduction:** so sánh xu hướng giữa CTset và DSWUN-tree trong cùng môi trường Java.

Không dùng kết quả Java để tuyên bố đã khớp tuyệt đối các con số của bài báo. Muốn làm điều đó cần xin tác giả source code, weight matrix/seed, raw measurements và cấu hình chạy đầy đủ.
