/** Formato de erro da API (RFC 9457 - Problem Details). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  erros?: ErroCampo[];
}

export interface ErroCampo {
  campo: string;
  mensagem: string;
}
