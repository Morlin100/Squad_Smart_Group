package com.riobranco.squadsmart.tarefa.app.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.riobranco.squadsmart.tarefa.app.R
import com.riobranco.squadsmart.tarefa.app.ui.comum.aplicarInsetsTopo
import com.riobranco.squadsmart.tarefa.app.ui.dashboard.DashboardFragment
import com.riobranco.squadsmart.tarefa.app.ui.pesquisa.PesquisaFragment
import com.riobranco.squadsmart.tarefa.app.ui.tarefas.TarefasFragment

/**
 * Tela principal com a barra inferior Tarefas / Dashboard / Avaliar.
 * As abas são escondidas/mostradas (não recriadas), então cada uma mantém sua rolagem e estado.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var textoTitulo: TextView
    private var abaAtual = R.id.nav_tarefas

    /** Voltar em Dashboard/Avaliar leva para Tarefas antes de sair do app. */
    private val voltarParaTarefas = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            bottomNav.selectedItemId = R.id.nav_tarefas
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        textoTitulo = findViewById(R.id.textoTitulo)
        aplicarInsetsTopo(findViewById(R.id.raiz), findViewById<AppBarLayout>(R.id.appBar))
        onBackPressedDispatcher.addCallback(this, voltarParaTarefas)

        abaAtual = savedInstanceState?.getInt(ABA_ATUAL) ?: R.id.nav_tarefas
        if (savedInstanceState == null) mostrarAba(abaAtual)
        atualizarCabecalho()

        bottomNav.setOnItemSelectedListener { item ->
            mostrarAba(item.itemId)
            true
        }
        // Tocar de novo na aba atual não faz nada
        bottomNav.setOnItemReselectedListener { }
    }

    private fun mostrarAba(id: Int) {
        val tag = tagDa(id)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            // Esconde as outras abas já criadas
            supportFragmentManager.fragments
                .filter { it.tag in TAGS && it.tag != tag }
                .forEach { hide(it) }
            val existente = supportFragmentManager.findFragmentByTag(tag)
            if (existente == null) add(R.id.container, novoFragment(id), tag) else show(existente)
        }
        abaAtual = id
        atualizarCabecalho()
    }

    private fun atualizarCabecalho() {
        textoTitulo.setText(
            when (abaAtual) {
                R.id.nav_dashboard -> R.string.aba_dashboard
                R.id.nav_avaliar -> R.string.aba_avaliar
                else -> R.string.aba_tarefas
            }
        )
        voltarParaTarefas.isEnabled = abaAtual != R.id.nav_tarefas
    }

    private fun novoFragment(id: Int): Fragment = when (id) {
        R.id.nav_dashboard -> DashboardFragment()
        R.id.nav_avaliar -> PesquisaFragment()
        else -> TarefasFragment()
    }

    private fun tagDa(id: Int) = when (id) {
        R.id.nav_dashboard -> TAG_DASHBOARD
        R.id.nav_avaliar -> TAG_AVALIAR
        else -> TAG_TAREFAS
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ABA_ATUAL, abaAtual)
    }

    private companion object {
        const val ABA_ATUAL = "aba_atual"
        const val TAG_TAREFAS = "aba_tarefas"
        const val TAG_DASHBOARD = "aba_dashboard"
        const val TAG_AVALIAR = "aba_avaliar"
        val TAGS = setOf(TAG_TAREFAS, TAG_DASHBOARD, TAG_AVALIAR)
    }
}
