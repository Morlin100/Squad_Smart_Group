package br.com.gerenciadortarefas.poker.dto;

import java.util.List;

/**
 * Resultado de uma rodada encerrada.
 *
 * @param media               média das cartas, com uma casa decimal
 * @param cartaMaisVotada     carta com mais votos (no empate, a maior delas)
 * @param divergente          true quando a menor e a maior carta estão a mais de dois níveis de distância
 * @param mensagemDivergencia aviso para o time conversar, ou null quando não há divergência
 */
public record ResultadoResponse(
        Long rodadaId,
        String tarefa,
        long totalVotos,
        List<VotoResponse> votos,
        double media,
        int cartaMaisVotada,
        boolean divergente,
        String mensagemDivergencia) {

    public record VotoResponse(String participante, int carta) {
    }
}
