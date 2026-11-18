package br.com.gerenciadortarefas.poker.domain;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Uma rodada de Planning Poker para estimar uma tarefa. */
@Entity
@Table(name = "rodada")
public class Rodada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String tarefa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SituacaoRodada situacao;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    protected Rodada() {
    }

    public Rodada(String tarefa) {
        this.tarefa = tarefa;
        this.situacao = SituacaoRodada.ABERTA;
    }

    @PrePersist
    void aoCriar() {
        this.criadaEm = Instant.now();
    }

    public boolean estaAberta() {
        return situacao == SituacaoRodada.ABERTA;
    }

    public void encerrar() {
        this.situacao = SituacaoRodada.ENCERRADA;
        this.encerradaEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTarefa() {
        return tarefa;
    }

    public SituacaoRodada getSituacao() {
        return situacao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }
}
