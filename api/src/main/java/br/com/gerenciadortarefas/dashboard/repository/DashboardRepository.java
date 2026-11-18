package br.com.gerenciadortarefas.dashboard.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import br.com.gerenciadortarefas.config.ClockConfig;
import br.com.gerenciadortarefas.dashboard.dto.DashboardFiltro;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import br.com.gerenciadortarefas.tarefa.domain.Tarefa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Agregações do dashboard feitas no banco (KAN-84): COUNT e GROUP BY, sem carregar as tarefas em memória.
 */
@Repository
public class DashboardRepository {

    private final EntityManager em;

    public DashboardRepository(EntityManager em) {
        this.em = em;
    }

    /** SELECT campo, COUNT(*) FROM tarefa WHERE filtros GROUP BY campo */
    public <T> Map<T, Long> contarAgrupadoPor(String campo, DashboardFiltro filtro) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Tarefa> tarefa = query.from(Tarefa.class);
        Expression<T> agrupador = tarefa.get(campo);
        Expression<Long> quantidade = cb.count(tarefa);

        query.multiselect(agrupador, quantidade)
                .where(predicados(cb, tarefa, filtro).toArray(Predicate[]::new))
                .groupBy(agrupador)
                .orderBy(cb.desc(quantidade));

        Map<T, Long> resultado = new LinkedHashMap<>();
        for (Tuple linha : em.createQuery(query).getResultList()) {
            resultado.put(linha.get(0, agrupador.getJavaType()), linha.get(1, Long.class));
        }
        return resultado;
    }

    /** Tarefas não concluídas com vencimento anterior a hoje. */
    public long contarVencidas(DashboardFiltro filtro, LocalDate hoje) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Tarefa> tarefa = query.from(Tarefa.class);

        List<Predicate> predicados = predicados(cb, tarefa, filtro);
        predicados.add(cb.notEqual(tarefa.get("status"), StatusTarefa.CONCLUIDA));
        predicados.add(cb.lessThan(tarefa.<LocalDate>get("dataVencimento"), hoje));

        query.select(cb.count(tarefa)).where(predicados.toArray(Predicate[]::new));
        return em.createQuery(query).getSingleResult();
    }

    /** Tarefas criadas a partir de um instante. */
    public long contarCriadasDesde(DashboardFiltro filtro, Instant desde) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Tarefa> tarefa = query.from(Tarefa.class);

        List<Predicate> predicados = predicados(cb, tarefa, filtro);
        predicados.add(cb.greaterThanOrEqualTo(tarefa.<Instant>get("criadoEm"), desde));

        query.select(cb.count(tarefa)).where(predicados.toArray(Predicate[]::new));
        return em.createQuery(query).getSingleResult();
    }

    private List<Predicate> predicados(CriteriaBuilder cb, Root<Tarefa> tarefa, DashboardFiltro filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro.responsavel() != null) {
            predicados.add(cb.like(cb.lower(tarefa.get("responsavel")),
                    "%" + filtro.responsavel().toLowerCase() + "%"));
        }
        if (filtro.status() != null) {
            predicados.add(cb.equal(tarefa.get("status"), filtro.status()));
        }
        if (filtro.dataInicio() != null) {
            predicados.add(cb.greaterThanOrEqualTo(tarefa.<Instant>get("criadoEm"), inicioDoDia(filtro.dataInicio())));
        }
        if (filtro.dataFim() != null) {
            // Fim do período é inclusivo: vai até o início do dia seguinte.
            predicados.add(cb.lessThan(tarefa.<Instant>get("criadoEm"), inicioDoDia(filtro.dataFim().plusDays(1))));
        }
        return predicados;
    }

    private static Instant inicioDoDia(LocalDate data) {
        return data.atStartOfDay(ClockConfig.FUSO).toInstant();
    }
}
