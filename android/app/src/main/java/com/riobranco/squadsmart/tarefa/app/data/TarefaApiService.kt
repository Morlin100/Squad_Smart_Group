package com.riobranco.squadsmart.tarefa.app.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Contrato da API (docs/modelagem.md). Parâmetros null não são enviados. */
interface TarefaApiService {

    @GET("tarefas")
    suspend fun listar(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String,
        @Query("termo") termo: String?,
        @Query("status") status: String?,
        @Query("responsavel") responsavel: String?
    ): Pagina<Tarefa>

    @GET("tarefas/{id}")
    suspend fun buscarPorId(@Path("id") id: Long): Tarefa

    @POST("tarefas")
    suspend fun criar(@Body tarefa: TarefaRequest): Tarefa

    @PUT("tarefas/{id}")
    suspend fun atualizar(@Path("id") id: Long, @Body tarefa: TarefaRequest): Tarefa

    @DELETE("tarefas/{id}")
    suspend fun excluir(@Path("id") id: Long): Response<Unit>

    @GET("tarefas/{id}/historico")
    suspend fun historico(@Path("id") id: Long): List<HistoricoTarefa>

    @GET("dashboard")
    suspend fun dashboard(
        @Query("responsavel") responsavel: String?,
        @Query("status") status: String?,
        @Query("dataInicio") dataInicio: String?,
        @Query("dataFim") dataFim: String?
    ): Dashboard
}
