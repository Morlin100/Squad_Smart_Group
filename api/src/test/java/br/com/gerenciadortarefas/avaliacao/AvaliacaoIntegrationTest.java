package br.com.gerenciadortarefas.avaliacao;

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
import org.springframework.test.web.servlet.ResultActions;

import br.com.gerenciadortarefas.avaliacao.repository.AvaliacaoRepository;

/** Pesquisa de satisfação: envio, validação e resultado consolidado, com o banco H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvaliacaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvaliacaoRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void resultadoSemRespostasDeveVirZerado() throws Exception {
        mockMvc.perform(get("/api/v1/avaliacoes/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvaliacoes").value(0))
                .andExpect(jsonPath("$.mediaNotaGeral").value(0.0))
                .andExpect(jsonPath("$.percentualRecomendariam").value(0.0))
                .andExpect(jsonPath("$.distribuicaoNotas.5").value(0))
                .andExpect(jsonPath("$.ultimosComentarios.length()").value(0));
    }

    @Test
    void deveConsolidarAsRespostas() throws Exception {
        enviar("{\"notaGeral\": 5, \"facilidadeUso\": 4, \"recomendaria\": true, "
                + "\"funcionalidadeFavorita\": \"DASHBOARD\", \"comentario\": \"  Muito bom!  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagem").value("Obrigado pela avaliação!"));
        enviar("{\"notaGeral\": 4, \"facilidadeUso\": 5, \"recomendaria\": true, \"funcionalidadeFavorita\": \"DASHBOARD\"}")
                .andExpect(status().isCreated());
        enviar("{\"notaGeral\": 2, \"facilidadeUso\": 2, \"recomendaria\": false, \"comentario\": \"   \"}")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/avaliacoes/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvaliacoes").value(3))
                .andExpect(jsonPath("$.mediaNotaGeral").value(3.7))
                .andExpect(jsonPath("$.mediaFacilidade").value(3.7))
                .andExpect(jsonPath("$.percentualRecomendariam").value(66.7))
                .andExpect(jsonPath("$.distribuicaoNotas.5").value(1))
                .andExpect(jsonPath("$.distribuicaoNotas.3").value(0))
                .andExpect(jsonPath("$.funcionalidadesFavoritas.DASHBOARD").value(2))
                // Comentário em branco não conta; o outro chega sem os espaços.
                .andExpect(jsonPath("$.ultimosComentarios.length()").value(1))
                .andExpect(jsonPath("$.ultimosComentarios[0].texto").value("Muito bom!"));
    }

    @Test
    void camposObrigatoriosEForaDaEscalaDevemSerRecusados() throws Exception {
        enviar("{\"facilidadeUso\": 6, \"recomendaria\": null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.length()").value(3));

        enviar("{\"notaGeral\": 3, \"facilidadeUso\": 3, \"recomendaria\": true, \"funcionalidadeFavorita\": \"NAO_EXISTE\"}")
                .andExpect(status().isBadRequest());

        enviar("{\"notaGeral\": 3, \"facilidadeUso\": 3, \"recomendaria\": true, \"comentario\": \"" + "x".repeat(501) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("comentario"));
    }

    @Test
    void telaDaPesquisaDeveSerServidaEmPesquisa() throws Exception {
        mockMvc.perform(get("/pesquisa"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/pesquisa/index.html"));
    }

    private ResultActions enviar(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/avaliacoes").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
