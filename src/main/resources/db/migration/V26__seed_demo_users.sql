INSERT INTO users (name, email, password, role)
SELECT seed.name, seed.email, seed.password, seed.role::user_role
FROM (VALUES
    ('Khvicha Kvaratskhelia', 'khvicha.kvaratskhelia@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'SUPPLIER'),
    ('Ousmane Dembélé', 'ousmane.dembele@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'MANAGER'),
    ('Désiré Doué', 'desire.doue@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'AUDITOR'),
    ('Fabián Ruiz', 'fabian.ruiz@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'MANAGER'),
    ('Vitinha', 'vitinha@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'SUPPLIER'),
    ('João Neves', 'joao.neves@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'AUDITOR'),
    ('Nuno Mendes', 'nuno.mendes@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'MANAGER'),
    ('Willian Pacho', 'willian.pacho@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'SUPPLIER'),
    ('Marquinhos', 'marquinhos@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'AUDITOR'),
    ('Warren Zaïre-Emery', 'warren.zaire-emery@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'ADMIN'),
    ('Matvey Safonov', 'matvey.safonov@psg.example.com', '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm', 'ADMIN')
) AS seed(name, email, password, role)
WHERE NOT EXISTS (
    SELECT 1
    FROM users existing
    WHERE existing.email = seed.email
);
