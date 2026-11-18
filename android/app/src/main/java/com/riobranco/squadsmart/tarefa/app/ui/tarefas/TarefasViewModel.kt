package com.riobranco.squadsmart.tarefa.app.ui.tarefas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riobranco.squadsmart.tarefa.app.data.Ordenacao
import com.riobranco.squadsmart.tarefa.app.data.Tarefa
import com.riobranco.squadsmart.tarefa.app.data.TarefaRepository
import com.riobranco.squadsmart.tarefa.app.data.paraErroApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListaUiState(
    val tarefas: List<Tarefa> = emptyList(),
    /** Carregando a primeira página (busca, filtro, ordenação ou após salvar). */
    val carregando: Boolean = false,
    /** Puxar para atualizar. */
    val atualizando: Boolean = false,
    /** Carregando a próxima página ao chegar no fim da lista. */
    val carregandoMais: Boolean = false,
    val ultimaPagina: Boolean = true,
    /** Erro ao carregar quando não há nada na tela. */
    val erro: String? = null,
    val excluindoId: Long? = null,
    // Filtros (também guardados no SavedStateHandle)
    val termo: String = "",
    val responsavel: String = "",
    val status: String = "",
    val ordenacao: String = Ordenacao.MAIS_RECENTES.valor
)

/**
 * Estado da lista (KAN-97). Sobrevive à rotação por ser um ViewModel, e os filtros
 * também ficam no SavedStateHandle para sobreviver se o Android matar o app (KAN-101).
 */
class TarefasViewModel(private val salvo: SavedStateHandle) : ViewModel() {

    private val _estado = MutableStateFlow(
        ListaUiState(
            termo = salvo[TERMO] ?: "",
            responsavel = salvo[RESPONSAVEL] ?: "",
            status = salvo[STATUS] ?: "",
            ordenacao = salvo[ORDENACAO] ?: Ordenacao.MAIS_RECENTES.valor
        )
    )
    val estado: StateFlow<ListaUiState> = _estado.asStateFlow()

    private val _mensagens = Channel<String>(Channel.BUFFERED)
    /** Mensagens de snackbar (sucesso ou erro com a lista já na tela). */
    val mensagens = _mensagens.receiveAsFlow()

    private var paginaAtual = 0
    /** Muda a cada nova carga; respostas de cargas antigas são descartadas. */
    private var geracao = 0
    private var jobCarga: Job? = null
    private var jobMais: Job? = null
    private var jobDigitacao: Job? = null

    init {
        carregar()
    }

    fun alterarTermo(valor: String) {
        if (valor == _estado.value.termo) return
        salvo[TERMO] = valor
        _estado.update { it.copy(termo = valor) }
        carregarDepoisDeDigitar()
    }

    fun alterarResponsavel(valor: String) {
        if (valor == _estado.value.responsavel) return
        salvo[RESPONSAVEL] = valor
        _estado.update { it.copy(responsavel = valor) }
        carregarDepoisDeDigitar()
    }

    fun alterarStatus(valor: String) {
        if (valor == _estado.value.status) return
        salvo[STATUS] = valor
        _estado.update { it.copy(status = valor) }
        carregar()
    }

    fun alterarOrdenacao(valor: String) {
        if (valor == _estado.value.ordenacao) return
        salvo[ORDENACAO] = valor
        _estado.update { it.copy(ordenacao = valor) }
        carregar()
    }

    fun limparFiltros() {
        salvo[TERMO] = ""
        salvo[RESPONSAVEL] = ""
        salvo[STATUS] = ""
        salvo[ORDENACAO] = Ordenacao.MAIS_RECENTES.valor
        _estado.update {
            it.copy(termo = "", responsavel = "", status = "", ordenacao = Ordenacao.MAIS_RECENTES.valor)
        }
        carregar()
    }

    /** Mesmo atraso de 300 ms da web, para não chamar a API a cada letra. */
    private fun carregarDepoisDeDigitar() {
        jobDigitacao?.cancel()
        jobDigitacao = viewModelScope.launch {
            delay(300)
            carregar()
        }
    }

    fun atualizar() = carregar(puxouParaAtualizar = true)

    /** Volta para a primeira página com os filtros atuais. */
    fun carregar(puxouParaAtualizar: Boolean = false) {
        jobDigitacao?.cancel()
        jobCarga?.cancel()
        jobMais?.cancel()
        val minhaGeracao = ++geracao
        _estado.update {
            it.copy(carregando = !puxouParaAtualizar, atualizando = puxouParaAtualizar, carregandoMais = false, erro = null)
        }
        jobCarga = viewModelScope.launch {
            val f = _estado.value
            try {
                val pagina = TarefaRepository.listar(0, f.termo, f.responsavel, f.status, f.ordenacao)
                if (minhaGeracao != geracao) return@launch
                paginaAtual = 0
                _estado.update {
                    it.copy(
                        tarefas = pagina.conteudo.orEmpty(),
                        ultimaPagina = pagina.pagina + 1 >= pagina.totalPaginas,
                        carregando = false,
                        atualizando = false
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (minhaGeracao != geracao) return@launch
                val erro = e.paraErroApi().mensagem
                // Com tarefas na tela, mantém a lista e avisa por snackbar
                if (_estado.value.tarefas.isEmpty()) {
                    _estado.update { it.copy(carregando = false, atualizando = false, erro = erro) }
                } else {
                    _estado.update { it.copy(carregando = false, atualizando = false) }
                    _mensagens.send(erro)
                }
            }
        }
    }

    /** Rolagem infinita: busca a próxima página e junta na lista. */
    fun carregarMais() {
        val atual = _estado.value
        if (atual.carregando || atual.atualizando || atual.carregandoMais || atual.ultimaPagina) return
        if (jobMais?.isActive == true) return
        val minhaGeracao = geracao
        _estado.update { it.copy(carregandoMais = true) }
        jobMais = viewModelScope.launch {
            try {
                val proxima = paginaAtual + 1
                val pagina = TarefaRepository.listar(proxima, atual.termo, atual.responsavel, atual.status, atual.ordenacao)
                if (minhaGeracao != geracao) return@launch
                paginaAtual = proxima
                _estado.update { estado ->
                    val ids = estado.tarefas.map { it.id }.toSet()
                    estado.copy(
                        tarefas = estado.tarefas + pagina.conteudo.orEmpty().filter { it.id !in ids },
                        ultimaPagina = pagina.pagina + 1 >= pagina.totalPaginas,
                        carregandoMais = false
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (minhaGeracao != geracao) return@launch
                _estado.update { it.copy(carregandoMais = false) }
                _mensagens.send(e.paraErroApi().mensagem)
            }
        }
    }

    fun excluir(id: Long) {
        if (_estado.value.excluindoId != null) return
        _estado.update { it.copy(excluindoId = id) }
        viewModelScope.launch {
            try {
                TarefaRepository.excluir(id)
                _mensagens.send(MSG_EXCLUIDA)
                carregar()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _mensagens.send(e.paraErroApi().mensagem)
            } finally {
                _estado.update { it.copy(excluindoId = null) }
            }
        }
    }

    /** Chamado ao voltar do formulário depois de salvar/excluir. */
    fun aoVoltarDoFormulario(mensagem: String) {
        viewModelScope.launch { _mensagens.send(mensagem) }
        carregar()
    }

    private companion object {
        const val TERMO = "termo"
        const val RESPONSAVEL = "responsavel"
        const val STATUS = "status"
        const val ORDENACAO = "ordenacao"
        const val MSG_EXCLUIDA = "Tarefa excluída com sucesso."
    }
}
