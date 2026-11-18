import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { TarefaService } from './tarefa.service';

const BASE = `${environment.apiUrl}/tarefas`;

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
    service
      .listar({ termo: '  estudar ', responsavel: ' Ana ', status: '', pagina: 1, tamanho: 10 })
      .subscribe();

    const req = http.expectOne((r) => r.url === BASE);
    expect(req.request.params.get('termo')).toBe('estudar');
    expect(req.request.params.get('responsavel')).toBe('Ana');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.get('page')).toBe('1');
    req.flush({ conteudo: [], pagina: 1, tamanho: 10, totalElementos: 0, totalPaginas: 0 });
  });

  it('deve buscar o histórico da tarefa', () => {
    service.historico(3).subscribe();

    const req = http.expectOne(`${BASE}/3/historico`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('deve excluir pelo id', () => {
    service.excluir(7).subscribe();

    const req = http.expectOne(`${BASE}/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
