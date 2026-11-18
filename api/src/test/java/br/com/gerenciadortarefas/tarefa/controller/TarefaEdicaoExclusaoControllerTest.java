package br.com.gerenciadortarefas.tarefa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.com.gerenciadortarefas.tarefa.service.TarefaHistoricoService;
import br.com.gerenciadortarefas.tarefa.service.TarefaService;

/** HU03 (KAN-60) testes de edição e HU04 (KAN-67) testes de exclusão. */
@WebMvcTest(TarefaController.class)
class TarefaEdicaoExclusaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TarefaService service;

    @MockitoBean
    private TarefaHistoricoService historicoService;

    @Test
    void editarDeveRetornar200ComTarefaAtualizada() throws Exception {
        TarefaResponse atualizada = new TarefaResponse(3L, "Novo título", null, "Ana", StatusTarefa.CONCLUIDA,
                PrioridadeTarefa.ALTA, null, Instant.now(), Instant.now());
        when(service.atualizar(eq(3L), any(TarefaRequest.class))).thenReturn(atualizada);

        mockMvc.perform(put("/api/v1/tarefas/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Novo título", "responsavel": "Ana", "status": "CONCLUIDA", "prioridade": "ALTA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Novo título"))
                .andExpect(jsonPath("$.responsavel").value("Ana"))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    void editarComTituloEmBrancoDeveRetornar400SemChamarOService() throws Exception {
        mockMvc.perform(put("/api/v1/tarefas/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "   ", "status": "PENDENTE", "prioridade": "MEDIA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("titulo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("O título é obrigatório"));

        verify(service, never()).atualizar(any(), any());
    }

    @Test
    void editarComResponsavelMuitoLongoDeveRetornar400() throws Exception {
        String nomeLongo = "a".repeat(101);
        mockMvc.perform(put("/api/v1/tarefas/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"Ok\", \"responsavel\": \"" + nomeLongo
                                + "\", \"status\": \"PENDENTE\", \"prioridade\": \"MEDIA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("responsavel"));
    }

    @Test
    void editarTarefaInexistenteDeveRetornar404() throws Exception {
        when(service.atualizar(eq(99L), any(TarefaRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Tarefa 99 não encontrada"));

        mockMvc.perform(put("/api/v1/tarefas/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Qualquer", "status": "PENDENTE", "prioridade": "MEDIA"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Tarefa 99 não encontrada"));
    }

    @Test
    void excluirDeveRetornar204() throws Exception {
        mockMvc.perform(delete("/api/v1/tarefas/5"))
                .andExpect(status().isNoContent());

        verify(service).excluir(5L);
    }

    @Test
    void excluirTarefaInexistenteDeveRetornar404() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Tarefa 77 não encontrada")).when(service).excluir(77L);

        mockMvc.perform(delete("/api/v1/tarefas/77"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }
}
