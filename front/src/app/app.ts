import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatIconModule],
  template: `
    <mat-toolbar class="toolbar">
      <div class="toolbar-interno">
        <a routerLink="/tarefas" class="marca">
          <span class="selo"><mat-icon>task_alt</mat-icon></span>
          <span class="nome">Gerenciador de Tarefas</span>
        </a>
        <nav class="menu" aria-label="Menu principal">
          <a routerLink="/tarefas" routerLinkActive="ativo"><mat-icon>list</mat-icon><span>Tarefas</span></a>
          <a routerLink="/dashboard" routerLinkActive="ativo"><mat-icon>insights</mat-icon><span>Dashboard</span></a>
          <a routerLink="/avaliar" routerLinkActive="ativo"><mat-icon>rate_review</mat-icon><span>Avaliar</span></a>
        </nav>
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
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      width: 100%;
      max-width: 1100px;
      margin: 0 auto;
      padding: 0 16px;
    }
    .menu {
      display: flex;
      gap: 4px;
    }
    .menu a {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      min-height: 44px;
      padding: 0 12px;
      border-radius: 12px;
      color: var(--texto-suave);
      font-size: 14px;
      font-weight: 600;
      text-decoration: none;
    }
    .menu a:hover,
    .menu a.ativo {
      background: var(--ouro-50);
      color: var(--ouro-700);
    }
    .menu mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }
    @media (max-width: 720px) {
      .marca .nome,
      .menu span {
        display: none;
      }
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
