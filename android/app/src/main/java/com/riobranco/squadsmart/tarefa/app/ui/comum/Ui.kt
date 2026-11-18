package com.riobranco.squadsmart.tarefa.app.ui.comum

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.riobranco.squadsmart.tarefa.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// ---------- Datas ----------

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss")

/** "2026-11-30" -> LocalDate (ou null se vier vazio/inválido). */
fun String?.paraLocalDate(): LocalDate? =
    if (this.isNullOrBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()

/** "2026-11-30" -> "30/11/2026" */
fun String?.dataFormatada(): String? = paraLocalDate()?.format(FORMATO_DATA)

fun LocalDate.formatada(): String = format(FORMATO_DATA)

/** Converte data/hora da API (com ou sem fuso) para o horário local do celular. */
private fun String?.paraDataHoraLocal(): LocalDateTime? {
    if (this.isNullOrBlank()) return null
    return runCatching {
        OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }.recoverCatching { LocalDateTime.parse(this) }.getOrNull()
}

fun String?.dataHoraFormatada(): String = paraDataHoraLocal()?.format(FORMATO_DATA_HORA) ?: (this ?: "")

fun String?.horaFormatada(): String = paraDataHoraLocal()?.format(FORMATO_HORA) ?: (this ?: "")

/** O MaterialDatePicker trabalha com milissegundos em UTC. */
fun LocalDate.paraMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.millisUtcParaLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

// ---------- Feedback ----------

/** Snackbar com as cores da web (fundo escuro, ação dourada). */
fun View.snackbar(mensagem: String, ancora: View? = null, longa: Boolean = false) {
    val snack = Snackbar.make(this, mensagem, if (longa) Snackbar.LENGTH_LONG else 3000)
    snack.setBackgroundTint(ContextCompat.getColor(context, R.color.texto_forte))
    snack.setTextColor(ContextCompat.getColor(context, R.color.branco))
    snack.setActionTextColor(ContextCompat.getColor(context, R.color.ouro_300))
    snack.setTextMaxLines(4)
    if (ancora != null && ancora.isShown) snack.anchorView = ancora
    snack.show()
}

// ---------- Campos ----------

/** Só altera o texto se for diferente (evita loop com o TextWatcher e não move o cursor). */
fun EditText.definirTextoSeDiferente(valor: String) {
    if (text?.toString() != valor) {
        setText(valor)
        setSelection(valor.length.coerceAtMost(text?.length ?: 0))
    }
}

/**
 * Lista suspensa (dropdown) que sempre mostra todas as opções.
 * O ArrayAdapter padrão filtra pelo texto selecionado e, depois de girar a tela,
 * mostraria só a opção atual.
 */
class OpcoesAdapter(context: Context, private val opcoes: List<String>) :
    ArrayAdapter<String>(context, R.layout.item_opcao, opcoes) {

    private val semFiltro = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) =
            FilterResults().apply { values = opcoes; count = opcoes.size }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) =
            notifyDataSetChanged()
    }

    override fun getFilter(): Filter = semFiltro
}

/** Configura o dropdown com as opções e chama [aoSelecionar] com o índice escolhido. */
fun MaterialAutoCompleteTextView.configurarOpcoes(opcoes: List<String>, aoSelecionar: (Int) -> Unit) {
    setAdapter(OpcoesAdapter(context, opcoes))
    setOnItemClickListener { _, _, posicao, _ -> aoSelecionar(posicao) }
}

fun MaterialAutoCompleteTextView.mostrarOpcao(rotulo: String) {
    if (text?.toString() != rotulo) setText(rotulo, false)
}

// ---------- Bordas do sistema (edge-to-edge) ----------

/** Aplica as margens da barra de status e das laterais na raiz, e a da barra superior na AppBar. */
fun aplicarInsetsTopo(raiz: View, appBar: View) {
    ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
        val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        v.updatePadding(left = barras.left, right = barras.right)
        appBar.updatePadding(top = barras.top)
        insets
    }
}

/** Empurra o conteúdo para cima da barra de navegação e do teclado. */
fun aplicarInsetsBase(view: View) {
    val paddingOriginal = view.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        val base = maxOf(
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
            insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        )
        v.updatePadding(bottom = paddingOriginal + base)
        insets
    }
}
