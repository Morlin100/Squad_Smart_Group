package br.com.gerenciadortarefas.tarefa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.gerenciadortarefas.shared.exception.RecursoNaoEncontradoException;
import br.com.gerenciadortarefas.tarefa.domain.PrioridadeTarefa;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import br.com.gerenciadortarefas.tarefa.dto.TarefaRequest;
import br.com.gerenciadortarefas.tarefa.dto.TarefaResponse;
import br.com.gerenciadortarefas.tarefa.service.TarefaService;

@WebMvcTest(TarefaController.class)
class TarefaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TarefaService service;

    @Test
    void criarDeveRetornar201ComLocation() throws Exception {
        TarefaResponse criada = new TarefaResponse(1L, "Estudar", null, StatusTarefa.PENDENTE,
                PrioridadeTarefa.MEDIA, null, Instant.now(), Instant.now());
        when(service.criar(any(TarefaRequest.class))).thenReturn(criada);

        mockMvc.perform(post("/api/v1/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Estudar", "status": "PENDENTE", "prioridade": "MEDIA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/tarefas/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void criarComDadosInvalidosDeveRetornar400ComErrosPorCampo() throws Exception {
        mockMvc.perform(post("/api/v1/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "", "status": null, "prioridade": "MEDIA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.erros.length()").value(2));
    }

    @Test
    void buscarInexistenteDeveRetornar404() throws Exception {
        when(service.buscarPorId(42L)).thenThrow(new RecursoNaoEncontradoException("Tarefa 42 não encontrada"));

        mockMvc.perform(get("/api/v1/tarefas/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Tarefa 42 não encontrada"));
    }
}
