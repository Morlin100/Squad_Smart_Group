package br.com.gerenciadortarefas.tarefa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gerenciadortarefas.tarefa.domain.TarefaHistorico;

public interface TarefaHistoricoRepository extends JpaRepository<TarefaHistorico, Long> {

    List<TarefaHistorico> findByTarefaIdOrderByRealizadoEmDescIdDesc(Long tarefaId);
}
