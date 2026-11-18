// Pesquisa de satisfação do Gerenciador de Tarefas.
// JavaScript puro, sem bibliotecas, para rodar em qualquer WebView. URLs relativas ao próprio servidor.
(function () {
  'use strict';

  var API = '/api/v1/avaliacoes';
  var FUNCIONALIDADES = [
    ['CADASTRO', 'Cadastrar tarefas'],
    ['EDICAO', 'Editar tarefas'],
    ['BUSCA_FILTROS', 'Busca e filtros'],
    ['DASHBOARD', 'Dashboard'],
    ['HISTORICO', 'Histórico'],
    ['APP_CELULAR', 'Usar no celular']
  ];
  var NOMES_FUNCIONALIDADE = {};
  FUNCIONALIDADES.forEach(function (f) { NOMES_FUNCIONALIDADE[f[0]] = f[1]; });

  var respostas = { notaGeral: null, facilidadeUso: null, recomendaria: null, funcionalidadeFavorita: null };

  var el = function (id) { return document.getElementById(id); };
  var todos = function (seletor) { return Array.prototype.slice.call(document.querySelectorAll(seletor)); };

  // ---------- comunicação com a API ----------

  function chamar(metodo, url, corpo) {
    if (!navigator.onLine) {
      return Promise.reject(new Error('Você está sem internet. Verifique a conexão e tente de novo.'));
    }
    var opcoes = { method: metodo, headers: { 'Accept': 'application/json' } };
    if (corpo) {
      opcoes.headers['Content-Type'] = 'application/json';
      opcoes.body = JSON.stringify(corpo);
    }
    return fetch(url, opcoes)
      .catch(function () {
        throw new Error('Não foi possível falar com o servidor. Verifique sua internet e tente de novo.');
      })
      .then(function (resposta) {
        return resposta.text().then(function (texto) {
          var dados = null;
          try { dados = texto ? JSON.parse(texto) : null; } catch (e) { /* resposta que não é JSON */ }
          if (!resposta.ok) {
            var erro = new Error((dados && dados.detail) || 'Algo deu errado. Tente de novo em instantes.');
            erro.campos = (dados && dados.erros) || [];
            throw erro;
          }
          return dados;
        });
      });
  }

  // ---------- montagem das perguntas ----------

  function grupoDeEscolha(container, campo, itens, criarBotao) {
    itens.forEach(function (item) {
      var botao = criarBotao(item);
      botao.type = 'button';
      botao.setAttribute('role', 'radio');
      botao.setAttribute('aria-checked', 'false');
      botao.addEventListener('click', function () { escolher(campo, item.valor, container); });
      container.appendChild(botao);
    });
  }

  function escolher(campo, valor, container) {
    respostas[campo] = valor;
    Array.prototype.forEach.call(container.children, function (b) {
      var marcado = String(b.dataset.valor) === String(valor);
      b.setAttribute('aria-checked', String(marcado));
      if (b.classList.contains('estrela')) {
        b.classList.toggle('acesa', Number(b.dataset.valor) <= Number(valor));
      }
    });
    esconderErro(campo);
  }

  function montarPerguntas() {
    var estrelas = document.querySelector('[data-campo="notaGeral"]');
    grupoDeEscolha(estrelas, 'notaGeral', [1, 2, 3, 4, 5].map(function (n) { return { valor: n }; }), function (item) {
      var b = document.createElement('button');
      b.className = 'estrela';
      b.dataset.valor = item.valor;
      b.textContent = '★';
      b.setAttribute('aria-label', item.valor + (item.valor === 1 ? ' estrela' : ' estrelas'));
      return b;
    });

    var escala = document.querySelector('[data-campo="facilidadeUso"]');
    grupoDeEscolha(escala, 'facilidadeUso', [1, 2, 3, 4, 5].map(function (n) { return { valor: n }; }), function (item) {
      var b = document.createElement('button');
      b.dataset.valor = item.valor;
      b.textContent = item.valor;
      return b;
    });

    var recomenda = document.querySelector('[data-campo="recomendaria"]');
    Array.prototype.forEach.call(recomenda.children, function (b) {
      b.addEventListener('click', function () { escolher('recomendaria', b.dataset.valor === 'true', recomenda); });
    });

    var lista = document.querySelector('[data-campo="funcionalidadeFavorita"]');
    grupoDeEscolha(lista, 'funcionalidadeFavorita', FUNCIONALIDADES.map(function (f) {
      return { valor: f[0], rotulo: f[1] };
    }), function (item) {
      var b = document.createElement('button');
      b.className = 'opcao';
      b.dataset.valor = item.valor;
      b.textContent = item.rotulo;
      return b;
    });

    el('comentario').addEventListener('input', function () {
      el('contador').textContent = el('comentario').value.length;
    });
  }

  // ---------- validação e envio ----------

  function mostrarErro(campo, texto) {
    var p = document.querySelector('[data-erro="' + campo + '"]');
    if (p) { p.textContent = texto; p.hidden = false; }
  }

  function esconderErro(campo) {
    var p = document.querySelector('[data-erro="' + campo + '"]');
    if (p) { p.hidden = true; }
  }

  function validar() {
    var ok = true;
    if (respostas.notaGeral === null) { mostrarErro('notaGeral', 'Escolha uma nota de 1 a 5.'); ok = false; }
    if (respostas.facilidadeUso === null) { mostrarErro('facilidadeUso', 'Escolha de 1 a 5.'); ok = false; }
    if (respostas.recomendaria === null) { mostrarErro('recomendaria', 'Escolha Sim ou Não.'); ok = false; }
    if (!ok) {
      var primeiro = document.querySelector('.erro-campo:not([hidden])');
      if (primeiro) { primeiro.parentElement.scrollIntoView({ behavior: 'smooth', block: 'center' }); }
    }
    return ok;
  }

  function enviar(evento) {
    evento.preventDefault();
    el('mensagem').hidden = true;
    if (!validar()) { return; }

    var botao = el('botao-enviar');
    botao.disabled = true;
    botao.textContent = 'Enviando...';

    chamar('POST', API, {
      notaGeral: respostas.notaGeral,
      facilidadeUso: respostas.facilidadeUso,
      recomendaria: respostas.recomendaria,
      funcionalidadeFavorita: respostas.funcionalidadeFavorita,
      comentario: el('comentario').value.trim() || null
    })
      .then(function () { mostrarTela('tela-obrigado'); })
      .catch(function (e) {
        // Mantém tudo que a pessoa preencheu para ela tentar de novo.
        (e.campos || []).forEach(function (c) { mostrarErro(c.campo, c.mensagem); });
        el('mensagem').textContent = e.message;
        el('mensagem').hidden = false;
      })
      .then(function () {
        botao.disabled = false;
        botao.textContent = 'Enviar avaliação';
      });
  }

  function limparFormulario() {
    respostas = { notaGeral: null, facilidadeUso: null, recomendaria: null, funcionalidadeFavorita: null };
    todos('[role="radio"]').forEach(function (b) {
      b.setAttribute('aria-checked', 'false');
      b.classList.remove('acesa');
    });
    el('comentario').value = '';
    el('contador').textContent = '0';
    el('mensagem').hidden = true;
    todos('.erro-campo').forEach(function (p) { p.hidden = true; });
  }

  // ---------- resultado ----------

  function numero(valor) {
    return String(valor).replace('.', ',');
  }

  function barras(container, itens) {
    container.innerHTML = '';
    var maior = Math.max.apply(null, [1].concat(itens.map(function (i) { return i[1]; })));
    itens.forEach(function (item) {
      var linha = document.createElement('div');
      linha.className = 'barra';
      var nome = document.createElement('span');
      nome.className = 'nome';
      nome.textContent = item[0];
      var trilho = document.createElement('span');
      trilho.className = 'trilho';
      var preenchido = document.createElement('span');
      preenchido.className = 'preenchido';
      preenchido.style.width = Math.round(item[1] / maior * 100) + '%';
      trilho.appendChild(preenchido);
      var qtd = document.createElement('span');
      qtd.className = 'qtd';
      qtd.textContent = item[1];
      linha.appendChild(nome);
      linha.appendChild(trilho);
      linha.appendChild(qtd);
      container.appendChild(linha);
    });
  }

  function carregarResultado() {
    mostrarTela('tela-resultado');
    el('mensagem-resultado').hidden = true;
    el('total').textContent = 'Carregando...';

    chamar('GET', API + '/resultado')
      .then(function (r) {
        el('total').textContent = r.totalAvaliacoes === 1
          ? '1 pessoa respondeu'
          : r.totalAvaliacoes + ' pessoas responderam';
        el('media-nota').textContent = numero(r.mediaNotaGeral);
        var cheias = Math.round(r.mediaNotaGeral);
        el('estrelas-media').textContent = '★★★★★'.slice(0, cheias) + '☆☆☆☆☆'.slice(0, 5 - cheias);
        el('media-facilidade').textContent = numero(r.mediaFacilidade) + ' / 5';
        el('recomendam').textContent = numero(r.percentualRecomendariam) + '%';

        barras(el('distribuicao'), [5, 4, 3, 2, 1].map(function (n) {
          return [n + (n === 1 ? ' estrela' : ' estrelas'), r.distribuicaoNotas[n] || 0];
        }));

        var funcs = Object.keys(r.funcionalidadesFavoritas).map(function (k) {
          return [NOMES_FUNCIONALIDADE[k] || k, r.funcionalidadesFavoritas[k]];
        });
        el('bloco-funcionalidades').hidden = funcs.length === 0;
        barras(el('funcionalidades'), funcs);

        var lista = el('comentarios');
        lista.innerHTML = '';
        el('bloco-comentarios').hidden = r.ultimosComentarios.length === 0;
        r.ultimosComentarios.forEach(function (c) {
          var item = document.createElement('li');
          var meta = document.createElement('span');
          meta.className = 'meta';
          meta.textContent = '★'.repeat(c.notaGeral) + ' · ' + new Date(c.criadoEm).toLocaleDateString('pt-BR');
          var texto = document.createElement('p');
          texto.textContent = c.texto;
          item.appendChild(meta);
          item.appendChild(texto);
          lista.appendChild(item);
        });
      })
      .catch(function (e) {
        el('total').textContent = '';
        el('mensagem-resultado').textContent = e.message;
        el('mensagem-resultado').hidden = false;
      });
  }

  // ---------- navegação entre telas ----------

  function mostrarTela(id) {
    ['tela-formulario', 'tela-obrigado', 'tela-resultado'].forEach(function (t) {
      el(t).hidden = t !== id;
    });
    window.scrollTo(0, 0);
  }

  document.addEventListener('click', function (evento) {
    var alvo = evento.target.closest('[data-acao]');
    if (!alvo) { return; }
    var acao = alvo.dataset.acao;
    if (acao === 'ver-resultado') { carregarResultado(); }
    if (acao === 'voltar') { mostrarTela('tela-formulario'); }
    if (acao === 'nova-resposta') { limparFormulario(); mostrarTela('tela-formulario'); }
  });

  function atualizarOffline() { el('aviso-offline').hidden = navigator.onLine; }
  window.addEventListener('online', atualizarOffline);
  window.addEventListener('offline', atualizarOffline);

  // ---------- início ----------

  montarPerguntas();
  atualizarOffline();
  el('tela-formulario').addEventListener('submit', enviar);
  if (location.hash === '#resultado') { carregarResultado(); }
})();
