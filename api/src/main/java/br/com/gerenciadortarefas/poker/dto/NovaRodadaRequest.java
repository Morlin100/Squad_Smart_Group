package br.com.gerenciadortarefas.poker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovaRodadaRequest(
        @NotBlank(message = "O nome da tarefa é obrigatório")
        @Size(max = 150, message = "O nome da tarefa deve ter no máximo 150 caracteres")
        String tarefa) {
}
