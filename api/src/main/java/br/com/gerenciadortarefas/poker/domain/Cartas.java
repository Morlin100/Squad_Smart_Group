package br.com.gerenciadortarefas.poker.domain;

import java.util.List;

/** Sequência de cartas aceitas no Planning Poker. */
public final class Cartas {

    public static final List<Integer> SEQUENCIA = List.of(1, 2, 3, 5, 8, 13);

    /** Distância máxima (em posições da sequência) entre a menor e a maior carta sem gerar aviso. */
    public static final int DISTANCIA_MAXIMA_SEM_DIVERGENCIA = 2;

    private Cartas() {
    }

    public static boolean valida(Integer carta) {
        return carta != null && SEQUENCIA.contains(carta);
    }

    public static int posicao(int carta) {
        return SEQUENCIA.indexOf(carta);
    }
}
