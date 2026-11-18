import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { Pagina } from '../../../shared/models/pagina.model';
import { Tarefa, TarefaFiltro, TarefaRequest } from '../models/tarefa.model';

@Injectable({ providedIn: 'root' })
export class TarefaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/tarefas`;

  listar(filtro: TarefaFiltro): Observable<Pagina<Tarefa>> {
    let params = new HttpParams().set('page', filtro.pagina).set('size', filtro.tamanho);
    if (filtro.termo?.trim()) {
      params = params.set('termo', filtro.termo.trim());
    }
    if (filtro.status) {
      params = params.set('status', filtro.status);
    }
    if (filtro.ordenacao) {
      params = params.set('sort', filtro.ordenacao);
    }
    return this.http.get<Pagina<Tarefa>>(this.baseUrl, { params });
  }

  buscarPorId(id: number): Observable<Tarefa> {
    return this.http.get<Tarefa>(`${this.baseUrl}/${id}`);
  }

  criar(tarefa: TarefaRequest): Observable<Tarefa> {
    return this.http.post<Tarefa>(this.baseUrl, tarefa);
  }

  atualizar(id: number, tarefa: TarefaRequest): Observable<Tarefa> {
    return this.http.put<Tarefa>(`${this.baseUrl}/${id}`, tarefa);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
