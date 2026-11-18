package br.com.gerenciadortarefas.dashboard.controller;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.gerenciadortarefas.dashboard.dto.DashboardFiltro;
import br.com.gerenciadortarefas.dashboard.dto.DashboardResponse;
import br.com.gerenciadortarefas.dashboard.service.DashboardService;
import br.com.gerenciadortarefas.tarefa.domain.StatusTarefa;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Resumo das tarefas (HU07)")
public class DashboardController {

    /** Logger próprio para os acessos (KAN-89), fácil de filtrar no log do Azure. */
    private static final Logger logAcesso = LoggerFactory.getLogger("acesso.dashboard");

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Resumo das tarefas com filtros opcionais por responsável, status e período de criação")
    public DashboardResponse resumo(
            @RequestParam(required = false) String responsavel,
            @RequestParam(required = false) StatusTarefa status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            HttpServletRequest request) {
        DashboardFiltro filtro = new DashboardFiltro(responsavel, status, dataInicio, dataFim);
        logAcesso.info("Acesso ao dashboard ip={} origem={} filtros={}",
                request.getRemoteAddr(), request.getHeader("User-Agent"), filtro);
        return service.resumo(filtro);
    }
}
