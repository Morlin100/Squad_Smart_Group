package com.riobranco.squadsmart.tarefa.app.ui.comum

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.riobranco.squadsmart.tarefa.app.R

/**
 * Diálogo "Excluir tarefa" (igual ao ConfirmDialog da web). É um DialogFragment para
 * continuar aberto ao girar a tela. A resposta volta por setFragmentResult.
 */
class ConfirmacaoDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val chave = args.getString(ARG_CHAVE)!!
        val id = args.getLong(ARG_ID)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.excluir_tarefa)
            .setMessage(getString(R.string.confirmar_exclusao, args.getString(ARG_TITULO)))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.excluir) { _, _ ->
                parentFragmentManager.setFragmentResult(chave, bundleOf(RESULTADO_ID to id))
            }
            .create()
    }

    companion object {
        const val RESULTADO_ID = "id"
        private const val ARG_CHAVE = "chave"
        private const val ARG_ID = "id"
        private const val ARG_TITULO = "titulo"

        fun mostrar(fm: FragmentManager, chave: String, id: Long, titulo: String) {
            if (fm.findFragmentByTag(chave) != null) return
            ConfirmacaoDialog().apply {
                arguments = bundleOf(ARG_CHAVE to chave, ARG_ID to id, ARG_TITULO to titulo)
            }.show(fm, chave)
        }
    }
}
