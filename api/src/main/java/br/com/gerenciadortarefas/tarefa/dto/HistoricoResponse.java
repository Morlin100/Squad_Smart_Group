package br.com.gerenciadortarefas.tarefa.dto;

import java.time.Instant;

import br.com.gerenciadortarefas.tarefa.domain.AcaoHistorico;

public record HistoricoResponse(
        Long id,
        AcaoHistorico acao,
        String detalhes,
        Instant realizadoEm) {
}
