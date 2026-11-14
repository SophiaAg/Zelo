package br.com.zelo.profissional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class AcompanhamentoService {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private final AcompanhamentoRepository repositorio;

    AcompanhamentoService(AcompanhamentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    AcompanhamentoRepository.Resumo resumo(long usuario) {
        var hoje = LocalDate.now(FUSO);
        return repositorio.resumo(usuario, hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay());
    }

    List<AcompanhamentoRepository.Paciente> pacientes(long usuario, String busca) {
        if (busca.length() > 160) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return repositorio.pacientes(usuario, busca.strip(), null);
    }

    AcompanhamentoRepository.Paciente paciente(long usuario, long id) {
        return repositorio.pacientes(usuario, "", id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    List<AcompanhamentoRepository.Atendimento> historico(long usuario, long paciente) {
        paciente(usuario, paciente);
        return repositorio.atendimentos(" AND p.id = ? ORDER BY a.data_hora DESC, a.id DESC", usuario, paciente);
    }

    List<AcompanhamentoRepository.Atendimento> agenda(long usuario, LocalDate dia) {
        if (dia != null) {
            if (dia.getYear() < 1000 || dia.getYear() >= 9999) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            return repositorio.atendimentos(" AND a.data_hora >= ? AND a.data_hora < ? ORDER BY a.data_hora, a.id",
                    usuario, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
        }
        return repositorio.atendimentos("""
                AND a.situacao = 'AGENDADO' AND e.situacao = 'AGENDADO' AND a.data_hora >= ?
                ORDER BY a.data_hora, a.id LIMIT 20
                """, usuario, LocalDate.now(FUSO).atStartOfDay());
    }

    AcompanhamentoRepository.Atendimento atendimento(long usuario, long id) {
        return repositorio.atendimentos(" AND a.id = ?", usuario, id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void registrar(long usuario, long id, boolean compareceu, String observacoes) {
        atendimento(usuario, id);
        int alterados = repositorio.registrar(usuario, id, compareceu ? "CONCLUIDO" : "AUSENTE",
                compareceu ? "CONCLUIDO" : "NECESSITA_REAGENDAMENTO", observacoes, LocalDateTime.now(FUSO));
        if (alterados == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este atendimento não pode ser registrado agora.");
        }
    }
}
