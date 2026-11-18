package com.riobranco.squadsmart.tarefa.app.data

object ApiConfig {
    /**
     * Endereço da API publicada no Azure (o mesmo usado pelo front web, mesmo banco).
     * Precisa terminar com "/" por causa do Retrofit.
     */
    const val BASE_URL = "https://app-gerenciador-gc6909.azurewebsites.net/api/v1/"

    /**
     * Pesquisa de satisfação da squad parceira (Task Up), aberta na WebView (KAN-102).
     * Mesma URL usada no front web (environment.pesquisaUrl).
     */
    const val PESQUISA_URL =
        "https://pesquisa-satisfacao-pf-h9dzbzdxf3etc5dv.brazilsouth-01.azurewebsites.net/pesquisa.html"

    /** Itens por página na lista de tarefas. */
    const val TAMANHO_PAGINA = 20
}
