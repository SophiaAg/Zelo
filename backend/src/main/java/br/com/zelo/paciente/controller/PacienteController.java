package br.com.zelo.paciente.controller;

import br.com.zelo.paciente.dto.PacienteCadastro;
import br.com.zelo.paciente.dto.PacienteResposta;
import br.com.zelo.paciente.service.PacienteService;

import java.net.URI;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes")
class PacienteController {

    private final PacienteService pacientes;

    PacienteController(PacienteService pacientes) {
        this.pacientes = pacientes;
    }

    @PostMapping
    ResponseEntity<PacienteResposta> cadastrar(@RequestBody PacienteCadastro cadastro) {
        PacienteResposta paciente = pacientes.cadastrar(cadastro);
        return ResponseEntity.created(URI.create("/api/pacientes/" + paciente.id())).body(paciente);
    }

    @GetMapping("/{id}")
    ResponseEntity<PacienteResposta> buscar(@PathVariable long id) {
        return ResponseEntity.of(pacientes.buscar(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail cadastroInvalido(IllegalArgumentException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ProblemDetail telefoneDuplicado(DuplicateKeyException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }
}
