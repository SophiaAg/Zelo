INSERT IGNORE INTO paciente (nome, telefone)
VALUES ('Marina de Demonstração', 'DEMO-001'), ('Rafael de Demonstração', 'DEMO-002');

INSERT IGNORE INTO procedimento (nome, tipo)
VALUES ('Consulta demonstrativa', 'CONSULTA'), ('Hemograma demonstrativo', 'EXAME');

INSERT INTO encaminhamento (paciente_id, procedimento_id, instituicao_id, situacao)
SELECT p.id, pr.id, i.id, 'AGENDADO'
FROM paciente p
JOIN procedimento pr ON
    (p.telefone = 'DEMO-001' AND pr.nome = 'Consulta demonstrativa' AND pr.tipo = 'CONSULTA')
    OR (p.telefone = 'DEMO-002' AND pr.nome = 'Hemograma demonstrativo' AND pr.tipo = 'EXAME')
JOIN instituicao i ON i.nome = 'Instituição demonstrativa Zelo'
LEFT JOIN encaminhamento e ON e.paciente_id = p.id AND e.procedimento_id = pr.id AND e.instituicao_id = i.id
WHERE e.id IS NULL;

INSERT INTO agendamento (encaminhamento_id, data_hora, nome_unidade, situacao, responsavel_id)
SELECT e.id, DATE(UTC_TIMESTAMP() - INTERVAL 3 HOUR), 'Unidade demonstrativa', 'AGENDADO',
    (SELECT MIN(v.usuario_id) FROM vinculo_institucional v WHERE v.instituicao_id = i.id AND v.ativo)
FROM encaminhamento e
JOIN instituicao i ON i.id = e.instituicao_id AND i.nome = 'Instituição demonstrativa Zelo'
JOIN paciente p ON p.id = e.paciente_id AND p.telefone IN ('DEMO-001', 'DEMO-002')
JOIN procedimento pr ON pr.id = e.procedimento_id AND pr.nome IN ('Consulta demonstrativa', 'Hemograma demonstrativo')
LEFT JOIN agendamento a ON a.encaminhamento_id = e.id
WHERE e.situacao = 'AGENDADO' AND a.id IS NULL;
