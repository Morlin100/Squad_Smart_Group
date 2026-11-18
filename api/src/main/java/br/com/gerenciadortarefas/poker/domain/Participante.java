package br.com.gerenciadortarefas.poker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Pessoa que participa de uma rodada, identificada pelo nome dentro da rodada. */
@Entity
@Table(name = "participante")
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rodada_id", nullable = false)
    private Rodada rodada;

    @Column(nullable = false, length = 60)
    private String nome;

    protected Participante() {
    }

    public Participante(Rodada rodada, String nome) {
        this.rodada = rodada;
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Rodada getRodada() {
        return rodada;
    }

    public String getNome() {
        return nome;
    }
}
