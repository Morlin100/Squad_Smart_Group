package br.com.gerenciadortarefas.tarefa.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import br.com.gerenciadortarefas.tarefa.domain.Tarefa;
import jakarta.persistence.criteria.Predicate;

public final class TarefaSpecifications {

    private TarefaSpecifications() {
    }

    public static Specification<Tarefa> comFiltros(StatusTarefa status, String termo) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (termo != null && !termo.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("titulo")), "%" + termo.strip().toLowerCase() + "%"));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
