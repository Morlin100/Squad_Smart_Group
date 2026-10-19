import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TarefaService } from './tarefa.service';

describe('TarefaService', () => {
  let service: TarefaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TarefaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve enviar apenas os filtros preenchidos', () => {
    service.listar({ termo: '  estudar ', status: '', pagina: 1, tamanho: 10 }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/v1/tarefas');
    expect(req.request.params.get('termo')).toBe('estudar');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.get('page')).toBe('1');
    req.flush({ conteudo: [], pagina: 1, tamanho: 10, totalElementos: 0, totalPaginas: 0 });
  });

  it('deve excluir pelo id', () => {
    service.excluir(7).subscribe();

    const req = http.expectOne('/api/v1/tarefas/7');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
