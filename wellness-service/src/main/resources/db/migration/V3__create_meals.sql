CREATE TABLE meals (
    id                    UUID PRIMARY KEY,
    user_id               UUID         NOT NULL,
    meal_type             VARCHAR(20)  NOT NULL,
    description           VARCHAR(1000) NOT NULL,
    meal_time             TIMESTAMPTZ  NOT NULL,
    total_calories        DOUBLE PRECISION,
    protein_grams         DOUBLE PRECISION,
    carbohydrate_grams    DOUBLE PRECISION,
    fat_grams             DOUBLE PRECISION,
    fibre_grams           DOUBLE PRECISION,
    notes                 VARCHAR(2000),
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,

    CONSTRAINT chk_meal_calories_non_negative CHECK (total_calories IS NULL OR total_calories >= 0),
    CONSTRAINT chk_meal_protein_non_negative CHECK (protein_grams IS NULL OR protein_grams >= 0),
    CONSTRAINT chk_meal_carbs_non_negative CHECK (carbohydrate_grams IS NULL OR carbohydrate_grams >= 0),
    CONSTRAINT chk_meal_fat_non_negative CHECK (fat_grams IS NULL OR fat_grams >= 0),
    CONSTRAINT chk_meal_fibre_non_negative CHECK (fibre_grams IS NULL OR fibre_grams >= 0)
);

CREATE INDEX idx_meals_user_id ON meals (user_id);
CREATE INDEX idx_meals_user_meal_time ON meals (user_id, meal_time);
CREATE INDEX idx_meals_not_deleted ON meals (user_id) WHERE deleted = FALSE;
