package br.com.zelo.paciente;

import org.springframework.data.jpa.repository.JpaRepository;

interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
