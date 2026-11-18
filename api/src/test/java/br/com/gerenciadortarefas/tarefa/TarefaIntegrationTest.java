package br.com.gerenciadortarefas.tarefa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import br.com.gerenciadortarefas.tarefa.repository.TarefaHistoricoRepository;
import br.com.gerenciadortarefas.tarefa.repository.TarefaRepository;

/**
 * HU05 (KAN-74): testes de integração com o banco H2 de verdade, passando por controller, service e repository.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TarefaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private TarefaHistoricoRepository historicoRepository;

    @BeforeEach
    void limparBanco() {
        tarefaRepository.deleteAll();
        historicoRepository.deleteAll();
    }

    @Test
    void cicloCompletoDeveSerPersistidoERegistradoNoHistorico() throws Exception {
        long id = criar("""
                {"titulo": "Estudar Spring", "responsavel": "Gabriel", "status": "PENDENTE", "prioridade": "ALTA"}
                """);

        mockMvc.perform(put("/api/v1/tarefas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Estudar Spring", "responsavel": "Bárbara", "status": "CONCLUIDA", "prioridade": "ALTA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsavel").value("Bárbara"));

        // Atualização foi persistida no banco (KAN-70)
        mockMvc.perform(get("/api/v1/tarefas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.responsavel").value("Bárbara"));

        mockMvc.perform(delete("/api/v1/tarefas/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/tarefas/" + id)).andExpect(status().isNotFound());

        // Histórico continua existindo depois da exclusão, do mais recente para o mais antigo (KAN-71)
        mockMvc.perform(get("/api/v1/tarefas/" + id + "/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].acao").value("EXCLUSAO"))
                .andExpect(jsonPath("$[1].acao").value("ATUALIZACAO"))
                .andExpect(jsonPath("$[1].detalhes").value(
                        "responsável: Gabriel → Bárbara; status: PENDENTE → CONCLUIDA"))
                .andExpect(jsonPath("$[2].acao").value("CRIACAO"));
    }

    @Test
    void deveFiltrarPorResponsavelEOrdenar() throws Exception {
        criar("""
                {"titulo": "B tarefa", "responsavel": "Ana Souza", "status": "PENDENTE", "prioridade": "MEDIA"}
                """);
        criar("""
                {"titulo": "A tarefa", "responsavel": "ana lima", "status": "PENDENTE", "prioridade": "MEDIA"}
                """);
        criar("""
                {"titulo": "C tarefa", "responsavel": "Felippe", "status": "PENDENTE", "prioridade": "MEDIA"}
                """);

        mockMvc.perform(get("/api/v1/tarefas").param("responsavel", "ANA").param("sort", "titulo,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("A tarefa"))
                .andExpect(jsonPath("$.conteudo[1].titulo").value("B tarefa"));
    }

    @Test
    void paginacaoDeveRespeitarTamanhoDaPagina() throws Exception {
        for (int i = 1; i <= 5; i++) {
            criar("{\"titulo\": \"Tarefa " + i + "\", \"status\": \"PENDENTE\", \"prioridade\": \"BAIXA\"}");
        }

        mockMvc.perform(get("/api/v1/tarefas").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.totalElementos").value(5));
    }

    @Test
    void ordenacaoPorCampoInexistenteDeveRetornar400() throws Exception {
        mockMvc.perform(get("/api/v1/tarefas").param("sort", "naoExiste,asc"))
                .andExpect(status().isBadRequest());
    }

    private long criar(String json) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/tarefas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }
}
