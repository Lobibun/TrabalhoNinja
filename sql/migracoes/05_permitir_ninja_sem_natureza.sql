-- Atualização para uma base que ainda exige uma natureza principal.
USE Trabalho_Ninja;

-- NULL indica que o ninja não tem uma natureza principal.
-- No aplicativo, esse ninja também fica sem registros na tabela de naturezas.
ALTER TABLE tb_ninja MODIFY COLUMN natureza_chakra INT NULL;
