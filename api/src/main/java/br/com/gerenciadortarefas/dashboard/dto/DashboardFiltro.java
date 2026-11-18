package br.com.gerenciadortarefas.dashboard.dto;

import java.time.LocalDate;

import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;

/**
 * Filtros de visualização do dashboard (KAN-85). Todos são opcionais.
 * O período se aplica à data de criação da tarefa.
 */
public record DashboardFiltro(
        String responsavel,
        StatusTarefa status,
        LocalDate dataInicio,
        LocalDate dataFim) {

    public DashboardFiltro {
        responsavel = responsavel == null || responsavel.isBlank() ? null : responsavel.strip();
    }

    public static DashboardFiltro vazio() {
        return new DashboardFiltro(null, null, null, null);
    }
}
