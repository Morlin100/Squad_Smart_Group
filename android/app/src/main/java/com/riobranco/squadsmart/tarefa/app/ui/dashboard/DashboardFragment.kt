package com.riobranco.squadsmart.tarefa.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.riobranco.squadsmart.tarefa.app.R
import com.riobranco.squadsmart.tarefa.app.data.Dashboard
import com.riobranco.squadsmart.tarefa.app.data.PrioridadeTarefa
import com.riobranco.squadsmart.tarefa.app.data.StatusTarefa
import com.riobranco.squadsmart.tarefa.app.ui.comum.configurarOpcoes
import com.riobranco.squadsmart.tarefa.app.ui.comum.dataFormatada
import com.riobranco.squadsmart.tarefa.app.ui.comum.definirTextoSeDiferente
import com.riobranco.squadsmart.tarefa.app.ui.comum.horaFormatada
import com.riobranco.squadsmart.tarefa.app.ui.comum.millisUtcParaLocalDate
import com.riobranco.squadsmart.tarefa.app.ui.comum.mostrarOpcao
import com.riobranco.squadsmart.tarefa.app.ui.comum.paraLocalDate
import com.riobranco.squadsmart.tarefa.app.ui.comum.paraMillisUtc
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

/** Indicadores e barras por status, prioridade e responsável (igual à web). */
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private val opcoesStatus = listOf("") + StatusTarefa.entries.map { it.name }
    private val formatoPercentual = DecimalFormat("0.#", DecimalFormatSymbols(Locale("pt", "BR")))
    private var dadosMostrados: Dashboard? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dadosMostrados = null
        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipe)
        val editResponsavel = view.findViewById<TextInputEditText>(R.id.editResponsavel)
        val dropStatus = view.findViewById<MaterialAutoCompleteTextView>(R.id.dropStatus)
        val campoPeriodo = view.findViewById<TextInputLayout>(R.id.campoPeriodo)
        val editPeriodo = view.findViewById<TextInputEditText>(R.id.editPeriodo)
        val botaoLimpar = view.findViewById<MaterialButton>(R.id.botaoLimpar)
        val progresso = view.findViewById<LinearProgressIndicator>(R.id.progresso)
        val textoAtualizado = view.findViewById<TextView>(R.id.textoAtualizado)
        val estado = view.findViewById<LinearLayout>(R.id.estado)
        val textoEstado = view.findViewById<TextView>(R.id.textoEstado)
        val botaoTentar = view.findViewById<MaterialButton>(R.id.botaoTentar)
        val conteudo = view.findViewById<LinearLayout>(R.id.conteudo)

        swipe.setColorSchemeResources(R.color.ouro_600)
        swipe.setOnRefreshListener { viewModel.carregar(puxouParaAtualizar = true) }
        editResponsavel.doAfterTextChanged { viewModel.alterarResponsavel(it?.toString().orEmpty()) }
        dropStatus.configurarOpcoes(opcoesStatus.map { rotuloStatus(it) }) { viewModel.alterarStatus(opcoesStatus[it]) }
        editPeriodo.setOnClickListener { abrirPeriodo() }
        campoPeriodo.setEndIconOnClickListener { viewModel.alterarPeriodo(null, null) }
        (childFragmentManager.findFragmentByTag(TAG_PERIODO) as? MaterialDatePicker<*>)?.let { conectarPeriodo(it) }
        botaoLimpar.setOnClickListener { viewModel.limpar() }
        botaoTentar.setOnClickListener { viewModel.carregar() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { s ->
                    editResponsavel.definirTextoSeDiferente(s.responsavel)
                    dropStatus.mostrarOpcao(rotuloStatus(s.status))
                    editPeriodo.definirTextoSeDiferente(textoPeriodo(s.dataInicio, s.dataFim))
                    campoPeriodo.isEndIconVisible = s.dataInicio != null

                    progresso.visibility = if (s.carregando) View.VISIBLE else View.GONE
                    swipe.isRefreshing = s.atualizando

                    val mensagem = when {
                        s.indisponivel -> getString(R.string.dashboard_indisponivel)
                        s.erro != null -> s.erro
                        else -> null
                    }
                    estado.visibility = if (mensagem != null) View.VISIBLE else View.GONE
                    textoEstado.text = mensagem

                    val dados = s.dados
                    val mostrarDados = dados != null && !s.indisponivel && s.erro == null
                    conteudo.visibility = if (mostrarDados) View.VISIBLE else View.GONE
                    textoAtualizado.visibility = if (mostrarDados) View.VISIBLE else View.GONE
                    if (dados != null && dados !== dadosMostrados) {
                        dadosMostrados = dados
                        textoAtualizado.text = getString(R.string.atualizado_as, dados.geradoEm.horaFormatada())
                        mostrarDados(view, dados)
                    }
                }
            }
        }
    }

    private fun mostrarDados(view: View, d: Dashboard) {
        indicador(view.findViewById(R.id.indTotal), R.drawable.ic_assignment, d.total.toString(), getString(R.string.ind_tarefas), false)
        indicador(
            view.findViewById(R.id.indConcluidas), R.drawable.ic_task_alt,
            formatoPercentual.format(d.percentualConcluidas) + "%",
            getString(R.string.ind_concluidas, d.concluidas.toInt()), false
        )
        indicador(view.findViewById(R.id.indVencidas), R.drawable.ic_event_busy, d.vencidas.toString(), getString(R.string.ind_vencidas), d.vencidas > 0)
        indicador(view.findViewById(R.id.indCriadas), R.drawable.ic_add_task, d.criadasUltimos7Dias.toString(), getString(R.string.ind_criadas_7), false)

        val porStatus = d.porStatus.orEmpty()
        grupo(view.findViewById(R.id.grupoStatus), getString(R.string.por_status),
            StatusTarefa.entries.map { it.rotulo to (porStatus[it.name] ?: 0L) })

        val porPrioridade = d.porPrioridade.orEmpty()
        grupo(view.findViewById(R.id.grupoPrioridade), getString(R.string.por_prioridade),
            PrioridadeTarefa.entries.map { it.rotulo to (porPrioridade[it.name] ?: 0L) })

        grupo(view.findViewById(R.id.grupoResponsavel), getString(R.string.por_responsavel),
            d.porResponsavel.orEmpty().map {
                (it.responsavel?.takeIf { r -> r.isNotBlank() } ?: getString(R.string.sem_responsavel)) to it.quantidade
            })
    }

    private fun indicador(cartao: View, icone: Int, valor: String, rotulo: String, alerta: Boolean) {
        val cor = ContextCompat.getColor(requireContext(), if (alerta) R.color.erro else R.color.ouro_500)
        cartao.findViewById<ImageView>(R.id.icone).apply {
            setImageResource(icone)
            setColorFilter(cor)
        }
        cartao.findViewById<TextView>(R.id.valor).apply {
            text = valor
            setTextColor(ContextCompat.getColor(requireContext(), if (alerta) R.color.erro else R.color.texto_forte))
        }
        cartao.findViewById<TextView>(R.id.rotulo).text = rotulo
        cartao.contentDescription = "$rotulo: $valor"
    }

    /** Barras com largura relativa ao maior valor do grupo (mesma conta da web). */
    private fun grupo(cartao: View, titulo: String, itens: List<kotlin.Pair<String, Long>>) {
        cartao.findViewById<TextView>(R.id.tituloGrupo).text = titulo
        val barras = cartao.findViewById<LinearLayout>(R.id.barras)
        cartao.findViewById<TextView>(R.id.textoVazio).visibility = if (itens.isEmpty()) View.VISIBLE else View.GONE
        barras.removeAllViews()
        val maior = maxOf(1L, itens.maxOfOrNull { it.second } ?: 0L)
        val inflater = LayoutInflater.from(requireContext())
        itens.forEach { (rotulo, quantidade) ->
            val linha = inflater.inflate(R.layout.item_barra, barras, false)
            linha.findViewById<TextView>(R.id.nome).text = rotulo
            linha.findViewById<TextView>(R.id.quantidade).text = quantidade.toString()
            linha.findViewById<LinearProgressIndicator>(R.id.barra).progress =
                ((quantidade.toDouble() / maior) * 100).roundToInt()
            linha.contentDescription = "$rotulo: $quantidade"
            barras.addView(linha)
        }
    }

    private fun abrirPeriodo() {
        if (childFragmentManager.findFragmentByTag(TAG_PERIODO) != null) return
        val s = viewModel.estado.value
        val inicio = s.dataInicio.paraLocalDate()
        val fim = s.dataFim.paraLocalDate()
        val seletor = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.criadas_no_periodo)
            .apply { if (inicio != null && fim != null) setSelection(Pair(inicio.paraMillisUtc(), fim.paraMillisUtc())) }
            .build()
        conectarPeriodo(seletor)
        seletor.show(childFragmentManager, TAG_PERIODO)
    }

    private fun conectarPeriodo(seletor: MaterialDatePicker<*>) {
        seletor.addOnPositiveButtonClickListener { selecao ->
            @Suppress("UNCHECKED_CAST")
            val par = selecao as? Pair<Long?, Long?> ?: return@addOnPositiveButtonClickListener
            val inicio = par.first?.millisUtcParaLocalDate()?.toString()
            val fim = par.second?.millisUtcParaLocalDate()?.toString() ?: inicio
            viewModel.alterarPeriodo(inicio, fim)
        }
    }

    private fun textoPeriodo(inicio: String?, fim: String?): String {
        val i = inicio.dataFormatada() ?: return ""
        val f = fim.dataFormatada() ?: return i
        return if (i == f) i else "$i – $f"
    }

    private fun rotuloStatus(codigo: String) =
        if (codigo.isEmpty()) getString(R.string.todos) else StatusTarefa.rotuloDe(codigo)

    private companion object {
        const val TAG_PERIODO = "periodo"
    }
}
