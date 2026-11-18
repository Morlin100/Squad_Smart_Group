package br.com.gerenciadortarefas.poker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.gerenciadortarefas.poker.repository.ParticipanteRepository;
import br.com.gerenciadortarefas.poker.repository.RodadaRepository;
import br.com.gerenciadortarefas.poker.repository.VotoRepository;

/** KAN-93: fluxo completo de uma rodada pela API, com os critérios de aceitação. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RodadaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private VotoRepository votoRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private RodadaRepository rodadaRepository;

    @BeforeEach
    void limpar() {
        votoRepository.deleteAll();
        participanteRepository.deleteAll();
        rodadaRepository.deleteAll();
    }

    @Test
    void fluxoCompletoDeUmaRodada() throws Exception {
        long id = abrirRodada("Tela de login");

        votar(id, "Ana", 2).andExpect(status().isCreated()).andExpect(jsonPath("$.totalVotos").value(1));
        votar(id, "Gabriel", 8).andExpect(status().isCreated()).andExpect(jsonPath("$.totalVotos").value(2));

        // Critério 3: votar de novo não muda o total
        votar(id, "ANA", 13)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Seu voto já foi registrado nesta rodada."));

        // Critério 2: com a rodada aberta, só aparece quem votou, nunca as cartas
        mockMvc.perform(get("/api/v1/rodadas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ABERTA"))
                .andExpect(jsonPath("$.totalVotos").value(2))
                .andExpect(jsonPath("$.participantesQueVotaram[0]").value("Ana"))
                .andExpect(jsonPath("$.votos").doesNotExist());

        mockMvc.perform(get("/api/v1/rodadas/" + id + "/resultado")).andExpect(status().isConflict());

        // Critérios 4 e 5: no encerramento aparecem votos, média, mais votada e o aviso de divergência
        mockMvc.perform(post("/api/v1/rodadas/" + id + "/encerrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotos").value(2))
                .andExpect(jsonPath("$.votos[0].participante").value("Ana"))
                .andExpect(jsonPath("$.votos[0].carta").value(2))
                .andExpect(jsonPath("$.media").value(5.0))
                .andExpect(jsonPath("$.cartaMaisVotada").value(8))
                .andExpect(jsonPath("$.divergente").value(true));

        mockMvc.perform(get("/api/v1/rodadas/" + id + "/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagemDivergencia").isNotEmpty());

        votar(id, "Bárbara", 3).andExpect(status().isConflict());
    }

    @Test
    void validacoesDeEntrada() throws Exception {
        long id = abrirRodada("Cadastro");

        votar(id, "Ana", 4).andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/rodadas/" + id + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participante\": \"\", \"carta\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("participante"));

        mockMvc.perform(post("/api/v1/rodadas").contentType(MediaType.APPLICATION_JSON).content("{\"tarefa\": \" \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/rodadas/999999")).andExpect(status().isNotFound());
    }

    @Test
    void telaDoPokerDeveSerServidaEmPoker() throws Exception {
        mockMvc.perform(get("/poker"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/poker/index.html"));
    }

    private long abrirRodada(String tarefa) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/rodadas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tarefa\": \"" + tarefa + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartas.length()").value(6))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions votar(long id, String nome, int carta) throws Exception {
        return mockMvc.perform(post("/api/v1/rodadas/" + id + "/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"participante\": \"" + nome + "\", \"carta\": " + carta + "}"));
    }
}
