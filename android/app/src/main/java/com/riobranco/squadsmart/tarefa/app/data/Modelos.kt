package com.riobranco.squadsmart.tarefa.app.data

// Modelos iguais aos do front web (front/src/app/.../tarefa.model.ts e dashboard.model.ts).
// Os campos vêm como anuláveis porque o Gson ignora a nulabilidade do Kotlin: assim o app
// não quebra se a API (ainda não republicada) não mandar "responsavel", por exemplo.

data class Tarefa(
    val id: Long,
    val titulo: String?,
    val descricao: String?,
    val responsavel: String?,
    val status: String?,
    val prioridade: String?,
    /** yyyy-MM-dd ou null */
    val dataVencimento: String?,
    val criadoEm: String?,
    val atualizadoEm: String?
)

data class TarefaRequest(
    val titulo: String,
    val descricao: String?,
    val responsavel: String?,
    val status: String,
    val prioridade: String,
    val dataVencimento: String?
)

data class Pagina<T>(
    val conteudo: List<T>?,
    val pagina: Int,
    val tamanho: Int,
    val totalElementos: Long,
    val totalPaginas: Int
)

data class HistoricoTarefa(
    val id: Long,
    val acao: String?,
    val detalhes: String?,
    val realizadoEm: String?
)

data class Dashboard(
    val total: Long,
    val concluidas: Long,
    val percentualConcluidas: Double,
    val vencidas: Long,
    val criadasUltimos7Dias: Long,
    val porStatus: Map<String, Long>?,
    val porPrioridade: Map<String, Long>?,
    val porResponsavel: List<ResponsavelQuantidade>?,
    val geradoEm: String?
)

data class ResponsavelQuantidade(
    val responsavel: String?,
    val quantidade: Long
)

/** Formato de erro da API (RFC 9457 - Problem Details). */
data class ProblemDetail(
    val title: String?,
    val status: Int?,
    val detail: String?,
    val erros: List<ErroCampo>?
)

data class ErroCampo(
    val campo: String?,
    val mensagem: String?
)

enum class StatusTarefa(val rotulo: String) {
    PENDENTE("Pendente"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída");

    companion object {
        fun rotuloDe(codigo: String?): String =
            entries.firstOrNull { it.name == codigo }?.rotulo ?: codigo ?: "—"
    }
}

enum class PrioridadeTarefa(val rotulo: String) {
    BAIXA("Baixa"),
    MEDIA("Média"),
    ALTA("Alta");

    companion object {
        fun rotuloDe(codigo: String?): String =
            entries.firstOrNull { it.name == codigo }?.rotulo ?: codigo ?: "—"
    }
}

enum class AcaoHistorico(val rotulo: String) {
    CRIACAO("Criada"),
    ATUALIZACAO("Alterada"),
    EXCLUSAO("Excluída");

    companion object {
        fun rotuloDe(codigo: String?): String =
            entries.firstOrNull { it.name == codigo }?.rotulo ?: codigo ?: "—"
    }
}

/** Opções do seletor "Ordenar por" (mesmas da web). */
enum class Ordenacao(val valor: String, val rotulo: String) {
    MAIS_RECENTES("criadoEm,desc", "Mais recentes"),
    TITULO("titulo,asc", "Título (A–Z)"),
    VENCIMENTO("dataVencimento,asc", "Vencimento mais próximo"),
    RESPONSAVEL("responsavel,asc", "Responsável (A–Z)"),
    STATUS("status,asc", "Status");

    companion object {
        fun de(valor: String?) = entries.firstOrNull { it.valor == valor } ?: MAIS_RECENTES
    }
}
