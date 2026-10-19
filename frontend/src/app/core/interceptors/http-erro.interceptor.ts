import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { ProblemDetail } from '../../shared/models/problem-detail.model';
import { NotificacaoService } from '../services/notificacao.service';

/**
 * Exibe uma notificação para erros HTTP. Erros de validação por campo (400 com `erros`)
 * são deixados para o formulário tratar.
 */
export const httpErroInterceptor: HttpInterceptorFn = (req, next) => {
  const notificacao = inject(NotificacaoService);

  return next(req).pipe(
    catchError((erro: HttpErrorResponse) => {
      const problem = erro.error as ProblemDetail | null;

      if (erro.status === 0) {
        notificacao.erro('Não foi possível conectar ao servidor.');
      } else if (!(erro.status === 400 && problem?.erros?.length)) {
        notificacao.erro(problem?.detail ?? 'Ocorreu um erro inesperado.');
      }

      return throwError(() => erro);
    }),
  );
};
