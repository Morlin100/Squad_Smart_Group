package com.riobranco.squadsmart.tarefa.app.data

import retrofit2.HttpException
import java.io.IOException

/** Erro já traduzido para mostrar ao usuário. */
data class ErroApi(
    val mensagem: String,
    /** Código HTTP, ou null se nem chegou ao servidor. */
    val status: Int? = null,
    /** Erros de validação por campo (400): campo -> mensagem. */
    val campos: Map<String, String> = emptyMap()
) {
    val naoEncontrado get() = status == 404
}

const val MSG_SEM_CONEXAO = "Não foi possível conectar ao servidor."
const val MSG_ERRO_INESPERADO = "Ocorreu um erro inesperado."

/** Converte qualquer exceção de rede no erro mostrado na tela (mesma regra do interceptor da web). */
fun Throwable.paraErroApi(): ErroApi = when (this) {
    is HttpException -> {
        val problem = try {
            response()?.errorBody()?.charStream()?.use {
                RetrofitClient.gson.fromJson(it, ProblemDetail::class.java)
            }
        } catch (e: Exception) {
            null
        }
        val campos = problem?.erros.orEmpty()
            .filter { !it.campo.isNullOrBlank() && !it.mensagem.isNullOrBlank() }
            .associate { it.campo!! to it.mensagem!! }
        ErroApi(
            mensagem = problem?.detail?.takeIf { it.isNotBlank() } ?: MSG_ERRO_INESPERADO,
            status = code(),
            campos = if (code() == 400) campos else emptyMap()
        )
    }
    is IOException -> ErroApi(MSG_SEM_CONEXAO)
    else -> ErroApi(MSG_ERRO_INESPERADO)
}
