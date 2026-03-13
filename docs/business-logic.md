# Tài liệu Nghiệp vụ Backend – Go Stock

## 1. Tổng quan hệ thống

Go Stock là hệ thống quản lý danh mục đầu tư chứng khoán cá nhân. Người dùng có thể theo dõi giao dịch mua/bán, quản lý dòng tiền, cập nhật giá thị trường và phân tích hiệu suất đầu tư theo từng tài khoản.

---

## 2. Các thực thể chính (Entity)

| Entity | Bảng DB | Mô tả |
|---|---|---|
| `Ticker` | `ticker` | Mã chứng khoán / tài sản (cổ phiếu, vàng, quỹ…) |
| `Broker` | `broker` | Công ty chứng khoán |
| `Account` | `account` | Tài khoản giao dịch (1 broker, nhiều account) |
| `Transaction` | `transaction` | Lệnh mua / bán |
| `Position` | `position` | Trạng thái nắm giữ hiện tại (1 account + 1 ticker) |
| `CashFlow` | `cash_flow` | Nạp / rút tiền vào tài khoản |
| `PriceHistory` | `price_history` | Lịch sử giá đóng cửa theo ngày |

### Quan hệ
```
Broker ──< Account ──< Transaction >── Ticker
                   ──< Position    >── Ticker
                   ──< CashFlow
Ticker ──< PriceHistory
```

---

## 3. Nghiệp vụ: Giao dịch (TransactionService)

### 3.1 Tạo lệnh mua (BUY)

**Luồng xử lý:**

1. Validate tồn tại `Account` và `Ticker`.
2. Tính toán các trường tài chính:
   - `tradeValue = matchedVolume × matchedPrice`
   - `fee` = nếu không nhập thì tự tính: `tradeValue × broker.defaultFeeRate` (làm tròn về đồng)
   - `tax = 0` (lệnh mua không chịu thuế)
   - `cost = tradeValue + fee + tax` (vốn thực bỏ ra)
3. Lưu `Transaction` với `status = COMPLETED`.
4. Cập nhật `Position` (bước quan trọng nhất):
   - Nếu chưa có Position → tạo mới với `holdingVolume = 0`.
   - Tính lại **giá vốn bình quân gia quyền (weighted average cost)**:
     ```
     newAvgCost = (oldAvgCost × oldVolume + newPrice × newVolume + fee)
                  ÷ (oldVolume + newVolume)
     ```
   - `holdingVolume += matchedVolume`

### 3.2 Tạo lệnh bán (SELL)

**Luồng xử lý:**

1. Tính `fee` theo `defaultFeeRate` (như mua).
2. Tính `tax = tradeValue × 0.1%` (thuế bán chứng khoán).
3. Tính **Realized PnL (lãi/lỗ đã thực hiện)** và ghi vào `transaction.returnAmount`:
   ```
   realizedPnL = (matchedPrice − avgCost) × volume − fee − tax
   ```
4. Ghi `transaction.cost = avgCost × volume` (vốn gốc của lô bán).
5. Cập nhật Position:
   - `holdingVolume -= matchedVolume` (không âm dưới 0)
   - `avgCost` giữ nguyên (phương pháp FIFO bình quân, không thay đổi khi bán)

### 3.3 Sửa giao dịch

Chỉ cập nhật trường thông tin của giao dịch (ngày, giá, số lượng…), **không** tự động tái tính lại Position. Dùng khi cần chỉnh sửa dữ liệu nhập sai.

### 3.4 Import từ Excel

- Hệ thống tách thành 3 endpoint import giao dịch riêng theo từng mẫu:
   - `POST /api/transactions/import/stock-history`
   - `POST /api/transactions/import/fund-history`
   - `POST /api/transactions/import/stock-statement`
- Endpoint cũ `POST /api/transactions/import` vẫn tồn tại để auto-detect.
- Mỗi màn hình import trên frontend chỉ gọi đúng endpoint tương ứng để tránh import nhầm mẫu.
- Các endpoint này hỗ trợ 3 mẫu sheet từ TCBS:
   - **Stock Transaction History** (Lịch sử giao dịch cổ phiếu)
   - **Fund Statement** (Lịch sử giao dịch quỹ)
   - **Stock Statement** (Sao kê cổ phiếu)
- Cơ chế parse theo mẫu:
   - **Stock Transaction History**:
      - map cột `Ticker`, `Trading date`, `Trade`, `Matched volume`, `Matched price`, `Fee`, `Tax`, `Order No.`
      - nếu có `Order No.` thì dùng để dedup; nếu thiếu thì sinh mã tạm theo dòng dữ liệu
   - **Fund Statement**:
      - map `Fund code`, `Trade`, `Trading date`, `Cost/Unit`, `Matched Volume`
      - tự tạo ticker nếu chưa có (type = `FUND_CERTIFICATE`)
      - `Order No.` sinh tự động theo ngày + mã + dòng
   - **Stock Statement**:
      - map `Ticker`, `Trading date`, `Actions`, `+/- Volume`, `Description`
      - do file không có giá khớp, hệ thống dùng giá xấp xỉ theo thứ tự: `avgCost` position -> `currentPrice` position -> giá đóng cửa gần nhất -> `1`
      - mục tiêu chính là đồng bộ biến động khối lượng theo sao kê
- Nếu upload nhầm **Cash Statement** vào endpoint transaction import, backend trả lỗi 400 với message hướng dẫn import ở màn tiền mặt.

---

## 4. Nghiệp vụ: Dòng tiền (CashFlowService)

| Loại | Tác động lên Account |
|---|---|
| `DEPOSIT` (nạp tiền) | `cashBalance += amount` · `purchasingPower += amount` · `availableForWithdrawal += amount` |
| `WITHDRAW` (rút tiền) | `cashBalance -= amount` · `purchasingPower -= amount` · `availableForWithdrawal -= amount` |

**Lưu ý:** Hệ thống hiện không kiểm tra số dư âm khi rút tiền — cần validate ở tầng business nếu cần.

Các trường tài khoản:
- `cashBalance` – số dư tiền mặt thực tế
- `purchasingPower` – sức mua (dùng để đặt lệnh mua)
- `availableForWithdrawal` – khả dụng rút

### 4.1 Import sao kê tiền (Cash Statement)

- Endpoint chuyên biệt: `POST /api/cash-flows/import/cash-statement`.
- Endpoint tương thích cũ: `POST /api/cash-flows/import`.
- Frontend dùng màn hình import sao kê tiền riêng để gọi endpoint chuyên biệt.
- Cột parse chính:
   - `Trading date`
   - `+/- Amount (VND)`
   - `Description`
- Quy tắc mapping:
   - `amount > 0` -> `DEPOSIT`
   - `amount < 0` -> `WITHDRAW` (lưu trị tuyệt đối)
- Dedup theo khóa: `accountId + type + amount + flowDate + note`.
- Mỗi dòng hợp lệ gọi lại `CashFlowService.create()` để đảm bảo cập nhật đủ 3 số dư tài khoản giống luồng nhập tay.

---

## 5. Nghiệp vụ: Cập nhật giá (PriceService)

**Luồng:**

1. Nhập giá mới (closePrice, openPrice, highPrice, lowPrice, volume, priceDate) theo mã ticker.
2. Lưu vào `PriceHistory` (dùng cho lịch sử / biểu đồ).
3. **Đồng bộ Position:** tìm tất cả Position đang có `holdingVolume > 0` của mã đó, cập nhật:
   - `currentPrice = newClosePrice`
   - `unrealizedPnL = (currentPrice − avgCost) × holdingVolume`

> Unrealized PnL được tính lại ngay mỗi lần cập nhật giá, không cần tính on-the-fly khi xem danh mục.

---

## 6. Nghiệp vụ: Phân tích (AnalyticsService)

### 6.1 Portfolio Summary (`GET /api/analytics/portfolio/{accountId}`)

Trả về tổng quan danh mục cho một tài khoản:

| Trường | Công thức |
|---|---|
| `totalInvested` | Σ (avgCost × holdingVolume) của các position đang nắm giữ |
| `currentValue` | Σ (currentPrice × holdingVolume) |
| `unrealizedPnL` | `currentValue − totalInvested` |
| `realizedPnL` | Σ `returnAmount` của tất cả lệnh SELL |
| `totalPnL` | `unrealizedPnL + realizedPnL` |
| `returnRate (%)` | `totalPnL ÷ totalInvested × 100` |

Kèm theo:
- **`holdings`** – danh sách từng mã: symbol, volume, avgCost, currentPrice, marketValue, unrealizedPnL, unrealizedPnLPct
- **`allocation`** – phân bổ danh mục theo % giá trị thị trường từng mã

### 6.2 Cash Summary (`GET /api/analytics/cash/{accountId}`)

| Trường | Ý nghĩa |
|---|---|
| `totalDeposit` | Tổng tiền đã nạp |
| `totalWithdraw` | Tổng tiền đã rút |
| `netCashIn` | `totalDeposit − totalWithdraw` |
| `cashBalance` | Số dư tiền mặt hiện tại |
| `purchasingPower` | Sức mua hiện tại |

---

## 7. REST API endpoints

| Method | URL | Mô tả |
|---|---|---|
| `GET` | `/api/ref/tickers` | Danh sách tất cả mã tài sản |
| `POST` | `/api/ref/tickers` | Thêm mã mới |
| `PUT` | `/api/ref/tickers/{id}` | Sửa mã |
| `GET` | `/api/ref/brokers` | Danh sách broker |
| `POST` | `/api/ref/brokers` | Thêm broker |
| `PUT` | `/api/ref/brokers/{id}` | Sửa broker |
| `GET` | `/api/ref/accounts` | Danh sách tài khoản |
| `POST` | `/api/ref/accounts` | Tạo tài khoản |
| `PUT` | `/api/ref/accounts/{id}` | Sửa tài khoản |
| `POST` | `/api/transactions` | Tạo giao dịch |
| `PUT` | `/api/transactions/{id}` | Sửa giao dịch |
| `GET` | `/api/transactions` | Danh sách giao dịch (lọc, phân trang) |
| `POST` | `/api/transactions/import` | Import auto-detect (legacy) |
| `POST` | `/api/transactions/import/stock-history` | Import Lịch sử giao dịch cổ phiếu |
| `POST` | `/api/transactions/import/fund-history` | Import Lịch sử giao dịch quỹ |
| `POST` | `/api/transactions/import/stock-statement` | Import Sao kê cổ phiếu |
| `POST` | `/api/cash-flows` | Nạp / rút tiền |
| `GET` | `/api/cash-flows?accountId=` | Lịch sử dòng tiền |
| `POST` | `/api/cash-flows/import` | Import Cash Statement |
| `POST` | `/api/cash-flows/import/cash-statement` | Import Sao kê tiền (endpoint chuyên biệt) |
| `POST` | `/api/prices` | Cập nhật giá |
| `GET` | `/api/analytics/portfolio/{accountId}` | Tổng quan danh mục |
| `GET` | `/api/analytics/cash/{accountId}` | Tổng quan dòng tiền |

---

## 8. Các hằng số và tham số hệ thống

| Tham số | Giá trị | Mô tả |
|---|---|---|
| `TAX_RATE` | `0.001` (0.1%) | Thuế bán chứng khoán (áp dụng trên matchedValue khi SELL) |
| `defaultFeeRate` | Cấu hình theo Broker | Phí giao dịch mặc định (ví dụ SSI/TCBS/VPS: 0.15%) |
| Scale giá vốn | 4 chữ số thập phân | `avgCost` lưu 4 decimal (NUMERIC 20,4) |
| Scale tiền | 2 chữ số thập phân | Các trường tiền tệ lưu 2 decimal (NUMERIC 20,2) |

---

## 9. Sơ đồ luồng giao dịch

```
Người dùng nhập lệnh
        │
        ▼
TransactionController.create()
        │
        ▼
TransactionService.create()
   ├── Validate Account + Ticker
   ├── buildTransaction()
   │     ├── Tính fee (tự động nếu không nhập)
   │     ├── Tính tax (0 nếu BUY, 0.1% nếu SELL)
   │     └── Tính cost (BUY: tradeValue+fee+tax)
   ├── transactionRepo.save()
   └── updatePosition()
         ├── [BUY]  Tính lại avgCost (bình quân gia quyền)
         │          holdingVolume += vol
         └── [SELL] Tính realizedPnL → tx.returnAmount
                    holdingVolume -= vol
```
