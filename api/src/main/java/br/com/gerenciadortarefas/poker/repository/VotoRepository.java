package br.com.gerenciadortarefas.poker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gerenciadortarefas.poker.domain.Voto;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    /** Consulta da regra "um voto por participante". */
    boolean existsByParticipanteId(Long participanteId);

    long countByRodadaId(Long rodadaId);

    @Query("select v from Voto v join fetch v.participante where v.rodada.id = :rodadaId order by v.id")
    List<Voto> buscarComParticipantes(@Param("rodadaId") Long rodadaId);

    @Query("select p.nome from Voto v join v.participante p where v.rodada.id = :rodadaId order by v.id")
    List<String> nomesDeQuemVotou(@Param("rodadaId") Long rodadaId);
}
