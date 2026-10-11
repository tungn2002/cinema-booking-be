<div align="center">

<br/>

<h1>
 <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=900&size=42&duration=3000&pause=1000&color=F97316&center=true&vCenter=true&width=750&lines=Cinema+Booking" alt="Cinema Booking Backend" />
</h1>

<h3>
  <p>Hệ thống Backend đặt vé xem phim — xử lý tranh chấp giữ ghế bằng Redis Lua Script, đồng bộ trạng thái Realtime qua WebSocket STOMP, tự động giải phóng ghế hết hạn bằng Redis Keyspace Notifications, xuất vé PDF & gửi Email bất đồng bộ, hỗ trợ thanh toán đa cổng (Stripe & PayPal), và tự động hóa triển khai trên VPS với Docker Compose & CI/CD GitHub Actions.</p>
</h3>

<br/>

<p>
  <img src="https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.3-6DB33F?style=flat-square&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-7.2_Alpine-DC382D?style=flat-square&logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/WebSocket-STOMP_Realtime-010101?style=flat-square&logo=socketdotio&logoColor=white" />
  <img src="https://img.shields.io/badge/MinIO-S3_Storage-C72C48?style=flat-square&logo=minio&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker_Compose-Multi--Container-2496ED?style=flat-square&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/Nginx-SSL_Reverse_Proxy-009639?style=flat-square&logo=nginx&logoColor=white" />
  <img src="https://img.shields.io/badge/Ubuntu_VPS-Production-E95420?style=flat-square&logo=ubuntu&logoColor=white" />
  <img src="https://img.shields.io/badge/GitHub_Actions-CI%2FCD-2088FF?style=flat-square&logo=githubactions&logoColor=white" />
  <img src="https://img.shields.io/badge/⚡_Atomic-Seat_Lock-16a34a?style=flat-square" />
  <img src="https://img.shields.io/badge/📧_Async_PDF-Receipt_Email-6366f1?style=flat-square" />
</p>

<br/>

</div>

---

## ✨ Key Features (Tính Năng Nổi Bật)

| Feature | Description |
|---|---|
| 🎟️ **Atomic Seat Locking (Redis Lua)** | Khóa ghế nguyên tử chống Race Condition bằng Redis Lua Script (TTL 10 phút = 600s), loại bỏ nguy cơ trùng ghế khi người dùng cùng lúc |
| ⚡ **Realtime WebSocket Sync** | Phát sóng tức thời (`/topic/showtimes/{id}/seats`) trạng thái ghế (LOCKED, AVAILABLE, RESERVED) qua Spring WebSocket STOMP tới tất cả clients |
| ⏳ **Redis TTL Keyspace Notification** | Lắng nghe sự kiện `__keyevent@*__:expired` khi hết 10 phút giữ ghế; tự động giải phóng ghế và broadcast WebSocket mà không cần polling liên tục |
| 🛡️ **Pessimistic Locking & Database Integrity** | Kết hợp khóa bi quan `PESSIMISTIC_WRITE` tầng JPA và khóa phân tán Redis đảm bảo tính nhất quán tuyệt đối của dữ liệu đặt vé |
| 💳 **Dual Payment Gateways (Strategy)** | Tích hợp cổng thanh toán quốc tế **Stripe** (Checkout Sessions & Webhook) và **PayPal** (REST SDK & Webhook IPN) |
| 📄 **Async PDF Invoice & Email Receipt** | Cơ chế hướng sự kiện `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` + `@Retryable`: sinh vé điện tử PDF bằng iText và gửi email hóa đơn tự động |
| 🚦 **Token Bucket Rate Limiting** | Bảo vệ API bằng thuật toán Token Bucket thực thi trực tiếp qua Redis Lua Script, ngăn chặn spam và brute-force request |
| ☁️ **S3-Compatible MinIO Storage** | Quản lý và lưu trữ poster phim, banner media tập trung với MinIO Object Storage, tương thích AWS S3 SDK v2 |
| 🔒 **Security & Authentication** | Bảo mật toàn diện với Spring Security, JWT (JJWT 0.12.6), Google OAuth2 Login và phân quyền Role-Based Access Control (USER, ADMIN) |
| 🐳 **Full-Stack VPS Deployment** | Triển khai nguyên khối khép kín trên Ubuntu VPS với Docker Compose (Nginx SSL, Spring Boot, MySQL 8, Redis 7, MinIO), tự động hóa CI/CD qua GitHub Actions |

---

## 🏗️ System Architecture (Kiến Trúc Hệ Thống)

```
                                      👤 Clients (Web Browser / Mobile)
                                                      │
                                                      │ HTTPS / WSS (Port 443 / 80)
                                                      ▼
                                       ┌─────────────────────────────┐
                                       │    Nginx Reverse Proxy      │
                                       │  (SSL Termination & Router) │
                                       └──────────────┬──────────────┘
                                                      │
                       ┌──────────────────────────────┼──────────────────────────────┐
                       │ HTTP API Requests (/api/*)   │ WebSocket (/ws/*)            │ Static Media (/storage/*)
                       ▼                              ▼                              ▼
        ┌─────────────────────────────────────────────────────────────┐        ┌────────────────────────┐
        │                 Spring Boot Backend (Port 8080)             │        │   MinIO Object Storage │
        │  • Security & JWT Filter      • Rate Limiter (Token Bucket) │        │ (Movie Posters, Assets)│
        │  • SeatLockService (Lua)      • WebSocket STOMP Broker      │        └────────────────────────┘
        │  • Transactional Event Bus    • PDF & Mail Async Worker     │
        └──────────────┬──────────────────────────────┬───────────────┘
                       │                              │
         Cache & Locks │                              │ ACID Transactions
                       ▼                              ▼
        ┌─────────────────────────────┐        ┌─────────────────────────────┐
        │        Redis 7.2 Cache      │        │       MySQL 8.0 Database    │
        │  • Seat Locks (10 min TTL)  │        │  • Showtimes & Theaters     │
        │  • Keyspace Expired Events  │        │  • Bookings & Reservations  │
        │  • Token Bucket Rate Limit  │        │  • Users, Roles & Payments  │
        └─────────────────────────────┘        └─────────────────────────────┘
                       │                                      │
                       ▼                                      ▼
        ┌─────────────────────────────┐        ┌─────────────────────────────┐
        │  WebSocket Broadcast Sync   │        │     External Gateways       │
        │   /topic/showtimes/{id}/... │        │  • Stripe / PayPal Webhooks │
        │   → Instant UI Seat Update  │        │  • SMTP Mail Server (Gmail) │
        └─────────────────────────────┘        └─────────────────────────────┘
```

---

## 🎟️ Use Case Đặt Vé Xem Phim (Ticket Booking Flow)

### 📊 Flowchart Luồng Chính (Main Booking Flow)

```
  [👤 Khách Hàng]                 [⚡ Spring Boot API]               [🔴 Redis 7]              [🐘 MySQL 8]           [💳 Stripe/PayPal]
         │                                 │                              │                          │                     │
         │ 1. Chọn ghế & Gọi Book          │                              │                          │                     │
         │────────────────────────────────>│                              │                          │                     │
         │                                 │ 2. Chạy Lua Lock Script      │                          │                     │
         │                                 │─────────────────────────────>│                          │                     │
         │                                 │   (Set key, TTL=600s)        │                          │                     │
         │                                 │<─────────────────────────────│                          │                     │
         │                                 │    Lock thành công (1)       │                          │                     │
         │                                 │                              │                          │                     │
         │                                 │ 3. Broadcast WebSocket      │                          │                     │
         │                                 │─── /topic/showtimes/... ────>│ (Client UI nhận LOCKED)  │                     │
         │                                 │                              │                          │                     │
         │                                 │ 4. Kiểm tra Pessimistic Lock & Lưu Đơn Booking          │                     │
         │                                 │────────────────────────────────────────────────────────>│                     │
         │                                 │    Tạo Reservation (Status=CONFIRMED, Paid=FALSE)       │                     │
         │                                 │<────────────────────────────────────────────────────────│                     │
         │                                 │                                                         │                     │
         │                                 │ 5. Khởi tạo phiên thanh toán (Checkout Session)                               │
         │                                 │──────────────────────────────────────────────────────────────────────────────>│
         │                                 │<──────────────────────────────────────────────────────────────────────────────│
         │ 6. Trả về Checkout URL / Session│                                                                               │
         │<────────────────────────────────│                                                                               │
         │                                 │                                                                               │
         │ 7. Khách hàng thanh toán        │                                                                               │
         │────────────────────────────────────────────────────────────────────────────────────────────────────────────────>│
         │                                 │                                                                               │
         │                                 │ 8. Webhook gửi tín hiệu thanh toán thành công (checkout.session.completed)   │
         │                                 │<──────────────────────────────────────────────────────────────────────────────│
         │                                 │                                                         │                     │
         │                                 │ 9. Update DB (Paid=TRUE) & Xóa Redis Lock               │                     │
         │                                 │────────────────────────────────────────────────────────>│                     │
         │                                 │─────────────────────────────>│                          │                     │
         │                                 │                              │                          │                     │
         │                                 │ 10. Kích hoạt PaymentSuccessEvent (AFTER_COMMIT)        │                     │
         │                                 │───┐                                                     │                     │
         │                                 │   │ Sinh vé PDF (iText)                                 │                     │
         │                                 │   │ Gửi Email hóa đơn kèm vé (@Async + @Retryable)     │                     │
         │                                 │<──┘                                                     │                     │
         │ 11. Nhận vé PDF qua Email       │                                                         │                     │
         │<────────────────────────────────│                                                         │                     │
```

---

### 📝 Chi Tiết Kịch Bản Use Case Đặt Vé
## 1. Luồng chính (Happy Path - Đặt vé và thanh toán thành công)

1. **Chọn ghế:** Khách hàng chọn suất chiếu và ghế, cập nhật realtime qua WebSocket STOMP.
2. **Giữ ghế:** Redis Lua Script khóa ghế nguyên tử với TTL 10 phút.
3. **Cập nhật realtime:** WebSocket thông báo trạng thái `LOCKED`.
4. **Tạo đơn:** MySQL sử dụng `PESSIMISTIC_WRITE` kiểm tra ghế và tạo đơn chưa thanh toán.
5. **Tạo phiên thanh toán:** Backend tạo phiên Stripe Checkout hoặc PayPal.
6. **Thanh toán:** Khách hàng thanh toán qua Stripe hoặc PayPal.
7. **Xác nhận thanh toán:** Webhook xác thực giao dịch và cập nhật trạng thái thanh toán.
8. **Sinh vé và gửi email:** Tạo vé PDF, gửi email bất đồng bộ bằng `@Async`, thử lại tối đa 3 lần.
9. **Hoàn tất:** Giải phóng Redis Lock, đánh dấu ghế `RESERVED` và thông báo kết quả.

---

## 2. Luồng ngoại lệ (Exception Flows)

- **Ghế đã bị giữ:** Redis từ chối khóa, giải phóng các ghế đã khóa trong yêu cầu và thông báo khách chọn ghế khác.
- **Hết hạn giữ ghế:** Sau 10 phút chưa thanh toán, hệ thống hủy đơn, giải phóng ghế và cập nhật `AVAILABLE`.
- **Đã có đơn chờ thanh toán:** Chặn tạo đơn mới cho đến khi thanh toán hoặc hủy đơn cũ.
- **Thanh toán thất bại:** Giữ ghế đến khi hết hạn 10 phút hoặc khách chủ động hủy.
- **Lỗi cơ sở dữ liệu:** Giải phóng Redis Lock để tránh trạng thái không nhất quán.
- **Lỗi tạo PDF hoặc gửi email:** Thử lại tối đa 3 lần; không rollback giao dịch thanh
---

## 🌐 Kiến Trúc Triển Khai Trên VPS (VPS Deployment Architecture)

Toàn bộ hệ thống Backend, Database, Cache, Storage và Nginx Reverse Proxy được đóng gói chuẩn hóa trong mạng ảo Docker Bridge (`cinema_network`) và vận hành trực tiếp trên máy chủ **Ubuntu VPS**.

```
                                  [🌍 Internet / Users]
                                            │
                                            │ Ports 80 / 443
                                            ▼
                           ┌─────────────────────────────────┐
                           │   VPS Firewall / UFW Ports      │
                           └────────────────┬────────────────┘
                                            │
                                            ▼
     ┌─────────────────────────────────────────────────────────────────────────────┐
     │                             Ubuntu VPS Host                                 │
     │                                                                             │
     │   Docker Network: cinema_network (Bridge Isolated)                          │
     │                                                                             │
     │   ┌──────────────────────────────┐                                          │
     │   │      cinema_frontend_prod    │◄─── Host Mount: ./ssl (Certbot SSL)      │
     │   │  • Nginx Reverse Proxy       │                                          │
     │   │  • Serves Built React SPA    │                                          │
     │   │  • Port 80 (HTTP -> HTTPS)   │                                          │
     │   │  • Port 443 (SSL TLS 1.3)    │                                          │
     │   └──────────────┬───────────────┘                                          │
     │                  │                                                          │
     │      Proxy Pass  │ /api/*, /ws/*, /oauth2/*                                 │
     │                  ▼                                                          │
     │   ┌──────────────────────────────┐        ┌──────────────────────────────┐  │
     │   │     cinema_backend_prod      │        │       cinema_redis_prod      │  │
     │   │  • Spring Boot 3.2.3 (Java17)│◄───────┤  • Redis 7.2 Alpine          │  │
     │   │  • Port 8080 (Internal Expose│        │  • Keyspace Events (Ex)      │  │
     │   │  • Profile: prod             │        │  • Vol: redis_data           │  │
     │   └──────────────┬───────────────┘        └──────────────────────────────┘  │
     │                  │                                                          │
     │                  ├────────────────────────┐                                 │
     │                  │ JDBC Connection        │ S3 Storage API                  │
     │                  ▼                        ▼                                 │
     │   ┌──────────────────────────────┐ ┌──────────────────────────────┐         │
     │   │      cinema_mysql_prod       │ │      cinema_minio_prod       │         │
     │   │  • MySQL 8.0 Engine          │ │  • MinIO Server (Port 9000)  │         │
     │   │  • Vol: mysql_data           │ │  • Console UI (Port 9001)    │         │
     │   │  • Healthcheck Ping Enabled  │ │  • Vol: minio_data           │         │
     │   └──────────────────────────────┘ └──────────────▲───────────────┘         │
     │                                                   │ Auto-setup Bucket       │
     │                                    ┌──────────────┴───────────────┐         │
     │                                    │  cinema_minio_setup_prod     │         │
     │                                    │  (Init public movie bucket)  │         │
     │                                    └──────────────────────────────┘         │
     └─────────────────────────────────────────────────────────────────────────────┘
```

### 📦 Các Container Dịch Vụ Trên VPS (`docker-compose-prod.yml`)

* **`frontend` (Nginx + React Frontend)**: Lắng nghe cổng `80` (tự động chuyển hướng HTTPS) và cổng `443`. Nhận chứng chỉ SSL thực tế từ máy chủ host qua mount thư mục `./ssl:/etc/nginx/ssl:ro`, tối ưu Gzip, định tuyến `/api/` và `/ws/` về backend, `/storage/` về MinIO.
* **`backend` (Spring Boot API)**: Chạy trên container `eclipse-temurin:17-jdk-alpine`, expose cổng nội bộ `8080`, phụ thuộc vào MySQL (chỉ khởi động khi MySQL đạt trạng thái `healthy`), Redis và MinIO.
* **`mysql` (MySQL 8.0)**: Cơ sở dữ liệu chính, lưu trữ bền vững qua Docker volume `mysql_data`, tích hợp cơ chế `healthcheck: mysqladmin ping` giúp backend không bị lỗi kết nối lúc khởi động.
* **`redis` (Redis 7.2-Alpine)**: Lưu trữ phân tán, bảo vệ bằng mật khẩu và kích hoạt cờ `--notify-keyspace-events Ex` phục vụ cơ chế tự động giải phóng ghế khi hết hạn TTL. Dữ liệu mount qua `redis_data`.
* **`minio` & `minio-create-bucket`**: Cung cấp kho lưu trữ đối tượng S3 tương thích, tự động tạo bucket `cinema-booking` và cấu hình quyền truy cập public cho poster/ảnh phim.

---
---

## 🛠️ Tech Stack Chi Tiết

| Thành Phần | Công Nghệ | Mô Tả Vai Trò |
|---|---|---|
| **Core Framework** | Java 17 · Spring Boot 3.2.3 | Nền tảng backend tiêu chuẩn doanh nghiệp |
| **Data & ORM** | Spring Data JPA · Hibernate | Tương tác dữ liệu, ánh xạ ORM, khóa bi quan |
| **Database** | MySQL 8.0 (InnoDB) | Lưu trữ quan hệ chính (ACID transactions) |
| **In-Memory & Cache** | Redis 7.2 (Alpine) | Khóa phân tán Lua Script, Rate Limiting, TTL Keyspace |
| **Realtime Messaging** | Spring WebSocket · STOMP · SockJS | Cập nhật sơ đồ ghế thời gian thực hai chiều |
| **Media Storage** | MinIO · AWS SDK for S3 v2 | Lưu trữ hình ảnh poster, media S3-compatible |
| **Payment Gateways** | Stripe SDK · PayPal REST API SDK | Xử lý thanh toán quốc tế và Webhook tự động |
| **Document & Email** | iText PDF 5 · Spring Mail (SMTP) | Sinh vé điện tử PDF và gửi email tự động (@Async) |
| **Security & Auth** | Spring Security · JJWT 0.12.6 · OAuth2 | Xác thực stateless JWT, đăng nhập Google, RBAC |
| **Resilience** | Spring Retry · Spring AOP | Cơ chế tự động thử lại tác vụ gửi mail/đồng bộ |
| **API Documentation** | SpringDoc OpenAPI 2.1 · Swagger UI | Sinh tài liệu API tự động chuẩn OpenAPI 3 |
| **Containerization** | Docker · Docker Compose | Đóng gói toàn bộ microservices & hạ tầng |
| **Reverse Proxy** | Nginx (SSL TLS 1.3) | Cân bằng tải, định tuyến đường dẫn, HTTPS |
| **CI/CD & Cloud** | GitHub Actions · Ubuntu VPS | Tự động hóa build, kiểm thử và deploy qua SSH/SCP |

---

## 🔍 Danh Sách API Chính (API Endpoints Overview)

### 🔐 Authentication & Users
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản người dùng mới |
| `POST` | `/api/v1/auth/login` | Public | Đăng nhập hệ thống, trả về JWT Access Token |
| `GET` | `/api/v1/auth/me` | Authenticated | Lấy thông tin cá nhân của người dùng đăng nhập |
| `GET` | `/login/oauth2/code/google` | Public | OAuth2 Redirect Callback đăng nhập qua Google |

### 🎬 Movies & Theaters
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/movies` | Public | Danh sách phim có phân trang, lọc theo thể loại/năm |
| `GET` | `/api/v1/movies/{id}` | Public | Lấy chi tiết thông tin và đánh giá của phim |
| `POST` | `/api/v1/movies` | Admin | Tạo mới phim và upload poster lên MinIO |
| `GET` | `/api/v1/theaters` | Public | Danh sách tất cả các rạp chiếu phim |
| `GET` | `/api/v1/theaters/{id}` | Public | Lấy thông tin chi tiết rạp chiếu theo ID |

### 🕒 Showtimes & Seats
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/showtimes?date=YYYY-MM-DD` | Public | Tra cứu các suất chiếu trong ngày |
| `GET` | `/api/v1/showtimes/movies/{movieId}` | Public | Lấy danh sách suất chiếu của một phim cụ thể |
| `GET` | `/api/v1/seats/showtimes/{showtimeId}` | Public | Lấy sơ đồ toàn bộ ghế của một suất chiếu |
| `GET` | `/api/v1/seats/showtimes/{showtimeId}/available` | Authenticated | Danh sách các ghế còn trống có thể đặt |
| `WS` | `/ws/` (Sub: `/topic/showtimes/{id}/seats`) | Public | Kênh WebSocket nhận biến động trạng thái ghế realtime |

### 🎟️ Reservations & Payments
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `POST` | `/api/v1/reservations` | Authenticated | Giữ ghế nguyên tử (Redis Lock + DB Reservation) |
| `POST` | `/api/v1/reservations/book-and-pay` | Authenticated | Tạo đơn giữ ghế và khởi tạo URL thanh toán 1-step |
| `GET` | `/api/v1/reservations/my-reservations` | Authenticated | Xem lịch sử đơn đặt vé của người dùng |
| `DELETE` | `/api/v1/reservations/{id}` | Authenticated | Hủy đơn giữ chỗ và giải phóng ghế lập tức |
| `POST` | `/api/v1/payments/create-checkout-session` | Authenticated | Tạo phiên thanh toán Stripe / PayPal cho đơn giữ chỗ |
| `POST` | `/api/v1/payments/webhook/stripe` | Public | Webhook tiếp nhận sự kiện thanh toán từ Stripe |
| `POST` | `/api/v1/payments/webhook/paypal` | Public | Webhook tiếp nhận sự kiện thanh toán từ PayPal |

---
## Cập nhật trong tương lai (Future Improvements)

- **Tích hợp Message Broker (RabbitMQ):** Xử lý các tác vụ bất đồng bộ
---
<div align="center">
  <sub>Built with ❤️ by <b>Lê Minh Tùng</b> • Spring Boot 3 · Redis · MySQL · MinIO · Docker Compose · Ubuntu VPS</sub>
</div>
