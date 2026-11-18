import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { NotificacaoService } from '../../../../core/services/notificacao.service';
import { ProblemDetail } from '../../../../shared/models/problem-detail.model';
import {
  PRIORIDADE_LABELS,
  PRIORIDADE_OPCOES,
  PrioridadeTarefa,
  STATUS_LABELS,
  STATUS_OPCOES,
  StatusTarefa,
  Tarefa,
  TarefaRequest,
} from '../../models/tarefa.model';
import { TarefaService } from '../../services/tarefa.service';

@Component({
  selector: 'app-tarefa-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './tarefa-form.html',
  styleUrl: './tarefa-form.scss',
})
export class TarefaForm implements OnInit {
  /** Parâmetro de rota `:id` (via withComponentInputBinding). Ausente no cadastro. */
  readonly id = input<string>();

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly tarefaService = inject(TarefaService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly prioridadeLabels = PRIORIDADE_LABELS;
  protected readonly statusOpcoes = STATUS_OPCOES;
  protected readonly prioridadeOpcoes = PRIORIDADE_OPCOES;

  protected readonly editando = computed(() => !!this.id());
  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    titulo: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)]],
    descricao: ['', Validators.maxLength(1000)],
    status: this.fb.control<StatusTarefa>('PENDENTE', Validators.required),
    prioridade: this.fb.control<PrioridadeTarefa>('MEDIA', Validators.required),
    dataVencimento: this.fb.control<Date | null>(null),
  });

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    this.carregando.set(true);
    this.tarefaService
      .buscarPorId(Number(id))
      .pipe(
        finalize(() => this.carregando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (tarefa) => this.preencher(tarefa),
        error: () => this.router.navigate(['/tarefas']),
      });
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const id = this.id();
    const requisicao = id
      ? this.tarefaService.atualizar(Number(id), this.paraRequest())
      : this.tarefaService.criar(this.paraRequest());

    this.salvando.set(true);
    requisicao
      .pipe(
        finalize(() => this.salvando.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notificacao.sucesso(id ? 'Tarefa atualizada com sucesso.' : 'Tarefa cadastrada com sucesso.');
          this.router.navigate(['/tarefas']);
        },
        error: (erro: HttpErrorResponse) => this.aplicarErrosDoServidor(erro),
      });
  }

  private preencher(tarefa: Tarefa): void {
    this.form.setValue({
      titulo: tarefa.titulo,
      descricao: tarefa.descricao ?? '',
      status: tarefa.status,
      prioridade: tarefa.prioridade,
      dataVencimento: tarefa.dataVencimento ? deIso(tarefa.dataVencimento) : null,
    });
  }

  private paraRequest(): TarefaRequest {
    const valor = this.form.getRawValue();
    return {
      titulo: valor.titulo.trim(),
      descricao: valor.descricao.trim() || null,
      status: valor.status,
      prioridade: valor.prioridade,
      dataVencimento: valor.dataVencimento ? paraIso(valor.dataVencimento) : null,
    };
  }

  /** Associa os erros de validação da API (400) aos campos do formulário. */
  private aplicarErrosDoServidor(erro: HttpErrorResponse): void {
    const problem = erro.error as ProblemDetail | null;
    problem?.erros?.forEach(({ campo, mensagem }) => {
      const controle = this.form.get(campo);
      controle?.setErrors({ servidor: mensagem });
      controle?.markAsTouched();
    });
  }
}

function paraIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

/** Converte yyyy-MM-dd para Date local (evita deslocamento de fuso horário). */
function deIso(valor: string): Date {
  const [ano, mes, dia] = valor.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}
