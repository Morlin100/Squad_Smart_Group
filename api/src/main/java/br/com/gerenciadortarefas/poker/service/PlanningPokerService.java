package br.com.gerenciadortarefas.poker.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gerenciadortarefas.poker.domain.Cartas;
import br.com.gerenciadortarefas.poker.domain.Participante;
import br.com.gerenciadortarefas.poker.domain.Rodada;
import br.com.gerenciadortarefas.poker.domain.Voto;
import br.com.gerenciadortarefas.poker.dto.NovaRodadaRequest;
import br.com.gerenciadortarefas.poker.dto.ResultadoResponse;
import br.com.gerenciadortarefas.poker.dto.ResultadoResponse.VotoResponse;
import br.com.gerenciadortarefas.poker.dto.RodadaResponse;
import br.com.gerenciadortarefas.poker.dto.VotoRegistradoResponse;
import br.com.gerenciadortarefas.poker.dto.VotoRequest;
import br.com.gerenciadortarefas.poker.repository.ParticipanteRepository;
import br.com.gerenciadortarefas.poker.repository.RodadaRepository;
import br.com.gerenciadortarefas.poker.repository.VotoRepository;
import br.com.gerenciadortarefas.shared.exception.RecursoNaoEncontradoException;
import br.com.gerenciadortarefas.shared.exception.RegraNegocioException;

/**
 * Regras do Planning Poker (KAN-106):
 * - um voto por participante;
 * - só aceita cartas da sequência 1, 2, 3, 5, 8 e 13;
 * - os votos ficam escondidos enquanto a rodada está aberta;
 * - no encerramento calcula a média, a carta mais votada e aponta divergência.
 */
@Service
@Transactional(readOnly = true)
public class PlanningPokerService {

    static final String MSG_VOTO_REPETIDO = "Seu voto já foi registrado nesta rodada.";
    static final String MSG_DIVERGENCIA = "As estimativas ficaram muito diferentes. O time precisa conversar antes de fechar o número.";

    private static final Logger log = LoggerFactory.getLogger(PlanningPokerService.class);

    private final RodadaRepository rodadaRepository;
    private final ParticipanteRepository participanteRepository;
    private final VotoRepository votoRepository;

    public PlanningPokerService(RodadaRepository rodadaRepository, ParticipanteRepository participanteRepository,
            VotoRepository votoRepository) {
        this.rodadaRepository = rodadaRepository;
        this.participanteRepository = participanteRepository;
        this.votoRepository = votoRepository;
    }

    @Transactional
    public RodadaResponse criarRodada(NovaRodadaRequest request) {
        Rodada rodada = rodadaRepository.save(new Rodada(request.tarefa().strip()));
        log.info("Rodada {} aberta para a tarefa \"{}\"", rodada.getId(), rodada.getTarefa());
        return paraResponse(rodada);
    }

    public List<RodadaResponse> listarRecentes() {
        return rodadaRepository.findTop20ByOrderByCriadaEmDescIdDesc().stream().map(this::paraResponse).toList();
    }

    public RodadaResponse buscar(Long id) {
        return paraResponse(buscarRodada(id));
    }

    @Transactional
    public VotoRegistradoResponse votar(Long rodadaId, VotoRequest request) {
        Rodada rodada = buscarRodada(rodadaId);
        if (!rodada.estaAberta()) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Esta rodada já foi encerrada. Não é possível votar.");
        }
        if (!Cartas.valida(request.carta())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST,
                    "Carta inválida. Escolha uma destas: " + Cartas.SEQUENCIA.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")) + ".");
        }

        String nome = request.participante().strip();
        Participante participante = participanteRepository.findByRodadaIdAndNomeIgnoreCase(rodadaId, nome)
                .orElseGet(() -> participanteRepository.save(new Participante(rodada, nome)));

        if (votoRepository.existsByParticipanteId(participante.getId())) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, MSG_VOTO_REPETIDO);
        }

        votoRepository.save(new Voto(rodada, participante, request.carta()));
        return new VotoRegistradoResponse("Voto registrado!", votoRepository.countByRodadaId(rodadaId));
    }

    @Transactional
    public ResultadoResponse encerrar(Long rodadaId) {
        Rodada rodada = buscarRodada(rodadaId);
        if (!rodada.estaAberta()) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Esta rodada já foi encerrada.");
        }
        if (votoRepository.countByRodadaId(rodadaId) == 0) {
            throw new RegraNegocioException(HttpStatus.CONFLICT,
                    "A rodada precisa de pelo menos um voto para ser encerrada.");
        }
        rodada.encerrar();
        log.info("Rodada {} encerrada", rodadaId);
        return calcularResultado(rodada);
    }

    public ResultadoResponse resultado(Long rodadaId) {
        Rodada rodada = buscarRodada(rodadaId);
        if (rodada.estaAberta()) {
            throw new RegraNegocioException(HttpStatus.CONFLICT,
                    "Os votos só aparecem quando a rodada é encerrada.");
        }
        return calcularResultado(rodada);
    }

    private ResultadoResponse calcularResultado(Rodada rodada) {
        List<Voto> votos = votoRepository.buscarComParticipantes(rodada.getId());
        List<Integer> cartas = votos.stream().map(Voto::getCarta).toList();
        boolean divergente = divergente(cartas);

        return new ResultadoResponse(
                rodada.getId(),
                rodada.getTarefa(),
                votos.size(),
                votos.stream().map(v -> new VotoResponse(v.getParticipante().getNome(), v.getCarta())).toList(),
                media(cartas),
                cartaMaisVotada(cartas),
                divergente,
                divergente ? MSG_DIVERGENCIA : null);
    }

    static double media(List<Integer> cartas) {
        double media = cartas.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(media * 10) / 10.0;
    }

    /** Carta com mais votos. No empate, fica a maior (estimativa mais conservadora). */
    static int cartaMaisVotada(List<Integer> cartas) {
        Map<Integer, Long> contagem = cartas.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return contagem.entrySet().stream()
                .max(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    /** Ex.: 2 e 8 estão a três posições de distância (2, 3, 5, 8), então é divergente. */
    static boolean divergente(List<Integer> cartas) {
        if (cartas.size() < 2) {
            return false;
        }
        int menor = Cartas.posicao(cartas.stream().min(Integer::compare).orElseThrow());
        int maior = Cartas.posicao(cartas.stream().max(Integer::compare).orElseThrow());
        return maior - menor > Cartas.DISTANCIA_MAXIMA_SEM_DIVERGENCIA;
    }

    private Rodada buscarRodada(Long id) {
        return rodadaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rodada " + id + " não encontrada"));
    }

    private RodadaResponse paraResponse(Rodada rodada) {
        List<String> quemVotou = votoRepository.nomesDeQuemVotou(rodada.getId());
        return new RodadaResponse(
                rodada.getId(),
                rodada.getTarefa(),
                rodada.getSituacao(),
                quemVotou.size(),
                quemVotou,
                Cartas.SEQUENCIA,
                rodada.getCriadaEm(),
                rodada.getEncerradaEm());
    }
}
