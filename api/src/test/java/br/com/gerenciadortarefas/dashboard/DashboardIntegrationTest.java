package br.com.gerenciadortarefas.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.gerenciadortarefas.config.CacheConfig;
import br.com.gerenciadortarefas.config.ClockConfig;
import br.com.gerenciadortarefas.tarefa.repository.TarefaHistoricoRepository;
import br.com.gerenciadortarefas.tarefa.repository.TarefaRepository;

/** HU07 (KAN-88): testes de integração do dashboard, incluindo filtros e cache. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private TarefaHistoricoRepository historicoRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void limpar() {
        tarefaRepository.deleteAll();
        historicoRepository.deleteAll();
        cacheManager.getCache(CacheConfig.DASHBOARD).clear();
    }

    @Test
    void deveAgregarAsTarefasNoBanco() throws Exception {
        String ontem = LocalDate.now(ClockConfig.FUSO).minusDays(1).toString();
        criar("Ana", "CONCLUIDA", "ALTA", null);
        criar("Ana", "PENDENTE", "MEDIA", ontem);
        criar(null, "EM_ANDAMENTO", "MEDIA", null);

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.concluidas").value(1))
                .andExpect(jsonPath("$.percentualConcluidas").value(33.3))
                .andExpect(jsonPath("$.vencidas").value(1))
                .andExpect(jsonPath("$.criadasUltimos7Dias").value(3))
                .andExpect(jsonPath("$.porStatus.PENDENTE").value(1))
                .andExpect(jsonPath("$.porPrioridade.MEDIA").value(2))
                .andExpect(jsonPath("$.porPrioridade.BAIXA").value(0))
                .andExpect(jsonPath("$.porResponsavel[0].responsavel").value("Ana"))
                .andExpect(jsonPath("$.porResponsavel[0].quantidade").value(2))
                .andExpect(jsonPath("$.porResponsavel[1].responsavel").value("Sem responsável"));
    }

    @Test
    void deveAplicarOsFiltros() throws Exception {
        criar("Ana", "CONCLUIDA", "ALTA", null);
        criar("Ana", "PENDENTE", "MEDIA", null);
        criar("Felippe", "PENDENTE", "BAIXA", null);

        mockMvc.perform(get("/api/v1/dashboard").param("responsavel", "ana").param("status", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        String amanha = LocalDate.now(ClockConfig.FUSO).plusDays(1).toString();
        mockMvc.perform(get("/api/v1/dashboard").param("dataInicio", amanha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("dataInicio", LocalDate.now(ClockConfig.FUSO).toString())
                        .param("dataFim", LocalDate.now(ClockConfig.FUSO).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void periodoInvertidoDeveRetornar400() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").param("dataInicio", "2026-11-10").param("dataFim", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A data inicial não pode ser depois da data final."));
    }

    @Test
    void cacheDeveSerReutilizadoELimpoQuandoUmaTarefaMuda() throws Exception {
        criar("Ana", "PENDENTE", "MEDIA", null);

        String primeiro = resumo();
        String segundo = resumo();
        // Mesma resposta, inclusive o instante em que foi gerada: veio do cache.
        assertThat(segundo).isEqualTo(primeiro);

        criar("Ana", "PENDENTE", "MEDIA", null);

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(jsonPath("$.total").value(2));
    }

    private String resumo() throws Exception {
        return mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void criar(String responsavel, String status, String prioridade, String vencimento) throws Exception {
        String json = "{\"titulo\": \"Tarefa\", \"status\": \"" + status + "\", \"prioridade\": \"" + prioridade + "\""
                + (responsavel != null ? ", \"responsavel\": \"" + responsavel + "\"" : "")
                + (vencimento != null ? ", \"dataVencimento\": \"" + vencimento + "\"" : "")
                + "}";
        String corpo = mockMvc.perform(post("/api/v1/tarefas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat((Object) JsonPath.read(corpo, "$.id")).isNotNull();
    }
}
