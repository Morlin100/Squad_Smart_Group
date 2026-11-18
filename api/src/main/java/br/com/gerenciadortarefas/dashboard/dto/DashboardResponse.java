package br.com.gerenciadortarefas.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;

/**
 * Métricas do dashboard (KAN-83).
 *
 * @param total                 quantidade de tarefas que passam nos filtros
 * @param concluidas            quantas estão com status CONCLUIDA
 * @param percentualConcluidas  concluídas / total, de 0 a 100, com uma casa decimal
 * @param vencidas              não concluídas com data de vencimento anterior a hoje
 * @param criadasUltimos7Dias   criadas nos últimos 7 dias, contando hoje
 * @param porStatus             quantidade por status (todos os status aparecem, mesmo com zero)
 * @param porPrioridade         quantidade por prioridade (todas aparecem, mesmo com zero)
 * @param porResponsavel        quantidade por responsável, da maior para a menor
 * @param geradoEm              quando os números foram calculados (mostra se veio do cache)
 */
public record DashboardResponse(
        long total,
        long concluidas,
        double percentualConcluidas,
        long vencidas,
        long criadasUltimos7Dias,
        Map<StatusTarefa, Long> porStatus,
        Map<PrioridadeTarefa, Long> porPrioridade,
        List<ContagemResponsavel> porResponsavel,
        Instant geradoEm) {

    public record ContagemResponsavel(String responsavel, long quantidade) {
    }
}
