-- Seed de demonstração da feature 010: usuários para os fornecedores que ficaram sem usuário na V8 e três
-- mercados novos, que recebem os usuários, os lotes e os relatórios do mercado de demonstração.
-- Senha de todos os usuários novos: a mesma do V2 (rolocompressor06).

-- Seeds não geram eventos (RN-07 da V5). As triggers continuam ativas, então os eventos desta migration são
-- apagados no fim; o Flyway roda como root, que pode apagar em audit_log.
SET @audit_before = (SELECT COALESCE(MAX(id), 0) FROM audit_log);
SET @demo_market_id = (SELECT MIN(id) FROM company WHERE type = 'MARKET');
SET @password = '$2a$12$ZnoooZceq6Q6mdlwzKtqBOQ4.qsMSK4RL9vbJCLEsDbJQ05xK9hvm';

-- 1. Três mercados no plano PRO (10 usuários).
INSERT INTO address (street, number, neighborhood, complement, zip_code, city, state)
VALUES ('Avenida Brigadeiro Faria Lima', '1500', 'Pinheiros', 'Loja âncora', '01452-000', 'São Paulo', 'SP');
INSERT INTO company (name, cnpj, address_id, phone, email, type)
VALUES ('Mercado Bom Preço', '20202020000152', LAST_INSERT_ID(), '(11) 3100-0001', 'contato@bompreco.example.com', 'MARKET');
SET @market_1 = LAST_INSERT_ID();

INSERT INTO address (street, number, neighborhood, complement, zip_code, city, state)
VALUES ('Avenida Afonso Pena', '2300', 'Funcionários', NULL, '30130-000', 'Belo Horizonte', 'MG');
INSERT INTO company (name, cnpj, address_id, phone, email, type)
VALUES ('Supermercado Vale Verde', '30303030000138', LAST_INSERT_ID(), '(31) 3100-0002', 'contato@valeverde.example.com', 'MARKET');
SET @market_2 = LAST_INSERT_ID();

INSERT INTO address (street, number, neighborhood, complement, zip_code, city, state)
VALUES ('Rua XV de Novembro', '800', 'Centro', 'Centro de distribuição', '80020-000', 'Curitiba', 'PR');
INSERT INTO company (name, cnpj, address_id, phone, email, type)
VALUES ('Rede Feira Viva', '40404040000113', LAST_INSERT_ID(), '(41) 3100-0003', 'contato@feiraviva.example.com', 'MARKET');
SET @market_3 = LAST_INSERT_ID();

INSERT INTO company_plan (company_id, plan_id)
SELECT c.id, p.id
FROM company c
JOIN plan p ON p.code = 'PRO'
WHERE c.id IN (@market_1, @market_2, @market_3);

-- 2. Os 15 usuários do mercado de demonstração vão para os mercados novos: um ADMIN, dois MANAGER e dois AUDITOR
-- em cada um, para que todos tenham ADMIN (RN-08).
UPDATE users SET company_id = @market_1
WHERE company_id = @demo_market_id
  AND email IN ('h.kane@bayern.com', 'j.tah@bayern.com', 'j.kimmich@bayern.com', 'j.stanisic@bayern.com',
                'a.davies@bayern.com');
UPDATE users SET company_id = @market_2
WHERE company_id = @demo_market_id
  AND email IN ('warren.zaire-emery@psg.example.com', 'j.musiala@bayern.com', 'ousmane.dembele@psg.example.com',
                'm.olise@bayern.com', 'desire.doue@psg.example.com');
UPDATE users SET company_id = @market_3
WHERE company_id = @demo_market_id
  AND email IN ('matvey.safonov@psg.example.com', 'fabian.ruiz@psg.example.com', 'nuno.mendes@psg.example.com',
                'joao.neves@psg.example.com', 'marquinhos@psg.example.com');

-- 3. Jogadores novos. market preenchido: vão para aquele mercado com o perfil indicado; nulo: viram o ADMIN de um
-- fornecedor sem usuário, na ordem de seq.
CREATE TEMPORARY TABLE tmp_player (
    seq    INT PRIMARY KEY,
    name   VARCHAR(180) NOT NULL,
    email  VARCHAR(180) NOT NULL,
    role   VARCHAR(20)  NOT NULL,
    market TINYINT      NULL
);

INSERT INTO tmp_player (seq, name, email, role, market) VALUES
    (1, 'Alisson', 'alisson@liverpool.example.com', 'ADMIN', NULL),
    (2, 'M. Kerkez', 'm.kerkez@liverpool.example.com', 'ADMIN', NULL),
    (3, 'V. van Dijk', 'v.vandijk@liverpool.example.com', 'MANAGER', 1),
    (4, 'J. Jacquet', 'j.jacquet@liverpool.example.com', 'ADMIN', NULL),
    (5, 'R. Araújo', 'r.araujo@liverpool.example.com', 'ADMIN', NULL),
    (6, 'T. Nyoni', 't.nyoni@liverpool.example.com', 'ADMIN', NULL),
    (7, 'A. Mac Allister', 'a.macallister@liverpool.example.com', 'ADMIN', NULL),
    (8, 'B. Barcola', 'b.barcola@liverpool.example.com', 'ADMIN', NULL),
    (9, 'F. Wirtz', 'f.wirtz@liverpool.example.com', 'ADMIN', NULL),
    (10, 'C. Gakpo', 'c.gakpo@liverpool.example.com', 'ADMIN', NULL),
    (11, 'A. Isak', 'a.isak@liverpool.example.com', 'ADMIN', NULL),
    (12, 'D. Raya', 'd.raya@arsenal.example.com', 'ADMIN', NULL),
    (13, 'J. Timber', 'j.timber@arsenal.example.com', 'ADMIN', NULL),
    (14, 'E. Konsa', 'e.konsa@arsenal.example.com', 'ADMIN', NULL),
    (15, 'G. Magalhães', 'g.magalhaes@arsenal.example.com', 'ADMIN', NULL),
    (16, 'R. Calafiori', 'r.calafiori@arsenal.example.com', 'ADMIN', NULL),
    (17, 'D. Rice', 'd.rice@arsenal.example.com', 'AUDITOR', 1),
    (18, 'B. Guimarães', 'b.guimaraes@arsenal.example.com', 'ADMIN', NULL),
    (19, 'C. Tzolis', 'c.tzolis@arsenal.example.com', 'ADMIN', NULL),
    (20, 'N. Madueke', 'n.madueke@arsenal.example.com', 'ADMIN', NULL),
    (21, 'M. Ødegaard', 'm.odegaard@arsenal.example.com', 'AUDITOR', 1),
    (22, 'K. Havertz', 'k.havertz@arsenal.example.com', 'ADMIN', NULL),
    (23, 'M. Penders', 'm.penders@chelsea.example.com', 'ADMIN', NULL),
    (24, 'M. Palestra', 'm.palestra@chelsea.example.com', 'ADMIN', NULL),
    (25, 'L. Colwill', 'l.colwill@chelsea.example.com', 'ADMIN', NULL),
    (26, 'W. Fofana', 'w.fofana@chelsea.example.com', 'ADMIN', NULL),
    (27, 'J. Acheampong', 'j.acheampong@chelsea.example.com', 'ADMIN', NULL),
    (28, 'M. Caicedo', 'm.caicedo@chelsea.example.com', 'MANAGER', 2),
    (29, 'R. Lavia', 'r.lavia@chelsea.example.com', 'AUDITOR', 2),
    (30, 'M. Rogers', 'm.rogers@chelsea.example.com', 'ADMIN', NULL),
    (31, 'C. Palmer', 'c.palmer@chelsea.example.com', 'AUDITOR', 2),
    (32, 'Estêvão', 'estevao@chelsea.example.com', 'ADMIN', NULL),
    (33, 'P. Neto', 'p.neto@chelsea.example.com', 'ADMIN', NULL),
    (34, 'S. Lammens', 's.lammens@manutd.example.com', 'ADMIN', NULL),
    (35, 'L. Yoro', 'l.yoro@manutd.example.com', 'ADMIN', NULL),
    (36, 'H. Maguire', 'h.maguire@manutd.example.com', 'ADMIN', NULL),
    (37, 'L. Martínez', 'l.martinez@manutd.example.com', 'ADMIN', NULL),
    (38, 'D. Dalot', 'd.dalot@manutd.example.com', 'ADMIN', NULL),
    (39, 'A. Diallo', 'a.diallo@manutd.example.com', 'ADMIN', NULL),
    (40, 'C. Baleba', 'c.baleba@manutd.example.com', 'ADMIN', NULL),
    (41, 'B. Fernandes', 'b.fernandes@manutd.example.com', 'MANAGER', 3),
    (42, 'A. Santos', 'a.santos@manutd.example.com', 'ADMIN', NULL),
    (43, 'M. Cunha', 'm.cunha@manutd.example.com', 'AUDITOR', 3),
    (44, 'B. Mbeumo', 'b.mbeumo@manutd.example.com', 'AUDITOR', 3);

-- 3a. Nove jogadores nos mercados novos: cada mercado fica com 8 usuários.
INSERT INTO users (name, email, password, role, company_id)
SELECT name, email, @password, role, CASE market WHEN 1 THEN @market_1 WHEN 2 THEN @market_2 ELSE @market_3 END
FROM tmp_player
WHERE market IS NOT NULL;

-- 3b. Os outros 35 jogadores, um por fornecedor sem usuário, como ADMIN (RN-08).
CREATE TEMPORARY TABLE tmp_supplier_player AS
SELECT name, email, ROW_NUMBER() OVER (ORDER BY seq) AS position
FROM tmp_player
WHERE market IS NULL;

CREATE TEMPORARY TABLE tmp_supplier_without_user AS
SELECT c.id, ROW_NUMBER() OVER (ORDER BY c.id) AS position
FROM company c
WHERE c.type = 'SUPPLIER'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.company_id = c.id);

INSERT INTO users (name, email, password, role, company_id)
SELECT p.name, p.email, @password, 'ADMIN', s.id
FROM tmp_supplier_player p
JOIN tmp_supplier_without_user s ON s.position = p.position;

DROP TEMPORARY TABLE tmp_supplier_without_user;
DROP TEMPORARY TABLE tmp_supplier_player;
DROP TEMPORARY TABLE tmp_player;

-- 4. Lotes e relatórios do mercado de demonstração divididos por fornecedor: todos os lotes e relatórios de um
-- fornecedor vão para o mesmo mercado, para que os totais dos relatórios continuem batendo com os lotes (RN-23).
UPDATE batch
SET market_id = CASE supplier_id MOD 3 WHEN 0 THEN @market_1 WHEN 1 THEN @market_2 ELSE @market_3 END
WHERE market_id = @demo_market_id;

UPDATE report
SET market_id = CASE supplier_id MOD 3 WHEN 0 THEN @market_1 WHEN 1 THEN @market_2 ELSE @market_3 END
WHERE market_id = @demo_market_id;

DELETE FROM audit_log WHERE id > @audit_before;
