package br.com.zelo.paciente.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(nullable = false, length = 32)
    private String telefone;

    @Column(name = "autorizou_contato", nullable = false)
    private boolean autorizouContato;

    protected Paciente() {
    }

    public Paciente(String nome, String telefone, boolean autorizouContato) {
        this.nome = nome;
        this.telefone = telefone;
        this.autorizouContato = autorizouContato;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public boolean isAutorizouContato() {
        return autorizouContato;
    }
}
