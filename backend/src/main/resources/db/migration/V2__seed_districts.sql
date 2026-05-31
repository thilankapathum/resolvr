-- ============================================================
-- V2__seed_districts.sql
-- All 25 administrative districts of Sri Lanka
-- Regions are left unassigned (NULL) — Admin assigns them
-- ============================================================

INSERT INTO districts (name, code) VALUES
    ('Colombo',        'CO'),
    ('Gampaha',        'GM'),
    ('Kalutara',       'KL'),
    ('Kandy',          'KY'),
    ('Matale',         'ML'),
    ('Nuwara Eliya',   'NU'),
    ('Galle',          'GL'),
    ('Matara',         'MT'),
    ('Hambantota',     'HM'),
    ('Jaffna',         'JF'),
    ('Kilinochchi',    'KN'),
    ('Mannar',         'MR'),
    ('Vavuniya',       'VU'),
    ('Mullaitivu',     'MU'),
    ('Batticaloa',     'BT'),
    ('Ampara',         'AM'),
    ('Trincomalee',    'TR'),
    ('Kurunegala',     'KU'),
    ('Puttalam',       'PT'),
    ('Anuradhapura',   'AN'),
    ('Polonnaruwa',    'PO'),
    ('Badulla',        'BD'),
    ('Monaragala',     'MN'),
    ('Ratnapura',      'RT'),
    ('Kegalle',        'KG');