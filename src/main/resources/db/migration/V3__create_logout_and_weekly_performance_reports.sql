ALTER TABLE calendars
    ADD COLUMN week_starts_on VARCHAR(30),
    ADD COLUMN week_ends_on VARCHAR(30);

UPDATE calendars
SET week_starts_on = CASE EXTRACT(ISODOW FROM starts)
        WHEN 1 THEN 'MONDAY'
        WHEN 2 THEN 'TUESDAY'
        WHEN 3 THEN 'WEDNESDAY'
        WHEN 4 THEN 'THURSDAY'
        WHEN 5 THEN 'FRIDAY'
        WHEN 6 THEN 'SATURDAY'
        ELSE 'SUNDAY'
    END,
    week_ends_on = CASE EXTRACT(ISODOW FROM starts)
        WHEN 1 THEN 'SUNDAY'
        WHEN 2 THEN 'MONDAY'
        WHEN 3 THEN 'TUESDAY'
        WHEN 4 THEN 'WEDNESDAY'
        WHEN 5 THEN 'THURSDAY'
        WHEN 6 THEN 'FRIDAY'
        ELSE 'SATURDAY'
    END;

ALTER TABLE calendars
    ALTER COLUMN week_starts_on SET NOT NULL,
    ALTER COLUMN week_ends_on SET NOT NULL;

ALTER TABLE activities
    ADD COLUMN week_starts_at DATE,
    ADD COLUMN week_ends_at DATE;

UPDATE activities
SET week_starts_at = calendars.starts + ((activities.week - 1) * 7),
    week_ends_at = calendars.starts + ((activities.week - 1) * 7) + 6
FROM calendars
WHERE activities.calendar_id = calendars.id;

ALTER TABLE activities
    ALTER COLUMN week_starts_at SET NOT NULL,
    ALTER COLUMN week_ends_at SET NOT NULL;

CREATE TABLE weekly_performance_reports (
    id UUID PRIMARY KEY,
    calendar_id UUID NOT NULL,
    week INTEGER NOT NULL,
    week_starts_at DATE NOT NULL,
    week_ends_at DATE NOT NULL,
    user_feedback TEXT NOT NULL,
    metrics JSONB NOT NULL,
    markdown_report TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_weekly_performance_reports_calendar
        FOREIGN KEY (calendar_id)
        REFERENCES calendars (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_weekly_performance_reports_calendar_week UNIQUE (calendar_id, week)
);

CREATE INDEX idx_weekly_performance_reports_calendar_id
    ON weekly_performance_reports (calendar_id);
