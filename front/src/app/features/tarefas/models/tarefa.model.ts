export type StatusTarefa = 'PENDENTE' | 'EM_ANDAMENTO' | 'CONCLUIDA';
export type PrioridadeTarefa = 'BAIXA' | 'MEDIA' | 'ALTA';

export interface Tarefa {
  id: number;
  titulo: string;
  descricao: string | null;
  responsavel: string | null;
  status: StatusTarefa;
  prioridade: PrioridadeTarefa;
  /** Data no formato ISO (yyyy-MM-dd). */
  dataVencimento: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface TarefaRequest {
  titulo: string;
  descricao: string | null;
  responsavel: string | null;
  status: StatusTarefa;
  prioridade: PrioridadeTarefa;
  dataVencimento: string | null;
}

export interface TarefaFiltro {
  termo?: string;
  responsavel?: string;
  status?: StatusTarefa | '';
  pagina: number;
  tamanho: number;
  ordenacao?: string;
}

export type AcaoHistorico = 'CRIACAO' | 'ATUALIZACAO' | 'EXCLUSAO';

export interface HistoricoTarefa {
  id: number;
  acao: AcaoHistorico;
  detalhes: string | null;
  realizadoEm: string;
}

export const ACAO_LABELS: Record<AcaoHistorico, string> = {
  CRIACAO: 'Criada',
  ATUALIZACAO: 'Alterada',
  EXCLUSAO: 'Excluída',
};

export const STATUS_LABELS: Record<StatusTarefa, string> = {
  PENDENTE: 'Pendente',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDA: 'Concluída',
};

export const PRIORIDADE_LABELS: Record<PrioridadeTarefa, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
};

export const STATUS_OPCOES = Object.keys(STATUS_LABELS) as StatusTarefa[];
export const PRIORIDADE_OPCOES = Object.keys(PRIORIDADE_LABELS) as PrioridadeTarefa[];
