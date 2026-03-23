-- Add matched_price and normalize matched_value semantics.
DO $$
DECLARE
    tx_table regclass;
BEGIN
    -- Self-heal: some environments have flyway version=1 but missing transaction table.
    IF to_regclass('public.transaction') IS NULL
       AND to_regclass('public.transactions') IS NULL
       AND to_regclass('transaction') IS NULL
       AND to_regclass('transactions') IS NULL THEN
        CREATE TABLE public.transaction (
            id             BIGSERIAL      PRIMARY KEY,
            order_no       VARCHAR(50),
            trading_date   DATE           NOT NULL,
            trade          VARCHAR(10)    NOT NULL,
            ticker_id      BIGINT         NOT NULL REFERENCES ticker(id),
            account_id     BIGINT         NOT NULL REFERENCES account(id),
            stock_exchange VARCHAR(10),
            order_type     VARCHAR(20),
            channel        VARCHAR(30),
            volume         BIGINT         NOT NULL,
            order_price    NUMERIC(20, 2) NOT NULL,
            matched_volume BIGINT,
            matched_price  NUMERIC(20, 2),
            matched_value  NUMERIC(20, 2),
            fee            NUMERIC(20, 2),
            tax            NUMERIC(20, 2),
            cost           NUMERIC(20, 2),
            return_amount  NUMERIC(20, 2),
            status         VARCHAR(20)    NOT NULL DEFAULT 'COMPLETED',
            note           TEXT,
            created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
            updated_at     TIMESTAMP      NOT NULL DEFAULT NOW()
        );

        CREATE INDEX IF NOT EXISTS idx_tx_ticker ON public.transaction(ticker_id);
        CREATE INDEX IF NOT EXISTS idx_tx_trading_date ON public.transaction(trading_date);
        CREATE INDEX IF NOT EXISTS idx_tx_account ON public.transaction(account_id);
    END IF;

    tx_table := COALESCE(
        to_regclass('public.transaction'),
        to_regclass('public.transactions'),
        to_regclass('transaction'),
        to_regclass('transactions'));

    IF tx_table IS NULL THEN
        RAISE EXCEPTION 'Cannot find transaction table. Expected one of: public.transaction, public.transactions';
    END IF;

    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS matched_price NUMERIC(20, 2)', tx_table);

    -- Existing matched_value data in old schema stored matched price.
    EXECUTE format('UPDATE %s SET matched_volume = COALESCE(matched_volume, volume), matched_price = COALESCE(matched_price, matched_value, order_price)', tx_table);

    -- Rebuild matched_value as matched_price * matched_volume.
    EXECUTE format('UPDATE %s SET matched_value = COALESCE(matched_price, order_price) * COALESCE(matched_volume, volume)', tx_table);
END $$;
