package br.com.zelo.paciente.repository;

import br.com.zelo.paciente.model.Paciente;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
