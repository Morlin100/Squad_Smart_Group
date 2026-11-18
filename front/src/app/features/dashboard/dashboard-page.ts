import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, finalize } from 'rxjs';

import {
  PRIORIDADE_LABELS,
  PRIORIDADE_OPCOES,
  STATUS_LABELS,
  STATUS_OPCOES,
  StatusTarefa,
} from '../tarefas/models/tarefa.model';
import { Dashboard } from './dashboard.model';
import { DashboardService } from './dashboard.service';

interface Barra {
  rotulo: string;
  quantidade: number;
  /** Largura da barra em %, relativa ao maior valor do grupo. */
  largura: number;
}

@Component({
  selector: 'app-dashboard-page',
  imports: [
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  private readonly service = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly statusOpcoes = STATUS_OPCOES;
  protected readonly statusLabels = STATUS_LABELS;

  protected readonly filtros = new FormGroup({
    responsavel: new FormControl('', { nonNullable: true }),
    status: new FormControl<StatusTarefa | ''>('', { nonNullable: true }),
    dataInicio: new FormControl<Date | null>(null),
    dataFim: new FormControl<Date | null>(null),
  });

  protected readonly dados = signal<Dashboard | null>(null);
  protected readonly carregando = signal(false);

  protected readonly barrasStatus = computed(() => {
    const d = this.dados();
    return d ? barras(STATUS_OPCOES.map((s) => [STATUS_LABELS[s], d.porStatus[s] ?? 0])) : [];
  });

  protected readonly barrasPrioridade = computed(() => {
    const d = this.dados();
    return d
      ? barras(PRIORIDADE_OPCOES.map((p) => [PRIORIDADE_LABELS[p], d.porPrioridade[p] ?? 0]))
      : [];
  });

  protected readonly barrasResponsavel = computed(() => {
    const d = this.dados();
    return d ? barras(d.porResponsavel.map((r) => [r.responsavel, r.quantidade])) : [];
  });

  constructor() {
    this.filtros.valueChanges.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => {
      if (this.filtros.valid) {
        this.carregar();
      }
    });
    this.carregar();
  }

  protected limpar(): void {
    this.filtros.reset();
  }

  protected carregar(): void {
    const { responsavel, status, dataInicio, dataFim } = this.filtros.getRawValue();
    this.carregando.set(true);
    this.service
      .resumo({
        responsavel,
        status,
        dataInicio: dataInicio ? paraIso(dataInicio) : null,
        dataFim: dataFim ? paraIso(dataFim) : null,
      })
      .pipe(
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dados) => this.dados.set(dados));
  }
}

function barras(itens: [string, number][]): Barra[] {
  const maior = Math.max(1, ...itens.map(([, q]) => q));
  return itens.map(([rotulo, quantidade]) => ({
    rotulo,
    quantidade,
    largura: Math.round((quantidade / maior) * 100),
  }));
}

function paraIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}
