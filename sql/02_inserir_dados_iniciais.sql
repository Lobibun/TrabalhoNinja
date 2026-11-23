-- Dados de exemplo para a instalação inicial, após o script 01.
USE Trabalho_Ninja;
START TRANSACTION;

-- Tabelas de referência
INSERT INTO tb_vila (nome)
VALUES ('Konoha'), ('Suna'), ('Kiri'), ('Kumo'), ('Iwa')
ON DUPLICATE KEY UPDATE nome = tb_vila.nome;

INSERT INTO tb_cla (nome)
VALUES
    ('Uzumaki'), ('Uchiha'), ('Haruno'), ('Hatake'), ('Kazekage'),
    ('Hyuga'), ('Nara'), ('Akimichi'), ('Yamanaka'), ('Inuzuka'),
    ('Aburame'), ('Senju')
ON DUPLICATE KEY UPDATE nome = tb_cla.nome;

INSERT INTO tb_rank_ninja (nome)
VALUES ('Genin'), ('Chunin'), ('Jounin'), ('Kage')
ON DUPLICATE KEY UPDATE nome = tb_rank_ninja.nome;

INSERT INTO tb_natureza_chakra (nome)
VALUES ('Fogo'), ('Vento'), ('Raio'), ('Terra'), ('Água'), ('Areia')
ON DUPLICATE KEY UPDATE nome = tb_natureza_chakra.nome;

INSERT INTO tb_status_ninja (nome)
VALUES ('Ativo'), ('Inativo')
ON DUPLICATE KEY UPDATE nome = tb_status_ninja.nome;

INSERT INTO tb_rank_missao (nome)
VALUES ('D'), ('C'), ('B'), ('A'), ('S')
ON DUPLICATE KEY UPDATE nome = tb_rank_missao.nome;

INSERT INTO tb_status_missao (nome)
VALUES ('Aberta'), ('Em andamento'), ('Concluída'), ('Cancelada')
ON DUPLICATE KEY UPDATE nome = tb_status_missao.nome;

INSERT INTO tb_funcao_missao (nome)
VALUES ('Líder'), ('Ataque'), ('Suporte'), ('Sensorial'), ('Médico'), ('Defesa')
ON DUPLICATE KEY UPDATE nome = tb_funcao_missao.nome;

-- Insere os exemplos somente quando os dois cadastros principais estão vazios.
SET @inserir_exemplos = NOT EXISTS (SELECT 1 FROM tb_ninja)
    AND NOT EXISTS (SELECT 1 FROM tb_missao);

INSERT INTO tb_ninja (nome, vila, cla, rank_ninja, natureza_chakra, status)
SELECT
    'Naruto',
    (SELECT id FROM tb_vila WHERE nome = 'Konoha'),
    (SELECT id FROM tb_cla WHERE nome = 'Uzumaki'),
    (SELECT id FROM tb_rank_ninja WHERE nome = 'Genin'),
    (SELECT id FROM tb_natureza_chakra WHERE nome = 'Vento'),
    (SELECT id FROM tb_status_ninja WHERE nome = 'Ativo')
WHERE @inserir_exemplos;
SET @id_naruto_exemplo = IF(@inserir_exemplos, LAST_INSERT_ID(), NULL);

INSERT INTO tb_ninja_natureza_chakra (id_ninja, id_natureza)
SELECT id, natureza_chakra
FROM tb_ninja
WHERE id = @id_naruto_exemplo;

-- Missões de exemplo
INSERT INTO tb_missao (titulo, descricao, rank_missao, vila_origem, status)
SELECT
        'Capturar gato perdido',
        'Localizar o gato e devolvê-lo ao dono.',
        (SELECT id FROM tb_rank_missao WHERE nome = 'D'),
        (SELECT id FROM tb_vila WHERE nome = 'Konoha'),
        (SELECT id FROM tb_status_missao WHERE nome = 'Aberta')
WHERE @inserir_exemplos;
SET @id_missao_exemplo = IF(@inserir_exemplos, LAST_INSERT_ID(), NULL);

INSERT INTO tb_missao (titulo, descricao, rank_missao, vila_origem, status)
SELECT
        'Escolta de comerciante',
        'Acompanhar o comerciante até seu destino.',
        (SELECT id FROM tb_rank_missao WHERE nome = 'C'),
        (SELECT id FROM tb_vila WHERE nome = 'Konoha'),
        (SELECT id FROM tb_status_missao WHERE nome = 'Aberta')
WHERE @inserir_exemplos;

-- Participação do Naruto na missão de rank D
INSERT INTO tb_ninja_missao (id_ninja, id_missao, funcao, data_participacao)
SELECT
    @id_naruto_exemplo,
    @id_missao_exemplo,
    (SELECT id FROM tb_funcao_missao WHERE nome = 'Suporte'),
    CURRENT_DATE
WHERE @inserir_exemplos;

COMMIT;
