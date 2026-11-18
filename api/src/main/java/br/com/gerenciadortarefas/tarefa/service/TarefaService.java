package br.com.gerenciadortarefas.tarefa.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gerenciadortarefas.config.CacheConfig;
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
    private final TarefaHistoricoService historico;

    public TarefaService(TarefaRepository repository, TarefaMapper mapper, TarefaHistoricoService historico) {
        this.repository = repository;
        this.mapper = mapper;
        this.historico = historico;
    }

    public Page<TarefaResponse> listar(StatusTarefa status, String termo, String responsavel, Pageable pageable) {
        return repository.findAll(TarefaSpecifications.comFiltros(status, termo, responsavel), pageable)
                .map(mapper::paraResponse);
    }

    public TarefaResponse buscarPorId(Long id) {
        return mapper.paraResponse(buscarEntidade(id));
    }

    // Toda escrita limpa o cache do dashboard (HU07), para os números não ficarem desatualizados.
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD, allEntries = true)
    public TarefaResponse criar(TarefaRequest request) {
        TarefaResponse criada = mapper.paraResponse(repository.save(mapper.paraEntidade(request)));
        historico.registrarCriacao(criada);
        return criada;
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD, allEntries = true)
    public TarefaResponse atualizar(Long id, TarefaRequest request) {
        Tarefa tarefa = buscarEntidade(id);
        TarefaResponse antes = mapper.paraResponse(tarefa);
        mapper.atualizarEntidade(tarefa, request);
        TarefaResponse depois = mapper.paraResponse(repository.saveAndFlush(tarefa));
        historico.registrarAtualizacao(antes, depois);
        return depois;
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.DASHBOARD, allEntries = true)
    public void excluir(Long id) {
        Tarefa tarefa = buscarEntidade(id);
        TarefaResponse excluida = mapper.paraResponse(tarefa);
        repository.delete(tarefa);
        historico.registrarExclusao(excluida);
    }

    private Tarefa buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa " + id + " não encontrada"));
    }
}
