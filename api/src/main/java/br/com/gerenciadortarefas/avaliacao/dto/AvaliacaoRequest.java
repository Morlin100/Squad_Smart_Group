package br.com.gerenciadortarefas.avaliacao.dto;

import br.com.gerenciadortarefas.avaliacao.domain.Funcionalidade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequest(
        @NotNull(message = "Dê uma nota geral de 1 a 5")
        @Min(value = 1, message = "A nota geral vai de 1 a 5")
        @Max(value = 5, message = "A nota geral vai de 1 a 5")
        Integer notaGeral,

        @NotNull(message = "Diga o quanto o app é fácil de usar, de 1 a 5")
        @Min(value = 1, message = "A facilidade de uso vai de 1 a 5")
        @Max(value = 5, message = "A facilidade de uso vai de 1 a 5")
        Integer facilidadeUso,

        @NotNull(message = "Diga se você recomendaria o app")
        Boolean recomendaria,

        Funcionalidade funcionalidadeFavorita,

        @Size(max = 500, message = "O comentário deve ter no máximo 500 caracteres")
        String comentario) {
}
