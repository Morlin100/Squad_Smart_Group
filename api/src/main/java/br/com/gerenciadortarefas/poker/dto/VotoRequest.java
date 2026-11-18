package br.com.gerenciadortarefas.poker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VotoRequest(
        @NotBlank(message = "Informe seu nome para votar")
        @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres")
        String participante,

        @NotNull(message = "Escolha uma carta")
        Integer carta) {
}
