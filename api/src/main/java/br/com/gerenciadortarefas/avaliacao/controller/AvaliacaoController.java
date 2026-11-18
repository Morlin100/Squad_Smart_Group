package br.com.gerenciadortarefas.avaliacao.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.gerenciadortarefas.avaliacao.dto.AvaliacaoRequest;
import br.com.gerenciadortarefas.avaliacao.dto.ResultadoAvaliacaoResponse;
import br.com.gerenciadortarefas.avaliacao.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/avaliacoes")
@Tag(name = "Pesquisa de satisfação", description = "Avaliações do Gerenciador de Tarefas (tela /pesquisa)")
public class AvaliacaoController {

    private final AvaliacaoService service;

    public AvaliacaoController(AvaliacaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma resposta da pesquisa")
    public Map<String, String> registrar(@Valid @RequestBody AvaliacaoRequest request) {
        service.registrar(request);
        return Map.of("mensagem", "Obrigado pela avaliação!");
    }

    @GetMapping("/resultado")
    @Operation(summary = "Resultado consolidado: médias, % que recomendaria, distribuição e últimos comentários")
    public ResultadoAvaliacaoResponse resultado() {
        return service.resultado();
    }
}
