package br.com.zelo.autenticacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AutenticacaoController.class)
@Import({AutenticacaoService.class, SegurancaConfig.class})
class AutenticacaoTest {
    private static final String EMAIL = "profissional@example.test";
    private static final String SENHA = "senha-sintetica-de-teste";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder senhas;
    @MockitoBean ContaRepository contas;

    private record Csrf(MockHttpSession sessao, String header, String valor) {}

    @BeforeEach
    void preparar() {
        var conta = conta(true, true, "PROFESSIONAL");
        when(contas.porEmail(EMAIL)).thenReturn(Optional.of(conta));
        when(contas.porId(1)).thenReturn(Optional.of(conta));
    }

    @Test
    void autenticaRenovaIdERecuperaSessao() throws Exception {
        var csrf = csrf(null);
        String idOriginal = csrf.sessao().getId();
        login(csrf, EMAIL, SENHA).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        assertNotEquals(idOriginal, csrf.sessao().getId());
        mvc.perform(get("/api/auth/me").session(csrf.sessao()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL));
    }

    @ParameterizedTest
    @CsvSource({"profissional@example.test, incorreta", "ausente@example.test, senha-sintetica-de-teste"})
    void rejeitaCredenciaisInvalidas(String email, String senha) throws Exception {
        login(csrf(null), email, senha).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"false,true,PROFESSIONAL", "true,false,PROFESSIONAL", "true,true,OUTRO"})
    void rejeitaContaSemAutorizacao(boolean ativa, boolean vinculo, String perfil) throws Exception {
        when(contas.porEmail(EMAIL)).thenReturn(Optional.of(conta(ativa, vinculo, perfil)));
        var csrf = csrf(null);
        login(csrf, EMAIL, SENHA).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(csrf.sessao())).andExpect(status().isUnauthorized());
    }

    @Test
    void exigeSessaoEConfereRevogacao() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profissional/pendencias")).andExpect(status().isUnauthorized());
        var csrf = csrf(null);
        login(csrf, EMAIL, SENHA).andExpect(status().isOk());
        when(contas.porId(1)).thenReturn(Optional.of(conta(true, false, "PROFESSIONAL")));
        mvc.perform(get("/api/auth/me").session(csrf.sessao())).andExpect(status().isForbidden());
        mvc.perform(get("/api/profissional/pendencias").session(csrf.sessao())).andExpect(status().isForbidden());
    }

    @Test
    void exigeCsrfNoLoginERejeitaDadosInvalidos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        login(csrf(null), "invalido", SENHA).andExpect(status().isBadRequest());
    }

    @Test
    void logoutInvalidaSessao() throws Exception {
        var inicial = csrf(null);
        login(inicial, EMAIL, SENHA).andExpect(status().isOk());
        var csrf = csrf(inicial.sessao());
        mvc.perform(post("/api/auth/logout").session(csrf.sessao())).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(csrf.sessao()).header(csrf.header(), csrf.valor()))
                .andExpect(status().isNoContent());
        assertTrue(csrf.sessao().isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    private ContaRepository.Conta conta(boolean ativa, boolean vinculo, String perfil) {
        return new ContaRepository.Conta(1, "Profissional de Teste", EMAIL, senhas.encode(SENHA), ativa, perfil, vinculo);
    }

    private Csrf csrf(MockHttpSession sessao) throws Exception {
        var request = get("/api/auth/csrf");
        if (sessao != null) request.session(sessao);
        var resultado = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var token = json.readTree(resultado.getResponse().getContentAsString());
        HttpSession novaSessao = resultado.getRequest().getSession(false);
        return new Csrf((MockHttpSession) novaSessao, token.get("headerName").asText(), token.get("token").asText());
    }

    private ResultActions login(Csrf csrf, String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").session(csrf.sessao()).header(csrf.header(), csrf.valor())
                .contentType("application/json")
                .content(json.writeValueAsString(new AutenticacaoController.Login(email, senha))));
    }
}
