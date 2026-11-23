-- Atualização para uma base criada antes do suporte a múltiplas naturezas de chakra.
USE Trabalho_Ninja;

CREATE TABLE IF NOT EXISTS tb_ninja_natureza_chakra (
    id_ninja INT NOT NULL,
    id_natureza INT NOT NULL,
    PRIMARY KEY (id_ninja, id_natureza),
    FOREIGN KEY (id_ninja) REFERENCES tb_ninja(id) ON DELETE CASCADE,
    FOREIGN KEY (id_natureza) REFERENCES tb_natureza_chakra(id)
);

-- Preserva a natureza registrada antes da atualização.
INSERT INTO tb_ninja_natureza_chakra (id_ninja, id_natureza)
SELECT n.id, n.natureza_chakra
FROM tb_ninja n
WHERE n.natureza_chakra IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM tb_ninja_natureza_chakra nn
    WHERE nn.id_ninja = n.id AND nn.id_natureza = n.natureza_chakra
);
