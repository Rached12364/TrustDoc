import { Component, OnInit, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { AuditEntry } from '../core/models';
import { ROLE_LABEL } from '../core/labels';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [DatePipe],
  template: `
  <div class="docs">
    <div class="docs-head">
      <div>
        <p class="eyebrow">Traçabilité</p>
        <h1>Journal d'audit</h1>
        <p class="muted">Chaque connexion, question, import et vérification est enregistré : qui, quand, et avec quelles sources.</p>
      </div>
      <button class="btn" (click)="load()">Actualiser</button>
    </div>

    <div class="kpis">
      <div class="kpi card"><b>{{ questions() }}</b><span>questions posées</span></div>
      <div class="kpi card"><b>{{ groundedRate() }}</b><span>réponses sourcées</span></div>
      <div class="kpi card"><b>{{ refused() }}</b><span>refus « pas de source »</span></div>
      <div class="kpi card"><b>{{ alerts() }}</b><span>documents modifiés détectés</span></div>
    </div>

    <div class="table-wrap card">
      <table>
        <thead><tr><th>Date</th><th>Utilisateur</th><th>Action</th><th>Détail</th><th>Résultat</th></tr></thead>
        <tbody>
          @for (e of entries(); track e.id) {
            <tr>
              <td class="nowrap">{{ e.time | date:'dd/MM HH:mm:ss' }}</td>
              <td><b>{{ e.username }}</b><small>{{ roleLabel(e) }}</small></td>
              <td><span class="act" [attr.data-a]="e.action">{{ e.action }}</span></td>
              <td class="detail">{{ e.detail }}</td>
              <td>
                @if (e.action === 'QUESTION') {
                  <span class="integ" [attr.data-i]="e.grounded ? 'OK' : 'NON_VERIFIE'">{{ e.grounded ? e.sourceCount + ' source(s)' : 'sans source' }}</span>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">Aucun événement pour le moment.</td></tr>
          }
        </tbody>
      </table>
    </div>
  </div>
  `
})
export class AuditComponent implements OnInit {
  entries = signal<AuditEntry[]>([]);
  private qs = computed(() => this.entries().filter(e => e.action === 'QUESTION'));
  questions = computed(() => this.qs().length);
  refused = computed(() => this.qs().filter(e => !e.grounded).length);
  groundedRate = computed(() => {
    const n = this.qs().length;
    return n ? `${Math.round(((n - this.refused()) / n) * 100)} %` : '—';
  });
  alerts = computed(() => this.entries().filter(e => e.action === 'ALERTE_INTEGRITE').length);

  constructor(private api: ApiService) {}

  ngOnInit(): void { this.load(); }

  load(): void { this.api.audit().subscribe(e => this.entries.set(e)); }

  roleLabel(e: AuditEntry): string { return ROLE_LABEL[e.role] ?? e.role; }
}
