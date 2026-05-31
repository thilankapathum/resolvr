-- ============================================================
-- V1__init_schema.sql
-- Resolvr - Initial Database Schema
-- ============================================================

-- ── COMPLAINTS REF SEQUENCE ───────────────────────────────────

CREATE TABLE regions (
                         id          BIGSERIAL PRIMARY KEY,
                         name        VARCHAR(100) NOT NULL UNIQUE,
                         created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE districts (
                           id          BIGSERIAL PRIMARY KEY,
                           name        VARCHAR(100) NOT NULL UNIQUE,
                           code        VARCHAR(10)  NOT NULL UNIQUE,
                           region_id   BIGINT REFERENCES regions(id) ON DELETE SET NULL,
                           created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
                       id                  BIGSERIAL PRIMARY KEY,
                       full_name           VARCHAR(150) NOT NULL,
                       email               VARCHAR(150) NOT NULL UNIQUE,
                       password_hash       VARCHAR(255) NOT NULL,
                       role                VARCHAR(30),
                       is_active           BOOLEAN NOT NULL DEFAULT FALSE,
                       region_id           BIGINT REFERENCES regions(id) ON DELETE SET NULL,
                       email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
                       verification_token  VARCHAR(255),
                       reset_token         VARCHAR(255),
                       reset_token_expiry  TIMESTAMP,
                       created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_districts (
                                user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                district_id BIGINT NOT NULL REFERENCES districts(id) ON DELETE CASCADE,
                                PRIMARY KEY (user_id, district_id)
);

CREATE TABLE refresh_tokens (
                                id          BIGSERIAL PRIMARY KEY,
                                user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token       VARCHAR(512) NOT NULL UNIQUE,
                                expiry      TIMESTAMP NOT NULL,
                                revoked     BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE complaints (
                            id                  BIGSERIAL PRIMARY KEY,

                            ref_number          VARCHAR(30) NOT NULL UNIQUE,
                            district_id         BIGINT NOT NULL REFERENCES districts(id),

                            status              VARCHAR(30)  NOT NULL DEFAULT 'NOT_ASSIGNED',
                            priority            VARCHAR(10)  NOT NULL,
                            target_date         TIMESTAMP NOT NULL,

                            created_by_id       BIGINT NOT NULL REFERENCES users(id),
                            assigned_to_id      BIGINT REFERENCES users(id),
                            raised_by           VARCHAR(150) NOT NULL,

                            customer_name       VARCHAR(150) NOT NULL,
                            contact_number      VARCHAR(30)  NOT NULL,
                            msisdns             VARCHAR(500) NOT NULL,
                            address             VARCHAR(500),
                            latitude            DECIMAL(10, 7),
                            longitude           DECIMAL(10, 7),
                            issue_category      VARCHAR(20)  NOT NULL,
                            issue_description   TEXT NOT NULL,
                            issue_duration      VARCHAR(100),
                            last_experienced    DATE,
                            technology          VARCHAR(5),
                            additional_info     TEXT,

                            device_type         VARCHAR(10),
                            signal_bars         SMALLINT CHECK (signal_bars BETWEEN 0 AND 5),
                            using_vpn_apn       BOOLEAN,

                            serving_sites_cells VARCHAR(500),
                            coverage_quality    VARCHAR(500),

                            customer_feedback_taken BOOLEAN NOT NULL DEFAULT FALSE,

                            created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                            updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                            resolved_at         TIMESTAMP,
                            closed_at           TIMESTAMP
);

CREATE TABLE complaint_ref_sequences (
                                         district_id  BIGINT NOT NULL REFERENCES districts(id),
                                         year_month   VARCHAR(6) NOT NULL,
                                         last_seq     INTEGER NOT NULL DEFAULT 0,
                                         PRIMARY KEY (district_id, year_month)
);

CREATE TABLE analysis_entries (
                                  id              BIGSERIAL PRIMARY KEY,
                                  complaint_id    BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
                                  author_id       BIGINT NOT NULL REFERENCES users(id),
                                  content         TEXT NOT NULL,
                                  is_edited       BOOLEAN NOT NULL DEFAULT FALSE,
                                  created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                                  updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE solution_entries (
                                  id                  BIGSERIAL PRIMARY KEY,
                                  complaint_id        BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
                                  author_id           BIGINT NOT NULL REFERENCES users(id),
                                  content             TEXT NOT NULL,
                                  solution_target_date DATE,
                                  remarks             TEXT,
                                  is_edited           BOOLEAN NOT NULL DEFAULT FALSE,
                                  created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                  updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
                            id              BIGSERIAL PRIMARY KEY,
                            complaint_id    BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
                            actor_id        BIGINT NOT NULL REFERENCES users(id),
                            action          VARCHAR(30) NOT NULL,
                            from_status     VARCHAR(30),
                            to_status       VARCHAR(30),
                            notes           TEXT,
                            created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── INDEXES ───────────────────────────────────────────────────

CREATE INDEX idx_complaints_status         ON complaints(status);
CREATE INDEX idx_complaints_district       ON complaints(district_id);
CREATE INDEX idx_complaints_assigned_to    ON complaints(assigned_to_id);
CREATE INDEX idx_complaints_created_by     ON complaints(created_by_id);
CREATE INDEX idx_complaints_priority       ON complaints(priority);
CREATE INDEX idx_complaints_created_at     ON complaints(created_at);
CREATE INDEX idx_analysis_complaint        ON analysis_entries(complaint_id);
CREATE INDEX idx_solution_complaint        ON solution_entries(complaint_id);
CREATE INDEX idx_audit_complaint           ON audit_logs(complaint_id);
CREATE INDEX idx_audit_actor               ON audit_logs(actor_id);
CREATE INDEX idx_refresh_token             ON refresh_tokens(token);
CREATE INDEX idx_users_email               ON users(email);
CREATE INDEX idx_users_role                ON users(role);
CREATE INDEX idx_users_region              ON users(region_id);
CREATE INDEX idx_districts_region          ON districts(region_id);

-- ── UPDATED_AT TRIGGER ────────────────────────────────────────

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_complaints_updated_at
    BEFORE UPDATE ON complaints
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_analysis_updated_at
    BEFORE UPDATE ON analysis_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_solution_updated_at
    BEFORE UPDATE ON solution_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_regions_updated_at
    BEFORE UPDATE ON regions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_districts_updated_at
    BEFORE UPDATE ON districts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();