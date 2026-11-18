package com.riobranco.squadsmart.tarefa.app.ui.pesquisa

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.riobranco.squadsmart.tarefa.app.R
import com.riobranco.squadsmart.tarefa.app.data.ApiConfig

/**
 * Pesquisa de satisfação da squad parceira (Task Up), aberta numa WebView (KAN-102).
 * É o equivalente da rota /avaliar do front web.
 */
class PesquisaFragment : Fragment(R.layout.fragment_pesquisa) {

    private var webView: WebView? = null
    private lateinit var progresso: LinearProgressIndicator
    private lateinit var estado: LinearLayout
    private lateinit var textoEstado: TextView
    private var falhou = false

    /** O botão voltar navega dentro da WebView antes de sair da tela. */
    private val voltarNaWebView = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            webView?.goBack()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val web = view.findViewById<WebView>(R.id.webView)
        webView = web
        progresso = view.findViewById(R.id.progresso)
        estado = view.findViewById(R.id.estado)
        textoEstado = view.findViewById(R.id.textoEstado)
        view.findViewById<MaterialButton>(R.id.botaoTentar).setOnClickListener { recarregar() }

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true

        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                falhou = false
                progresso.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progresso.visibility = View.GONE
                if (!falhou) estado.visibility = View.GONE
                atualizarVoltar()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    mostrarErro(getString(if (temInternet()) R.string.pesquisa_erro_carregar else R.string.pesquisa_sem_internet))
                }
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, resposta: WebResourceResponse) {
                if (request.isForMainFrame && resposta.statusCode >= 400) {
                    mostrarErro(getString(R.string.pesquisa_indisponivel, resposta.statusCode))
                }
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                atualizarVoltar()
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progresso.setProgressCompat(newProgress, true)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, voltarNaWebView)

        // Ao girar a tela, volta para a mesma página em vez de recarregar do início
        val restaurou = savedInstanceState?.getBundle(ESTADO_WEB)?.let { web.restoreState(it) } != null
        if (!restaurou) recarregar()
    }

    private fun recarregar() {
        val web = webView ?: return
        if (!temInternet()) {
            mostrarErro(getString(R.string.pesquisa_sem_internet))
            return
        }
        estado.visibility = View.GONE
        if (web.url.isNullOrEmpty()) web.loadUrl(ApiConfig.PESQUISA_URL) else web.reload()
    }

    private fun mostrarErro(mensagem: String) {
        falhou = true
        progresso.visibility = View.GONE
        textoEstado.text = mensagem
        estado.visibility = View.VISIBLE
    }

    private fun atualizarVoltar() {
        voltarNaWebView.isEnabled = !isHidden && webView?.canGoBack() == true
    }

    /** A MainActivity esconde/mostra as abas; só intercepta o voltar quando a pesquisa está visível. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        atualizarVoltar()
    }

    private fun temInternet(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capacidades = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.let { web -> outState.putBundle(ESTADO_WEB, Bundle().also { web.saveState(it) }) }
    }

    override fun onDestroyView() {
        webView?.destroy()
        webView = null
        super.onDestroyView()
    }

    private companion object {
        const val ESTADO_WEB = "estado_web"
    }
}
