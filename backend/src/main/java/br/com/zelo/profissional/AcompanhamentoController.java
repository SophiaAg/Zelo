package br.com.zelo.profissional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class AcompanhamentoController {
    record Conclusao(@Size(max = 2000) String observacoes) {}

    private final AcompanhamentoService acompanhamento;

    AcompanhamentoController(AcompanhamentoService acompanhamento) {
        this.acompanhamento = acompanhamento;
    }

    @GetMapping("/dashboard/summary")
    AcompanhamentoRepository.Resumo resumo(@AuthenticationPrincipal Long usuario) {
        return acompanhamento.resumo(usuario);
    }

    @GetMapping("/patients")
    List<AcompanhamentoRepository.Paciente> pacientes(@AuthenticationPrincipal Long usuario,
            @RequestParam(defaultValue = "") String search) {
        return acompanhamento.pacientes(usuario, search);
    }

    @GetMapping("/patients/{id}")
    AcompanhamentoRepository.Paciente paciente(@AuthenticationPrincipal Long usuario, @PathVariable long id) {
        return acompanhamento.paciente(usuario, id);
    }

    @GetMapping("/patients/{id}/appointments")
    List<AcompanhamentoRepository.Atendimento> historico(@AuthenticationPrincipal Long usuario, @PathVariable long id) {
        return acompanhamento.historico(usuario, id);
    }

    @GetMapping("/appointments")
    List<AcompanhamentoRepository.Atendimento> agenda(@AuthenticationPrincipal Long usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return acompanhamento.agenda(usuario, date);
    }

    @GetMapping("/appointments/{id}")
    AcompanhamentoRepository.Atendimento atendimento(@AuthenticationPrincipal Long usuario, @PathVariable long id) {
        return acompanhamento.atendimento(usuario, id);
    }

    @PostMapping("/appointments/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void concluir(@AuthenticationPrincipal Long usuario, @PathVariable long id,
            @Valid @RequestBody(required = false) Conclusao conclusao) {
        acompanhamento.registrar(usuario, id, true, conclusao == null ? null : conclusao.observacoes());
    }

    @PostMapping("/appointments/{id}/no-show")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void falta(@AuthenticationPrincipal Long usuario, @PathVariable long id) {
        acompanhamento.registrar(usuario, id, false, null);
    }
}
