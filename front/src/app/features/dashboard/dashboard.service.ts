import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Dashboard, DashboardFiltro } from './dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/dashboard`;

  resumo(filtro: DashboardFiltro): Observable<Dashboard> {
    let params = new HttpParams();
    if (filtro.responsavel?.trim()) {
      params = params.set('responsavel', filtro.responsavel.trim());
    }
    if (filtro.status) {
      params = params.set('status', filtro.status);
    }
    if (filtro.dataInicio) {
      params = params.set('dataInicio', filtro.dataInicio);
    }
    if (filtro.dataFim) {
      params = params.set('dataFim', filtro.dataFim);
    }
    return this.http.get<Dashboard>(this.url, { params });
  }
}
