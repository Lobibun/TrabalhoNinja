-- Instalação completa em uma base nova. Em seguida, execute os scripts 02 e 03.
-- Para atualizar uma base antiga, consulte os scripts da pasta migracoes.
CREATE DATABASE IF NOT EXISTS Trabalho_Ninja
    DEFAULT CHARACTER SET utf8mb4;
USE Trabalho_Ninja;

-- Tabelas de referência
CREATE TABLE tb_status_missao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE tb_funcao_missao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE tb_rank_missao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome CHAR(1) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
    CONSTRAINT chk_rank_missao_nome CHECK (nome IN ('D', 'C', 'B', 'A', 'S'))
);

CREATE TABLE tb_status_ninja (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE tb_natureza_chakra (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE tb_rank_ninja (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE tb_cla (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE tb_vila (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(60) NOT NULL UNIQUE
);

-- Cadastros principais
CREATE TABLE tb_ninja (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    vila INT NOT NULL,
    cla INT NOT NULL,
    rank_ninja INT NOT NULL,
    natureza_chakra INT NULL,
    status INT NOT NULL,
    FOREIGN KEY (vila) REFERENCES tb_vila(id),
    FOREIGN KEY (cla) REFERENCES tb_cla(id),
    FOREIGN KEY (rank_ninja) REFERENCES tb_rank_ninja(id),
    FOREIGN KEY (natureza_chakra) REFERENCES tb_natureza_chakra(id),
    FOREIGN KEY (status) REFERENCES tb_status_ninja(id)
);

-- Um ninja pode ter várias naturezas de chakra ou nenhuma.
CREATE TABLE tb_ninja_natureza_chakra (
    id_ninja INT NOT NULL,
    id_natureza INT NOT NULL,
    PRIMARY KEY (id_ninja, id_natureza),
    FOREIGN KEY (id_ninja) REFERENCES tb_ninja(id) ON DELETE CASCADE,
    FOREIGN KEY (id_natureza) REFERENCES tb_natureza_chakra(id)
);

CREATE TABLE tb_missao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    descricao TEXT,
    rank_missao INT NOT NULL,
    vila_origem INT NOT NULL,
    status INT NOT NULL,
    FOREIGN KEY (rank_missao) REFERENCES tb_rank_missao(id),
    FOREIGN KEY (vila_origem) REFERENCES tb_vila(id),
    FOREIGN KEY (status) REFERENCES tb_status_missao(id)
);

-- Vínculo entre ninjas e missões
CREATE TABLE tb_ninja_missao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_ninja INT NOT NULL,
    id_missao INT NOT NULL,
    funcao INT NOT NULL,
    data_participacao DATE NOT NULL,
    FOREIGN KEY (id_ninja) REFERENCES tb_ninja(id),
    FOREIGN KEY (id_missao) REFERENCES tb_missao(id),
    FOREIGN KEY (funcao) REFERENCES tb_funcao_missao(id),
    CONSTRAINT uq_ninja_missao UNIQUE (id_ninja, id_missao)
);

CREATE TABLE tb_totalizador_ninja (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descricao TEXT NOT NULL,
    quantidade INT NOT NULL,
    data_geracao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
