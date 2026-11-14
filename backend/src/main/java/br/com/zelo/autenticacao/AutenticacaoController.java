package br.com.zelo.autenticacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
class AutenticacaoController {
    record Login(@NotBlank @Email @Size(max = 254) String email,
                 @NotBlank @Size(max = 72) String password) {}

    record Usuario(long id, String name, String email, String role) {
        Usuario(ContaRepository.Conta conta) {
            this(conta.id(), conta.nome(), conta.email(), conta.perfil());
        }
    }

    record Token(String headerName, String token) {}

    private final AutenticacaoService autenticacao;
    private final ContaRepository contas;
    private final HttpSessionSecurityContextRepository sessoes;
    private final HttpSessionCsrfTokenRepository csrf;

    AutenticacaoController(AutenticacaoService autenticacao, ContaRepository contas,
            HttpSessionSecurityContextRepository sessoes, HttpSessionCsrfTokenRepository csrf) {
        this.autenticacao = autenticacao;
        this.contas = contas;
        this.sessoes = sessoes;
        this.csrf = csrf;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<Void> dadosInvalidos() {
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/csrf")
    Token csrf(CsrfToken token) {
        return new Token(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/login")
    Usuario login(@Valid @RequestBody Login login, HttpServletRequest request, HttpServletResponse response) {
        var conta = autenticacao.entrar(login.email(), login.password(), request.getRemoteAddr());
        request.changeSessionId();
        csrf.saveToken(null, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(conta.id(), null, List.of()));
        SecurityContextHolder.setContext(context);
        sessoes.saveContext(context, request, response);
        return new Usuario(conta);
    }

    @GetMapping("/me")
    Usuario me(Authentication authentication) {
        return contas.porId((Long) authentication.getPrincipal()).filter(ContaRepository.Conta::autorizada)
                .map(Usuario::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
