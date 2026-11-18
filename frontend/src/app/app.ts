import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatIconModule],
  template: `
    <mat-toolbar class="toolbar">
      <div class="toolbar-interno">
        <a routerLink="/tarefas" class="marca">
          <span class="selo"><mat-icon>task_alt</mat-icon></span>
          <span>Gerenciador de Tarefas</span>
        </a>
      </div>
    </mat-toolbar>
    <main class="conteudo">
      <router-outlet />
    </main>
  `,
  styles: `
    .toolbar {
      position: sticky;
      top: 0;
      z-index: 10;
      height: 72px;
      background: rgb(255 255 255 / 88%);
      backdrop-filter: blur(10px);
      border-bottom: 1px solid var(--borda-suave);
      box-shadow: 0 4px 20px -12px rgb(90 67 23 / 25%);
    }
    .toolbar::after {
      content: '';
      position: absolute;
      inset: auto 0 -1px;
      height: 2px;
      background: var(--gradiente-ouro);
      opacity: 0.7;
    }
    .toolbar-interno {
      width: 100%;
      max-width: 1100px;
      margin: 0 auto;
      padding: 0 16px;
    }
    .marca {
      display: inline-flex;
      align-items: center;
      gap: 12px;
      color: var(--texto-forte);
      text-decoration: none;
      font-weight: 700;
      font-size: 18px;
      letter-spacing: -0.01em;
    }
    .selo {
      display: grid;
      place-items: center;
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: var(--gradiente-ouro);
      color: #fff;
      box-shadow: 0 6px 14px -6px rgb(154 116 38 / 60%);
    }
    .conteudo {
      max-width: 1100px;
      margin: 0 auto;
      padding: 32px 16px 48px;
    }
  `,
})
export class App {}
