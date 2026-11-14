package br.com.zelo.autenticacao;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
class UsuarioDemonstracao implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder senhas;
    private final String email;
    private final String senha;

    UsuarioDemonstracao(JdbcTemplate jdbc, PasswordEncoder senhas,
            @Value("${ZELO_DEMO_EMAIL:}") String email, @Value("${ZELO_DEMO_PASSWORD:}") String senha) {
        this.jdbc = jdbc;
        this.senhas = senhas;
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!email.endsWith("@example.test") || !email.matches("[a-z0-9._+-]{1,64}@example\\.test")
                || senha.isBlank() || senha.length() < 12 || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configure ZELO_DEMO_EMAIL e ZELO_DEMO_PASSWORD conforme o README.");
        }
        int criada = jdbc.update("""
                INSERT IGNORE INTO usuario (nome, email, senha_hash, perfil)
                VALUES ('Profissional de demonstração', ?, ?, 'PROFESSIONAL')
                """, email, senhas.encode(senha));
        if (criada > 0) {
            jdbc.update("INSERT IGNORE INTO instituicao (nome) VALUES ('Instituição demonstrativa Zelo')");
            jdbc.update("""
                INSERT INTO vinculo_institucional (usuario_id, instituicao_id)
                SELECT u.id, i.id FROM usuario u CROSS JOIN instituicao i
                WHERE u.email = ? AND i.nome = 'Instituição demonstrativa Zelo'
                """, email);
        }
        jdbc.execute((ConnectionCallback<Void>) conexao -> {
            ScriptUtils.executeSqlScript(conexao,
                    new EncodedResource(new ClassPathResource("demo/acompanhamento.sql"), StandardCharsets.UTF_8));
            return null;
        });
    }
}
