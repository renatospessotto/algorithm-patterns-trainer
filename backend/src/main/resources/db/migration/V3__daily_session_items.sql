-- Fixa os slots da sessão do dia assim que são decididos (SessionPlannerService),
-- em vez de recalcular do zero a cada GET /today. Sem isso, um problema
-- resolvido em uma chamada deixava de ser "não introduzido" na chamada
-- seguinte e era silenciosamente trocado por outro, ou um pick aleatório de
-- pool (fase consolidação) mudava a cada request -- os dois casos faziam o
-- exercício já feito hoje "sumir" da tela inicial.
CREATE TABLE daily_session_items (
    id          BIGSERIAL PRIMARY KEY,
    day_number  INTEGER NOT NULL,
    slot_index  INTEGER NOT NULL,
    problem_id  BIGINT NOT NULL REFERENCES problems (id),
    origin      VARCHAR(20) NOT NULL,
    urgency     DOUBLE PRECISION NULL,
    UNIQUE (day_number, slot_index)
);
CREATE INDEX idx_daily_session_items_day_number ON daily_session_items (day_number);
