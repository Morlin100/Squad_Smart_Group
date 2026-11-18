package br.com.gerenciadortarefas.tarefa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gerenciadortarefas.tarefa.domain.AcaoHistorico;
import br.com.gerenciadortarefas.tarefa.domain.TarefaHistorico;
import br.com.gerenciadortarefas.tarefa.dto.HistoricoResponse;
import br.com.gerenciadortarefas.tarefa.dto.TarefaResponse;
import br.com.gerenciadortarefas.tarefa.repository.TarefaHistoricoRepository;

/**
 * HU05: registra no banco e no log cada criação, alteração e exclusão de tarefa.
 */
@Service
@Transactional(readOnly = true)
public class TarefaHistoricoService {

    private static final Logger log = LoggerFactory.getLogger(TarefaHistoricoService.class);
    private static final int TAMANHO_MAXIMO_DETALHES = 2000;

    private final TarefaHistoricoRepository repository;

    public TarefaHistoricoService(TarefaHistoricoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrarCriacao(TarefaResponse tarefa) {
        registrar(tarefa.id(), AcaoHistorico.CRIACAO, "Tarefa criada: \"" + tarefa.titulo() + "\"");
    }

    @Transactional
    public void registrarAtualizacao(TarefaResponse antes, TarefaResponse depois) {
        String mudancas = descreverMudancas(antes, depois);
        registrar(depois.id(), AcaoHistorico.ATUALIZACAO, mudancas.isEmpty() ? "Salva sem alterações" : mudancas);
    }

    @Transactional
    public void registrarExclusao(TarefaResponse tarefa) {
        registrar(tarefa.id(), AcaoHistorico.EXCLUSAO, "Tarefa excluída: \"" + tarefa.titulo() + "\"");
    }

    public List<HistoricoResponse> listar(Long tarefaId) {
        return repository.findByTarefaIdOrderByRealizadoEmDescIdDesc(tarefaId).stream()
                .map(h -> new HistoricoResponse(h.getId(), h.getAcao(), h.getDetalhes(), h.getRealizadoEm()))
                .toList();
    }

    /** Monta um texto como: status: PENDENTE → CONCLUIDA; responsável: (vazio) → Ana */
    static String descreverMudancas(TarefaResponse antes, TarefaResponse depois) {
        List<String> mudancas = new ArrayList<>();
        comparar(mudancas, "título", antes.titulo(), depois.titulo());
        comparar(mudancas, "descrição", antes.descricao(), depois.descricao());
        comparar(mudancas, "responsável", antes.responsavel(), depois.responsavel());
        comparar(mudancas, "status", antes.status(), depois.status());
        comparar(mudancas, "prioridade", antes.prioridade(), depois.prioridade());
        comparar(mudancas, "vencimento", antes.dataVencimento(), depois.dataVencimento());
        return String.join("; ", mudancas);
    }

    private static void comparar(List<String> mudancas, String campo, Object antes, Object depois) {
        if (!Objects.equals(antes, depois)) {
            mudancas.add(campo + ": " + texto(antes) + " → " + texto(depois));
        }
    }

    private static String texto(Object valor) {
        return valor == null ? "(vazio)" : valor.toString();
    }

    private void registrar(Long tarefaId, AcaoHistorico acao, String detalhes) {
        String detalhesCortados = detalhes.length() > TAMANHO_MAXIMO_DETALHES
                ? detalhes.substring(0, TAMANHO_MAXIMO_DETALHES - 3) + "..."
                : detalhes;
        repository.save(new TarefaHistorico(tarefaId, acao, detalhesCortados));
        log.info("Tarefa {} - {}: {}", tarefaId, acao, detalhesCortados);
    }
}
