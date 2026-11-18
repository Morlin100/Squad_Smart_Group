// Tela do Planning Poker (KAN-108). JavaScript puro, sem bibliotecas, para rodar em qualquer WebView.
// As URLs são relativas ao próprio servidor, então funciona igual local e no Azure.
(function () {
  'use strict';

  var API = '/api/v1/rodadas';
  var INTERVALO_ATUALIZACAO_MS = 3000;

  var params = new URLSearchParams(location.search);
  var rodadaId = params.get('rodada');
  var cartaEscolhida = null;
  var timer = null;

  var el = function (id) { return document.getElementById(id); };

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
          try { dados = texto ? JSON.parse(texto) : null; } catch (e) { /* resposta que não é JSON (ex.: 503 do Azure) */ }
          if (!resposta.ok) {
            var erro = new Error(mensagemDeErro(dados));
            erro.status = resposta.status;
            throw erro;
          }
          return dados;
        });
      });
  }

  function mensagemDeErro(problema) {
    if (problema && problema.erros && problema.erros.length) {
      return problema.erros[0].mensagem;
    }
    return (problema && problema.detail) || 'Algo deu errado. Tente de novo.';
  }

  // ---------- mensagens ----------

  function mostrarMensagem(texto, tipo) {
    var caixa = el('mensagem');
    caixa.textContent = texto;
    caixa.className = 'aviso' + (tipo === 'erro' ? ' aviso-erro' : tipo === 'alerta' ? ' aviso-alerta' : '');
    caixa.hidden = false;
  }

  function atualizarAvisoOffline() {
    el('aviso-offline').hidden = navigator.onLine;
  }

  window.addEventListener('online', function () {
    atualizarAvisoOffline();
    if (rodadaId) { carregarRodada(); } else { carregarLista(); }
  });
  window.addEventListener('offline', atualizarAvisoOffline);

  // ---------- tela inicial ----------

  function iniciarTelaInicio() {
    el('tela-inicio').hidden = false;
    el('form-rodada').addEventListener('submit', function (evento) {
      evento.preventDefault();
      var tarefa = el('campo-tarefa').value.trim();
      var erro = el('erro-tarefa');
      if (!tarefa) {
        erro.textContent = 'Informe o nome da tarefa.';
        erro.hidden = false;
        return;
      }
      erro.hidden = true;
      chamar('POST', API, { tarefa: tarefa })
        .then(function (rodada) { location.href = '/poker?rodada=' + rodada.id; })
        .catch(function (e) { erro.textContent = e.message; erro.hidden = false; });
    });
    carregarLista();
  }

  function carregarLista() {
    chamar('GET', API)
      .then(function (rodadas) {
        var lista = el('lista-rodadas');
        lista.innerHTML = '';
        el('sem-rodadas').hidden = rodadas.length > 0;
        rodadas.forEach(function (r) {
          var item = document.createElement('li');
          var link = document.createElement('a');
          link.href = '/poker?rodada=' + r.id;
          var nome = document.createElement('span');
          nome.textContent = r.tarefa;
          var situacao = document.createElement('span');
          situacao.className = 'etiqueta' + (r.situacao === 'ENCERRADA' ? ' encerrada' : '');
          situacao.textContent = r.situacao === 'ABERTA' ? 'Aberta' : 'Encerrada';
          link.appendChild(nome);
          link.appendChild(situacao);
          item.appendChild(link);
          lista.appendChild(item);
        });
      })
      .catch(function (e) {
        el('sem-rodadas').textContent = e.message;
        el('sem-rodadas').hidden = false;
      });
  }

  // ---------- tela da rodada ----------

  function chaveVoto() { return 'poker-votou-' + rodadaId; }

  function lerLocal(chave) {
    try { return localStorage.getItem(chave); } catch (e) { return null; }
  }

  function gravarLocal(chave, valor) {
    try { localStorage.setItem(chave, valor); } catch (e) { /* sem armazenamento local: segue sem lembrar */ }
  }

  function iniciarTelaRodada() {
    el('tela-rodada').hidden = false;
    el('campo-nome').value = lerLocal('poker-nome') || '';
    el('campo-nome').addEventListener('input', atualizarBotaoVotar);
    el('botao-votar').addEventListener('click', votar);
    el('botao-encerrar').addEventListener('click', encerrar);
    carregarRodada();
    timer = setInterval(function () {
      if (!document.hidden) { carregarRodada(); }
    }, INTERVALO_ATUALIZACAO_MS);
  }

  function montarCartas(cartas, bloqueadas) {
    var area = el('cartas');
    if (area.childElementCount === cartas.length) {
      Array.prototype.forEach.call(area.children, function (b) { b.disabled = bloqueadas; });
      return;
    }
    area.innerHTML = '';
    cartas.forEach(function (valor) {
      var botao = document.createElement('button');
      botao.type = 'button';
      botao.className = 'carta';
      botao.textContent = valor;
      botao.setAttribute('role', 'radio');
      botao.setAttribute('aria-checked', 'false');
      botao.disabled = bloqueadas;
      botao.addEventListener('click', function () {
        cartaEscolhida = valor;
        Array.prototype.forEach.call(area.children, function (b) {
          b.setAttribute('aria-checked', String(b === botao));
        });
        atualizarBotaoVotar();
      });
      area.appendChild(botao);
    });
  }

  function atualizarBotaoVotar() {
    var jaVotou = !!lerLocal(chaveVoto());
    el('botao-votar').disabled = jaVotou || cartaEscolhida === null || !el('campo-nome').value.trim();
  }

  function carregarRodada() {
    chamar('GET', API + '/' + rodadaId)
      .then(function (rodada) {
        var aberta = rodada.situacao === 'ABERTA';
        el('nome-tarefa').textContent = rodada.tarefa;
        document.title = rodada.tarefa + ' · Planning Poker';
        var situacao = el('situacao');
        situacao.textContent = aberta ? 'Votação aberta' : 'Encerrada';
        situacao.className = 'etiqueta' + (aberta ? '' : ' encerrada');

        var jaVotou = !!lerLocal(chaveVoto());
        el('area-voto').hidden = !aberta;
        montarCartas(rodada.cartas, jaVotou);
        if (jaVotou && aberta) {
          mostrarMensagem('Seu voto foi registrado. Aguarde o encerramento para ver os votos.');
        }
        atualizarBotaoVotar();

        el('contagem').textContent = rodada.totalVotos === 1
          ? '1 pessoa já votou'
          : rodada.totalVotos + ' pessoas já votaram';
        var lista = el('quem-votou');
        lista.innerHTML = '';
        rodada.participantesQueVotaram.forEach(function (nome) {
          var item = document.createElement('li');
          item.textContent = nome;
          lista.appendChild(item);
        });
        el('botao-encerrar').hidden = !aberta;

        if (!aberta) {
          clearInterval(timer);
          carregarResultado();
        }
      })
      .catch(function (e) {
        if (e.status === 404) {
          clearInterval(timer);
          el('area-voto').hidden = true;
        }
        mostrarMensagem(e.message, 'erro');
      });
  }

  function votar() {
    var nome = el('campo-nome').value.trim();
    if (!nome || cartaEscolhida === null) { return; }
    gravarLocal('poker-nome', nome);
    el('botao-votar').disabled = true;

    chamar('POST', API + '/' + rodadaId + '/votos', { participante: nome, carta: cartaEscolhida })
      .then(function (resposta) {
        gravarLocal(chaveVoto(), String(cartaEscolhida));
        mostrarMensagem(resposta.mensagem + ' ' + resposta.totalVotos + ' voto(s) até agora.');
        carregarRodada();
      })
      .catch(function (e) {
        if (e.status === 409) {
          // Voto repetido: lembra que já votou para não oferecer o botão de novo
          gravarLocal(chaveVoto(), 'sim');
          mostrarMensagem(e.message, 'alerta');
          carregarRodada();
        } else {
          mostrarMensagem(e.message, 'erro');
          atualizarBotaoVotar();
        }
      });
  }

  function encerrar() {
    if (!confirm('Encerrar a rodada e revelar os votos? Ninguém mais poderá votar.')) { return; }
    chamar('POST', API + '/' + rodadaId + '/encerrar')
      .then(function (resultado) {
        clearInterval(timer);
        mostrarResultado(resultado);
        carregarRodada();
      })
      .catch(function (e) { mostrarMensagem(e.message, 'erro'); });
  }

  function carregarResultado() {
    chamar('GET', API + '/' + rodadaId + '/resultado')
      .then(mostrarResultado)
      .catch(function (e) { mostrarMensagem(e.message, 'erro'); });
  }

  function mostrarResultado(resultado) {
    el('mensagem').hidden = true;
    el('resultado').hidden = false;
    el('media').textContent = String(resultado.media).replace('.', ',');
    el('mais-votada').textContent = resultado.cartaMaisVotada;

    var divergencia = el('divergencia');
    divergencia.hidden = !resultado.divergente;
    divergencia.textContent = resultado.mensagemDivergencia || '';

    var lista = el('votos');
    lista.innerHTML = '';
    resultado.votos.forEach(function (voto) {
      var item = document.createElement('li');
      var nome = document.createElement('span');
      nome.textContent = voto.participante;
      var carta = document.createElement('strong');
      carta.textContent = voto.carta;
      item.appendChild(nome);
      item.appendChild(carta);
      lista.appendChild(item);
    });
  }

  // ---------- início ----------

  atualizarAvisoOffline();
  if (rodadaId) {
    iniciarTelaRodada();
  } else {
    iniciarTelaInicio();
  }
})();
