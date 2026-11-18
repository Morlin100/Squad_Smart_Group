package br.com.gerenciadortarefas.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Regra de negócio recusada. O status HTTP vai junto para o handler montar a resposta
 * (ex.: 409 para voto repetido, 400 para carta inválida).
 */
public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;

    public RegraNegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
