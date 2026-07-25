ALTER TABLE expense_wallets
    ADD COLUMN starts_at DATE,
    ADD COLUMN target_spending_day INTEGER;

UPDATE expense_wallets wallet
SET starts_at = COALESCE(
        (
            SELECT MIN(cycle.starts_at)
            FROM expense_cycles cycle
            WHERE cycle.wallet_id = wallet.id
        ),
        CURRENT_DATE
    ),
    target_spending_day = cycle_end_day;

ALTER TABLE expense_wallets
    ALTER COLUMN starts_at SET NOT NULL,
    ALTER COLUMN target_spending_day SET NOT NULL,
    DROP COLUMN cycle_end_day;

ALTER TABLE expense_cycles
    ADD COLUMN target_spending_date DATE;

WITH target_candidates AS (
    SELECT
        cycle.id,
        cycle.starts_at,
        cycle.ends_at,
        make_date(
            EXTRACT(YEAR FROM cycle.starts_at)::INTEGER,
            EXTRACT(MONTH FROM cycle.starts_at)::INTEGER,
            LEAST(wallet.target_spending_day, EXTRACT(DAY FROM (date_trunc('month', cycle.starts_at) + INTERVAL '1 month - 1 day'))::INTEGER)
        ) AS start_month_target,
        make_date(
            EXTRACT(YEAR FROM cycle.ends_at)::INTEGER,
            EXTRACT(MONTH FROM cycle.ends_at)::INTEGER,
            LEAST(wallet.target_spending_day, EXTRACT(DAY FROM (date_trunc('month', cycle.ends_at) + INTERVAL '1 month - 1 day'))::INTEGER)
        ) AS end_month_target
    FROM expense_cycles cycle
    INNER JOIN expense_wallets wallet ON wallet.id = cycle.wallet_id
)
UPDATE expense_cycles cycle
SET target_spending_date = CASE
        WHEN target.start_month_target BETWEEN target.starts_at AND target.ends_at THEN target.start_month_target
        WHEN target.end_month_target BETWEEN target.starts_at AND target.ends_at THEN target.end_month_target
        ELSE target.ends_at
    END
FROM target_candidates target
WHERE target.id = cycle.id;

ALTER TABLE expense_cycles
    ALTER COLUMN target_spending_date SET NOT NULL;

ALTER TABLE expenses
    ADD COLUMN category VARCHAR(50),
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE installment_expenses
    ADD COLUMN category VARCHAR(50);

ALTER TABLE recurring_expenses
    ADD COLUMN category VARCHAR(50);

UPDATE expenses expense
SET category = CASE
        WHEN LOWER(category.name) LIKE '%aliment%' OR LOWER(category.name) LIKE '%mercado%' OR LOWER(category.name) LIKE '%restaurante%' THEN 'ALIMENTACAO'
        WHEN LOWER(category.name) LIKE '%moradia%' OR LOWER(category.name) LIKE '%casa%' OR LOWER(category.name) LIKE '%aluguel%' THEN 'MORADIA'
        WHEN LOWER(category.name) LIKE '%transporte%' OR LOWER(category.name) LIKE '%combust%' OR LOWER(category.name) LIKE '%uber%' THEN 'TRANSPORTE'
        WHEN LOWER(category.name) LIKE '%saude%' OR LOWER(category.name) LIKE '%saúde%' OR LOWER(category.name) LIKE '%remedio%' THEN 'SAUDE'
        WHEN LOWER(category.name) LIKE '%educa%' OR LOWER(category.name) LIKE '%curso%' THEN 'EDUCACAO'
        WHEN LOWER(category.name) LIKE '%lazer%' OR LOWER(category.name) LIKE '%viagem%' THEN 'LAZER'
        WHEN LOWER(category.name) LIKE '%servi%' OR LOWER(category.name) LIKE '%internet%' OR LOWER(category.name) LIKE '%telefone%' THEN 'SERVICOS'
        WHEN LOWER(category.name) LIKE '%compra%' OR LOWER(category.name) LIKE '%roupa%' THEN 'COMPRAS'
        WHEN LOWER(category.name) LIKE '%trabalho%' THEN 'TRABALHO'
        WHEN LOWER(category.name) LIKE '%pet%' THEN 'PETS'
        ELSE 'OUTROS'
    END
FROM expense_categories category
WHERE category.id = expense.category_id;

UPDATE installment_expenses installment
SET category = CASE
        WHEN LOWER(category.name) LIKE '%aliment%' OR LOWER(category.name) LIKE '%mercado%' OR LOWER(category.name) LIKE '%restaurante%' THEN 'ALIMENTACAO'
        WHEN LOWER(category.name) LIKE '%moradia%' OR LOWER(category.name) LIKE '%casa%' OR LOWER(category.name) LIKE '%aluguel%' THEN 'MORADIA'
        WHEN LOWER(category.name) LIKE '%transporte%' OR LOWER(category.name) LIKE '%combust%' OR LOWER(category.name) LIKE '%uber%' THEN 'TRANSPORTE'
        WHEN LOWER(category.name) LIKE '%saude%' OR LOWER(category.name) LIKE '%saúde%' OR LOWER(category.name) LIKE '%remedio%' THEN 'SAUDE'
        WHEN LOWER(category.name) LIKE '%educa%' OR LOWER(category.name) LIKE '%curso%' THEN 'EDUCACAO'
        WHEN LOWER(category.name) LIKE '%lazer%' OR LOWER(category.name) LIKE '%viagem%' THEN 'LAZER'
        WHEN LOWER(category.name) LIKE '%servi%' OR LOWER(category.name) LIKE '%internet%' OR LOWER(category.name) LIKE '%telefone%' THEN 'SERVICOS'
        WHEN LOWER(category.name) LIKE '%compra%' OR LOWER(category.name) LIKE '%roupa%' THEN 'COMPRAS'
        WHEN LOWER(category.name) LIKE '%trabalho%' THEN 'TRABALHO'
        WHEN LOWER(category.name) LIKE '%pet%' THEN 'PETS'
        ELSE 'OUTROS'
    END
FROM expense_categories category
WHERE category.id = installment.category_id;

UPDATE recurring_expenses recurring
SET category = CASE
        WHEN LOWER(category.name) LIKE '%aliment%' OR LOWER(category.name) LIKE '%mercado%' OR LOWER(category.name) LIKE '%restaurante%' THEN 'ALIMENTACAO'
        WHEN LOWER(category.name) LIKE '%moradia%' OR LOWER(category.name) LIKE '%casa%' OR LOWER(category.name) LIKE '%aluguel%' THEN 'MORADIA'
        WHEN LOWER(category.name) LIKE '%transporte%' OR LOWER(category.name) LIKE '%combust%' OR LOWER(category.name) LIKE '%uber%' THEN 'TRANSPORTE'
        WHEN LOWER(category.name) LIKE '%saude%' OR LOWER(category.name) LIKE '%saúde%' OR LOWER(category.name) LIKE '%remedio%' THEN 'SAUDE'
        WHEN LOWER(category.name) LIKE '%educa%' OR LOWER(category.name) LIKE '%curso%' THEN 'EDUCACAO'
        WHEN LOWER(category.name) LIKE '%lazer%' OR LOWER(category.name) LIKE '%viagem%' THEN 'LAZER'
        WHEN LOWER(category.name) LIKE '%servi%' OR LOWER(category.name) LIKE '%internet%' OR LOWER(category.name) LIKE '%telefone%' THEN 'SERVICOS'
        WHEN LOWER(category.name) LIKE '%compra%' OR LOWER(category.name) LIKE '%roupa%' THEN 'COMPRAS'
        WHEN LOWER(category.name) LIKE '%trabalho%' THEN 'TRABALHO'
        WHEN LOWER(category.name) LIKE '%pet%' THEN 'PETS'
        ELSE 'OUTROS'
    END
FROM expense_categories category
WHERE category.id = recurring.category_id;

UPDATE expenses SET category = 'OUTROS' WHERE category IS NULL;
UPDATE installment_expenses SET category = 'OUTROS' WHERE category IS NULL;
UPDATE recurring_expenses SET category = 'OUTROS' WHERE category IS NULL;

ALTER TABLE expenses
    ALTER COLUMN category SET NOT NULL,
    DROP CONSTRAINT fk_expenses_category,
    DROP COLUMN category_id;

ALTER TABLE installment_expenses
    ALTER COLUMN category SET NOT NULL,
    DROP CONSTRAINT fk_installment_expenses_category,
    DROP COLUMN category_id;

ALTER TABLE recurring_expenses
    ALTER COLUMN category SET NOT NULL,
    DROP CONSTRAINT fk_recurring_expenses_category,
    DROP COLUMN category_id;

DROP TABLE expense_categories;
