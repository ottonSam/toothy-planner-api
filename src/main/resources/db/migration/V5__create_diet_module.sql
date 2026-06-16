CREATE TABLE diet_foods (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    kcal_per_gram NUMERIC(19, 4) NOT NULL,
    protein_per_gram NUMERIC(19, 4) NOT NULL,
    carbohydrate_per_gram NUMERIC(19, 4) NOT NULL,
    fat_per_gram NUMERIC(19, 4) NOT NULL,
    kcal_per_portion NUMERIC(19, 4) NOT NULL,
    protein_per_portion NUMERIC(19, 4) NOT NULL,
    carbohydrate_per_portion NUMERIC(19, 4) NOT NULL,
    fat_per_portion NUMERIC(19, 4) NOT NULL,
    portion_description VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_diet_foods_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_diet_foods_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_diet_foods_user_id
    ON diet_foods (user_id);

CREATE TABLE diet_default_goals (
    id UUID PRIMARY KEY,
    kcal NUMERIC(19, 4) NOT NULL,
    protein NUMERIC(19, 4) NOT NULL,
    carbohydrate NUMERIC(19, 4) NOT NULL,
    fat NUMERIC(19, 4) NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_diet_default_goals_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_diet_default_goals_user UNIQUE (user_id)
);

CREATE TABLE daily_diet_goals (
    id UUID PRIMARY KEY,
    goal_date DATE NOT NULL,
    kcal NUMERIC(19, 4) NOT NULL,
    protein NUMERIC(19, 4) NOT NULL,
    carbohydrate NUMERIC(19, 4) NOT NULL,
    fat NUMERIC(19, 4) NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_daily_diet_goals_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_daily_diet_goals_user_date UNIQUE (user_id, goal_date)
);

CREATE INDEX idx_daily_diet_goals_user_date
    ON daily_diet_goals (user_id, goal_date);

CREATE TABLE diet_entries (
    id UUID PRIMARY KEY,
    food_id UUID NOT NULL,
    user_id UUID NOT NULL,
    entry_date DATE NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    kcal NUMERIC(19, 4) NOT NULL,
    protein NUMERIC(19, 4) NOT NULL,
    carbohydrate NUMERIC(19, 4) NOT NULL,
    fat NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_diet_entries_food
        FOREIGN KEY (food_id)
        REFERENCES diet_foods (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_diet_entries_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_diet_entries_user_date
    ON diet_entries (user_id, entry_date);

