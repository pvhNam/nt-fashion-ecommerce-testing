<div align="center">

# 🛍️ NT Fashion

### Website thương mại điện tử thời trang — xây dựng và kiểm thử

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Jakarta EE](https://img.shields.io/badge/Jakarta_Servlet-6.0-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql&logoColor=white)
![Tomcat](https://img.shields.io/badge/Tomcat-10.1-F8DC75?logo=apachetomcat&logoColor=black)
![Gradle](https://img.shields.io/badge/Build-Gradle-02303A?logo=gradle&logoColor=white)
![JUnit](https://img.shields.io/badge/Test-JUnit_5-25A162?logo=junit5&logoColor=white)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?logo=githubactions&logoColor=white)

</div>

**NT Fashion** là đồ án xây dựng và kiểm thử website thương mại điện tử thời trang. Hệ thống kết nối trải nghiệm mua hàng của khách với công việc vận hành cửa hàng: từ chọn biến thể (màu, kích cỡ), tính ưu đãi và đặt hàng đến nhập kho, giao hàng, gửi hóa đơn và báo cáo doanh thu.

Website được phát triển theo mô hình **MVC** với **Java 21, Jakarta Servlet/JSP và MySQL**, gồm hai khu vực chính: **cửa hàng trực tuyến** và **trang quản trị**.

## 📑 Mục lục

1. [Thành viên và phân công](#-thành-viên-và-phân-công)
2. [Tính năng chính](#-tính-năng-chính)
3. [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
4. [Cài đặt và khởi động](#-cài-đặt-và-khởi-động)
5. [Hướng dẫn sử dụng](#-hướng-dẫn-sử-dụng)
6. [Kiến trúc và cấu trúc dự án](#-kiến-trúc-và-cấu-trúc-dự-án)
7. [Kiểm thử và đảm bảo chất lượng](#-kiểm-thử-và-đảm-bảo-chất-lượng)
8. [Quy trình làm việc nhóm](#-quy-trình-làm-việc-nhóm)
9. [Mức độ hoàn thành và hạn chế](#-mức-độ-hoàn-thành-và-hạn-chế)

---

## 👥 Thành viên và phân công

| STT | Họ và tên | MSSV | Vai trò | Số commit* |
| :---: | --- | :---: | --- | :---: |
| 1 | Phạm Văn Hoài Nam | 23130200 | Nhóm trưởng, phát triển nghiệp vụ, kiểm thử tích hợp, CI/CD | 219 |
| 2 | Nguyễn Quang Thành | 23130307 | Phát triển nghiệp vụ, thanh toán, kiểm thử quản trị | 197 |
| 3 | Ngô Quang Khánh | 23130152 | Thực thi kiểm thử phía khách hàng, sửa lỗi bảo mật và nghiệp vụ | 35 |
| 4 | Nguyễn Đức Khải | 23130142 | Phát triển chức năng phía khách hàng | 35 |

<sub>*Tính trên toàn bộ nhánh tại ngày 27/11/2026, đã gộp các tên tác giả khác nhau của cùng một người và gồm cả commit gộp nhánh. Số commit chỉ là chỉ báo tham khảo, không phản ánh đầy đủ khối lượng công việc.</sub>

### 🧑‍💼 Phạm Văn Hoài Nam — Nhóm trưởng

> Khởi tạo repository, định hướng kiến trúc, phụ trách vận hành, triển khai và kiểm thử tích hợp.

**Phát triển chức năng**
- **Nền tảng dự án:** khởi tạo repo (10/2025), dựng model, controller, trang chủ, giới thiệu, tin tức và bộ sưu tập theo MVC; chuyển trang `about` từ HTML sang JSP.
- **Giỏ hàng, thanh toán và đơn hàng:** xây phần giảm giá, thanh toán giỏ hàng, trừ tồn kho khi đặt thành công, áp voucher trong giỏ và trang chi tiết đơn hàng. Làm lại lịch sử đơn thành các tab *Tất cả, Chưa giao, Đang giao, Đã giao, Đã hủy*.
- **Quản trị:** khởi tạo trang admin, quản lý voucher, quản lý nhân viên (thêm, xóa, đổi mật khẩu, giới hạn quyền, nhánh `HR_Management`), quản lý đơn hàng kèm **thời gian nhận hàng**.
- **Sản phẩm và kho:** quản lý kho theo màu và kích cỡ, thêm nhiều ảnh cho một sản phẩm, tách phiếu nhập kho thành trang riêng và xuất Excel.
- **Báo cáo:** xuất báo cáo kho và doanh thu theo ngày, tháng; tách thành **trang báo cáo doanh thu riêng** có in và xuất Excel.
- **Tài khoản và thông báo:** quên mật khẩu qua email, trang thông tin cá nhân, hạng thành viên, thông báo đơn qua email và SMS (webhook), bước đầu tích hợp VNPay.
- **Hóa đơn, chiến dịch, liên hệ:** lập hóa đơn cá nhân hoặc công ty và gửi email (kể cả khi chạy Tomcat từ IntelliJ), trang chiến dịch khuyến mãi, hộp thư liên hệ.

**Hạ tầng và DevOps**
- Viết workflow **GitHub Actions CI/CD** (build WAR bằng Gradle, chuyển tệp qua SSH, chạy migration, triển khai lên Tomcat) và sửa các lỗi triển khai.
- Dùng cơ sở dữ liệu dùng chung (TiDB) trong giai đoạn đầu để cả nhóm cùng làm việc, sau đó chuyển lại kết nối local; sửa đường dẫn controller, tách CSS khỏi JSP.
- Quản lý repo: tạo nhánh, duyệt và gộp pull request của cả nhóm (smoke test, kiểm thử khách hàng, kiểm thử admin...).

**Kiểm thử**
- Phụ trách **kiểm thử tích hợp và hồi quy** (hồ sơ W7–W10) cho giỏ hàng, đơn hàng, tồn kho, voucher, thanh toán và phân quyền.
- Sửa các lỗi phát hiện: phân quyền admin, badge giỏ hàng, tồn kho, xung đột khi dọn giỏ lúc thanh toán COD, chặn tab giỏ cũ ghi đè, xử lý thao tác trùng hoặc đồng thời, cho phép giảm số lượng khi giỏ vượt tồn kho, đọc đúng tồn kho theo màu và kích cỡ cho khách chưa đăng nhập.

### 🧑‍💻 Nguyễn Quang Thành

> Xây dựng bộ khung giao diện, giỏ hàng, thanh toán MoMo, sổ địa chỉ, đánh giá sản phẩm và phụ trách kiểm thử quản trị.

**Phát triển chức năng**
- **Khởi đầu dự án (11/2025):** kết nối MySQL, dựng trang `index` (header, footer, logo), chỉnh font và màu sắc.
- **Giỏ hàng:** servlet và controller giỏ hàng, **lưu giỏ theo tài khoản trong database**, sửa lỗi cập nhật số lượng.
- **Lịch sử mua hàng:** thêm xem lịch sử đơn và trạng thái đơn đã đặt.
- **Quản trị:** thiết kế lại giao diện admin, dashboard, trang quản lý khách hàng (`admin-user`), phân quyền admin.
- **Tìm kiếm sản phẩm** và **đa ngôn ngữ** Việt–Anh; sửa tiếng Việt không dấu thành có dấu.
- **Thanh toán MoMo / chuyển khoản:** `MomoService`, nút thanh toán MoMo, callback xử lý kết quả, hiển thị phương thức thanh toán, cộng doanh thu MoMo vào báo cáo; refactor `OrderDAO` (dùng `mapOrder`) và trạng thái đơn trong model `Order`.
- **Sổ địa chỉ giao hàng:** `AddressDAO`, trang danh sách địa chỉ, thêm, sửa, xóa, lấy tỉnh thành từ API.
- **Đánh giá sản phẩm** (31/08/2026): giao diện và luồng gửi đánh giá.
- **Hạ tầng:** **chuyển dự án từ Maven sang Gradle** (06/2026), dọn thư mục thừa.

**Kiểm thử quản trị**
- Xây bộ test JUnit và robustness cho Admin trong [`src/test/java/admin`](src/test/java/admin): `AdminAccessTest`, `AdminCrudTest`, `AdminModulesTest`, `AdminInputRobustnessTest`, `AdminProductRobustnessTest`, `AdminVoucherRobustnessTest`.
- Sửa lỗi tìm được: kiểm tra giá và tồn, đảm bảo transaction khi lưu sản phẩm (BUG-ADM); kiểm tra miền giá trị, ngày hết hạn và mã trùng của voucher (BUG-05, BUG-ADM-07); xử lý ID không hợp lệ khi mở trang sửa tin tức (BUG-ADM-09).

### 🧑‍🔬 Ngô Quang Khánh

> Thực thi kiểm thử phía khách hàng; trực tiếp sửa lỗi bảo mật, xác thực dữ liệu và nghiệp vụ theo từng mã lỗi (ST-xx, IS-xx, W1-xx).

**Bảo mật và phiên làm việc**
- `AdminAuthFilter` kiểm tra quyền trang quản trị; bỏ tài khoản admin viết cứng trong mã (ST-09, ST-10, IS-02).
- Chống giả mạo yêu cầu (**CSRF**) cho mọi POST, đổi nút xóa từ GET sang form POST (W1-04); sửa thông báo khi thiếu hoặc sai mã CSRF (ST-45).
- Mã hóa dữ liệu khi in ra trang (**chống XSS**) cho từ khóa, tin tức, link ảnh (ST-13, ST-17, ST-18–20); thêm `state` chống giả mạo cho đăng nhập Google.
- Đổi mã phiên sau khi đăng nhập (ST-21); đọc lại quyền từ DB mỗi lần vào trang quản trị để chặn ngay tài khoản bị xóa hoặc hạ quyền (ST-22).

**Xác thực dữ liệu và tài khoản**
- Kiểm tra họ tên, email, số điện thoại khi đăng ký và thêm/sửa địa chỉ (ST-14, IS-12, IS-13); dùng chung quy định mật khẩu mạnh (ST-15, IS-08).
- Sửa luồng **quên mật khẩu bằng OTP**: thông báo chung, kiểm tra OTP, giới hạn 5 lần sai, hết hạn sau 5 phút (ST-31, ST-38–41).

**Nghiệp vụ đặt hàng và voucher**
- Giữ voucher khi tạo đơn online, trả lại khi thất bại hoặc quá 15 phút (ST-16, IS-05); chặn đặt đơn trùng; hủy đơn trả lại tồn kho và voucher; xử lý thanh toán khi hết hàng (ST-30, ST-04, ST-05, ST-37).
- Thêm ngày bắt đầu voucher và cho Admin tạo voucher riêng (ST-42, ST-43); làm tròn tổng tiền một lần ở tổng cuối (ST-44).
- Hoàn thiện điều kiện **chỉ khách đã mua và nhận hàng mới được đánh giá** sản phẩm.

**Tìm kiếm, lọc và giao diện**
- Sắp xếp tên theo bảng chữ cái tiếng Việt, ô tìm kiếm chuyển sang trang Sản phẩm, lọc đúng danh mục, giữ đủ bộ lọc khi đổi từng điều kiện (ST-11, ST-12, ST-35, ST-36).
- Sửa nút `+` / `−` trong giỏ hàng, trang lỗi chung tiếng Việt, đổi ngôn ngữ cho trạng thái đơn, thẻ viewport, vị trí ô vàng của menu (ST-24, ST-26–29).
- Đưa migration 004 và `gradle-wrapper.jar` lên Git để cả nhóm dựng lại được môi trường (ST-25).

**Thực thi kiểm thử:** ghi test case và lỗi vào bảng chung, thực hiện trên nhánh `TEST/Execution-Customer` và kiểm tra lại các bản sửa ở `FIX/RE-TESTING`.

### 🧑‍🎨 Nguyễn Đức Khải

> Phát triển và hoàn thiện trải nghiệm mua sắm phía khách hàng.

- **Bộ sưu tập và danh mục:** hoàn thiện chức năng bộ sưu tập, tách sản phẩm theo danh mục; sửa bộ lọc danh mục ở trang Cửa hàng; sửa nút *Xem tất cả sản phẩm* ở trang chủ.
- **Chuẩn hóa kiến trúc:** đưa trang chủ về servlet xử lý, thêm controller cho trang giới thiệu, chuyển `product_details`, `about` và giỏ hàng sang **JSTL**; chỉnh header và dropdown tài khoản.
- **Đăng nhập Google OAuth 2.0:** tích hợp đăng nhập bằng Google, sau đó sửa lỗi sau khi chuyển sang Gradle và sửa đường dẫn trong `web.xml`.
- **Mua ngay và Thêm vào giỏ:** thêm nút *Mua ngay* ở trang chủ, trang cửa hàng và chi tiết sản phẩm; mở **modal chọn màu, kích cỡ, số lượng** trước khi thêm; *Thêm vào giỏ* giữ nguyên trang và hiện thông báo ngắn; đồng bộ logic hai nút giữa các trang.
- **Giỏ hàng:** hiển thị màu và kích cỡ từng món, sửa lỗi xóa sản phẩm không thành công.

### 📅 Các mốc phát triển theo lịch sử Git

<details>
<summary>Xem chi tiết các mốc</summary>

| Giai đoạn | Công việc tiêu biểu |
| --- | --- |
| 10–12/2025 | Khởi tạo repository, kết nối MySQL, lưu giỏ hàng theo tài khoản và phát triển luồng đặt đơn. [Khởi tạo](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/a24ca82) · [MySQL](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/2e3e05d) · [Giỏ hàng](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/15ca0e7) |
| 01–03/2026 | Mở rộng hồ sơ, giao diện Việt–Anh, voucher quản trị và bộ sưu tập theo danh mục. [Hồ sơ](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/36ec557) · [Việt–Anh](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/5591de2) · [Bộ sưu tập](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/0b581f3) |
| 04–06/2026 | Quản lý nhân viên, MoMo, đăng nhập Google, workflow triển khai và chuyển sang Gradle. [Nhân viên](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/17cf157) · [MoMo](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/f4716c2) · [Google OAuth](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/769b084) · [CI/CD](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/e19b897) · [Gradle](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/e66cae3) |
| 08–09/2026 | Tách trang báo cáo doanh thu và phiếu nhập. Bổ sung đánh giá, hạng thành viên, thông báo đơn và bắt đầu tích hợp VNPay. [Doanh thu](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/06a2b4b) · [Nhập kho](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/792d363) · [Đánh giá](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/fba2a33) · [Thông báo đơn](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/5c14268) |
| 10–11/2026 | Kiểm thử môi trường, luồng khách hàng, quản trị và tích hợp. Sửa phân quyền, OTP, tồn kho, voucher và điều kiện đánh giá. Bổ sung thời gian giao hàng, chiến dịch, liên hệ và gửi hóa đơn. [Smoke test](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/7960a2e) · [Admin](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/52a9777) · [Khách hàng](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/ffcd545) · [Tích hợp](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/a0cccdc) · [Hóa đơn](https://github.com/pvhNam/nt-fashion-ecommerce-testing/commit/7d123e2) |

</details>

---

## ✨ Tính năng chính

### Khách hàng

- **Tài khoản và hồ sơ** — Đăng ký, đăng nhập bằng tài khoản hoặc Google, đặt lại mật khẩu qua OTP, cập nhật hồ sơ, quản lý sổ địa chỉ giao hàng, hạng thành viên.
- **Khám phá sản phẩm** — Tìm kiếm, lọc theo danh mục và giá, sắp xếp, xem bộ sưu tập, chọn màu và kích cỡ. Khách đã mua và nhận sản phẩm có thể gửi đánh giá.
- **Giỏ hàng và thanh toán** — Mua ngay hoặc chọn món trong giỏ, áp dụng giảm giá thành viên và voucher, thanh toán qua COD, chuyển khoản, MoMo hoặc VNPay.
- **Đơn hàng và hóa đơn** — Xem lịch sử, lọc theo trạng thái, hủy đơn khi trạng thái cho phép. Chi tiết đơn hiển thị thời gian đặt, thời gian giao thành công và hóa đơn đã lập.
- **Thông tin và hỗ trợ** — Xem tin tức, chiến dịch khuyến mãi, trao đổi với cửa hàng qua mục Liên hệ. Giao diện có tiếng Việt và tiếng Anh.

### Quản trị

- **Tổng quan và vận hành** — Thống kê, quản lý đơn hàng, khách hàng, nhân viên và tin tức.
- **Sản phẩm và kho** — Quản lý sản phẩm, bộ sưu tập, biến thể (nhiều ảnh, màu, kích cỡ). Lập phiếu nhập, theo dõi tồn kho và xuất phiếu nhập.
- **Khuyến mãi và liên hệ** — Tạo voucher theo phần trăm hoặc số tiền cố định, đơn tối thiểu, thời gian áp dụng. Liên kết voucher với chiến dịch và trả lời tin nhắn trong hộp thư của cửa hàng.
- **Hóa đơn** — Lập hóa đơn cá nhân hoặc công ty và gửi tệp HTML đến email của đơn hàng.
- **Doanh thu** — Xem theo ngày, tháng, sáu tháng hoặc năm; in kỳ đang xem và xuất Excel.

### Phân quyền và bảo mật

Hệ thống có ba nhóm quyền: **Khách hàng**, **Nhân viên**, **Quản trị viên**. Nhân viên chỉ truy cập các chức năng quản trị được cấp. Các biện pháp đã áp dụng gồm: `AdminAuthFilter` (đọc lại quyền từ DB), `CsrfFilter` cho mọi POST, mã hóa dữ liệu khi hiển thị (chống XSS), đổi mã phiên sau đăng nhập, `state` cho OAuth, giới hạn số lần nhập OTP và quy định mật khẩu mạnh.

---

## 🧰 Công nghệ sử dụng

| Thành phần | Công nghệ và vai trò |
| --- | --- |
| Ứng dụng web | Java 21, Jakarta Servlet 6.0, JSP và JSTL 3.0 |
| Dữ liệu | MySQL 8.x, JDBC và các lớp DAO |
| Tích hợp | Google OAuth 2.0, Jakarta Mail, MoMo, VNPay, Apache POI |
| Build và chạy | Gradle Wrapper, WAR, Apache Tomcat 10.1 |
| CI/CD | GitHub Actions (build, chuyển tệp qua SSH, migration, triển khai Tomcat) |
| Kiểm thử | JUnit 5, Jsoup, kiểm thử HTTP và đối chiếu dữ liệu MySQL |
| Quản lý công việc | Git/GitHub (pull request theo nhánh), Jira, Google Sheets |

---

## 🚀 Cài đặt và khởi động

**Yêu cầu:** JDK 21, MySQL 8.x, Apache Tomcat 10.1. Có thể chạy từ IntelliJ IDEA Ultimate hoặc build WAR bằng Gradle Wrapper.

### 1. Tải dự án

```bash
git clone https://github.com/pvhNam/nt-fashion-ecommerce-testing.git
cd nt-fashion-ecommerce-testing
```

### 2. Chuẩn bị cơ sở dữ liệu

Tạo database `ltweb` với bộ ký tự `utf8mb4`, sau đó chạy các tệp SQL trong [`migrations`](migrations) theo thứ tự tên tệp, từ `001_init.sql` đến `006_voucher_start_date.sql` (có hai tệp cùng tiền tố `004` và `005`, chạy cả hai). Các bảng hóa đơn, chiến dịch và liên hệ (`order_invoices`, `promotion_campaigns`, `contact_threads`, `contact_messages`) được ứng dụng tự tạo khi khởi động thông qua `DatabaseUpgradeListener`.

Trong cấu hình chạy Tomcat, đặt các biến môi trường:

| Biến | Ví dụ |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/ltweb` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(mật khẩu MySQL của bạn)* |

Với database demo mới: đăng ký một tài khoản thử, đặt `is_admin = 1` cho đúng tài khoản đó trong bảng `users`, rồi đăng nhập lại. Thêm sản phẩm và nhập tồn kho trước khi thử luồng đặt hàng.

### 3. Chạy trên cổng 8080

Trong IntelliJ, vào **Run → Edit Configurations → Tomcat Server → Local**, chọn Tomcat 10.1 và thêm artifact **WAR exploded** trong tab **Deployment**. Đặt **Application context** thành `/DOANWEB_war_exploded` rồi bấm **Run**.

| Trang | Địa chỉ |
| --- | --- |
| Cửa hàng | [Trang chủ](http://localhost:8080/DOANWEB_war_exploded/home) |
| Tài khoản | [Đăng nhập](http://localhost:8080/DOANWEB_war_exploded/login) |
| Quản trị | [Admin](http://localhost:8080/DOANWEB_war_exploded/admin) |

Nếu đã có cấu hình Tomcat với context riêng, hãy thay `/DOANWEB_war_exploded` trong URL bằng context hiện tại.

**Triển khai thủ công bằng WAR:**

```bash
# Windows
.\gradlew.bat war

# Linux / macOS
./gradlew war
```

Chép tệp WAR trong `build/libs/` vào thư mục `webapps/` của Tomcat.

### 4. Bật các tích hợp khi cần

| Tích hợp | Cấu hình cho Tomcat |
| --- | --- |
| Email OTP, thông báo đơn và hóa đơn | `SMTP_USERNAME`, `SMTP_PASSWORD` (Gmail dùng mật khẩu ứng dụng). Tùy chọn: `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`. |
| Thông báo đơn qua SMS | `ORDER_SMS_WEBHOOK`, tùy chọn `ORDER_SMS_WEBHOOK_TOKEN` theo dịch vụ nhận webhook. |
| Đăng nhập Google | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`; redirect URI trỏ đúng `/google-callback`. |
| MoMo sandbox | `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY`, `MOMO_ENDPOINT`. Kết quả trả về tại `/momo-callback`. |
| VNPay sandbox | `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAYMENT_URL`, `VNPAY_RETURN_URL`. Khách quay lại `/vnpay-return`, thông báo từ cổng thanh toán đi qua `/vnpay-ipn`. |
| Xuất Excel và in | Apache POI tạo tệp `.xlsx` cho báo cáo và phiếu nhập. Trang doanh thu dùng hộp thoại in của trình duyệt. |
| Triển khai tự động | [Workflow GitHub Actions](.github/workflows/deploy.yml) chạy khi push lên `main`. |

> 🔒 Không đưa mật khẩu và khóa bí mật vào mã nguồn. Hãy giữ chúng trong cấu hình môi trường và khởi động lại Tomcat sau khi thay đổi.

---

## 🧭 Hướng dẫn sử dụng

### Đặt hàng với tài khoản khách

1. Vào **Cửa hàng**, mở sản phẩm, chọn màu và kích cỡ rồi bấm **Thêm vào giỏ** hoặc **Mua ngay**.
2. Tại **Giỏ hàng**, chọn sản phẩm cần mua rồi mở trang thanh toán. Kiểm tra số lượng, địa chỉ giao hàng và tổng tiền sau ưu đãi, chọn phương thức thanh toán và xác nhận. Có thể chọn **COD** khi demo trên máy local.
3. Mở **Lịch sử đơn hàng → Xem chi tiết** để theo dõi trạng thái. Khi cửa hàng lập hóa đơn, mục **Xem hóa đơn** xuất hiện trong chi tiết đơn.
4. Muốn hỏi về đơn hoặc sản phẩm, đăng nhập rồi mở **Liên hệ** để gửi tin nhắn và xem phản hồi.

### Xử lý đơn với tài khoản quản trị

| Công việc | Thao tác |
| --- | --- |
| Chuẩn bị hàng bán | **Quản lý Sản phẩm** → thêm sản phẩm và biến thể → **Đặt hàng nhập kho** để nhập số lượng. |
| Xử lý giao hàng | **Quản lý Đơn hàng** → chọn đơn → cập nhật theo tiến độ. Khi bấm **Giao thành công**, hệ thống ghi thời gian nhận hàng. |
| Gửi hóa đơn | **Hóa đơn đơn hàng** → chọn đơn → chọn **Cá nhân** hoặc **Công ty** → điền thông tin người mua → **Gửi email**. |
| Tạo ưu đãi | **Chiến dịch khuyến mãi** → nhập nội dung, thời gian chạy và voucher liên kết nếu có. |
| Phản hồi khách | **Hộp thư liên hệ** → mở cuộc trao đổi → gửi phản hồi. |
| In báo cáo theo ngày | **Báo cáo doanh thu** → chọn kỳ **Ngày** và ngày muốn xem → **In báo cáo**. Có thể lưu PDF từ hộp thoại in hoặc xuất Excel từ trang báo cáo. |

---

## 🏗️ Kiến trúc và cấu trúc dự án

Ứng dụng theo mô hình **MVC**: JSP/JSTL đảm nhận View, Servlet là Controller, các lớp `service` chứa nghiệp vụ (giá, hạng thành viên, thanh toán, thông báo, hóa đơn), `dao` truy cập MySQL qua JDBC, `filter` xử lý phân quyền và CSRF.

```text
nt-fashion-ecommerce-testing/
├── .github/workflows/     Workflow CI/CD triển khai lên Tomcat
├── migrations/            Khởi tạo và nâng cấp MySQL (001–006)
├── src/main/java/
│   ├── controller/        admin/ và user/ — nhận yêu cầu, điều hướng
│   ├── service/           Giá, hạng thành viên, MoMo, VNPay, email, SMS, hóa đơn
│   ├── dao/               Truy cập dữ liệu: User, Product, Order, Cart, Voucher...
│   ├── model/             Các đối tượng nghiệp vụ
│   ├── filter/            AdminAuthFilter, CsrfFilter, OrderCartBadgeFilter...
│   ├── util/              I18n, InputValidator, PasswordPolicy, EmailUtil...
│   └── resources/         messages_vi / messages_en
├── src/main/webapp/       JSP, CSS, JavaScript và tài nguyên giao diện
├── src/test/java/admin/   Kiểm thử tích hợp và robustness cho khu vực Admin
├── build.gradle           Cấu hình Gradle (Java 21, JUnit 5, Jsoup)
└── gradlew, gradlew.bat   Gradle Wrapper
```

Các nhóm bảng chính: `users` và `addresses` (tài khoản), `product` và `product_variants` (sản phẩm), `cart`, `orders` và `order_details` (giao dịch), cùng `order_invoices`, `promotion_campaigns`, `contact_threads` và `contact_messages` (hóa đơn và hỗ trợ khách hàng).

---

## 🧪 Kiểm thử và đảm bảo chất lượng

Hồ sơ của nhóm bao phủ ba phạm vi: nghiệp vụ phía khách, nghiệp vụ quản trị và luồng tích hợp giữa các phân hệ. Test case và lỗi được ghi trong bảng chung, công việc theo dõi trên Jira, kết quả kiểm tra lại có kèm bằng chứng.

[Google Sheets của nhóm](https://docs.google.com/spreadsheets/d/1KOZKqI4D8PwDAtMPZfc0OAj8W1Nqzka6JrrK2vpuHic/edit?gid=0#gid=0) · [Jira KAN](https://kiemthuwebthoitrang.atlassian.net/jira/software/projects/KAN/boards/2)

| Phạm vi | Người phụ trách | Nguồn đối chiếu |
| --- | --- | --- |
| Khách hàng | Ngô Quang Khánh | Nhánh `TEST/Execution-Customer`, các bản kiểm tra lại trong `FIX/RE-TESTING`. |
| Quản trị | Nguyễn Quang Thành | Tab **Thành - Test cases admin** trong Google Sheets và [mã kiểm thử Admin](src/test/java/admin). |
| Tích hợp và hồi quy | Phạm Văn Hoài Nam | Hồ sơ kiểm thử W7–W10, smoke test môi trường (`TEST/Smoke-test-moi-truong`) và kết quả kiểm tra lại sau khi sửa. |

**Chạy kiểm thử tự động:**

```bash
./gradlew adminTest              # kiểm thử chức năng Admin
./gradlew adminRobustnessTest    # kiểm thử độ bền với dữ liệu đầu vào bất thường
```

Hai task này dùng Tomcat và database QA riêng. Báo cáo Gradle nằm trong `build/reports/tests/`.

---

## 🔀 Quy trình làm việc nhóm

- **Nhánh `main`** là nhánh ổn định và là nhánh kích hoạt triển khai tự động.
- Mỗi công việc làm trên **nhánh riêng** đặt tên theo loại: `FEAT/…` (tính năng), `FIX/…` (sửa lỗi), `TEST/…` (kiểm thử), `ADD/…`.
- Gộp vào `main` qua **Pull Request**, do nhóm trưởng xem xét và gộp.
- Commit đặt theo tiền tố `[ADD]`, `[FIX]`, `[refactor]`, `[build]`; commit sửa lỗi kiểm thử ghi kèm mã lỗi (ví dụ `ST-16`, `BUG-ADM-07`) để truy vết về bảng test case.

---

## 📌 Mức độ hoàn thành và hạn chế

| Hạng mục | Trạng thái và phạm vi xác nhận |
| --- | --- |
| Cửa hàng và quản trị | Đã triển khai các chức năng chính. Có thể dựng trên máy local và demo luồng: chọn sản phẩm → đặt COD → quản trị xử lý đơn → giao thành công. |
| Báo cáo và hóa đơn | Đã có in báo cáo theo kỳ, xuất Excel, lập hóa đơn cá nhân hoặc công ty và gửi email. Luồng gửi Gmail local đã xác nhận với cấu hình SMTP của máy demo. |
| Kiểm thử | Có test case phía khách và quản trị, mã kiểm thử tự động cho Admin và bằng chứng kiểm tra lại các lỗi tích hợp. Kết quả cần đọc theo từng lượt chạy và phạm vi ghi trong hồ sơ. |
| Tích hợp bên ngoài | Google OAuth, MoMo/VNPay, SMTP và SMS qua webhook cần cấu hình theo từng môi trường. Luồng COD là kịch bản demo local. Kết quả sandbox, gửi SMS thực tế và triển khai trên máy chủ cần xác nhận riêng. |

**Hướng phát triển tiếp theo:** hoàn thiện tích hợp VNPay, mở rộng kiểm thử tự động sang phía khách hàng, bổ sung kiểm thử hiệu năng và hoàn thiện tính năng chiến dịch khuyến mãi, hộp thư liên hệ.

---

<div align="center">

Đồ án thực hiện bởi nhóm **NT Fashion** · [Kho mã nguồn](https://github.com/pvhNam/nt-fashion-ecommerce-testing)

</div>
