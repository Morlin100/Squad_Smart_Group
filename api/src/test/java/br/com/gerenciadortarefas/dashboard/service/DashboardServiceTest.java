package br.com.gerenciadortarefas.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.gerenciadortarefas.config.ClockConfig;
import br.com.gerenciadortarefas.dashboard.dto.DashboardFiltro;
import br.com.gerenciadortarefas.dashboard.dto.DashboardResponse;
import br.com.gerenciadortarefas.dashboard.repository.DashboardRepository;
import br.com.gerenciadortarefas.shared.exception.RegraNegocioException;
import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;

/** HU07 (KAN-87): testes unitários do dashboard. */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-05T15:00:00Z");

    @Mock
    private DashboardRepository repository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(repository, Clock.fixed(AGORA, ClockConfig.FUSO));
    }

    @Test
    void deveMontarAsMetricasECompletarStatusSemTarefas() {
        DashboardFiltro filtro = DashboardFiltro.vazio();
        Map<StatusTarefa, Long> porStatus = new LinkedHashMap<>();
        porStatus.put(StatusTarefa.PENDENTE, 3L);
        porStatus.put(StatusTarefa.CONCLUIDA, 1L);
        Map<String, Long> porResponsavel = new LinkedHashMap<>();
        porResponsavel.put("Ana", 3L);
        porResponsavel.put(null, 1L);

        when(repository.<StatusTarefa>contarAgrupadoPor("status", filtro)).thenReturn(porStatus);
        when(repository.<PrioridadeTarefa>contarAgrupadoPor("prioridade", filtro))
                .thenReturn(Map.of(PrioridadeTarefa.ALTA, 4L));
        when(repository.<String>contarAgrupadoPor("responsavel", filtro)).thenReturn(porResponsavel);
        when(repository.contarVencidas(filtro, LocalDate.of(2026, 11, 5))).thenReturn(2L);
        when(repository.contarCriadasDesde(eq(filtro), any(Instant.class))).thenReturn(4L);

        DashboardResponse resumo = service.resumo(filtro);

        assertThat(resumo.total()).isEqualTo(4);
        assertThat(resumo.concluidas()).isEqualTo(1);
        assertThat(resumo.percentualConcluidas()).isEqualTo(25.0);
        assertThat(resumo.vencidas()).isEqualTo(2);
        assertThat(resumo.criadasUltimos7Dias()).isEqualTo(4);
        assertThat(resumo.porStatus()).containsEntry(StatusTarefa.EM_ANDAMENTO, 0L);
        assertThat(resumo.porPrioridade()).containsEntry(PrioridadeTarefa.BAIXA, 0L).containsEntry(PrioridadeTarefa.ALTA, 4L);
        assertThat(resumo.porResponsavel()).extracting(DashboardResponse.ContagemResponsavel::responsavel)
                .containsExactly("Ana", DashboardService.SEM_RESPONSAVEL);
        assertThat(resumo.geradoEm()).isEqualTo(AGORA);
    }

    @Test
    void percentualDeveTerUmaCasaDecimalESerZeroSemTarefas() {
        assertThat(DashboardService.percentual(1, 3)).isEqualTo(33.3);
        assertThat(DashboardService.percentual(2, 3)).isEqualTo(66.7);
        assertThat(DashboardService.percentual(0, 0)).isZero();
    }

    @Test
    void dataInicialDepoisDaFinalDeveSerRecusada() {
        DashboardFiltro filtro = new DashboardFiltro(null, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> service.resumo(filtro))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("data inicial");
    }

    @Test
    void responsavelEmBrancoDeveVirarNulo() {
        assertThat(new DashboardFiltro("   ", null, null, null).responsavel()).isNull();
        assertThat(new DashboardFiltro("  Ana ", null, null, null).responsavel()).isEqualTo("Ana");
    }
}
