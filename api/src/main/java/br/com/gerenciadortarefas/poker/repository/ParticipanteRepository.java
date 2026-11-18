package br.com.gerenciadortarefas.poker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gerenciadortarefas.poker.domain.Participante;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    Optional<Participante> findByRodadaIdAndNomeIgnoreCase(Long rodadaId, String nome);
}
