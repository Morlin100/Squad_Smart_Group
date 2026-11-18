package br.com.gerenciadortarefas.avaliacao.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gerenciadortarefas.avaliacao.domain.Avaliacao;
import br.com.gerenciadortarefas.avaliacao.domain.Funcionalidade;
import br.com.gerenciadortarefas.avaliacao.dto.AvaliacaoRequest;
import br.com.gerenciadortarefas.avaliacao.dto.ResultadoAvaliacaoResponse;
import br.com.gerenciadortarefas.avaliacao.dto.ResultadoAvaliacaoResponse.Comentario;
import br.com.gerenciadortarefas.avaliacao.repository.AvaliacaoRepository;

@Service
@Transactional(readOnly = true)
public class AvaliacaoService {

    private static final Logger log = LoggerFactory.getLogger(AvaliacaoService.class);

    private final AvaliacaoRepository repository;

    public AvaliacaoService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(AvaliacaoRequest request) {
        String comentario = request.comentario() == null || request.comentario().isBlank()
                ? null
                : request.comentario().strip();
        Avaliacao salva = repository.save(new Avaliacao(request.notaGeral(), request.facilidadeUso(),
                request.recomendaria(), request.funcionalidadeFavorita(), comentario));
        log.info("Avaliação {} recebida: nota {}", salva.getId(), salva.getNotaGeral());
    }

    public ResultadoAvaliacaoResponse resultado() {
        long total = repository.count();

        Map<Integer, Long> distribuicao = new LinkedHashMap<>();
        for (int nota = 1; nota <= 5; nota++) {
            distribuicao.put(nota, 0L);
        }
        for (Object[] linha : repository.contarPorNota()) {
            distribuicao.put(((Number) linha[0]).intValue(), ((Number) linha[1]).longValue());
        }

        Map<Funcionalidade, Long> funcionalidades = new LinkedHashMap<>();
        for (Object[] linha : repository.contarPorFuncionalidade()) {
            funcionalidades.put((Funcionalidade) linha[0], ((Number) linha[1]).longValue());
        }

        return new ResultadoAvaliacaoResponse(
                total,
                umaCasa(repository.mediaNotaGeral()),
                umaCasa(repository.mediaFacilidade()),
                total == 0 ? 0 : umaCasa(repository.countByRecomendariaTrue() * 100.0 / total),
                distribuicao,
                funcionalidades,
                repository.findTop5ByComentarioIsNotNullOrderByCriadaEmDescIdDesc().stream()
                        .map(a -> new Comentario(a.getNotaGeral(), a.getComentario(), a.getCriadaEm()))
                        .toList());
    }

    static double umaCasa(Double valor) {
        return valor == null ? 0 : Math.round(valor * 10) / 10.0;
    }
}
