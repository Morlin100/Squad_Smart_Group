package com.riobranco.squadsmart.tarefa.app.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riobranco.squadsmart.tarefa.app.data.Dashboard
import com.riobranco.squadsmart.tarefa.app.data.TarefaRepository
import com.riobranco.squadsmart.tarefa.app.data.paraErroApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val dados: Dashboard? = null,
    val carregando: Boolean = false,
    val atualizando: Boolean = false,
    val erro: String? = null,
    /** A API publicada ainda não tem /dashboard (404). */
    val indisponivel: Boolean = false,
    val responsavel: String = "",
    val status: String = "",
    /** yyyy-MM-dd */
    val dataInicio: String? = null,
    val dataFim: String? = null
)

/** Dashboard (HU07) com os mesmos filtros da web. */
class DashboardViewModel(private val salvo: SavedStateHandle) : ViewModel() {

    private val _estado = MutableStateFlow(
        DashboardUiState(
            responsavel = salvo[RESPONSAVEL] ?: "",
            status = salvo[STATUS] ?: "",
            dataInicio = salvo[INICIO],
            dataFim = salvo[FIM]
        )
    )
    val estado: StateFlow<DashboardUiState> = _estado.asStateFlow()

    private var jobCarga: Job? = null
    private var jobDigitacao: Job? = null

    init {
        carregar()
    }

    fun alterarResponsavel(valor: String) {
        if (valor == _estado.value.responsavel) return
        salvo[RESPONSAVEL] = valor
        _estado.update { it.copy(responsavel = valor) }
        jobDigitacao?.cancel()
        jobDigitacao = viewModelScope.launch {
            delay(300)
            carregar()
        }
    }

    fun alterarStatus(valor: String) {
        if (valor == _estado.value.status) return
        salvo[STATUS] = valor
        _estado.update { it.copy(status = valor) }
        carregar()
    }

    fun alterarPeriodo(inicio: String?, fim: String?) {
        salvo[INICIO] = inicio
        salvo[FIM] = fim
        _estado.update { it.copy(dataInicio = inicio, dataFim = fim) }
        carregar()
    }

    fun limpar() {
        salvo[RESPONSAVEL] = ""
        salvo[STATUS] = ""
        salvo[INICIO] = null
        salvo[FIM] = null
        _estado.update { it.copy(responsavel = "", status = "", dataInicio = null, dataFim = null) }
        carregar()
    }

    fun carregar(puxouParaAtualizar: Boolean = false) {
        jobDigitacao?.cancel()
        jobCarga?.cancel()
        _estado.update { it.copy(carregando = !puxouParaAtualizar, atualizando = puxouParaAtualizar, erro = null) }
        jobCarga = viewModelScope.launch {
            val f = _estado.value
            try {
                val dados = TarefaRepository.dashboard(f.responsavel, f.status, f.dataInicio, f.dataFim)
                _estado.update { it.copy(dados = dados, carregando = false, atualizando = false, indisponivel = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val erro = e.paraErroApi()
                _estado.update {
                    it.copy(
                        carregando = false,
                        atualizando = false,
                        indisponivel = erro.naoEncontrado,
                        erro = if (erro.naoEncontrado) null else erro.mensagem
                    )
                }
            }
        }
    }

    private companion object {
        const val RESPONSAVEL = "responsavel"
        const val STATUS = "status"
        const val INICIO = "dataInicio"
        const val FIM = "dataFim"
    }
}
