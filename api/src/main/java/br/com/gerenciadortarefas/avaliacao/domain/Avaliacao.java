package br.com.gerenciadortarefas.avaliacao.domain;

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

/** Uma resposta da pesquisa de satisfação. */
@Entity
@Table(name = "avaliacao")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nota_geral", nullable = false)
    private Integer notaGeral;

    @Column(name = "facilidade_uso", nullable = false)
    private Integer facilidadeUso;

    @Column(nullable = false)
    private Boolean recomendaria;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "funcionalidade_favorita", length = 30)
    private Funcionalidade funcionalidadeFavorita;

    @Column(length = 500)
    private String comentario;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    protected Avaliacao() {
    }

    public Avaliacao(int notaGeral, int facilidadeUso, boolean recomendaria, Funcionalidade funcionalidadeFavorita,
            String comentario) {
        this.notaGeral = notaGeral;
        this.facilidadeUso = facilidadeUso;
        this.recomendaria = recomendaria;
        this.funcionalidadeFavorita = funcionalidadeFavorita;
        this.comentario = comentario;
    }

    @PrePersist
    void aoCriar() {
        this.criadaEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Integer getNotaGeral() {
        return notaGeral;
    }

    public Integer getFacilidadeUso() {
        return facilidadeUso;
    }

    public Boolean getRecomendaria() {
        return recomendaria;
    }

    public Funcionalidade getFuncionalidadeFavorita() {
        return funcionalidadeFavorita;
    }

    public String getComentario() {
        return comentario;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
