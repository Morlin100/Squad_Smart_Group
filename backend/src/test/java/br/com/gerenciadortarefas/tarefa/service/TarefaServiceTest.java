package br.com.gerenciadortarefas.tarefa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.gerenciadortarefas.shared.exception.RecursoNaoEncontradoException;
import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import br.com.gerenciadortarefas.tarefa.domain.Tarefa;
import br.com.gerenciadortarefas.tarefa.dto.TarefaRequest;
import br.com.gerenciadortarefas.tarefa.dto.TarefaResponse;
import br.com.gerenciadortarefas.tarefa.mapper.TarefaMapper;
import br.com.gerenciadortarefas.tarefa.repository.TarefaRepository;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository repository;

    private TarefaService service;

    @BeforeEach
    void setUp() {
        service = new TarefaService(repository, new TarefaMapper());
    }

    @Test
    void criarDeveNormalizarCamposESalvar() {
        TarefaRequest request = new TarefaRequest("  Estudar Angular  ", "   ", StatusTarefa.PENDENTE,
                PrioridadeTarefa.ALTA, LocalDate.of(2026, 10, 1));
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> {
            Tarefa tarefa = inv.getArgument(0);
            tarefa.setId(1L);
            return tarefa;
        });

        TarefaResponse response = service.criar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.titulo()).isEqualTo("Estudar Angular");
        assertThat(response.descricao()).isNull();
        assertThat(response.prioridade()).isEqualTo(PrioridadeTarefa.ALTA);
    }

    @Test
    void atualizarDeveAlterarTarefaExistente() {
        Tarefa existente = new Tarefa();
        existente.setId(5L);
        existente.setTitulo("Antigo");
        existente.setStatus(StatusTarefa.PENDENTE);
        existente.setPrioridade(PrioridadeTarefa.BAIXA);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.saveAndFlush(existente)).thenReturn(existente);

        TarefaResponse response = service.atualizar(5L,
                new TarefaRequest("Novo", "desc", StatusTarefa.CONCLUIDA, PrioridadeTarefa.MEDIA, null));

        assertThat(response.titulo()).isEqualTo("Novo");
        assertThat(response.status()).isEqualTo(StatusTarefa.CONCLUIDA);
    }

    @Test
    void excluirTarefaInexistenteDeveLancarExcecao() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
        verify(repository, never()).delete(any(Tarefa.class));
    }
}
