package br.com.zelo.paciente.service;

import br.com.zelo.paciente.dto.PacienteCadastro;
import br.com.zelo.paciente.dto.PacienteResposta;
import br.com.zelo.paciente.model.Paciente;
import br.com.zelo.paciente.repository.PacienteRepository;

import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class PacienteService {

    private final PacienteRepository pacientes;

    PacienteService(PacienteRepository pacientes) {
        this.pacientes = pacientes;
    }

    public Optional<PacienteResposta> buscar(long id) {
        return pacientes.findById(id).map(this::paraResposta);
    }

    public PacienteResposta cadastrar(PacienteCadastro cadastro) {
        String nome = validar(cadastro.nome(), "nome", 160);
        String telefone = validar(cadastro.telefone(), "telefone", 32);
        try {
            Paciente paciente = pacientes.saveAndFlush(new Paciente(nome, telefone, cadastro.autorizouContato()));
            return paraResposta(paciente);
        } catch (DataIntegrityViolationException erro) {
            if (erro.getMostSpecificCause() instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062) {
                throw new DuplicateKeyException("Telefone ja cadastrado.", erro);
            }
            throw erro;
        }
    }

    private PacienteResposta paraResposta(Paciente paciente) {
        return new PacienteResposta(paciente.getId(), paciente.getNome(), paciente.getTelefone(),
                paciente.isAutorizouContato());
    }

    private String validar(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank() || valor.strip().length() > limite) {
            throw new IllegalArgumentException(campo + " deve conter de 1 a " + limite + " caracteres.");
        }
        return valor.strip();
    }
}
