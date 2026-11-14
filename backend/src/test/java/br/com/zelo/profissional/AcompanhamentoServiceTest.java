package br.com.zelo.profissional;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AcompanhamentoServiceTest {
    private final AcompanhamentoRepository repositorio = mock(AcompanhamentoRepository.class);
    private final AcompanhamentoService acompanhamento = new AcompanhamentoService(repositorio);

    @Test
    void semVisibilidadeNaoTentaAlterar() {
        var erro = assertThrows(ResponseStatusException.class, () -> acompanhamento.registrar(1, 2, true, null));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verify(repositorio, never()).registrar(anyLong(), anyLong(), anyString(), anyString(), nullable(String.class), any());
    }

    @Test
    void registraComparecimentoComAutoriaEObservacoes() {
        visivel();
        when(repositorio.registrar(eq(1L), eq(2L), eq("CONCLUIDO"), eq("CONCLUIDO"), eq("Observação"), any()))
                .thenReturn(1);
        acompanhamento.registrar(1, 2, true, "Observação");
        verify(repositorio).registrar(eq(1L), eq(2L), eq("CONCLUIDO"), eq("CONCLUIDO"),
                eq("Observação"), any(LocalDateTime.class));
    }

    @Test
    void faltaPermiteReagendarAMesmaEtapa() {
        visivel();
        when(repositorio.registrar(eq(1L), eq(2L), eq("AUSENTE"), eq("NECESSITA_REAGENDAMENTO"), isNull(), any()))
                .thenReturn(1);
        acompanhamento.registrar(1, 2, false, null);
        verify(repositorio).registrar(eq(1L), eq(2L), eq("AUSENTE"), eq("NECESSITA_REAGENDAMENTO"),
                isNull(), any(LocalDateTime.class));
    }

    @Test
    void atualizacaoCondicionalRecusadaGeraConflito() {
        visivel();
        var erro = assertThrows(ResponseStatusException.class, () -> acompanhamento.registrar(1, 2, true, null));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    private void visivel() {
        when(repositorio.atendimentos(" AND a.id = ?", 1L, 2L))
                .thenReturn(List.of(mock(AcompanhamentoRepository.Atendimento.class)));
    }
}
