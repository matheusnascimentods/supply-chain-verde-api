-- Padroniza os emails do seed: usuários (jogadores) no domínio do time, @<time>.com, e companhias em
-- contato@<nome-da-companhia>.com. Filtra pelos emails e CNPJs exatos do seed para não tocar em cadastros reais.
-- Diferente da V10/V11, os eventos de auditoria desta migration ficam registrados.

-- 1. Os 11 jogadores do PSG da V2 perdem o .example (Liverpool, Arsenal, Chelsea e Man Utd já saíram na V11).
UPDATE users
SET email = REPLACE(email, '@psg.example.com', '@psg.com')
WHERE email IN ('khvicha.kvaratskhelia@psg.example.com', 'ousmane.dembele@psg.example.com',
                'desire.doue@psg.example.com', 'fabian.ruiz@psg.example.com', 'vitinha@psg.example.com',
                'joao.neves@psg.example.com', 'nuno.mendes@psg.example.com', 'willian.pacho@psg.example.com',
                'marquinhos@psg.example.com', 'warren.zaire-emery@psg.example.com',
                'matvey.safonov@psg.example.com');

-- 2. Companhias: nome sem acento, espaço nem hífen; os mercados mantêm o nome curto que já tinham.
CREATE TEMPORARY TABLE tmp_company_email (
    cnpj  CHAR(14)     NOT NULL PRIMARY KEY,
    email VARCHAR(180) NOT NULL
);

INSERT INTO tmp_company_email (cnpj, email) VALUES
    ('11444777000161', 'contato@mercadoverde.com'),
    ('20202020000152', 'contato@bompreco.com'),
    ('30303030000138', 'contato@valeverde.com'),
    ('40404040000113', 'contato@feiraviva.com'),
    ('12345678000195', 'contato@cooperativaverdedehortifruti.com'),
    ('23456789000195', 'contato@produtoresdecafesustentaveldointerior.com'),
    ('34567890000130', 'contato@beneficiadoracirculardegraos.com'),
    ('45678901000175', 'contato@agroflorestalrioclaro.com'),
    ('56789012000100', 'contato@alimentosorganicosdocampo.com'),
    ('67890123000116', 'contato@centrologisticoverdecampinas.com'),
    ('78901234000105', 'contato@distribuidoradeinsumosrenovaveis.com'),
    ('89012345000179', 'contato@transportesdebaixocarbono.com'),
    ('90123456000131', 'contato@embalagensrecicladassorocaba.com'),
    ('11223344000186', 'contato@auditoriasupplychainverde.com'),
    ('22334455000186', 'contato@redevarejistaconsumoconsciente.com'),
    ('10000000100018', 'contato@cooperativaraizesdocerrado.com'),
    ('10000001100143', 'contato@cacauicultoresdamataatlantica.com'),
    ('10000002100279', 'contato@quintaisprodutivosdonordeste.com'),
    ('10000003100302', 'contato@graosdaserracooperativa.com'),
    ('10000004100420', 'contato@castanhavivaextrativismo.com'),
    ('10000005100555', 'contato@acaidavarzeaprodutores.com'),
    ('10000006100680', 'contato@apiarioabelhasdovale.com'),
    ('10000007100706', 'contato@algodaojustodosemiarido.com'),
    ('10000008100831', 'contato@fruticultoresdovaledosaofrancisco.com'),
    ('10000009100967', 'contato@hortasurbanasintegradas.com'),
    ('10000010101025', 'contato@agroflorestabananeiras.com'),
    ('10000011101150', 'contato@ervamatenativadoparana.com'),
    ('10000012101286', 'contato@pecuariacampovivo.com'),
    ('10000013101301', 'contato@aviculturacaipirahorizonte.com'),
    ('10000014101437', 'contato@leitedepastocooperativasul.com'),
    ('10000015101562', 'contato@ovinossustentaveisdacampanha.com'),
    ('10000016101698', 'contato@laticiniosserraclara.com'),
    ('10000017101713', 'contato@cozinhacircularalimentos.com'),
    ('10000018101849', 'contato@frutasdovaleprocessamento.com'),
    ('10000019101974', 'contato@moinhosdotrigonacional.com'),
    ('10000020102032', 'contato@granolaraizbrasileira.com'),
    ('10000021102168', 'contato@engenhoorganicodamata.com'),
    ('10000022102293', 'contato@cafeliofilizadodobrasil.com'),
    ('10000023102319', 'contato@cooperativafarinhaverde.com'),
    ('10000024102444', 'contato@bebidasvegetaishorizonte.com'),
    ('10000025102570', 'contato@florestasrenovaveisdosul.com'),
    ('10000026102603', 'contato@madeiralegalamazonica.com'),
    ('10000027102720', 'contato@fibrasnaturaisdobrasil.com'),
    ('10000028102856', 'contato@confeccaocircularpaulista.com'),
    ('10000029102981', 'contato@compostovivoreciclagemorganica.com');

UPDATE company c
JOIN tmp_company_email t ON t.cnpj = c.cnpj
SET c.email = t.email;

DROP TEMPORARY TABLE tmp_company_email;
