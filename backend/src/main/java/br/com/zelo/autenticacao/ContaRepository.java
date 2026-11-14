package br.com.zelo.autenticacao;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ContaRepository {
    record Conta(long id, String nome, String email, String senhaHash, boolean ativa,
                 String perfil, boolean vinculoAtivo) {
        boolean autorizada() {
            return ativa && vinculoAtivo && "PROFESSIONAL".equals(perfil);
        }
    }

    private static final String CONSULTA = """
            SELECT u.id, u.nome, u.email, u.senha_hash, u.ativo, u.perfil,
                EXISTS (SELECT 1 FROM vinculo_institucional v
                    JOIN instituicao i ON i.id = v.instituicao_id
                    WHERE v.usuario_id = u.id AND v.ativo AND i.ativa) AS vinculo_ativo
            FROM usuario u
            """;

    private final JdbcTemplate jdbc;

    ContaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<Conta> porEmail(String email) {
        return buscar(" WHERE u.email = ?", email);
    }

    Optional<Conta> porId(long id) {
        return buscar(" WHERE u.id = ?", id);
    }

    private Optional<Conta> buscar(String filtro, Object valor) {
        return jdbc.query(CONSULTA + filtro, (rs, linha) -> new Conta(
                rs.getLong("id"), rs.getString("nome"), rs.getString("email"),
                rs.getString("senha_hash"), rs.getBoolean("ativo"), rs.getString("perfil"),
                rs.getBoolean("vinculo_ativo")), valor).stream().findFirst();
    }
}
