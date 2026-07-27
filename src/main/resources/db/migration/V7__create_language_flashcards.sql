CREATE TABLE flashcard_decks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    context VARCHAR(255) NOT NULL,
    target_language VARCHAR(50) NOT NULL,
    base_language VARCHAR(50) NOT NULL,
    type VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_flashcard_decks_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_flashcard_decks_user_id
    ON flashcard_decks (user_id);

CREATE TABLE flashcard_tags (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_normalized VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_flashcard_tags_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_flashcard_tags_user_name_normalized UNIQUE (user_id, name_normalized)
);

CREATE INDEX idx_flashcard_tags_user_id
    ON flashcard_tags (user_id);

CREATE TABLE flashcards (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    deck_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    word VARCHAR(255),
    base_verb VARCHAR(255),
    past_simple VARCHAR(255),
    past_participle VARCHAR(255),
    expression VARCHAR(255),
    translation VARCHAR(255) NOT NULL,
    phonetic VARCHAR(255),
    level VARCHAR(50),
    usage_note VARCHAR(1000),
    active BOOLEAN NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    last_reviewed_at TIMESTAMP WITH TIME ZONE,
    next_review_at TIMESTAMP WITH TIME ZONE,
    review_count INTEGER NOT NULL,
    correct_count INTEGER NOT NULL,
    wrong_count INTEGER NOT NULL,
    consecutive_correct INTEGER NOT NULL,
    consecutive_wrong INTEGER NOT NULL,
    difficulty NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_flashcards_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_flashcards_deck
        FOREIGN KEY (deck_id)
        REFERENCES flashcard_decks (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_flashcards_user_active
    ON flashcards (user_id, active);

CREATE INDEX idx_flashcards_deck_id
    ON flashcards (deck_id);

CREATE UNIQUE INDEX uq_flashcards_deck_word
    ON flashcards (deck_id, lower(word))
    WHERE word IS NOT NULL;

CREATE UNIQUE INDEX uq_flashcards_deck_base_verb
    ON flashcards (deck_id, lower(base_verb))
    WHERE base_verb IS NOT NULL;

CREATE UNIQUE INDEX uq_flashcards_deck_expression
    ON flashcards (deck_id, lower(expression))
    WHERE expression IS NOT NULL;

CREATE TABLE flashcard_examples (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    text VARCHAR(1000) NOT NULL,
    translation VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_flashcard_examples_card
        FOREIGN KEY (card_id)
        REFERENCES flashcards (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_flashcard_examples_card_id
    ON flashcard_examples (card_id);

CREATE TABLE flashcard_card_tags (
    card_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (card_id, tag_id),
    CONSTRAINT fk_flashcard_card_tags_card
        FOREIGN KEY (card_id)
        REFERENCES flashcards (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_flashcard_card_tags_tag
        FOREIGN KEY (tag_id)
        REFERENCES flashcard_tags (id)
        ON DELETE CASCADE
);

CREATE TABLE flashcard_generation_jobs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    deck_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    context VARCHAR(255) NOT NULL,
    target_language VARCHAR(50) NOT NULL,
    base_language VARCHAR(50) NOT NULL,
    requested_count INTEGER NOT NULL,
    created_count INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_flashcard_generation_jobs_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_flashcard_generation_jobs_deck
        FOREIGN KEY (deck_id)
        REFERENCES flashcard_decks (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_flashcard_generation_jobs_user_id
    ON flashcard_generation_jobs (user_id);

CREATE TABLE flashcard_generation_batches (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    batch_number INTEGER NOT NULL,
    requested_count INTEGER NOT NULL,
    created_count INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_flashcard_generation_batches_job
        FOREIGN KEY (job_id)
        REFERENCES flashcard_generation_jobs (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_flashcard_generation_batches_job_number UNIQUE (job_id, batch_number)
);

CREATE TABLE flashcard_review_answers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    card_id UUID NOT NULL,
    rating VARCHAR(40) NOT NULL,
    answered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_flashcard_review_answers_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_flashcard_review_answers_card
        FOREIGN KEY (card_id)
        REFERENCES flashcards (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_flashcard_review_answers_user_answered_at
    ON flashcard_review_answers (user_id, answered_at);
