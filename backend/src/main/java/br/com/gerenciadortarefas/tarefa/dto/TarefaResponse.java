package br.com.gerenciadortarefas.tarefa.dto;

import java.time.Instant;
import java.time.LocalDate;

import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;

public record TarefaResponse(
        Long id,
        String titulo,
        String descricao,
        StatusTarefa status,
        PrioridadeTarefa prioridade,
        LocalDate dataVencimento,
        Instant criadoEm,
        Instant atualizadoEm) {
}
