-- =============================================================
-- V1__init.sql – Khởi tạo schema cho hệ thống Go Stock
-- =============================================================

-- ── Ticker (Mã chứng khoán / tài sản tài chính) ──────────────
CREATE TABLE ticker (
    id          BIGSERIAL PRIMARY KEY,
    symbol      VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    type        VARCHAR(30)  NOT NULL,   -- STOCK, GOLD, SILVER, STOCK_FUND, BOND_FUND, FUND_CERTIFICATE
    exchange    VARCHAR(10),             -- HOSE, HNX, UPCOM
    industry    VARCHAR(100),
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

-- ── Broker (Công ty chứng khoán) ─────────────────────────────
CREATE TABLE broker (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(20)    NOT NULL UNIQUE,
    name             VARCHAR(200)   NOT NULL,
    default_fee_rate NUMERIC(10, 4),  -- Phí mặc định (%)
    website          VARCHAR(200),
    active           BOOLEAN NOT NULL DEFAULT TRUE
);

-- ── Account (Tài khoản giao dịch) ────────────────────────────
CREATE TABLE account (
    id                       BIGSERIAL PRIMARY KEY,
    account_number           VARCHAR(50)    NOT NULL,
    name                     VARCHAR(100)   NOT NULL,
    broker_id                BIGINT         NOT NULL REFERENCES broker(id),
    cash_balance             NUMERIC(20, 2) NOT NULL DEFAULT 0,
    purchasing_power         NUMERIC(20, 2) NOT NULL DEFAULT 0,
    available_for_withdrawal NUMERIC(20, 2) NOT NULL DEFAULT 0,
    active                   BOOLEAN        NOT NULL DEFAULT TRUE
);

-- ── Transaction (Giao dịch mua/bán) ──────────────────────────
CREATE TABLE transaction (
    id             BIGSERIAL    PRIMARY KEY,
    order_no       VARCHAR(50),
    trading_date   DATE         NOT NULL,
    trade          VARCHAR(10)  NOT NULL,  -- BUY / SELL
    ticker_id      BIGINT       NOT NULL REFERENCES ticker(id),
    account_id     BIGINT       NOT NULL REFERENCES account(id),
    stock_exchange VARCHAR(10),
    order_type     VARCHAR(20),            -- NORMAL / DERIVATIVE
    channel        VARCHAR(30),            -- online, phone…
    volume         BIGINT       NOT NULL,
    order_price    NUMERIC(20, 2) NOT NULL,
    matched_volume BIGINT,
    matched_value  NUMERIC(20, 2),
    fee            NUMERIC(20, 2),
    tax            NUMERIC(20, 2),
    cost           NUMERIC(20, 2),
    return_amount  NUMERIC(20, 2),
    status         VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    note           TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tx_ticker       ON transaction(ticker_id);
CREATE INDEX idx_tx_trading_date ON transaction(trading_date);
CREATE INDEX idx_tx_account      ON transaction(account_id);

-- ── CashFlow (Nạp / Rút tiền) ────────────────────────────────
CREATE TABLE cash_flow (
    id         BIGSERIAL    PRIMARY KEY,
    account_id BIGINT       NOT NULL REFERENCES account(id),
    type       VARCHAR(10)  NOT NULL,  -- DEPOSIT / WITHDRAW
    amount     NUMERIC(20, 2) NOT NULL,
    flow_date  DATE         NOT NULL,
    note       TEXT,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cf_account ON cash_flow(account_id);

-- ── PriceHistory (Lịch sử giá) ───────────────────────────────
CREATE TABLE price_history (
    id          BIGSERIAL      PRIMARY KEY,
    ticker_id   BIGINT         NOT NULL REFERENCES ticker(id),
    price_date  DATE           NOT NULL,
    close_price NUMERIC(20, 2) NOT NULL,
    open_price  NUMERIC(20, 2),
    high_price  NUMERIC(20, 2),
    low_price   NUMERIC(20, 2),
    volume      BIGINT,
    source      VARCHAR(10),   -- MANUAL / CRAWL
    UNIQUE (ticker_id, price_date)
);

CREATE INDEX idx_ph_ticker_date ON price_history(ticker_id, price_date);

-- ── Position (Trạng thái nắm giữ tài sản) ────────────────────
CREATE TABLE position (
    id              BIGSERIAL      PRIMARY KEY,
    account_id      BIGINT         NOT NULL REFERENCES account(id),
    ticker_id       BIGINT         NOT NULL REFERENCES ticker(id),
    holding_volume  BIGINT         NOT NULL DEFAULT 0,
    avg_cost        NUMERIC(20, 4) NOT NULL DEFAULT 0,
    current_price   NUMERIC(20, 2) NOT NULL DEFAULT 0,
    unrealized_pn_l NUMERIC(20, 2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    UNIQUE (account_id, ticker_id)
);

-- ── Dữ liệu mẫu ──────────────────────────────────────────────
INSERT INTO broker (code, name, default_fee_rate, website) VALUES
    ('SSI',  'Công ty CP Chứng khoán SSI',         0.0015, 'https://www.ssi.com.vn'),
    ('TCBS', 'Công ty CP Chứng khoán Techcombank', 0.0015, 'https://www.tcbs.com.vn'),
    ('VPS',  'Công ty CP Chứng khoán VPS',          0.0015, 'https://www.vps.com.vn');

INSERT INTO ticker (symbol, name, type, exchange, industry) VALUES
    ('VNM',  'Công ty CP Sữa Việt Nam',            'STOCK', 'HOSE', 'Thực phẩm'),
    ('FPT',  'Công ty CP FPT',                      'STOCK', 'HOSE', 'Công nghệ'),
    ('VIC',  'Tập đoàn Vingroup',                   'STOCK', 'HOSE', 'Bất động sản'),
    ('ACB',  'Ngân hàng TMCP Á Châu',               'STOCK', 'HOSE', 'Ngân hàng'),
    ('HPG',  'Tập đoàn Hòa Phát',                   'STOCK', 'HOSE', 'Thép'),
    ('DCDS', 'Quỹ mở Dragon Capital',               'FUND_CERTIFICATE', NULL, NULL),
    ('GOLD', 'Vàng SJC',                            'GOLD',  NULL,   NULL);
