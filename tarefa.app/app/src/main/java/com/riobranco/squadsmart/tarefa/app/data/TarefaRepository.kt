package com.riobranco.squadsmart.tarefa.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/** Acesso à API. Todas as chamadas rodam em Dispatchers.IO (fora da thread principal). */
object TarefaRepository {

    private val api get() = RetrofitClient.service

    suspend fun listar(
        pagina: Int,
        termo: String,
        responsavel: String,
        status: String,
        ordenacao: String
    ): Pagina<Tarefa> = withContext(Dispatchers.IO) {
        api.listar(
            page = pagina,
            size = ApiConfig.TAMANHO_PAGINA,
            sort = ordenacao,
            termo = termo.trim().ifEmpty { null },
            status = status.ifEmpty { null },
            responsavel = responsavel.trim().ifEmpty { null }
        )
    }

    suspend fun buscarPorId(id: Long): Tarefa = withContext(Dispatchers.IO) { api.buscarPorId(id) }

    suspend fun criar(request: TarefaRequest): Tarefa = withContext(Dispatchers.IO) { api.criar(request) }

    suspend fun atualizar(id: Long, request: TarefaRequest): Tarefa =
        withContext(Dispatchers.IO) { api.atualizar(id, request) }

    suspend fun excluir(id: Long) = withContext(Dispatchers.IO) {
        val resposta = api.excluir(id)
        if (!resposta.isSuccessful) throw HttpException(resposta)
    }

    suspend fun historico(id: Long): List<HistoricoTarefa> =
        withContext(Dispatchers.IO) { api.historico(id) }

    suspend fun dashboard(
        responsavel: String,
        status: String,
        dataInicio: String?,
        dataFim: String?
    ): Dashboard = withContext(Dispatchers.IO) {
        api.dashboard(
            responsavel = responsavel.trim().ifEmpty { null },
            status = status.ifEmpty { null },
            dataInicio = dataInicio,
            dataFim = dataFim
        )
    }
}
