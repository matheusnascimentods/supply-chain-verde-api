WITH product_seed(name, category, unit, description) AS (
    VALUES
        ('Café Bourbon Amarelo de Montanha', 'AGRICULTURE', 'KG', 'Grãos de altitude com origem rastreada, manejo sombreado e secagem em terreiro suspenso.'),
        ('Cacau de Agrofloresta do Sul da Bahia', 'AGRICULTURE', 'KG', 'Amêndoas cultivadas em sistema cabruca com conservação da Mata Atlântica.'),
        ('Quinoa Orgânica do Cerrado', 'AGRICULTURE', 'KG', 'Quinoa de produção orgânica com rotação de culturas e monitoramento do solo.'),
        ('Lentilha de Sequeiro', 'AGRICULTURE', 'KG', 'Leguminosa cultivada com baixo consumo de água e fixação natural de nitrogênio.'),
        ('Feijão-Caupi Agroecológico', 'AGRICULTURE', 'KG', 'Feijão de agricultura familiar com sementes crioulas e manejo agroecológico.'),
        ('Castanha-do-Pará de Manejo Comunitário', 'AGRICULTURE', 'KG', 'Castanha coletada por comunidades extrativistas em floresta nativa manejada.'),
        ('Açaí Orgânico Congelado', 'AGRICULTURE', 'KG', 'Polpa de açaí rastreada desde o manejo sustentável de várzea.'),
        ('Mel de Abelhas Nativas', 'AGRICULTURE', 'KG', 'Mel de abelhas sem ferrão produzido com práticas de conservação de polinizadores.'),
        ('Algodão Agroecológico em Pluma', 'AGRICULTURE', 'KG', 'Algodão cultivado sem agrotóxicos sintéticos por cooperativa de pequenos produtores.'),
        ('Semente de Girassol de Baixo Carbono', 'AGRICULTURE', 'KG', 'Sementes produzidas em rotação com cobertura permanente do solo.'),
        ('Uva de Mesa com Irrigação Eficiente', 'AGRICULTURE', 'KG', 'Uvas com irrigação por gotejamento e controle de uso de água por talhão.'),
        ('Tomate Italiano de Cultivo Protegido', 'AGRICULTURE', 'KG', 'Tomates com manejo integrado de pragas e reaproveitamento de água.'),
        ('Batata-Doce de Agricultura Familiar', 'AGRICULTURE', 'KG', 'Raízes produzidas em sistema diversificado e comercializadas em circuito curto.'),
        ('Banana Prata de Sistema Agroflorestal', 'AGRICULTURE', 'KG', 'Bananas cultivadas em consórcio agroflorestal com cobertura vegetal.'),
        ('Erva-Mate de Floresta Nativa', 'AGRICULTURE', 'KG', 'Erva-mate sombreada e manejada em floresta nativa com rastreabilidade.'),
        ('Ração Bovina com Coprodutos Vegetais', 'LIVESTOCK', 'TON', 'Ração com ingredientes rastreáveis e coprodutos da indústria de alimentos.'),
        ('Ovos Caipiras de Galinhas Livres', 'LIVESTOCK', 'UNIT', 'Ovos de sistema caipira com alimentação vegetal e bem-estar animal monitorado.'),
        ('Carne Bovina de Pastagem Regenerativa', 'LIVESTOCK', 'KG', 'Carne rastreada de propriedades com recuperação de pastagens e solo.'),
        ('Queijo Artesanal de Leite A2', 'LIVESTOCK', 'KG', 'Queijo produzido em pequena escala com leite de origem identificada.'),
        ('Lã de Ovelha de Manejo Responsável', 'LIVESTOCK', 'KG', 'Lã rastreada de rebanhos com práticas de bem-estar e manejo rotacionado.'),
        ('Iogurte Natural de Leite Orgânico', 'PROCESSED_FOOD', 'LITER', 'Iogurte sem aditivos artificiais feito com leite orgânico certificado.'),
        ('Óleo de Coco Extra Virgem', 'PROCESSED_FOOD', 'LITER', 'Óleo prensado a frio com aproveitamento integral do fruto.'),
        ('Polpa de Manga Congelada', 'PROCESSED_FOOD', 'KG', 'Polpa de frutas de fornecedores locais com reaproveitamento de resíduos.'),
        ('Macarrão Integral de Trigo Nacional', 'PROCESSED_FOOD', 'KG', 'Massa integral feita com trigo rastreado de produtores nacionais.'),
        ('Granola de Castanhas e Frutas', 'PROCESSED_FOOD', 'KG', 'Alimento processado com ingredientes rastreados e embalagem reciclável.'),
        ('Melado de Cana Orgânico', 'PROCESSED_FOOD', 'LITER', 'Melado de produção orgânica com uso eficiente de energia na moagem.'),
        ('Café Solúvel de Origem Certificada', 'PROCESSED_FOOD', 'KG', 'Café solúvel produzido com grãos certificados e energia renovável.'),
        ('Farinha de Banana Verde', 'PROCESSED_FOOD', 'KG', 'Farinha sem glúten feita com frutos fora do padrão comercial.'),
        ('Bebida Vegetal de Aveia', 'PROCESSED_FOOD', 'LITER', 'Bebida vegetal com aveia rastreada e embalagem de menor impacto.'),
        ('Papelão Ondulado de Fibra Reciclada', 'FORESTRY', 'TON', 'Papelão produzido com aparas recicladas e cadeia de custódia monitorada.'),
        ('Celulose de Eucalipto Certificado', 'FORESTRY', 'TON', 'Celulose de florestas plantadas certificadas e controle de origem.'),
        ('Painel de Madeira Reflorestada', 'FORESTRY', 'M3', 'Painéis feitos com madeira de florestas plantadas e resinas de baixa emissão.'),
        ('Carvão Vegetal de Manejo Sustentável', 'FORESTRY', 'TON', 'Carvão com origem legal rastreada e controle de emissões do processo.'),
        ('Tecido de Linho Brasileiro', 'TEXTILE', 'KG', 'Tecido de fibra natural com rastreabilidade de cultivo e tingimento responsável.'),
        ('Uniforme de Poliéster Reciclado', 'TEXTILE', 'UNIT', 'Uniforme confeccionado com fio reciclado e plano de logística reversa.'),
        ('Toalha de Algodão Orgânico', 'TEXTILE', 'UNIT', 'Toalha de algodão orgânico com processo de tingimento de baixo consumo hídrico.'),
        ('Bolsa de Fibras de Buriti', 'TEXTILE', 'UNIT', 'Acessório artesanal feito por cooperativa extrativista com fibras renováveis.'),
        ('Adubo Orgânico de Compostagem', 'OTHER', 'TON', 'Composto produzido a partir de resíduos orgânicos rastreados e desviados de aterro.'),
        ('Biogás de Resíduos Agroindustriais', 'OTHER', 'M3', 'Biogás gerado por digestão de resíduos orgânicos de cadeias agroindustriais.'),
        ('Caixa Térmica de Papel Reciclável', 'OTHER', 'UNIT', 'Embalagem de transporte reciclável com insumos de origem certificada.')
)
INSERT INTO product (name, category, unit, description)
SELECT seed.name, seed.category::product_category, seed.unit::product_unit, seed.description
FROM product_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM product existing WHERE existing.name = seed.name
);

WITH supplier_seed(name, cnpj, street, number, neighborhood, zip_code, city, state, phone) AS (
    VALUES
        ('Cooperativa Raízes do Cerrado', '10000000100018', 'Estrada do Cerrado', 'KM 12', 'Zona Rural', '73700-000', 'Planaltina', 'DF', '(61) 3333-2001'),
        ('Cacauicultores da Mata Atlântica', '10000001100143', 'Rodovia do Cacau', 'KM 28', 'Zona Rural', '45650-000', 'Ilhéus', 'BA', '(73) 3333-2002'),
        ('Quintais Produtivos do Nordeste', '10000002100279', 'Rua das Sementes', '85', 'Centro', '48900-000', 'Juazeiro', 'BA', '(74) 3333-2003'),
        ('Grãos da Serra Cooperativa', '10000003100302', 'Estrada da Serra', 'S/N', 'Zona Rural', '37500-000', 'Itajubá', 'MG', '(35) 3333-2004'),
        ('Castanha Viva Extrativismo', '10000004100420', 'Avenida Castanheira', '410', 'Floresta', '69900-000', 'Rio Branco', 'AC', '(68) 3333-2005'),
        ('Açaí da Várzea Produtores', '10000005100555', 'Travessa do Açaí', '120', 'Várzea', '66000-000', 'Belém', 'PA', '(91) 3333-2006'),
        ('Apiário Abelhas do Vale', '10000006100680', 'Rua das Colmeias', '73', 'Jardim Rural', '12200-000', 'São José dos Campos', 'SP', '(12) 3333-2007'),
        ('Algodão Justo do Semiárido', '10000007100706', 'Rodovia da Fibra', 'KM 6', 'Zona Rural', '58400-000', 'Campina Grande', 'PB', '(83) 3333-2008'),
        ('Fruticultores do Vale do São Francisco', '10000008100831', 'Avenida das Videiras', '560', 'Projeto Irrigado', '56300-000', 'Petrolina', 'PE', '(87) 3333-2009'),
        ('Hortas Urbanas Integradas', '10000009100967', 'Rua da Horta', '214', 'Jardim Novo', '80000-000', 'Curitiba', 'PR', '(41) 3333-2010'),
        ('Agrofloresta Bananeiras', '10000010101025', 'Estrada das Bananeiras', 'KM 3', 'Zona Rural', '11900-000', 'Registro', 'SP', '(13) 3333-2011'),
        ('Erva-Mate Nativa do Paraná', '10000011101150', 'Rua dos Ervais', '95', 'Centro', '84600-000', 'União da Vitória', 'PR', '(42) 3333-2012'),
        ('Pecuária Campo Vivo', '10000012101286', 'Estrada da Fazenda', 'S/N', 'Zona Rural', '79000-000', 'Campo Grande', 'MS', '(67) 3333-2013'),
        ('Avicultura Caipira Horizonte', '10000013101301', 'Rodovia dos Ovos', 'KM 9', 'Zona Rural', '35400-000', 'Ouro Preto', 'MG', '(31) 3333-2014'),
        ('Leite de Pasto Cooperativa Sul', '10000014101437', 'Avenida dos Laticínios', '310', 'Distrito Rural', '99000-000', 'Passo Fundo', 'RS', '(54) 3333-2015'),
        ('Ovinos Sustentáveis da Campanha', '10000015101562', 'Estrada da Campanha', 'KM 17', 'Zona Rural', '97500-000', 'Uruguaiana', 'RS', '(55) 3333-2016'),
        ('Laticínios Serra Clara', '10000016101698', 'Rua do Leite', '188', 'São Bento', '36300-000', 'São João del-Rei', 'MG', '(32) 3333-2017'),
        ('Cozinha Circular Alimentos', '10000017101713', 'Avenida do Alimento', '700', 'Distrito Industrial', '13000-000', 'Campinas', 'SP', '(19) 3333-2018'),
        ('Frutas do Vale Processamento', '10000018101849', 'Rua das Polpas', '240', 'Distrito Industrial', '59000-000', 'Natal', 'RN', '(84) 3333-2019'),
        ('Moinhos do Trigo Nacional', '10000019101974', 'Avenida do Moinho', '1020', 'Distrito Industrial', '90000-000', 'Porto Alegre', 'RS', '(51) 3333-2020'),
        ('Granola Raiz Brasileira', '10000020102032', 'Rua das Castanhas', '67', 'Centro', '30100-000', 'Belo Horizonte', 'MG', '(31) 3333-2021'),
        ('Engenho Orgânico da Mata', '10000021102168', 'Estrada do Engenho', 'KM 5', 'Zona Rural', '55600-000', 'Vitória de Santo Antão', 'PE', '(81) 3333-2022'),
        ('Café Liofilizado do Brasil', '10000022102293', 'Rua do Café', '422', 'Distrito Industrial', '14000-000', 'Ribeirão Preto', 'SP', '(16) 3333-2023'),
        ('Cooperativa Farinha Verde', '10000023102319', 'Rua da Mandioca', '36', 'Centro', '65000-000', 'São Luís', 'MA', '(98) 3333-2024'),
        ('Bebidas Vegetais Horizonte', '10000024102444', 'Avenida das Aveias', '940', 'Distrito Industrial', '88000-000', 'Florianópolis', 'SC', '(48) 3333-2025'),
        ('Florestas Renováveis do Sul', '10000025102570', 'Estrada da Celulose', 'KM 22', 'Zona Rural', '29000-000', 'Linhares', 'ES', '(27) 3333-2026'),
        ('Madeira Legal Amazônica', '10000026102603', 'Avenida do Manejo', 'S/N', 'Distrito Florestal', '68000-000', 'Santarém', 'PA', '(93) 3333-2027'),
        ('Fibras Naturais do Brasil', '10000027102720', 'Rua do Linho', '315', 'Centro', '64000-000', 'Teresina', 'PI', '(86) 3333-2028'),
        ('Confecção Circular Paulista', '10000028102856', 'Avenida Têxtil', '1250', 'Distrito Industrial', '07000-000', 'Guarulhos', 'SP', '(11) 3333-2029'),
        ('Composto Vivo Reciclagem Orgânica', '10000029102981', 'Estrada da Compostagem', 'KM 2', 'Zona Rural', '88000-001', 'Biguaçu', 'SC', '(48) 3333-2030')
)
INSERT INTO address (street, number, neighborhood, zip_code, city, state)
SELECT seed.street, seed.number, seed.neighborhood, seed.zip_code, seed.city, seed.state
FROM supplier_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM address existing
    WHERE existing.street = seed.street AND existing.number = seed.number
      AND existing.zip_code = seed.zip_code
);

WITH supplier_seed(name, cnpj, street, number, neighborhood, zip_code, city, state, phone) AS (
    VALUES
        ('Cooperativa Raízes do Cerrado', '10000000100018', 'Estrada do Cerrado', 'KM 12', 'Zona Rural', '73700-000', 'Planaltina', 'DF', '(61) 3333-2001'),
        ('Cacauicultores da Mata Atlântica', '10000001100143', 'Rodovia do Cacau', 'KM 28', 'Zona Rural', '45650-000', 'Ilhéus', 'BA', '(73) 3333-2002'),
        ('Quintais Produtivos do Nordeste', '10000002100279', 'Rua das Sementes', '85', 'Centro', '48900-000', 'Juazeiro', 'BA', '(74) 3333-2003'),
        ('Grãos da Serra Cooperativa', '10000003100302', 'Estrada da Serra', 'S/N', 'Zona Rural', '37500-000', 'Itajubá', 'MG', '(35) 3333-2004'),
        ('Castanha Viva Extrativismo', '10000004100420', 'Avenida Castanheira', '410', 'Floresta', '69900-000', 'Rio Branco', 'AC', '(68) 3333-2005'),
        ('Açaí da Várzea Produtores', '10000005100555', 'Travessa do Açaí', '120', 'Várzea', '66000-000', 'Belém', 'PA', '(91) 3333-2006'),
        ('Apiário Abelhas do Vale', '10000006100680', 'Rua das Colmeias', '73', 'Jardim Rural', '12200-000', 'São José dos Campos', 'SP', '(12) 3333-2007'),
        ('Algodão Justo do Semiárido', '10000007100706', 'Rodovia da Fibra', 'KM 6', 'Zona Rural', '58400-000', 'Campina Grande', 'PB', '(83) 3333-2008'),
        ('Fruticultores do Vale do São Francisco', '10000008100831', 'Avenida das Videiras', '560', 'Projeto Irrigado', '56300-000', 'Petrolina', 'PE', '(87) 3333-2009'),
        ('Hortas Urbanas Integradas', '10000009100967', 'Rua da Horta', '214', 'Jardim Novo', '80000-000', 'Curitiba', 'PR', '(41) 3333-2010'),
        ('Agrofloresta Bananeiras', '10000010101025', 'Estrada das Bananeiras', 'KM 3', 'Zona Rural', '11900-000', 'Registro', 'SP', '(13) 3333-2011'),
        ('Erva-Mate Nativa do Paraná', '10000011101150', 'Rua dos Ervais', '95', 'Centro', '84600-000', 'União da Vitória', 'PR', '(42) 3333-2012'),
        ('Pecuária Campo Vivo', '10000012101286', 'Estrada da Fazenda', 'S/N', 'Zona Rural', '79000-000', 'Campo Grande', 'MS', '(67) 3333-2013'),
        ('Avicultura Caipira Horizonte', '10000013101301', 'Rodovia dos Ovos', 'KM 9', 'Zona Rural', '35400-000', 'Ouro Preto', 'MG', '(31) 3333-2014'),
        ('Leite de Pasto Cooperativa Sul', '10000014101437', 'Avenida dos Laticínios', '310', 'Distrito Rural', '99000-000', 'Passo Fundo', 'RS', '(54) 3333-2015'),
        ('Ovinos Sustentáveis da Campanha', '10000015101562', 'Estrada da Campanha', 'KM 17', 'Zona Rural', '97500-000', 'Uruguaiana', 'RS', '(55) 3333-2016'),
        ('Laticínios Serra Clara', '10000016101698', 'Rua do Leite', '188', 'São Bento', '36300-000', 'São João del-Rei', 'MG', '(32) 3333-2017'),
        ('Cozinha Circular Alimentos', '10000017101713', 'Avenida do Alimento', '700', 'Distrito Industrial', '13000-000', 'Campinas', 'SP', '(19) 3333-2018'),
        ('Frutas do Vale Processamento', '10000018101849', 'Rua das Polpas', '240', 'Distrito Industrial', '59000-000', 'Natal', 'RN', '(84) 3333-2019'),
        ('Moinhos do Trigo Nacional', '10000019101974', 'Avenida do Moinho', '1020', 'Distrito Industrial', '90000-000', 'Porto Alegre', 'RS', '(51) 3333-2020'),
        ('Granola Raiz Brasileira', '10000020102032', 'Rua das Castanhas', '67', 'Centro', '30100-000', 'Belo Horizonte', 'MG', '(31) 3333-2021'),
        ('Engenho Orgânico da Mata', '10000021102168', 'Estrada do Engenho', 'KM 5', 'Zona Rural', '55600-000', 'Vitória de Santo Antão', 'PE', '(81) 3333-2022'),
        ('Café Liofilizado do Brasil', '10000022102293', 'Rua do Café', '422', 'Distrito Industrial', '14000-000', 'Ribeirão Preto', 'SP', '(16) 3333-2023'),
        ('Cooperativa Farinha Verde', '10000023102319', 'Rua da Mandioca', '36', 'Centro', '65000-000', 'São Luís', 'MA', '(98) 3333-2024'),
        ('Bebidas Vegetais Horizonte', '10000024102444', 'Avenida das Aveias', '940', 'Distrito Industrial', '88000-000', 'Florianópolis', 'SC', '(48) 3333-2025'),
        ('Florestas Renováveis do Sul', '10000025102570', 'Estrada da Celulose', 'KM 22', 'Zona Rural', '29000-000', 'Linhares', 'ES', '(27) 3333-2026'),
        ('Madeira Legal Amazônica', '10000026102603', 'Avenida do Manejo', 'S/N', 'Distrito Florestal', '68000-000', 'Santarém', 'PA', '(93) 3333-2027'),
        ('Fibras Naturais do Brasil', '10000027102720', 'Rua do Linho', '315', 'Centro', '64000-000', 'Teresina', 'PI', '(86) 3333-2028'),
        ('Confecção Circular Paulista', '10000028102856', 'Avenida Têxtil', '1250', 'Distrito Industrial', '07000-000', 'Guarulhos', 'SP', '(11) 3333-2029'),
        ('Composto Vivo Reciclagem Orgânica', '10000029102981', 'Estrada da Compostagem', 'KM 2', 'Zona Rural', '88000-001', 'Biguaçu', 'SC', '(48) 3333-2030')
)
INSERT INTO supplier (name, cnpj, address_id, phone)
SELECT seed.name, seed.cnpj, address.address_id, seed.phone
FROM supplier_seed seed
JOIN address ON address.street = seed.street
    AND address.number = seed.number
    AND address.zip_code = seed.zip_code
WHERE NOT EXISTS (
    SELECT 1 FROM supplier existing WHERE existing.cnpj = seed.cnpj
);
