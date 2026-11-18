package br.com.gerenciadortarefas.poker.dto;

import java.time.Instant;
import java.util.List;

import br.com.gerenciadortarefas.poker.domain.SituacaoRodada;

/**
 * Situação da rodada. Enquanto a rodada está aberta, mostra só quem já votou, nunca as cartas.
 */
public record RodadaResponse(
        Long id,
        String tarefa,
        SituacaoRodada situacao,
        long totalVotos,
        List<String> participantesQueVotaram,
        List<Integer> cartas,
        Instant criadaEm,
        Instant encerradaEm) {
}
