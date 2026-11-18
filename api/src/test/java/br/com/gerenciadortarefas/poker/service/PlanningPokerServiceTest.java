package br.com.gerenciadortarefas.poker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.gerenciadortarefas.poker.domain.Participante;
import br.com.gerenciadortarefas.poker.domain.Rodada;
import br.com.gerenciadortarefas.poker.domain.Voto;
import br.com.gerenciadortarefas.poker.dto.VotoRequest;
import br.com.gerenciadortarefas.poker.repository.ParticipanteRepository;
import br.com.gerenciadortarefas.poker.repository.RodadaRepository;
import br.com.gerenciadortarefas.poker.repository.VotoRepository;
import br.com.gerenciadortarefas.shared.exception.RegraNegocioException;

/** KAN-106: regras de negócio do Planning Poker. */
@ExtendWith(MockitoExtension.class)
class PlanningPokerServiceTest {

    @Mock
    private RodadaRepository rodadaRepository;
    @Mock
    private ParticipanteRepository participanteRepository;
    @Mock
    private VotoRepository votoRepository;

    private PlanningPokerService service;
    private Rodada rodada;

    @BeforeEach
    void setUp() {
        service = new PlanningPokerService(rodadaRepository, participanteRepository, votoRepository);
        rodada = new Rodada("Tela de login");
        rodada.setId(1L);
    }

    @Test
    void deveRegistrarOVotoEDevolverOTotal() {
        Participante ana = participante("Ana", 10L);
        when(rodadaRepository.findById(1L)).thenReturn(Optional.of(rodada));
        when(participanteRepository.findByRodadaIdAndNomeIgnoreCase(1L, "Ana")).thenReturn(Optional.empty());
        when(participanteRepository.save(any(Participante.class))).thenReturn(ana);
        when(votoRepository.existsByParticipanteId(10L)).thenReturn(false);
        when(votoRepository.countByRodadaId(1L)).thenReturn(3L);

        var resposta = service.votar(1L, new VotoRequest("  Ana ", 5));

        assertThat(resposta.totalVotos()).isEqualTo(3);
        verify(votoRepository).save(any(Voto.class));
    }

    @Test
    void segundoVotoDoMesmoParticipanteDeveSerRecusado() {
        when(rodadaRepository.findById(1L)).thenReturn(Optional.of(rodada));
        when(participanteRepository.findByRodadaIdAndNomeIgnoreCase(1L, "ana"))
                .thenReturn(Optional.of(participante("Ana", 10L)));
        when(votoRepository.existsByParticipanteId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.votar(1L, new VotoRequest("ana", 8)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(PlanningPokerService.MSG_VOTO_REPETIDO)
                .extracting(e -> ((RegraNegocioException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(votoRepository, never()).save(any());
    }

    @Test
    void cartaForaDaSequenciaDeveSerRecusada() {
        when(rodadaRepository.findById(1L)).thenReturn(Optional.of(rodada));

        assertThatThrownBy(() -> service.votar(1L, new VotoRequest("Ana", 4)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("1, 2, 3, 5, 8, 13")
                .extracting(e -> ((RegraNegocioException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void naoDeveVotarEmRodadaEncerrada() {
        rodada.encerrar();
        when(rodadaRepository.findById(1L)).thenReturn(Optional.of(rodada));

        assertThatThrownBy(() -> service.votar(1L, new VotoRequest("Ana", 3)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("encerrada");
    }

    @Test
    void resultadoNaoPodeAparecerComARodadaAberta() {
        when(rodadaRepository.findById(1L)).thenReturn(Optional.of(rodada));

        assertThatThrownBy(() -> service.resultado(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("só aparecem quando a rodada é encerrada");
    }

    @Test
    void naoDeveEncerrarRodadaSemVotos() {
        when(rodadaRepository.findById(1L)).thenReturn(Optional.of(rodada));
        when(votoRepository.countByRodadaId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> service.encerrar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("pelo menos um voto");
    }

    @Test
    void mediaECartaMaisVotada() {
        assertThat(PlanningPokerService.media(List.of(3, 5, 5, 8))).isEqualTo(5.3);
        assertThat(PlanningPokerService.cartaMaisVotada(List.of(3, 5, 5, 8))).isEqualTo(5);
        // Empate: fica a maior carta
        assertThat(PlanningPokerService.cartaMaisVotada(List.of(3, 8))).isEqualTo(8);
    }

    @Test
    void divergenciaQuandoMenorEMaiorCartaEstaoAMaisDeDoisNiveis() {
        assertThat(PlanningPokerService.divergente(List.of(2, 3, 5))).isFalse(); // 2 níveis
        assertThat(PlanningPokerService.divergente(List.of(2, 8))).isTrue();     // 3 níveis
        assertThat(PlanningPokerService.divergente(List.of(1, 13))).isTrue();
        assertThat(PlanningPokerService.divergente(List.of(13))).isFalse();
    }

    private Participante participante(String nome, Long id) {
        Participante p = new Participante(rodada, nome);
        p.setId(id);
        return p;
    }
}
