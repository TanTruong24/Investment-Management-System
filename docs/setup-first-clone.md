# Setup lần đầu sau khi clone

Tài liệu này hướng dẫn chạy dự án lần đầu trên máy mới.

## 1) Yêu cầu môi trường

Cài sẵn các công cụ sau:

- Git
- Java 21 (theo backend/pom.xml)
- Maven 3.9+
- Node.js 18+ (khuyến nghị Node.js 20 LTS)
- npm 9+
- PostgreSQL 14+

Kiểm tra nhanh phiên bản:

```powershell
git --version
java -version
mvn -version
node -v
npm -v
psql --version
```

## 2) Clone source

```powershell
git clone <REPO_URL>
cd hello-java-stock
```

## 3) Tạo database PostgreSQL

Backend đang dùng cấu hình mặc định:

- DB host: localhost
- DB port: 5432
- DB name: go_stock
- User: postgres
- Password: postgres

Tạo database:

```powershell
psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE go_stock;"
```

Nếu user/password/host của bạn khác mặc định, sửa trong backend/src/main/resources/application.yml.

## 4) Chạy backend (Spring Boot)

Mở terminal 1:

```powershell
cd backend
mvn clean spring-boot:run
```

Khi chạy lần đầu:

- Maven sẽ tải dependencies
- Flyway tự chạy migration từ backend/src/main/resources/db/migration/V1__init.sql

Backend chạy tại:

- http://localhost:8080/api

## 5) Chạy frontend (Vue + Vite)

Mở terminal 2:

```powershell
cd frontend
npm install
npm run dev
```

Frontend chạy tại:

- http://localhost:3000

Ghi chú:

- Frontend đã cấu hình proxy /api -> http://localhost:8080 trong frontend/vite.config.js.
- Vì vậy chỉ cần đảm bảo backend đang chạy cổng 8080.

## 6) Kiểm tra nhanh sau khi chạy

- Mở http://localhost:3000
- Vào màn hình danh sách giao dịch, tài khoản, mã chứng khoán
- Nếu backend chạy đúng, các API gọi qua /api sẽ có dữ liệu mẫu (seed từ migration)

## 7) Lệnh chạy hằng ngày (sau lần đầu)

Terminal backend:

```powershell
cd backend
mvn spring-boot:run
```

Terminal frontend:

```powershell
cd frontend
npm run dev
```

## 8) Troubleshooting nhanh

### Lỗi cổng 8080 đã được dùng

```powershell
netstat -ano | findstr :8080
Stop-Process -Id <PID> -Force
```

Sau đó chạy lại backend.

### Lỗi kết nối database

Kiểm tra:

- PostgreSQL đã start chưa
- Database go_stock đã tồn tại chưa
- username/password trong application.yml đúng chưa

### Frontend không gọi được API

Kiểm tra:

- Backend có đang chạy ở http://localhost:8080 không
- frontend/vite.config.js có proxy /api đúng target không

## 9) Build production

Backend:

```powershell
cd backend
mvn clean package
```

Frontend:

```powershell
cd frontend
npm run build
```
