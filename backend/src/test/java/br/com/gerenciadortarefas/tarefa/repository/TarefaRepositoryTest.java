package br.com.gerenciadortarefas.tarefa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import br.com.gerenciadortarefas.tarefa.domain.Tarefa;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TarefaRepositoryTest {

    @Autowired
    private TarefaRepository repository;

    @BeforeEach
    void setUp() {
        repository.save(tarefa("Estudar Java", StatusTarefa.PENDENTE));
        repository.save(tarefa("Estudar Angular", StatusTarefa.CONCLUIDA));
        repository.save(tarefa("Comprar pão", StatusTarefa.PENDENTE));
    }

    @Test
    void devePreencherDatasDeAuditoria() {
        Tarefa salva = repository.findAll().get(0);

        assertThat(salva.getCriadoEm()).isNotNull();
        assertThat(salva.getAtualizadoEm()).isNotNull();
    }

    @Test
    void deveFiltrarPorStatusETermoIgnorandoCaixa() {
        Page<Tarefa> resultado = repository.findAll(
                TarefaSpecifications.comFiltros(StatusTarefa.PENDENTE, "ESTUDAR"), PageRequest.of(0, 10));

        assertThat(resultado.getContent()).extracting(Tarefa::getTitulo).containsExactly("Estudar Java");
    }

    @Test
    void semFiltrosDeveRetornarTodas() {
        assertThat(repository.findAll(TarefaSpecifications.comFiltros(null, " "), PageRequest.of(0, 10)))
                .hasSize(3);
    }

    private Tarefa tarefa(String titulo, StatusTarefa status) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setStatus(status);
        tarefa.setPrioridade(PrioridadeTarefa.MEDIA);
        return tarefa;
    }
}
