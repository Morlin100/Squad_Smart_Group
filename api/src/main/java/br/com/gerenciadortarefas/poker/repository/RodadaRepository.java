package br.com.gerenciadortarefas.poker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gerenciadortarefas.poker.domain.Rodada;

public interface RodadaRepository extends JpaRepository<Rodada, Long> {

    List<Rodada> findTop20ByOrderByCriadaEmDescIdDesc();
}
