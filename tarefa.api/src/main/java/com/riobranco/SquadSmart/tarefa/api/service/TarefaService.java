package com.riobranco.SquadSmart.tarefa.api.service;

import com.riobranco.SquadSmart.tarefa.api.model.Tarefa;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Guarda as tarefas em memória (os dados somem quando a API reinicia). */
@Service
public class TarefaService {

    private final List<Tarefa> tarefas = new CopyOnWriteArrayList<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    public List<Tarefa> listar() {
        return new ArrayList<>(tarefas);
    }

    public Optional<Tarefa> buscarPorId(Long id) {
        return tarefas.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public Tarefa criar(Tarefa nova) {
        nova.setId(proximoId.getAndIncrement());
        tarefas.add(nova);
        return nova;
    }

    public Optional<Tarefa> atualizar(Long id, Tarefa dados) {
        return buscarPorId(id).map(existente -> {
            existente.setTitulo(dados.getTitulo());
            existente.setDescricao(dados.getDescricao());
            existente.setConcluida(dados.isConcluida());
            return existente;
        });
    }

    public boolean remover(Long id) {
        return tarefas.removeIf(t -> t.getId().equals(id));
    }
}
