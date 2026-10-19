package br.com.gerenciadortarefas.tarefa.mapper;

import org.springframework.stereotype.Component;

import br.com.gerenciadortarefas.tarefa.domain.Tarefa;
import br.com.gerenciadortarefas.tarefa.dto.TarefaRequest;
import br.com.gerenciadortarefas.tarefa.dto.TarefaResponse;

@Component
public class TarefaMapper {

    public Tarefa paraEntidade(TarefaRequest request) {
        Tarefa tarefa = new Tarefa();
        atualizarEntidade(tarefa, request);
        return tarefa;
    }

    public void atualizarEntidade(Tarefa tarefa, TarefaRequest request) {
        tarefa.setTitulo(request.titulo().strip());
        tarefa.setDescricao(normalizar(request.descricao()));
        tarefa.setStatus(request.status());
        tarefa.setPrioridade(request.prioridade());
        tarefa.setDataVencimento(request.dataVencimento());
    }

    public TarefaResponse paraResponse(Tarefa tarefa) {
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getStatus(),
                tarefa.getPrioridade(),
                tarefa.getDataVencimento(),
                tarefa.getCriadoEm(),
                tarefa.getAtualizadoEm());
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
