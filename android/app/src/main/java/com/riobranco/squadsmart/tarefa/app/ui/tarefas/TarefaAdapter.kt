package com.riobranco.squadsmart.tarefa.app.ui.tarefas

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.riobranco.squadsmart.tarefa.app.R
import com.riobranco.squadsmart.tarefa.app.data.PrioridadeTarefa
import com.riobranco.squadsmart.tarefa.app.data.StatusTarefa
import com.riobranco.squadsmart.tarefa.app.data.Tarefa
import com.riobranco.squadsmart.tarefa.app.ui.comum.formatada
import com.riobranco.squadsmart.tarefa.app.ui.comum.paraLocalDate
import java.time.LocalDate

/** Cartões da lista, iguais aos da versão celular da web (KAN-97). */
class TarefaAdapter(
    private val aoAbrir: (Tarefa) -> Unit,
    private val aoExcluir: (Tarefa) -> Unit
) : ListAdapter<Tarefa, TarefaAdapter.TarefaViewHolder>(DIFF) {

    var excluindoId: Long? = null
        set(valor) {
            if (field == valor) return
            field = valor
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder =
        TarefaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tarefa, parent, false))

    override fun onBindViewHolder(holder: TarefaViewHolder, position: Int) = holder.bind(getItem(position))

    inner class TarefaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titulo: TextView = view.findViewById(R.id.textoTitulo)
        private val responsavel: TextView = view.findViewById(R.id.textoResponsavel)
        private val status: TextView = view.findViewById(R.id.textoStatus)
        private val prioridade: TextView = view.findViewById(R.id.textoPrioridade)
        private val vencimento: TextView = view.findViewById(R.id.textoVencimento)
        private val botaoExcluir: ImageButton = view.findViewById(R.id.botaoExcluir)

        fun bind(tarefa: Tarefa) {
            val ctx = itemView.context
            fun cor(id: Int) = ContextCompat.getColor(ctx, id)

            titulo.text = tarefa.titulo.orEmpty()
            responsavel.text = tarefa.responsavel?.takeIf { it.isNotBlank() } ?: ctx.getString(R.string.sem_responsavel)

            // Etiqueta de status com as mesmas cores da web
            status.text = StatusTarefa.rotuloDe(tarefa.status)
            val (fundo, corTexto) = when (tarefa.status) {
                StatusTarefa.EM_ANDAMENTO.name -> R.drawable.bg_etiqueta_andamento to R.color.ouro_700
                StatusTarefa.CONCLUIDA.name -> R.drawable.bg_etiqueta_concluida to R.color.branco
                else -> R.drawable.bg_etiqueta_pendente to R.color.pendente_texto
            }
            status.setBackgroundResource(fundo)
            status.setTextColor(cor(corTexto))
            TextViewCompat.setCompoundDrawableTintList(status, ColorStateList.valueOf(cor(corTexto)))

            // Prioridade com ponto colorido (Alta em negrito)
            prioridade.text = PrioridadeTarefa.rotuloDe(tarefa.prioridade)
            val corPonto = when (tarefa.prioridade) {
                PrioridadeTarefa.ALTA.name -> R.color.prioridade_alta
                PrioridadeTarefa.MEDIA.name -> R.color.prioridade_media
                else -> R.color.prioridade_baixa
            }
            TextViewCompat.setCompoundDrawableTintList(prioridade, ColorStateList.valueOf(cor(corPonto)))
            prioridade.setTypeface(null, if (tarefa.prioridade == PrioridadeTarefa.ALTA.name) Typeface.BOLD else Typeface.NORMAL)

            // Vencimento em vermelho se já passou e a tarefa não foi concluída
            val data = tarefa.dataVencimento.paraLocalDate()
            if (data == null) {
                vencimento.visibility = View.GONE
            } else {
                val vencida = tarefa.status != StatusTarefa.CONCLUIDA.name && data.isBefore(LocalDate.now())
                vencimento.visibility = View.VISIBLE
                vencimento.text = ctx.getString(if (vencida) R.string.vencida_em else R.string.vencimento_em, data.formatada())
                val corVenc = cor(if (vencida) R.color.erro else R.color.texto_suave)
                vencimento.setTextColor(corVenc)
                vencimento.setTypeface(null, if (vencida) Typeface.BOLD else Typeface.NORMAL)
                TextViewCompat.setCompoundDrawableTintList(
                    vencimento, ColorStateList.valueOf(if (vencida) corVenc else cor(R.color.ouro_500))
                )
            }

            val saindo = excluindoId == tarefa.id
            itemView.alpha = if (saindo) 0.4f else 1f
            botaoExcluir.isEnabled = !saindo
            botaoExcluir.contentDescription = ctx.getString(R.string.excluir) + " " + tarefa.titulo.orEmpty()

            itemView.setOnClickListener { aoAbrir(tarefa) }
            botaoExcluir.setOnClickListener { aoExcluir(tarefa) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Tarefa>() {
            override fun areItemsTheSame(a: Tarefa, b: Tarefa) = a.id == b.id
            override fun areContentsTheSame(a: Tarefa, b: Tarefa) = a == b
        }
    }
}

/** Rodapé com o indicador de "carregando mais" no fim da lista. */
class RodapeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var visivel = false
        set(valor) {
            if (field == valor) return
            field = valor
            if (valor) notifyItemInserted(0) else notifyItemRemoved(0)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_rodape, parent, false)
    ) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    override fun getItemCount() = if (visivel) 1 else 0
}
