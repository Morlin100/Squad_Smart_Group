package br.com.gerenciadortarefas.tarefa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.gerenciadortarefas.tarefa.domain.AcaoHistorico;
import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import br.com.gerenciadortarefas.tarefa.domain.TarefaHistorico;
import br.com.gerenciadortarefas.tarefa.dto.TarefaResponse;
import br.com.gerenciadortarefas.tarefa.repository.TarefaHistoricoRepository;

/** HU05 (KAN-72/KAN-73): testes unitários do histórico de alterações. */
@ExtendWith(MockitoExtension.class)
class TarefaHistoricoServiceTest {

    @Mock
    private TarefaHistoricoRepository repository;

    private TarefaHistoricoService service;

    @BeforeEach
    void setUp() {
        service = new TarefaHistoricoService(repository);
    }

    @Test
    void deveDescreverSomenteOsCamposQueMudaram() {
        TarefaResponse antes = tarefa("Estudar", null, StatusTarefa.PENDENTE, null);
        TarefaResponse depois = tarefa("Estudar", "Ana", StatusTarefa.CONCLUIDA, LocalDate.of(2026, 12, 1));

        String texto = TarefaHistoricoService.descreverMudancas(antes, depois);

        assertThat(texto).isEqualTo(
                "responsável: (vazio) → Ana; status: PENDENTE → CONCLUIDA; vencimento: (vazio) → 2026-12-01");
    }

    @Test
    void semMudancasDeveRegistrarQueFoiSalvaSemAlteracoes() {
        TarefaResponse tarefa = tarefa("Estudar", "Ana", StatusTarefa.PENDENTE, null);

        service.registrarAtualizacao(tarefa, tarefa);

        TarefaHistorico salvo = capturarSalvo();
        assertThat(salvo.getAcao()).isEqualTo(AcaoHistorico.ATUALIZACAO);
        assertThat(salvo.getDetalhes()).isEqualTo("Salva sem alterações");
    }

    @Test
    void deveRegistrarCriacaoComOTitulo() {
        TarefaResponse tarefa = tarefa("Comprar pão", null, StatusTarefa.PENDENTE, null);

        service.registrarCriacao(tarefa);
        TarefaHistorico criacao = capturarSalvo();

        assertThat(criacao.getTarefaId()).isEqualTo(1L);
        assertThat(criacao.getAcao()).isEqualTo(AcaoHistorico.CRIACAO);
        assertThat(criacao.getDetalhes()).contains("Comprar pão");
    }

    @Test
    void detalhesMuitoLongosDevemSerCortadosEm2000Caracteres() {
        // Título e descrição longos trocados: o texto da mudança passa de 2000 caracteres.
        TarefaResponse antes = new TarefaResponse(1L, "b".repeat(150), "x".repeat(1000), null,
                StatusTarefa.PENDENTE, PrioridadeTarefa.MEDIA, null, Instant.now(), Instant.now());
        TarefaResponse depois = new TarefaResponse(1L, "c".repeat(150), "y".repeat(1000), null,
                StatusTarefa.PENDENTE, PrioridadeTarefa.MEDIA, null, Instant.now(), Instant.now());

        service.registrarAtualizacao(antes, depois);

        assertThat(capturarSalvo().getDetalhes()).hasSize(2000).endsWith("...");
    }

    private TarefaHistorico capturarSalvo() {
        ArgumentCaptor<TarefaHistorico> captor = ArgumentCaptor.forClass(TarefaHistorico.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static TarefaResponse tarefa(String titulo, String responsavel, StatusTarefa status, LocalDate vencimento) {
        return new TarefaResponse(1L, titulo, null, responsavel, status, PrioridadeTarefa.MEDIA, vencimento,
                Instant.now(), Instant.now());
    }
}
