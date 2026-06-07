CREATE TABLE goals (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    is_complete BOOLEAN NOT NULL DEFAULT FALSE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_goals_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_goals_user_id
    ON goals (user_id);

CREATE TABLE calendars (
    id UUID PRIMARY KEY,
    description VARCHAR(1024) NOT NULL,
    weeks INTEGER NOT NULL,
    starts DATE NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_calendars_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_calendars_user_id
    ON calendars (user_id);

CREATE TABLE calendar_goals (
    calendar_id UUID NOT NULL,
    goal_id UUID NOT NULL,
    PRIMARY KEY (calendar_id, goal_id),
    CONSTRAINT fk_calendar_goals_calendar
        FOREIGN KEY (calendar_id)
        REFERENCES calendars (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_calendar_goals_goal
        FOREIGN KEY (goal_id)
        REFERENCES goals (id)
        ON DELETE CASCADE
);

CREATE TABLE activities (
    id UUID PRIMARY KEY,
    description VARCHAR(1024) NOT NULL,
    week INTEGER NOT NULL,
    calendar_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    goal INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_activities_calendar
        FOREIGN KEY (calendar_id)
        REFERENCES calendars (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_activities_calendar_id
    ON activities (calendar_id);

CREATE TABLE activity_progress_days (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL,
    week_day VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_activity_progress_days_activity
        FOREIGN KEY (activity_id)
        REFERENCES activities (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_activity_progress_days_activity_day UNIQUE (activity_id, week_day)
);

CREATE TABLE activity_progress_counts (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL,
    progress_value INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_activity_progress_counts_activity
        FOREIGN KEY (activity_id)
        REFERENCES activities (id)
        ON DELETE CASCADE
);

CREATE TABLE activity_progress_times (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL,
    minutes INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_activity_progress_times_activity
        FOREIGN KEY (activity_id)
        REFERENCES activities (id)
        ON DELETE CASCADE
);
