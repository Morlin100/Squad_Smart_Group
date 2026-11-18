package br.com.gerenciadortarefas.shared.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Contrato estável de paginação exposto pela API (evita serializar o {@link Page} do Spring Data diretamente).
 */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
