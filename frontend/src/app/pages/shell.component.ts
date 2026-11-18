import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ApiService } from '../core/api.service';
import { Status } from '../core/models';
import { MODE_LABEL, ROLE_LABEL } from '../core/labels';
import { LogoComponent } from './logo.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LogoComponent],
  template: `
  <header class="topbar">
    <app-logo />
    <nav>
      <a routerLink="/chat" routerLinkActive="active">Assistant</a>
      <a routerLink="/documents" routerLinkActive="active">Documents</a>
      @if (isAdmin()) { <a routerLink="/audit" routerLinkActive="active">Journal d'audit</a> }
    </nav>
    <div class="who">
      @if (status(); as s) {
        <span class="mode" [class.local]="s.llmMode === 'ollama'" [title]="s.llmMode === 'ollama_off' ? 'Lancez Ollama puis : ollama pull ' + s.model : s.model">{{ modeLabel(s.llmMode) }}</span>
      }
      <span class="user"><b>{{ auth.session()?.displayName }}</b><small>{{ roleLabel() }}</small></span>
      <button class="btn ghost" (click)="auth.logout()">Déconnexion</button>
    </div>
  </header>
  <main class="page"><router-outlet /></main>
  `
})
export class ShellComponent implements OnInit, OnDestroy {
  private timer?: ReturnType<typeof setInterval>;
  status = signal<Status | null>(null);
  isAdmin = computed(() => this.auth.role() === 'ADMIN');
  roleLabel = computed(() => {
    const r = this.auth.role();
    return r ? ROLE_LABEL[r] : '';
  });

  constructor(public auth: AuthService, private api: ApiService) {}

  ngOnInit(): void {
    this.refresh();
    // l'état d'Ollama est revérifié toutes les 20 secondes (il peut être lancé après TrustDoc)
    this.timer = setInterval(() => this.refresh(), 20000);
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }

  private refresh(): void {
    this.api.status().subscribe({ next: s => this.status.set(s), error: () => {} });
  }

  modeLabel(m: string): string {
    return MODE_LABEL[m] ?? m;
  }
}
