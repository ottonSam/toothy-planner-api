CREATE TABLE ai_monthly_token_usage (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    period_start DATE NOT NULL,
    limit_tokens BIGINT NOT NULL,
    used_tokens BIGINT NOT NULL,
    reserved_tokens BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ai_monthly_token_usage_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_ai_monthly_token_usage_user_period UNIQUE (user_id, period_start),
    CONSTRAINT ck_ai_monthly_token_usage_limit CHECK (limit_tokens > 0),
    CONSTRAINT ck_ai_monthly_token_usage_used CHECK (used_tokens >= 0),
    CONSTRAINT ck_ai_monthly_token_usage_reserved CHECK (reserved_tokens >= 0),
    CONSTRAINT ck_ai_monthly_token_usage_total CHECK (used_tokens + reserved_tokens <= limit_tokens)
);

CREATE INDEX idx_ai_monthly_token_usage_user_period
    ON ai_monthly_token_usage (user_id, period_start);

CREATE TABLE ai_token_usage_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    monthly_usage_id UUID NOT NULL,
    feature VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reserved_tokens BIGINT NOT NULL,
    prompt_tokens BIGINT,
    completion_tokens BIGINT,
    total_tokens BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ai_token_usage_events_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ai_token_usage_events_monthly
        FOREIGN KEY (monthly_usage_id)
        REFERENCES ai_monthly_token_usage (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_ai_token_usage_events_reserved CHECK (reserved_tokens > 0),
    CONSTRAINT ck_ai_token_usage_events_prompt CHECK (prompt_tokens IS NULL OR prompt_tokens >= 0),
    CONSTRAINT ck_ai_token_usage_events_completion CHECK (completion_tokens IS NULL OR completion_tokens >= 0),
    CONSTRAINT ck_ai_token_usage_events_total CHECK (total_tokens IS NULL OR total_tokens >= 0)
);

CREATE INDEX idx_ai_token_usage_events_user_created_at
    ON ai_token_usage_events (user_id, created_at);

CREATE INDEX idx_ai_token_usage_events_monthly_status
    ON ai_token_usage_events (monthly_usage_id, status);
