CREATE TABLE paciente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(160) NOT NULL,
    telefone VARCHAR(32) NOT NULL,
    autorizou_contato BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_paciente_telefone (telefone)
) ENGINE=InnoDB;

CREATE TABLE procedimento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(160) NOT NULL,
    tipo VARCHAR(16) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_procedimento_tipo CHECK (tipo IN ('EXAME', 'CONSULTA')),
    UNIQUE KEY uk_procedimento_nome_tipo (nome, tipo)
) ENGINE=InnoDB;

CREATE TABLE encaminhamento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    paciente_id BIGINT NOT NULL,
    procedimento_id BIGINT NOT NULL,
    necessita_retorno BOOLEAN NOT NULL DEFAULT FALSE,
    encaminhamento_origem_id BIGINT NULL,
    situacao VARCHAR(32) NOT NULL,
    data_criacao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_encaminhamento_paciente_situacao (paciente_id, situacao),
    KEY idx_encaminhamento_procedimento (procedimento_id),
    CONSTRAINT fk_encaminhamento_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id),
    CONSTRAINT fk_encaminhamento_procedimento FOREIGN KEY (procedimento_id) REFERENCES procedimento (id),
    CONSTRAINT fk_encaminhamento_origem FOREIGN KEY (encaminhamento_origem_id) REFERENCES encaminhamento (id),
    CONSTRAINT chk_encaminhamento_situacao CHECK (situacao IN (
        'AGUARDANDO_PRE_REQUISITO', 'PENDENTE_AGENDAMENTO', 'AGENDADO', 'CONCLUIDO',
        'NECESSITA_REAGENDAMENTO', 'NECESSITA_ASSISTENCIA', 'CANCELADO'
    )),
    CONSTRAINT chk_encaminhamento_origem CHECK (encaminhamento_origem_id IS NULL OR encaminhamento_origem_id <> id)
) ENGINE=InnoDB;

CREATE TABLE agendamento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    encaminhamento_id BIGINT NOT NULL,
    data_hora DATETIME(6) NOT NULL,
    nome_unidade VARCHAR(160) NOT NULL,
    situacao VARCHAR(24) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_agendamento_encaminhamento (encaminhamento_id),
    KEY idx_agendamento_data_situacao (data_hora, situacao),
    CONSTRAINT fk_agendamento_encaminhamento FOREIGN KEY (encaminhamento_id) REFERENCES encaminhamento (id),
    CONSTRAINT chk_agendamento_situacao CHECK (situacao IN ('AGENDADO', 'CONCLUIDO', 'AUSENTE', 'CANCELADO'))
) ENGINE=InnoDB;
