package br.com.gerenciadortarefas.tarefa.domain;

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

/** Registro de uma alteração feita em uma tarefa (HU05). */
@Entity
@Table(name = "tarefa_historico")
public class TarefaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tarefa_id", nullable = false)
    private Long tarefaId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AcaoHistorico acao;

    @Column(length = 2000)
    private String detalhes;

    @Column(name = "realizado_em", nullable = false, updatable = false)
    private Instant realizadoEm;

    protected TarefaHistorico() {
    }

    public TarefaHistorico(Long tarefaId, AcaoHistorico acao, String detalhes) {
        this.tarefaId = tarefaId;
        this.acao = acao;
        this.detalhes = detalhes;
    }

    @PrePersist
    void aoCriar() {
        this.realizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTarefaId() {
        return tarefaId;
    }

    public AcaoHistorico getAcao() {
        return acao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public Instant getRealizadoEm() {
        return realizadoEm;
    }
}
