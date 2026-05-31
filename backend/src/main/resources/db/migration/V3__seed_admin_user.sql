-- ============================================================
-- V3__seed_admin_user.sql
-- Default admin user. Password: Admin@1234
-- ============================================================

INSERT INTO users (full_name, email, password_hash, role, is_active, email_verified)
VALUES (
           'System Administrator',
           'admin@resolvr.local',
           '$2a$12$92CiBmEDSPENxCFnFhq2WewOTcGHF8t5Y0bSMQCYBhMNzHyNfVbaq',
           'ADMIN',
           TRUE,
           TRUE
       );