package br.com.gerenciadortarefas.avaliacao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.gerenciadortarefas.avaliacao.domain.Avaliacao;

/** Consultas agregadas feitas no banco para o resultado consolidado. */
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @Query("select avg(a.notaGeral) from Avaliacao a")
    Double mediaNotaGeral();

    @Query("select avg(a.facilidadeUso) from Avaliacao a")
    Double mediaFacilidade();

    long countByRecomendariaTrue();

    /** Linhas [nota, quantidade]. */
    @Query("select a.notaGeral, count(a) from Avaliacao a group by a.notaGeral")
    List<Object[]> contarPorNota();

    /** Linhas [funcionalidade, quantidade], só de quem escolheu uma. */
    @Query("select a.funcionalidadeFavorita, count(a) from Avaliacao a "
            + "where a.funcionalidadeFavorita is not null group by a.funcionalidadeFavorita order by count(a) desc")
    List<Object[]> contarPorFuncionalidade();

    List<Avaliacao> findTop5ByComentarioIsNotNullOrderByCriadaEmDescIdDesc();
}
