package br.com.gerenciadortarefas.tarefa.dto;

import java.time.LocalDate;

import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TarefaRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String titulo,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao,

        @NotNull(message = "O status é obrigatório")
        StatusTarefa status,

        @NotNull(message = "A prioridade é obrigatória")
        PrioridadeTarefa prioridade,

        LocalDate dataVencimento) {
}
