package com.riobranco.squadsmart.tarefa.app.ui.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riobranco.squadsmart.tarefa.app.data.HistoricoTarefa
import com.riobranco.squadsmart.tarefa.app.data.PrioridadeTarefa
import com.riobranco.squadsmart.tarefa.app.data.StatusTarefa
import com.riobranco.squadsmart.tarefa.app.data.Tarefa
import com.riobranco.squadsmart.tarefa.app.data.TarefaRepository
import com.riobranco.squadsmart.tarefa.app.data.TarefaRequest
import com.riobranco.squadsmart.tarefa.app.data.paraErroApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormUiState(
    val titulo: String = "",
    val descricao: String = "",
    val responsavel: String = "",
    val status: String = StatusTarefa.PENDENTE.name,
    val prioridade: String = PrioridadeTarefa.MEDIA.name,
    /** yyyy-MM-dd ou null */
    val dataVencimento: String? = null,
    /** Erros por campo (validação local ou "erros" do Problem Details): campo -> mensagem. */
    val errosCampo: Map<String, String> = emptyMap(),
    val carregando: Boolean = false,
    val salvando: Boolean = false,
    val excluindo: Boolean = false,
    /** Falha ao buscar a tarefa para edição (ex.: sem internet). */
    val erroCarga: String? = null,
    /** null = esconder (API antiga sem histórico, erro ou lista vazia). */
    val historico: List<HistoricoTarefa>? = null,
    /** Título salvo no servidor, usado no diálogo de exclusão. */
    val tituloOriginal: String = ""
) {
    val ocupado get() = carregando || salvando || excluindo
}

sealed interface FormEvento {
    /** Mensagem em snackbar, sem sair da tela (o que foi digitado continua). */
    data class Mensagem(val texto: String) : FormEvento
    /** Fecha o formulário e mostra [mensagem] na lista. */
    data class Concluido(val mensagem: String) : FormEvento
}

/**
 * Formulário de cadastro/edição (KAN-98, KAN-99). Cada campo digitado vai para o
 * SavedStateHandle, então girar a tela (ou o Android matar o app) não perde nada (KAN-101).
 */
class TarefaFormViewModel(private val salvo: SavedStateHandle) : ViewModel() {

    /** Vem do Intent (extra "id"); -1 = nova tarefa. */
    val id: Long = salvo.get<Long>(TarefaFormActivity.EXTRA_ID) ?: -1L
    val editando get() = id > 0

    private val _estado = MutableStateFlow(
        FormUiState(
            titulo = salvo[TITULO] ?: "",
            descricao = salvo[DESCRICAO] ?: "",
            responsavel = salvo[RESPONSAVEL] ?: "",
            status = salvo[STATUS] ?: StatusTarefa.PENDENTE.name,
            prioridade = salvo[PRIORIDADE] ?: PrioridadeTarefa.MEDIA.name,
            dataVencimento = salvo[DATA],
            tituloOriginal = salvo[TITULO_ORIGINAL] ?: ""
        )
    )
    val estado: StateFlow<FormUiState> = _estado.asStateFlow()

    private val _eventos = Channel<FormEvento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        if (editando) {
            // Se os campos já foram preenchidos antes (app recriado), não sobrescreve o que foi digitado
            if (salvo.get<Boolean>(PREENCHIDO) != true) carregarTarefa()
            carregarHistorico()
        }
    }

    fun carregarTarefa() {
        _estado.update { it.copy(carregando = true, erroCarga = null) }
        viewModelScope.launch {
            try {
                preencher(TarefaRepository.buscarPorId(id))
                _estado.update { it.copy(carregando = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val erro = e.paraErroApi()
                _estado.update { it.copy(carregando = false) }
                if (erro.naoEncontrado) {
                    // Igual à web: tarefa não existe mais -> volta para a lista
                    _eventos.send(FormEvento.Concluido(erro.mensagem))
                } else {
                    _estado.update { it.copy(erroCarga = erro.mensagem) }
                }
            }
        }
    }

    /** O histórico é opcional: se a API ainda não tiver o endpoint (404) ou falhar, ele só não aparece. */
    private fun carregarHistorico() {
        viewModelScope.launch {
            val itens = try {
                TarefaRepository.historico(id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }
            _estado.update { it.copy(historico = itens?.takeIf { lista -> lista.isNotEmpty() }) }
        }
    }

    private fun preencher(t: Tarefa) {
        alterarCampos(
            titulo = t.titulo.orEmpty(),
            descricao = t.descricao.orEmpty(),
            responsavel = t.responsavel.orEmpty(),
            status = t.status ?: StatusTarefa.PENDENTE.name,
            prioridade = t.prioridade ?: PrioridadeTarefa.MEDIA.name,
            data = t.dataVencimento?.takeIf { it.isNotBlank() }
        )
        salvo[TITULO_ORIGINAL] = t.titulo.orEmpty()
        salvo[PREENCHIDO] = true
        _estado.update { it.copy(tituloOriginal = t.titulo.orEmpty()) }
    }

    private fun alterarCampos(
        titulo: String, descricao: String, responsavel: String,
        status: String, prioridade: String, data: String?
    ) {
        salvo[TITULO] = titulo
        salvo[DESCRICAO] = descricao
        salvo[RESPONSAVEL] = responsavel
        salvo[STATUS] = status
        salvo[PRIORIDADE] = prioridade
        salvo[DATA] = data
        _estado.update {
            it.copy(
                titulo = titulo, descricao = descricao, responsavel = responsavel,
                status = status, prioridade = prioridade, dataVencimento = data
            )
        }
    }

    // ----- Alterações vindas da tela (cada uma limpa o erro do próprio campo) -----

    fun alterarTitulo(v: String) = alterar(TITULO, "titulo", v) { copy(titulo = v) }
    fun alterarDescricao(v: String) = alterar(DESCRICAO, "descricao", v) { copy(descricao = v) }
    fun alterarResponsavel(v: String) = alterar(RESPONSAVEL, "responsavel", v) { copy(responsavel = v) }
    fun alterarStatus(v: String) = alterar(STATUS, "status", v) { copy(status = v) }
    fun alterarPrioridade(v: String) = alterar(PRIORIDADE, "prioridade", v) { copy(prioridade = v) }

    fun alterarData(v: String?) {
        salvo[DATA] = v
        _estado.update { it.copy(dataVencimento = v, errosCampo = it.errosCampo - "dataVencimento") }
    }

    private inline fun alterar(chave: String, campo: String, valor: String, crossinline muda: FormUiState.() -> FormUiState) {
        salvo[chave] = valor
        _estado.update { atual ->
            val novo = atual.muda()
            if (novo == atual) atual else novo.copy(errosCampo = atual.errosCampo - campo)
        }
    }

    // ----- Salvar / excluir -----

    fun salvar() {
        val s = _estado.value
        if (s.ocupado) return

        val erros = validar(s)
        if (erros.isNotEmpty()) {
            _estado.update { it.copy(errosCampo = erros) }
            return
        }

        val request = TarefaRequest(
            titulo = s.titulo.trim(),
            descricao = s.descricao.trim().ifEmpty { null },
            responsavel = s.responsavel.trim().ifEmpty { null },
            status = s.status,
            prioridade = s.prioridade,
            dataVencimento = s.dataVencimento
        )

        _estado.update { it.copy(salvando = true, errosCampo = emptyMap()) }
        viewModelScope.launch {
            try {
                if (editando) TarefaRepository.atualizar(id, request) else TarefaRepository.criar(request)
                _eventos.send(FormEvento.Concluido(if (editando) MSG_ATUALIZADA else MSG_CADASTRADA))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val erro = e.paraErroApi()
                // 400: cada "mensagem" vai para o seu campo; o "detail" aparece no snackbar
                _estado.update { it.copy(errosCampo = erro.campos) }
                _eventos.send(FormEvento.Mensagem(erro.mensagem))
            } finally {
                _estado.update { it.copy(salvando = false) }
            }
        }
    }

    fun excluir() {
        if (!editando || _estado.value.ocupado) return
        _estado.update { it.copy(excluindo = true) }
        viewModelScope.launch {
            try {
                TarefaRepository.excluir(id)
                _eventos.send(FormEvento.Concluido(MSG_EXCLUIDA))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _eventos.send(FormEvento.Mensagem(e.paraErroApi().mensagem))
            } finally {
                _estado.update { it.copy(excluindo = false) }
            }
        }
    }

    /** Mesmas regras do formulário da web. */
    private fun validar(s: FormUiState): Map<String, String> = buildMap {
        when {
            s.titulo.isBlank() -> put("titulo", "O título é obrigatório")
            s.titulo.trim().length > 150 -> put("titulo", "O título deve ter no máximo 150 caracteres")
        }
        if (s.descricao.trim().length > 1000) put("descricao", "A descrição deve ter no máximo 1000 caracteres")
        if (s.responsavel.trim().length > 100) put("responsavel", "O responsável deve ter no máximo 100 caracteres")
    }

    private companion object {
        const val TITULO = "titulo"
        const val DESCRICAO = "descricao"
        const val RESPONSAVEL = "responsavel"
        const val STATUS = "status"
        const val PRIORIDADE = "prioridade"
        const val DATA = "dataVencimento"
        const val TITULO_ORIGINAL = "tituloOriginal"
        const val PREENCHIDO = "preenchido"

        const val MSG_CADASTRADA = "Tarefa cadastrada com sucesso."
        const val MSG_ATUALIZADA = "Tarefa atualizada com sucesso."
        const val MSG_EXCLUIDA = "Tarefa excluída com sucesso."
    }
}
