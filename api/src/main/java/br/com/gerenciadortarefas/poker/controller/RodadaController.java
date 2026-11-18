package br.com.gerenciadortarefas.poker.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.gerenciadortarefas.poker.dto.NovaRodadaRequest;
import br.com.gerenciadortarefas.poker.dto.ResultadoResponse;
import br.com.gerenciadortarefas.poker.dto.RodadaResponse;
import br.com.gerenciadortarefas.poker.dto.VotoRegistradoResponse;
import br.com.gerenciadortarefas.poker.dto.VotoRequest;
import br.com.gerenciadortarefas.poker.service.PlanningPokerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** KAN-107: endpoints da rodada de Planning Poker. */
@RestController
@RequestMapping("/api/v1/rodadas")
@Tag(name = "Planning Poker", description = "Rodadas de estimativa (KAN-93)")
public class RodadaController {

    private final PlanningPokerService service;

    public RodadaController(PlanningPokerService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Abre uma rodada para uma tarefa")
    public ResponseEntity<RodadaResponse> criar(@Valid @RequestBody NovaRodadaRequest request) {
        RodadaResponse criada = service.criarRodada(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    @Operation(summary = "Lista as 20 rodadas mais recentes")
    public List<RodadaResponse> listar() {
        return service.listarRecentes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Situação da rodada: quantos votaram e quem, sem mostrar as cartas")
    public RodadaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/votos")
    @Operation(summary = "Registra o voto de um participante (um voto por pessoa)")
    public ResponseEntity<VotoRegistradoResponse> votar(@PathVariable Long id, @Valid @RequestBody VotoRequest request) {
        return ResponseEntity.status(201).body(service.votar(id, request));
    }

    @PostMapping("/{id}/encerrar")
    @Operation(summary = "Encerra a rodada e revela os votos")
    public ResultadoResponse encerrar(@PathVariable Long id) {
        return service.encerrar(id);
    }

    @GetMapping("/{id}/resultado")
    @Operation(summary = "Resultado da rodada encerrada: votos, média, carta mais votada e divergência")
    public ResultadoResponse resultado(@PathVariable Long id) {
        return service.resultado(id);
    }
}
