ALTER TABLE encaminhamento
    ADD COLUMN instituicao_id BIGINT NULL,
    ADD FOREIGN KEY (instituicao_id) REFERENCES instituicao (id);

ALTER TABLE agendamento
    ADD COLUMN responsavel_id BIGINT NULL,
    ADD COLUMN registrado_por BIGINT NULL,
    ADD COLUMN registrado_em DATETIME(6) NULL,
    ADD COLUMN observacoes VARCHAR(2000) NULL,
    ADD FOREIGN KEY (responsavel_id) REFERENCES usuario (id),
    ADD FOREIGN KEY (registrado_por) REFERENCES usuario (id);
