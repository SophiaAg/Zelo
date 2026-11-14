CREATE TABLE instituicao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_instituicao_nome (nome)
) ENGINE=InnoDB;

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(60) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    perfil VARCHAR(32) NOT NULL,
    UNIQUE KEY uk_usuario_email (email)
) ENGINE=InnoDB;

CREATE TABLE vinculo_institucional (
    usuario_id BIGINT NOT NULL,
    instituicao_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (usuario_id, instituicao_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    FOREIGN KEY (instituicao_id) REFERENCES instituicao (id)
) ENGINE=InnoDB;
