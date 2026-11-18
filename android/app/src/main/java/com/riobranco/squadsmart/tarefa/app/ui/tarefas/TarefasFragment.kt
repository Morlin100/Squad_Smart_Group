package com.riobranco.squadsmart.tarefa.app.ui.tarefas

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.riobranco.squadsmart.tarefa.app.R
import com.riobranco.squadsmart.tarefa.app.data.Ordenacao
import com.riobranco.squadsmart.tarefa.app.data.StatusTarefa
import com.riobranco.squadsmart.tarefa.app.ui.comum.ConfirmacaoDialog
import com.riobranco.squadsmart.tarefa.app.ui.comum.configurarOpcoes
import com.riobranco.squadsmart.tarefa.app.ui.comum.definirTextoSeDiferente
import com.riobranco.squadsmart.tarefa.app.ui.comum.mostrarOpcao
import com.riobranco.squadsmart.tarefa.app.ui.comum.snackbar
import com.riobranco.squadsmart.tarefa.app.ui.form.TarefaFormActivity
import kotlinx.coroutines.launch

class TarefasFragment : Fragment(R.layout.fragment_tarefas) {

    private val viewModel: TarefasViewModel by viewModels()

    private lateinit var adapter: TarefaAdapter
    private val rodape = RodapeAdapter()

    // Abre o formulário e, ao voltar com sucesso, mostra a mensagem e atualiza a lista
    private val abrirFormulario = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val mensagem = resultado.data?.getStringExtra(TarefaFormActivity.EXTRA_MENSAGEM)
            if (mensagem != null) viewModel.aoVoltarDoFormulario(mensagem) else viewModel.carregar()
        }
    }

    // Opções dos filtros: "Todos" + os status
    private val opcoesStatus = listOf("") + StatusTarefa.entries.map { it.name }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editTermo = view.findViewById<TextInputEditText>(R.id.editTermo)
        val editResponsavel = view.findViewById<TextInputEditText>(R.id.editResponsavel)
        val dropStatus = view.findViewById<MaterialAutoCompleteTextView>(R.id.dropStatus)
        val dropOrdenacao = view.findViewById<MaterialAutoCompleteTextView>(R.id.dropOrdenacao)
        val botaoLimpar = view.findViewById<MaterialButton>(R.id.botaoLimpar)
        val progresso = view.findViewById<LinearProgressIndicator>(R.id.progresso)
        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipe)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        val estado = view.findViewById<LinearLayout>(R.id.estado)
        val iconeEstado = view.findViewById<ImageView>(R.id.iconeEstado)
        val textoEstado = view.findViewById<TextView>(R.id.textoEstado)
        val botaoTentar = view.findViewById<Button>(R.id.botaoTentar)
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.fabNova)

        // ----- Lista -----
        adapter = TarefaAdapter(
            aoAbrir = { abrirFormulario.launch(TarefaFormActivity.intentEditar(requireContext(), it.id)) },
            aoExcluir = { ConfirmacaoDialog.mostrar(childFragmentManager, CHAVE_EXCLUIR, it.id, it.titulo.orEmpty()) }
        )
        val layoutManager = LinearLayoutManager(requireContext())
        recycler.layoutManager = layoutManager
        recycler.adapter = ConcatAdapter(adapter, rodape)

        fun chegouNoFim() {
            val ultimo = layoutManager.findLastVisibleItemPosition()
            if (ultimo != RecyclerView.NO_POSITION && ultimo >= adapter.itemCount - 5) viewModel.carregarMais()
        }
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    chegouNoFim()
                    fab.shrink()
                } else if (dy < 0) {
                    fab.extend()
                }
            }
        })

        // O filho do SwipeRefreshLayout é um FrameLayout, então dizemos quando a lista ainda pode subir
        swipe.setColorSchemeResources(R.color.ouro_600)
        swipe.setOnChildScrollUpCallback { _, _ -> recycler.canScrollVertically(-1) }
        swipe.setOnRefreshListener { viewModel.atualizar() }

        fab.setOnClickListener { abrirFormulario.launch(TarefaFormActivity.intentNova(requireContext())) }
        botaoTentar.setOnClickListener { viewModel.carregar() }

        // ----- Filtros -----
        editTermo.doAfterTextChanged { viewModel.alterarTermo(it?.toString().orEmpty()) }
        editResponsavel.doAfterTextChanged { viewModel.alterarResponsavel(it?.toString().orEmpty()) }
        dropStatus.configurarOpcoes(opcoesStatus.map { rotuloStatus(it) }) { viewModel.alterarStatus(opcoesStatus[it]) }
        dropOrdenacao.configurarOpcoes(Ordenacao.entries.map { it.rotulo }) {
            viewModel.alterarOrdenacao(Ordenacao.entries[it].valor)
        }
        botaoLimpar.setOnClickListener { viewModel.limparFiltros() }

        // Resposta do diálogo de exclusão
        childFragmentManager.setFragmentResultListener(CHAVE_EXCLUIR, viewLifecycleOwner) { _, bundle ->
            viewModel.excluir(bundle.getLong(ConfirmacaoDialog.RESULTADO_ID))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.estado.collect { s ->
                        editTermo.definirTextoSeDiferente(s.termo)
                        editResponsavel.definirTextoSeDiferente(s.responsavel)
                        dropStatus.mostrarOpcao(rotuloStatus(s.status))
                        dropOrdenacao.mostrarOpcao(Ordenacao.de(s.ordenacao).rotulo)

                        progresso.visibility = if (s.carregando) View.VISIBLE else View.GONE
                        swipe.isRefreshing = s.atualizando
                        rodape.visivel = s.carregandoMais
                        adapter.excluindoId = s.excluindoId
                        // Depois de mostrar a lista, verifica se ela já cabe na tela e precisa da próxima página
                        adapter.submitList(s.tarefas) { recycler.post { chegouNoFim() } }

                        when {
                            s.erro != null -> {
                                estado.visibility = View.VISIBLE
                                iconeEstado.setImageResource(R.drawable.ic_cloud_off)
                                textoEstado.text = s.erro
                                botaoTentar.visibility = View.VISIBLE
                            }
                            s.tarefas.isEmpty() && !s.carregando && !s.atualizando -> {
                                estado.visibility = View.VISIBLE
                                iconeEstado.setImageResource(R.drawable.ic_assignment)
                                textoEstado.setText(R.string.nenhuma_tarefa)
                                botaoTentar.visibility = View.GONE
                            }
                            else -> estado.visibility = View.GONE
                        }
                    }
                }
                launch {
                    viewModel.mensagens.collect { view.snackbar(it, ancora = fab) }
                }
            }
        }
    }

    private fun rotuloStatus(codigo: String) =
        if (codigo.isEmpty()) getString(R.string.todos) else StatusTarefa.rotuloDe(codigo)

    private companion object {
        const val CHAVE_EXCLUIR = "excluir_tarefa_lista"
    }
}
