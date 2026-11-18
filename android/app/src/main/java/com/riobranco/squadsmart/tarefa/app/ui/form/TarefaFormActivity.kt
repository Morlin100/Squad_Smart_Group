package com.riobranco.squadsmart.tarefa.app.ui.form

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.riobranco.squadsmart.tarefa.app.R
import com.riobranco.squadsmart.tarefa.app.data.AcaoHistorico
import com.riobranco.squadsmart.tarefa.app.data.HistoricoTarefa
import com.riobranco.squadsmart.tarefa.app.data.PrioridadeTarefa
import com.riobranco.squadsmart.tarefa.app.data.StatusTarefa
import com.riobranco.squadsmart.tarefa.app.ui.comum.ConfirmacaoDialog
import com.riobranco.squadsmart.tarefa.app.ui.comum.aplicarInsetsBase
import com.riobranco.squadsmart.tarefa.app.ui.comum.aplicarInsetsTopo
import com.riobranco.squadsmart.tarefa.app.ui.comum.configurarOpcoes
import com.riobranco.squadsmart.tarefa.app.ui.comum.dataFormatada
import com.riobranco.squadsmart.tarefa.app.ui.comum.dataHoraFormatada
import com.riobranco.squadsmart.tarefa.app.ui.comum.definirTextoSeDiferente
import com.riobranco.squadsmart.tarefa.app.ui.comum.millisUtcParaLocalDate
import com.riobranco.squadsmart.tarefa.app.ui.comum.mostrarOpcao
import com.riobranco.squadsmart.tarefa.app.ui.comum.paraLocalDate
import com.riobranco.squadsmart.tarefa.app.ui.comum.paraMillisUtc
import com.riobranco.squadsmart.tarefa.app.ui.comum.snackbar
import kotlinx.coroutines.launch

/** Cadastro (KAN-98) e edição (KAN-99) de tarefa, com histórico e exclusão. */
class TarefaFormActivity : AppCompatActivity() {

    private val viewModel: TarefaFormViewModel by viewModels()

    private lateinit var raiz: View
    private lateinit var campoTitulo: TextInputLayout
    private lateinit var campoDescricao: TextInputLayout
    private lateinit var campoResponsavel: TextInputLayout
    private lateinit var campoStatus: TextInputLayout
    private lateinit var campoPrioridade: TextInputLayout
    private lateinit var campoData: TextInputLayout
    private var historicoMostrado: List<HistoricoTarefa>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarefa_form)

        raiz = findViewById(R.id.raiz)
        val appBar = findViewById<AppBarLayout>(R.id.appBar)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val progresso = findViewById<LinearProgressIndicator>(R.id.progresso)
        val scroll = findViewById<NestedScrollView>(R.id.scroll)
        val cartaoErro = findViewById<MaterialCardView>(R.id.cartaoErro)
        val textoErro = findViewById<TextView>(R.id.textoErro)
        val botaoTentar = findViewById<MaterialButton>(R.id.botaoTentar)
        campoTitulo = findViewById(R.id.campoTitulo)
        campoDescricao = findViewById(R.id.campoDescricao)
        campoResponsavel = findViewById(R.id.campoResponsavel)
        campoStatus = findViewById(R.id.campoStatus)
        campoPrioridade = findViewById(R.id.campoPrioridade)
        campoData = findViewById(R.id.campoData)
        val editTitulo = findViewById<TextInputEditText>(R.id.editTitulo)
        val editDescricao = findViewById<TextInputEditText>(R.id.editDescricao)
        val editResponsavel = findViewById<TextInputEditText>(R.id.editResponsavel)
        val dropStatus = findViewById<MaterialAutoCompleteTextView>(R.id.dropStatus)
        val dropPrioridade = findViewById<MaterialAutoCompleteTextView>(R.id.dropPrioridade)
        val editData = findViewById<TextInputEditText>(R.id.editData)
        val botaoSalvar = findViewById<MaterialButton>(R.id.botaoSalvar)
        val botaoCancelar = findViewById<MaterialButton>(R.id.botaoCancelar)
        val botaoExcluir = findViewById<MaterialButton>(R.id.botaoExcluir)
        val cartaoHistorico = findViewById<MaterialCardView>(R.id.cartaoHistorico)
        val listaHistorico = findViewById<LinearLayout>(R.id.listaHistorico)

        aplicarInsetsTopo(raiz, appBar)
        aplicarInsetsBase(scroll)

        toolbar.title = getString(if (viewModel.editando) R.string.editar_tarefa else R.string.nova_tarefa)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
        botaoCancelar.setOnClickListener { finish() }
        botaoExcluir.visibility = if (viewModel.editando) View.VISIBLE else View.GONE

        // ----- Campos -> ViewModel -----
        editTitulo.doAfterTextChanged { viewModel.alterarTitulo(it?.toString().orEmpty()) }
        editDescricao.doAfterTextChanged { viewModel.alterarDescricao(it?.toString().orEmpty()) }
        editResponsavel.doAfterTextChanged { viewModel.alterarResponsavel(it?.toString().orEmpty()) }
        val status = StatusTarefa.entries
        val prioridades = PrioridadeTarefa.entries
        dropStatus.configurarOpcoes(status.map { it.rotulo }) { viewModel.alterarStatus(status[it].name) }
        dropPrioridade.configurarOpcoes(prioridades.map { it.rotulo }) { viewModel.alterarPrioridade(prioridades[it].name) }

        // ----- Data de vencimento (DatePicker) -----
        editData.setOnClickListener { abrirCalendario() }
        campoData.setEndIconOnClickListener { viewModel.alterarData(null) }
        // Se a tela girou com o calendário aberto, reconecta o retorno dele
        (supportFragmentManager.findFragmentByTag(TAG_CALENDARIO) as? MaterialDatePicker<*>)?.let { conectarCalendario(it) }

        botaoSalvar.setOnClickListener { viewModel.salvar() }
        botaoTentar.setOnClickListener { viewModel.carregarTarefa() }
        botaoExcluir.setOnClickListener {
            ConfirmacaoDialog.mostrar(
                supportFragmentManager, CHAVE_EXCLUIR, viewModel.id,
                viewModel.estado.value.tituloOriginal.ifBlank { viewModel.estado.value.titulo }
            )
        }
        supportFragmentManager.setFragmentResultListener(CHAVE_EXCLUIR, this) { _, _ -> viewModel.excluir() }

        // ----- ViewModel -> tela -----
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.estado.collect { s ->
                        editTitulo.definirTextoSeDiferente(s.titulo)
                        editDescricao.definirTextoSeDiferente(s.descricao)
                        editResponsavel.definirTextoSeDiferente(s.responsavel)
                        dropStatus.mostrarOpcao(StatusTarefa.rotuloDe(s.status))
                        dropPrioridade.mostrarOpcao(PrioridadeTarefa.rotuloDe(s.prioridade))
                        editData.definirTextoSeDiferente(s.dataVencimento.dataFormatada().orEmpty())
                        campoData.isEndIconVisible = s.dataVencimento != null

                        mostrarErros(s.errosCampo)

                        progresso.visibility = if (s.ocupado) View.VISIBLE else View.GONE
                        botaoSalvar.isEnabled = !s.ocupado
                        botaoExcluir.isEnabled = !s.ocupado && !s.carregando

                        cartaoErro.visibility = if (s.erroCarga != null) View.VISIBLE else View.GONE
                        textoErro.text = s.erroCarga

                        if (s.historico !== historicoMostrado) {
                            historicoMostrado = s.historico
                            mostrarHistorico(cartaoHistorico, listaHistorico, s.historico)
                        }
                    }
                }
                launch {
                    viewModel.eventos.collect { evento ->
                        when (evento) {
                            is FormEvento.Mensagem -> raiz.snackbar(evento.texto, longa = true)
                            is FormEvento.Concluido -> {
                                setResult(RESULT_OK, Intent().putExtra(EXTRA_MENSAGEM, evento.mensagem))
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun mostrarErros(erros: Map<String, String>) {
        campoTitulo.error = erros["titulo"]
        campoDescricao.error = erros["descricao"]
        campoResponsavel.error = erros["responsavel"]
        campoStatus.error = erros["status"]
        campoPrioridade.error = erros["prioridade"]
        campoData.error = erros["dataVencimento"]
    }

    private fun abrirCalendario() {
        if (supportFragmentManager.findFragmentByTag(TAG_CALENDARIO) != null) return
        val atual = viewModel.estado.value.dataVencimento.paraLocalDate()
        val calendario = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.data_vencimento)
            .apply { if (atual != null) setSelection(atual.paraMillisUtc()) }
            .build()
        conectarCalendario(calendario)
        calendario.show(supportFragmentManager, TAG_CALENDARIO)
    }

    private fun conectarCalendario(calendario: MaterialDatePicker<*>) {
        calendario.addOnPositiveButtonClickListener { selecao ->
            val millis = selecao as? Long ?: return@addOnPositiveButtonClickListener
            viewModel.alterarData(millis.millisUtcParaLocalDate().toString())
        }
    }

    private fun mostrarHistorico(cartao: View, lista: LinearLayout, itens: List<HistoricoTarefa>?) {
        lista.removeAllViews()
        if (itens.isNullOrEmpty()) {
            cartao.visibility = View.GONE
            return
        }
        cartao.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(this)
        itens.forEachIndexed { i, item ->
            val linha = inflater.inflate(R.layout.item_historico, lista, false)
            linha.findViewById<TextView>(R.id.textoAcao).text = AcaoHistorico.rotuloDe(item.acao)
            linha.findViewById<TextView>(R.id.textoQuando).text = item.realizadoEm.dataHoraFormatada()
            linha.findViewById<TextView>(R.id.textoDetalhes).apply {
                text = item.detalhes
                visibility = if (item.detalhes.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            lista.addView(linha)
            if (i < itens.lastIndex) {
                lista.addView(View(this).apply {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.borda_suave))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                })
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_MENSAGEM = "mensagem"
        private const val TAG_CALENDARIO = "calendario"
        private const val CHAVE_EXCLUIR = "excluir_tarefa_form"

        fun intentNova(context: Context) = Intent(context, TarefaFormActivity::class.java)

        fun intentEditar(context: Context, id: Long) =
            Intent(context, TarefaFormActivity::class.java).putExtra(EXTRA_ID, id)
    }
}
