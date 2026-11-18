package br.com.gerenciadortarefas.avaliacao.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import br.com.gerenciadortarefas.avaliacao.domain.Funcionalidade;

/**
 * Resultado consolidado da pesquisa.
 *
 * @param totalAvaliacoes          quantas respostas existem
 * @param mediaNotaGeral           média da nota geral (1 a 5), uma casa decimal; 0 sem respostas
 * @param mediaFacilidade          média da facilidade de uso (1 a 5), uma casa decimal
 * @param percentualRecomendariam  % de quem recomendaria, uma casa decimal
 * @param distribuicaoNotas        quantidade por nota geral, de 1 a 5 (todas aparecem)
 * @param funcionalidadesFavoritas quantidade por funcionalidade, da mais escolhida para a menos
 * @param ultimosComentarios       até 5 comentários mais recentes
 */
public record ResultadoAvaliacaoResponse(
        long totalAvaliacoes,
        double mediaNotaGeral,
        double mediaFacilidade,
        double percentualRecomendariam,
        Map<Integer, Long> distribuicaoNotas,
        Map<Funcionalidade, Long> funcionalidadesFavoritas,
        List<Comentario> ultimosComentarios) {

    public record Comentario(int notaGeral, String texto, Instant criadoEm) {
    }
}
