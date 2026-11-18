import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { debounceTime, filter, finalize, switchMap } from 'rxjs';

import { NotificacaoService } from '../../../../core/services/notificacao.service';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../../../shared/components/confirm-dialog/confirm-dialog';
import {
  PRIORIDADE_LABELS,
  PrioridadeTarefa,
  STATUS_LABELS,
  STATUS_OPCOES,
  StatusTarefa,
  Tarefa,
} from '../../models/tarefa.model';
import { TarefaService } from '../../services/tarefa.service';

const ORDENACAO_PADRAO = 'criadoEm,desc';

@Component({
  selector: 'app-tarefa-lista',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatCardModule,
  ],
  templateUrl: './tarefa-lista.html',
  styleUrl: './tarefa-lista.scss',
})
export class TarefaLista {
  private readonly tarefaService = inject(TarefaService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly colunas = ['titulo', 'status', 'prioridade', 'dataVencimento', 'acoes'];
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly prioridadeLabels = PRIORIDADE_LABELS;
  protected readonly statusOpcoes = STATUS_OPCOES;
  protected readonly hoje = hojeIso();

  protected readonly filtros = new FormGroup({
    termo: new FormControl('', { nonNullable: true }),
    status: new FormControl<StatusTarefa | ''>('', { nonNullable: true }),
  });

  protected readonly tarefas = signal<Tarefa[]>([]);
  protected readonly totalElementos = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(10);
  protected readonly carregando = signal(false);
  private ordenacao = ORDENACAO_PADRAO;

  constructor() {
    this.filtros.valueChanges.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => {
      this.pagina.set(0);
      this.carregar();
    });
    this.carregar();
  }

  protected aoPaginar(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected aoOrdenar(sort: Sort): void {
    this.ordenacao = sort.direction ? `${sort.active},${sort.direction}` : ORDENACAO_PADRAO;
    this.pagina.set(0);
    this.carregar();
  }

  protected limparFiltros(): void {
    this.filtros.reset();
  }

  // O mat-table tipa as linhas como `any`; estes métodos garantem a tipagem no template.
  protected rotuloStatus(status: StatusTarefa): string {
    return this.statusLabels[status];
  }

  protected rotuloPrioridade(prioridade: PrioridadeTarefa): string {
    return this.prioridadeLabels[prioridade];
  }

  protected estaVencida(tarefa: Tarefa): boolean {
    return (
      tarefa.status !== 'CONCLUIDA' && !!tarefa.dataVencimento && tarefa.dataVencimento < this.hoje
    );
  }

  protected excluir(tarefa: Tarefa): void {
    this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          titulo: 'Excluir tarefa',
          mensagem: `Deseja realmente excluir a tarefa "${tarefa.titulo}"?`,
          textoConfirmar: 'Excluir',
        },
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.tarefaService.excluir(tarefa.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.notificacao.sucesso('Tarefa excluída com sucesso.');
        // Se era o último item da página, volta uma página.
        if (this.tarefas().length === 1 && this.pagina() > 0) {
          this.pagina.update((p) => p - 1);
        }
        this.carregar();
      });
  }

  private carregar(): void {
    const { termo, status } = this.filtros.getRawValue();
    this.carregando.set(true);
    this.tarefaService
      .listar({
        termo,
        status,
        pagina: this.pagina(),
        tamanho: this.tamanho(),
        ordenacao: this.ordenacao,
      })
      .pipe(
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((resultado) => {
        this.tarefas.set(resultado.conteudo);
        this.totalElementos.set(resultado.totalElementos);
      });
  }
}

function hojeIso(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}
