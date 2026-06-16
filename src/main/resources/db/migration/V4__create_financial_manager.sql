CREATE TABLE expense_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(100) NOT NULL,
    icon VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_expense_categories_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_expense_categories_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_expense_categories_user_id
    ON expense_categories (user_id);

CREATE TABLE expense_wallets (
    id UUID PRIMARY KEY,
    description VARCHAR(1024) NOT NULL,
    spending_goal NUMERIC(19, 2) NOT NULL,
    cycle_end_day INTEGER NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_expense_wallets_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_expense_wallets_user_description UNIQUE (user_id, description)
);

CREATE INDEX idx_expense_wallets_user_id
    ON expense_wallets (user_id);

CREATE TABLE expense_cycles (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    reference_month INTEGER NOT NULL,
    reference_year INTEGER NOT NULL,
    starts_at DATE NOT NULL,
    ends_at DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_expense_cycles_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES expense_wallets (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_expense_cycles_wallet_reference UNIQUE (wallet_id, reference_year, reference_month)
);

CREATE INDEX idx_expense_cycles_wallet_id
    ON expense_cycles (wallet_id);

CREATE TABLE installment_expenses (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    category_id UUID NOT NULL,
    description VARCHAR(1024) NOT NULL,
    total_amount NUMERIC(19, 2),
    installment_amount NUMERIC(19, 2),
    installments INTEGER NOT NULL,
    first_expense_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_installment_expenses_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES expense_wallets (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_installment_expenses_category
        FOREIGN KEY (category_id)
        REFERENCES expense_categories (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_installment_expenses_wallet_id
    ON installment_expenses (wallet_id);

CREATE TABLE recurring_expenses (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    category_id UUID NOT NULL,
    description VARCHAR(1024) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    starts_at DATE NOT NULL,
    canceled_at DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_recurring_expenses_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES expense_wallets (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_recurring_expenses_category
        FOREIGN KEY (category_id)
        REFERENCES expense_categories (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_recurring_expenses_wallet_id
    ON recurring_expenses (wallet_id);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    cycle_id UUID NOT NULL,
    category_id UUID NOT NULL,
    description VARCHAR(1024) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    expense_date DATE NOT NULL,
    type VARCHAR(30) NOT NULL,
    parent_expense_id UUID,
    installment_number INTEGER,
    installment_total INTEGER,
    recurrence_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_expenses_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES expense_wallets (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_expenses_cycle
        FOREIGN KEY (cycle_id)
        REFERENCES expense_cycles (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category
        FOREIGN KEY (category_id)
        REFERENCES expense_categories (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_expenses_parent
        FOREIGN KEY (parent_expense_id)
        REFERENCES installment_expenses (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_expenses_recurrence
        FOREIGN KEY (recurrence_id)
        REFERENCES recurring_expenses (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_expenses_wallet_id
    ON expenses (wallet_id);

CREATE INDEX idx_expenses_cycle_id
    ON expenses (cycle_id);

