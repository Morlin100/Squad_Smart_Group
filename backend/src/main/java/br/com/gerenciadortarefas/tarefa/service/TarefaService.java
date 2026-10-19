package br.com.gerenciadortarefas.tarefa.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gerenciadortarefas.shared.exception.RecursoNaoEncontradoException;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import br.com.gerenciadortarefas.tarefa.domain.Tarefa;
import br.com.gerenciadortarefas.tarefa.dto.TarefaRequest;
import br.com.gerenciadortarefas.tarefa.dto.TarefaResponse;
import br.com.gerenciadortarefas.tarefa.mapper.TarefaMapper;
import br.com.gerenciadortarefas.tarefa.repository.TarefaRepository;
import br.com.gerenciadortarefas.tarefa.repository.TarefaSpecifications;

@Service
@Transactional(readOnly = true)
public class TarefaService {

    private final TarefaRepository repository;
    private final TarefaMapper mapper;

    public TarefaService(TarefaRepository repository, TarefaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<TarefaResponse> listar(StatusTarefa status, String termo, Pageable pageable) {
        return repository.findAll(TarefaSpecifications.comFiltros(status, termo), pageable)
                .map(mapper::paraResponse);
    }

    public TarefaResponse buscarPorId(Long id) {
        return mapper.paraResponse(buscarEntidade(id));
    }

    @Transactional
    public TarefaResponse criar(TarefaRequest request) {
        Tarefa tarefa = repository.save(mapper.paraEntidade(request));
        return mapper.paraResponse(tarefa);
    }

    @Transactional
    public TarefaResponse atualizar(Long id, TarefaRequest request) {
        Tarefa tarefa = buscarEntidade(id);
        mapper.atualizarEntidade(tarefa, request);
        return mapper.paraResponse(repository.saveAndFlush(tarefa));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarEntidade(id));
    }

    private Tarefa buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa " + id + " não encontrada"));
    }
}
