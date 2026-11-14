package br.com.zelo.autenticacao;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
class SegurancaConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    HttpSessionCsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ContaRepository contas,
            HttpSessionSecurityContextRepository sessoes, HttpSessionCsrfTokenRepository csrf) throws Exception {
        return http
                .securityContext(config -> config.securityContextRepository(sessoes))
                .requestCache(config -> config.disable())
                .csrf(config -> config.csrfTokenRepository(csrf))
                .logout(config -> config.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((request, response, error) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, error) -> response.setStatus(403)))
                .authorizeHttpRequests(config -> config
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .anyRequest().access((authentication, request) -> {
                            boolean autorizado = authentication.get().getPrincipal() instanceof Long id
                                    && contas.porId(id).filter(ContaRepository.Conta::autorizada).isPresent();
                            return new AuthorizationDecision(autorizado);
                        }))
                .build();
    }
}
