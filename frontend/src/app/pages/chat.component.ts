import { Component, ElementRef, ViewChild, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { ChatAnswer, ChatTurn, Source } from '../core/models';
import { MODE_LABEL } from '../core/labels';

interface Segment { text: string; cite?: number; }

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [FormsModule],
  template: `
  <div class="chat">
    <section class="conv">
      <div class="conv-scroll" #scroller>
        @if (turns().length === 0) {
          <div class="empty">
            <p class="eyebrow">Assistant documentaire</p>
            <h1>Posez une question sur les documents de l'entreprise.</h1>
            <p class="muted">Les réponses viennent uniquement des documents auxquels votre rôle a accès, avec la page exacte.</p>
            <div class="suggest">
              @for (q of suggestions(); track q) {
                <button class="chip" (click)="ask(q)">{{ q }}</button>
              }
            </div>
          </div>
        }
        @for (t of turns(); track $index; let i = $index) {
          <div class="turn">
            <div class="q">{{ t.question }}</div>
            @if (t.pending) {
              <div class="a pending"><span class="dots"><i></i><i></i><i></i></span> Recherche dans les documents…</div>
            } @else if (t.error) {
              <div class="a error">{{ t.error }}</div>
            }
            @if (!t.pending && t.answer; as a) {
              <div class="a" [class.refused]="!a.grounded" (click)="select(i)">
                @if (!a.grounded) { <span class="badge warn">Aucune source trouvée</span> }
                <p>
                  @for (s of segments(a); track $index) {
                    @if (s.cite) {
                      <button class="cite" [class.on]="selected() === i && focusN() === s.cite"
                              (click)="focus(i, s.cite); $event.stopPropagation()">{{ s.cite }}</button>
                    } @else { {{ s.text }} }
                  }
                </p>
                <div class="meta">
                  <span>{{ a.sources.length }} source(s)</span>
                  <span>{{ modeLabel(a.mode) }}</span>
                  <span>{{ a.latencyMs }} ms</span>
                </div>
              </div>
            }
          </div>
        }
      </div>
      <form class="composer" (ngSubmit)="ask(draft)">
        <input name="q" [(ngModel)]="draft" placeholder="Ex. : combien de jours de télétravail par semaine ?" autocomplete="off" [disabled]="busy()">
        <button class="btn primary" [disabled]="busy() || !draft.trim()">Envoyer</button>
      </form>
    </section>

    <aside class="sources">
      <p class="eyebrow">Sources de la réponse</p>
      @if (current(); as a) {
        @if (a.sources.length === 0) {
          <p class="muted">Aucun passage assez proche de la question n'a été trouvé dans vos documents. L'assistant préfère ne pas répondre plutôt qu'inventer.</p>
        }
        @for (s of a.sources; track s.n) {
          <article class="src" [class.on]="focusN() === s.n" (click)="focusN.set(s.n)">
            <header>
              <span class="cite static">{{ s.n }}</span>
              <b>{{ s.documentName }}</b>
              <span class="page">p. {{ s.page }}</span>
            </header>
            <blockquote>{{ s.excerpt }}</blockquote>
            <div class="score"><i [style.width.%]="scorePct(s, a)"></i></div>
          </article>
        }
      } @else {
        <p class="muted">Les passages utilisés pour répondre s'afficheront ici, avec le document et la page.</p>
      }
    </aside>
  </div>
  `
})
export class ChatComponent {
  @ViewChild('scroller') scroller?: ElementRef<HTMLDivElement>;
  draft = '';
  turns = signal<ChatTurn[]>([]);
  selected = signal<number>(-1);
  focusN = signal<number>(0);
  busy = computed(() => this.turns().some(t => t.pending));
  current = computed<ChatAnswer | null>(() => this.turns()[this.selected()]?.answer ?? null);

  suggestions = computed(() => {
    const base = [
      'Combien de jours de télétravail par semaine ?',
      'Quelle est la longueur minimale d\'un mot de passe ?',
      'Que faire si je reçois un e-mail suspect ?'
    ];
    const r = this.auth.role();
    if (r === 'RH' || r === 'ADMIN') base.push('Quel est le délai pour déposer une note de frais ?');
    if (r === 'ADMIN') base.push('En combien de temps les services critiques doivent-ils redémarrer ?');
    return base;
  });

  constructor(private api: ApiService, private auth: AuthService) {}

  ask(question: string): void {
    const q = question.trim();
    if (!q || this.busy()) return;
    this.draft = '';
    const index = this.turns().length;
    this.turns.update(t => [...t, { question: q, pending: true }]);
    this.scrollDown();
    this.api.ask(q).subscribe({
      next: answer => {
        this.patch(index, { answer, pending: false });
        this.selected.set(index);
        this.focusN.set(answer.sources[0]?.n ?? 0);
        this.scrollDown();
      },
      error: () => this.patch(index, { error: 'Le serveur n\'a pas pu répondre. Vérifiez que le backend est lancé.', pending: false })
    });
  }

  select(i: number): void {
    this.selected.set(i);
  }

  focus(i: number, n: number): void {
    this.selected.set(i);
    this.focusN.set(n);
  }

  segments(a: ChatAnswer): Segment[] {
    const valid = new Set(a.sources.map(s => s.n));
    return a.answer.split(/(\[\d+\])/g).filter(Boolean).map(part => {
      const m = /^\[(\d+)\]$/.exec(part);
      return m && valid.has(+m[1]) ? { text: part, cite: +m[1] } : { text: part };
    });
  }

  scorePct(s: Source, a: ChatAnswer): number {
    const max = Math.max(...a.sources.map(x => x.score), 0.0001);
    return Math.round((s.score / max) * 100);
  }

  modeLabel(m: string): string {
    return MODE_LABEL[m] ?? m;
  }

  private patch(i: number, p: Partial<ChatTurn>): void {
    this.turns.update(t => t.map((x, j) => (j === i ? { ...x, ...p } : x)));
  }

  private scrollDown(): void {
    setTimeout(() => {
      const el = this.scroller?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}
