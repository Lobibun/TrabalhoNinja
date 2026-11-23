-- Views do relatório. Na instalação inicial, execute após os scripts 01 e 02.
USE Trabalho_Ninja;

-- 1. Ninjas e suas respectivas missões
CREATE OR REPLACE VIEW vw_ninja_missoes AS
SELECT
    n.nome AS nome_ninja,
    v.nome AS vila,
    c.nome AS cla,
    rn.nome AS rank_ninja,
    m.titulo AS titulo_missao,
    rm.nome AS rank_missao,
    f.nome AS funcao,
    sm.nome AS status_missao
FROM tb_ninja_missao AS nm
INNER JOIN tb_ninja AS n ON n.id = nm.id_ninja
INNER JOIN tb_missao AS m ON m.id = nm.id_missao
INNER JOIN tb_vila AS v ON v.id = n.vila
INNER JOIN tb_cla AS c ON c.id = n.cla
INNER JOIN tb_rank_ninja AS rn ON rn.id = n.rank_ninja
INNER JOIN tb_rank_missao AS rm ON rm.id = m.rank_missao
INNER JOIN tb_funcao_missao AS f ON f.id = nm.funcao
INNER JOIN tb_status_missao AS sm ON sm.id = m.status;

-- 2. Quantidade de ninjas cadastrados por vila
CREATE OR REPLACE VIEW vw_total_ninjas_por_vila AS
SELECT
    v.nome AS vila,
    COUNT(*) AS quantidade_ninjas
FROM tb_ninja AS n
INNER JOIN tb_vila AS v ON v.id = n.vila
GROUP BY v.id, v.nome;

-- 3. Quantidade de missões cadastradas por rank
CREATE OR REPLACE VIEW vw_total_missoes_por_rank AS
SELECT
    rm.nome AS rank_missao,
    COUNT(*) AS quantidade_missoes
FROM tb_missao AS m
INNER JOIN tb_rank_missao AS rm ON rm.id = m.rank_missao
GROUP BY rm.id, rm.nome;

-- 4. Missões sem ninjas vinculados
CREATE OR REPLACE VIEW vw_missoes_sem_ninjas AS
SELECT
    m.id AS id_missao,
    m.titulo,
    rm.nome AS rank_missao,
    v.nome AS vila_origem,
    sm.nome AS status
FROM tb_missao AS m
INNER JOIN tb_rank_missao AS rm ON rm.id = m.rank_missao
INNER JOIN tb_vila AS v ON v.id = m.vila_origem
INNER JOIN tb_status_missao AS sm ON sm.id = m.status
LEFT JOIN tb_ninja_missao AS nm ON nm.id_missao = m.id
WHERE nm.id IS NULL;
