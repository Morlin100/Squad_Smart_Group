package br.com.gerenciadortarefas.poker.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Carta escolhida por um participante em uma rodada. */
@Entity
@Table(name = "voto")
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rodada_id", nullable = false)
    private Rodada rodada;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participante_id", nullable = false)
    private Participante participante;

    @Column(nullable = false)
    private Integer carta;

    @Column(name = "votado_em", nullable = false, updatable = false)
    private Instant votadoEm;

    protected Voto() {
    }

    public Voto(Rodada rodada, Participante participante, int carta) {
        this.rodada = rodada;
        this.participante = participante;
        this.carta = carta;
    }

    @PrePersist
    void aoCriar() {
        this.votadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Rodada getRodada() {
        return rodada;
    }

    public Participante getParticipante() {
        return participante;
    }

    public Integer getCarta() {
        return carta;
    }

    public Instant getVotadoEm() {
        return votadoEm;
    }
}
