package br.com.zelo.profissional;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AcompanhamentoRepository {
    record Paciente(long id, String nome, String telefone, boolean ativo) {}
    record Atendimento(long id, long pacienteId, String paciente, String procedimento, String tipo,
            LocalDateTime dataHora, String unidade, String instituicao, String responsavel, String situacao,
            String situacaoEncaminhamento, String registradoPor, LocalDateTime registradoEm, String observacoes) {}
    record Resumo(long hoje, long pendentes, long faltas) {}

    private static final String ACESSO = """
            EXISTS (SELECT 1 FROM vinculo_institucional v
                JOIN instituicao ativa ON ativa.id = v.instituicao_id AND ativa.ativa
                WHERE v.usuario_id = ? AND v.ativo AND v.instituicao_id = e.instituicao_id)
            """;
    private static final String CONSULTA = """
            FROM agendamento a
            JOIN encaminhamento e ON e.id = a.encaminhamento_id
            JOIN paciente p ON p.id = e.paciente_id
            JOIN procedimento pr ON pr.id = e.procedimento_id
            JOIN instituicao i ON i.id = e.instituicao_id
            LEFT JOIN usuario r ON r.id = a.responsavel_id
            LEFT JOIN usuario autor ON autor.id = a.registrado_por
            WHERE
            """ + ACESSO;

    private final JdbcTemplate jdbc;

    AcompanhamentoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Paciente> pacientes(long usuario, String busca, Long id) {
        return jdbc.query("""
                SELECT p.id, p.nome, p.telefone,
                    MAX(e.situacao NOT IN ('CONCLUIDO', 'CANCELADO')) AS ativo
                FROM paciente p JOIN encaminhamento e ON e.paciente_id = p.id
                WHERE
                """ + ACESSO + """
                AND (LOCATE(LOWER(?), LOWER(p.nome)) > 0 OR CAST(p.id AS CHAR) = ?)
                AND (? IS NULL OR p.id = ?)
                GROUP BY p.id, p.nome, p.telefone ORDER BY p.nome, p.id
                """, (rs, n) -> new Paciente(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)),
                usuario, busca, busca, id, id);
    }

    Resumo resumo(long usuario, LocalDateTime dia, LocalDateTime amanha) {
        return jdbc.queryForObject("""
                SELECT COUNT(CASE WHEN a.situacao = 'AGENDADO' AND e.situacao = 'AGENDADO'
                        AND a.data_hora >= ? AND a.data_hora < ? THEN 1 END),
                    COUNT(CASE WHEN a.situacao = 'AGENDADO' AND e.situacao = 'AGENDADO'
                        AND a.data_hora < ? THEN 1 END),
                    COUNT(CASE WHEN a.situacao = 'AUSENTE' THEN 1 END)
                """ + CONSULTA,
                (rs, n) -> new Resumo(rs.getLong(1), rs.getLong(2), rs.getLong(3)), dia, amanha, amanha, usuario);
    }

    List<Atendimento> atendimentos(String filtro, Object... parametros) {
        return jdbc.query("""
                SELECT a.id, p.id, p.nome, pr.nome,
                    CASE WHEN e.encaminhamento_origem_id IS NOT NULL THEN 'RETORNO' ELSE pr.tipo END,
                    a.data_hora, a.nome_unidade, i.nome, r.nome, a.situacao, e.situacao,
                    autor.nome, a.registrado_em, a.observacoes
                """ + CONSULTA + filtro, (rs, n) -> new Atendimento(rs.getLong(1), rs.getLong(2),
                rs.getString(3), rs.getString(4), rs.getString(5), rs.getObject(6, LocalDateTime.class),
                rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11),
                rs.getString(12), rs.getObject(13, LocalDateTime.class), rs.getString(14)), parametros);
    }

    int registrar(long usuario, long id, String situacao, String encaminhamento, String observacoes,
            LocalDateTime agora) {
        return jdbc.update("""
                UPDATE agendamento a JOIN encaminhamento e ON e.id = a.encaminhamento_id
                SET a.situacao = ?, e.situacao = ?, a.registrado_por = ?, a.registrado_em = ?, a.observacoes = ?
                WHERE a.id = ? AND a.situacao = 'AGENDADO' AND e.situacao = 'AGENDADO'
                    AND a.data_hora <= ? AND
                """ + ACESSO, situacao, encaminhamento, usuario, agora, observacoes, id, agora, usuario);
    }
}
