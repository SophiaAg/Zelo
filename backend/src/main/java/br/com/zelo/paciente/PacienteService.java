package br.com.zelo.paciente;

import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
class PacienteService {

    private final PacienteRepository pacientes;

    PacienteService(PacienteRepository pacientes) {
        this.pacientes = pacientes;
    }

    Optional<PacienteResposta> buscar(long id) {
        return pacientes.findById(id).map(Paciente::paraResposta);
    }

    PacienteResposta cadastrar(PacienteCadastro cadastro) {
        String nome = validar(cadastro.nome(), "nome", 160);
        String telefone = validar(cadastro.telefone(), "telefone", 32);
        try {
            return pacientes.saveAndFlush(new Paciente(nome, telefone, cadastro.autorizouContato()))
                    .paraResposta();
        } catch (DataIntegrityViolationException erro) {
            if (erro.getMostSpecificCause() instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062) {
                throw new DuplicateKeyException("Telefone ja cadastrado.", erro);
            }
            throw erro;
        }
    }

    private String validar(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank() || valor.strip().length() > limite) {
            throw new IllegalArgumentException(campo + " deve conter de 1 a " + limite + " caracteres.");
        }
        return valor.strip();
    }
}
