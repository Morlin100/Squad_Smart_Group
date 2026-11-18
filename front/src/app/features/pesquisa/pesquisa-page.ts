import { Component, DestroyRef, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DomSanitizer } from '@angular/platform-browser';

import { environment } from '../../../environments/environment';

/**
 * Pesquisa de satisfação da squad parceira (Task Up), embutida no nosso app.
 * É o equivalente web da WebView do Android (KAN-102).
 */
@Component({
  selector: 'app-pesquisa-page',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <header class="cabecalho">
      <h1>Avaliar o app</h1>
      <a mat-button [href]="url" target="_blank" rel="noopener">
        <mat-icon>open_in_new</mat-icon>
        Abrir em nova aba
      </a>
    </header>

    <mat-card appearance="outlined">
      @if (!online()) {
        <p class="aviso">Você está sem internet. A pesquisa aparece assim que a conexão voltar.</p>
      } @else {
        @if (carregando()) {
          <mat-progress-bar mode="indeterminate" />
        }
        <iframe
          [src]="urlSegura"
          title="Pesquisa de satisfação"
          (load)="carregando.set(false)"
        ></iframe>
      }
    </mat-card>
  `,
  styles: `
    :host {
      display: block;
      max-width: 820px;
      margin: 0 auto;
    }
    .cabecalho {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      margin-bottom: 24px;
    }
    h1 {
      margin: 0;
      font: 700 30px/1.2 var(--mat-sys-headline-small-font);
      letter-spacing: -0.02em;
    }
    iframe {
      display: block;
      width: 100%;
      height: min(760px, calc(100vh - 220px));
      min-height: 480px;
      border: 0;
    }
    .aviso {
      margin: 0;
      padding: 32px 24px;
      text-align: center;
      color: var(--mat-sys-error);
      font-weight: 600;
    }
  `,
})
export class PesquisaPage {
  protected readonly url = environment.pesquisaUrl;
  // URL fixa, definida por nós no environment: seguro liberar para o iframe.
  protected readonly urlSegura = inject(DomSanitizer).bypassSecurityTrustResourceUrl(this.url);
  protected readonly carregando = signal(true);
  protected readonly online = signal(navigator.onLine);

  constructor() {
    const atualizar = () => this.online.set(navigator.onLine);
    window.addEventListener('online', atualizar);
    window.addEventListener('offline', atualizar);
    inject(DestroyRef).onDestroy(() => {
      window.removeEventListener('online', atualizar);
      window.removeEventListener('offline', atualizar);
    });
  }
}
