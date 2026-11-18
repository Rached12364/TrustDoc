import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { LogoComponent } from './logo.component';

interface DemoAccount { user: string; pass: string; label: string; scope: string; }

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, LogoComponent],
  template: `
  <div class="login">
    <section class="login-intro">
      <app-logo [light]="true" />
      <h1>La base de connaissances<br>qui cite ses sources.</h1>
      <ul class="pillars">
        <li><span class="pi">1</span><div><b>Réponses sourcées</b><p>Chaque réponse indique le document et la page. Sans source, pas de réponse.</p></div></li>
        <li><span class="pi">2</span><div><b>Documents intègres</b><p>Empreinte SHA-256 à l'import : toute modification est détectée.</p></div></li>
        <li><span class="pi">3</span><div><b>Accès par rôle</b><p>L'IA ne lit que les documents que vous avez le droit de voir.</p></div></li>
        <li><span class="pi">4</span><div><b>IA souveraine</b><p>Modèle local avec Ollama : les documents ne quittent pas le serveur.</p></div></li>
      </ul>
    </section>

    <section class="login-form">
      <form (ngSubmit)="submit()" class="card">
        <h2>Connexion</h2>
        <label>Identifiant
          <input name="u" [(ngModel)]="username" autocomplete="username" required>
        </label>
        <label>Mot de passe
          <input name="p" type="password" [(ngModel)]="password" autocomplete="current-password" required>
        </label>
        @if (error()) { <p class="form-error">{{ error() }}</p> }
        <button class="btn primary wide" [disabled]="loading()">{{ loading() ? 'Connexion…' : 'Se connecter' }}</button>

        <div class="demo">
          <p class="eyebrow">Comptes de démonstration</p>
          @for (a of accounts; track a.user) {
            <button type="button" class="demo-acc" (click)="fill(a)">
              <span><b>{{ a.label }}</b><small>{{ a.user }} / {{ a.pass }}</small></span>
              <em>{{ a.scope }}</em>
            </button>
          }
        </div>
      </form>
    </section>
  </div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  loading = signal(false);
  error = signal('');

  accounts: DemoAccount[] = [
    { user: 'employe', pass: 'employe123', label: 'Collaborateur', scope: 'Public' },
    { user: 'rh', pass: 'rh123', label: 'Ressources humaines', scope: 'Public + RH' },
    { user: 'admin', pass: 'admin123', label: 'Administrateur', scope: 'Tout + audit' }
  ];

  constructor(private auth: AuthService, private router: Router) {
    if (auth.isLoggedIn()) router.navigate(['/chat']);
  }

  fill(a: DemoAccount): void {
    this.username = a.user;
    this.password = a.pass;
    this.submit();
  }

  submit(): void {
    if (!this.username || !this.password) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.username, this.password).subscribe({
      next: () => this.router.navigate(['/chat']),
      error: () => {
        this.error.set('Identifiant ou mot de passe incorrect.');
        this.loading.set(false);
      }
    });
  }
}
