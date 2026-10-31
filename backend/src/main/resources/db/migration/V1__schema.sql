-- Schema inicial (CLAUDE.md secao 3).

CREATE TABLE patterns (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    slug            VARCHAR(120) NOT NULL UNIQUE,
    description     TEXT NOT NULL,
    order_in_plan   INTEGER NULL -- nulo se o padrao so existe no pool de exploracao
);

CREATE TABLE problems (
    id                  BIGSERIAL PRIMARY KEY,
    external_slug       VARCHAR(200) NOT NULL UNIQUE,
    title               VARCHAR(300) NOT NULL,
    leetcode_url        VARCHAR(500) NOT NULL,
    difficulty_label    VARCHAR(20) NOT NULL,
    difficulty_rating   DOUBLE PRECISION NULL,
    is_core             BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_problems_is_core ON problems (is_core);

CREATE TABLE problem_patterns (
    problem_id  BIGINT NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    pattern_id  BIGINT NOT NULL REFERENCES patterns (id) ON DELETE CASCADE,
    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (problem_id, pattern_id)
);
CREATE INDEX idx_problem_patterns_pattern ON problem_patterns (pattern_id);

CREATE TABLE plan_items (
    id           BIGSERIAL PRIMARY KEY,
    day_number   INTEGER NOT NULL,
    pattern_id   BIGINT NOT NULL REFERENCES patterns (id),
    problem_id   BIGINT NOT NULL REFERENCES problems (id),
    slot         VARCHAR(20) NOT NULL DEFAULT 'novo',
    UNIQUE (day_number, problem_id)
);
CREATE INDEX idx_plan_items_day_number ON plan_items (day_number);

CREATE TABLE attempts (
    id                  BIGSERIAL PRIMARY KEY,
    problem_id          BIGINT NOT NULL REFERENCES problems (id),
    attempted_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    duration_seconds    INTEGER NOT NULL,
    result              VARCHAR(30) NOT NULL,
    confidence          SMALLINT NULL,
    note                TEXT NULL
);
CREATE INDEX idx_attempts_problem_id ON attempts (problem_id);
CREATE INDEX idx_attempts_attempted_at ON attempts (attempted_at);

CREATE TABLE review_state (
    problem_id          BIGINT PRIMARY KEY REFERENCES problems (id),
    stability_days      DOUBLE PRECISION NOT NULL,
    ema_quality         DOUBLE PRECISION NOT NULL,
    last_seen_at        TIMESTAMPTZ NOT NULL,
    attempts_count      INTEGER NOT NULL DEFAULT 0,
    successes_count     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE app_state (
    key     VARCHAR(60) PRIMARY KEY,
    value   VARCHAR(200) NOT NULL
);

-- Chaves conhecidas de app_state: current_phase, current_day, last_pure_repetition_day
INSERT INTO app_state (key, value) VALUES
    ('current_phase', 'introducao'),
    ('current_day', '1'),
    ('last_pure_repetition_day', '0');

-- Temas de leitura -- feed de papers acadêmicos (banco de dados, IA, ML
-- tradicional etc), não artigos de blog. search_query é texto livre usado
-- direto na busca da API de papers (ex: "database systems").
CREATE TABLE topics (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    search_query    VARCHAR(500) NOT NULL
);

-- Cache de papers por tema (14 dias, ver PapersService). Ordenados por
-- citation_count como proxy de "bem-sucedido" -- não existe fonte aberta
-- confiável de "papers premiados".
CREATE TABLE papers (
    id                  BIGSERIAL PRIMARY KEY,
    topic_id            BIGINT NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    title               VARCHAR(500) NOT NULL,
    url                 VARCHAR(1000) NOT NULL UNIQUE,
    source              VARCHAR(50) NOT NULL DEFAULT 'semanticscholar',
    venue               VARCHAR(300) NULL,
    publication_year    INTEGER NULL,
    citation_count      INTEGER NULL,
    authors             VARCHAR(500) NULL,
    cached_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_papers_topic_id ON papers (topic_id);
