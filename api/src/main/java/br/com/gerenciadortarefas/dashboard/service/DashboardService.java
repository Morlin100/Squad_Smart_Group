package br.com.gerenciadortarefas.dashboard.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gerenciadortarefas.config.CacheConfig;
import br.com.gerenciadortarefas.config.ClockConfig;
import br.com.gerenciadortarefas.dashboard.dto.DashboardFiltro;
import br.com.gerenciadortarefas.dashboard.dto.DashboardResponse;
import br.com.gerenciadortarefas.dashboard.dto.DashboardResponse.ContagemResponsavel;
import br.com.gerenciadortarefas.dashboard.repository.DashboardRepository;
import br.com.gerenciadortarefas.shared.exception.RegraNegocioException;
import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;

/** HU07: resumo das tarefas para o dashboard. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    static final String SEM_RESPONSAVEL = "Sem responsável";

    private final DashboardRepository repository;
    private final Clock clock;

    public DashboardService(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * O resultado fica em cache por filtro e por dia (KAN-86). O cache é limpo sempre que
     * uma tarefa é criada, editada ou excluída (ver TarefaService).
     */
    @Cacheable(cacheNames = CacheConfig.DASHBOARD, key = "{#filtro, T(java.time.LocalDate).now(@clock)}")
    public DashboardResponse resumo(DashboardFiltro filtro) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser depois da data final.");
        }

        LocalDate hoje = LocalDate.now(clock);

        Map<StatusTarefa, Long> porStatus = completar(StatusTarefa.class,
                repository.<StatusTarefa>contarAgrupadoPor("status", filtro));
        Map<PrioridadeTarefa, Long> porPrioridade = completar(PrioridadeTarefa.class,
                repository.<PrioridadeTarefa>contarAgrupadoPor("prioridade", filtro));
        List<ContagemResponsavel> porResponsavel = repository.<String>contarAgrupadoPor("responsavel", filtro)
                .entrySet().stream()
                .map(e -> new ContagemResponsavel(e.getKey() == null ? SEM_RESPONSAVEL : e.getKey(), e.getValue()))
                .toList();

        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long concluidas = porStatus.get(StatusTarefa.CONCLUIDA);
        Instant seteDiasAtras = hoje.minusDays(6).atStartOfDay(ClockConfig.FUSO).toInstant();

        return new DashboardResponse(
                total,
                concluidas,
                percentual(concluidas, total),
                repository.contarVencidas(filtro, hoje),
                repository.contarCriadasDesde(filtro, seteDiasAtras),
                porStatus,
                porPrioridade,
                porResponsavel,
                Instant.now(clock));
    }

    static double percentual(long parte, long total) {
        if (total == 0) {
            return 0;
        }
        return Math.round(parte * 1000.0 / total) / 10.0;
    }

    /** Garante que todos os valores do enum aparecem no mapa, mesmo com zero. */
    private static <E extends Enum<E>> Map<E, Long> completar(Class<E> tipo, Map<E, Long> contagem) {
        Map<E, Long> completo = new EnumMap<>(tipo);
        for (E valor : tipo.getEnumConstants()) {
            completo.put(valor, contagem.getOrDefault(valor, 0L));
        }
        return completo;
    }
}
