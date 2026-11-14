package br.com.zelo.autenticacao;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class AutenticacaoService {
    private final ContaRepository contas;
    private final PasswordEncoder senhas;
    private final String hashAusente;
    private final Map<String, Integer> tentativas = new HashMap<>();
    private final Clock relogio = Clock.systemUTC();
    private long janela = -1;

    AutenticacaoService(ContaRepository contas, PasswordEncoder senhas) {
        this.contas = contas;
        this.senhas = senhas;
        this.hashAusente = senhas.encode(UUID.randomUUID().toString());
    }

    ContaRepository.Conta entrar(String email, String senha, String endereco) {
        String emailNormalizado = email.strip().toLowerCase(Locale.ROOT);
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados de login inválidos.");
        }
        registrarTentativa(emailNormalizado, endereco);
        var conta = contas.porEmail(emailNormalizado);
        if (!senhas.matches(senha, conta.map(ContaRepository.Conta::senhaHash).orElse(hashAusente))
                || conta.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível entrar com os dados informados.");
        }
        return conta.filter(ContaRepository.Conta::autorizada)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Não foi possível entrar com os dados informados."));
    }

    private synchronized void registrarTentativa(String email, String endereco) {
        long minuto = relogio.instant().getEpochSecond() / 60;
        if (minuto != janela) {
            tentativas.clear();
            janela = minuto;
        }
        String chaveEmail = "email:" + email;
        String chaveEndereco = "ip:" + endereco;
        if (tentativas.size() >= 10_000 || tentativas.getOrDefault(chaveEmail, 0) >= 10
                || tentativas.getOrDefault(chaveEndereco, 0) >= 100) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Tente novamente em instantes.");
        }
        tentativas.merge(chaveEmail, 1, Integer::sum);
        tentativas.merge(chaveEndereco, 1, Integer::sum);
    }
}
