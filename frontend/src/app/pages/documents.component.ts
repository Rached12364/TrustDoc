import { Component, OnInit, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { AccessLevel, DocumentInfo, VerifyResult } from '../core/models';
import { INTEGRITY_LABEL, LEVEL_LABEL, shortHash } from '../core/labels';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [FormsModule, DatePipe],
  template: `
  <div class="docs">
    <div class="docs-head">
      <div>
        <p class="eyebrow">Base documentaire</p>
        <h1>Documents</h1>
        <p class="muted">Chaque PDF reçoit une empreinte SHA-256 à l'import. La vérification recalcule l'empreinte du fichier stocké : si un seul octet change, le document est signalé.</p>
      </div>
      <button class="btn" (click)="verifyAll()" [disabled]="checking()">{{ checking() ? 'Vérification…' : 'Vérifier l\\'intégrité de tout' }}</button>
    </div>

    @if (canUpload()) {
      <form class="upload card" (ngSubmit)="upload()">
        <label class="file">
          <input type="file" accept="application/pdf" (change)="pick($event)">
          <span>{{ file?.name ?? 'Choisir un PDF…' }}</span>
        </label>
        <label>Niveau d'accès
          <select name="lvl" [(ngModel)]="level">
            @for (l of levels(); track l) { <option [value]="l">{{ levelLabel(l) }}</option> }
          </select>
        </label>
        <button class="btn primary" [disabled]="!file || uploading()">{{ uploading() ? 'Import…' : 'Importer et indexer' }}</button>
        @if (message()) { <span class="note">{{ message() }}</span> }
      </form>
    }

    <div class="table-wrap card">
      <table>
        <thead>
          <tr><th>Document</th><th>Accès</th><th>Pages</th><th>Passages</th><th>Empreinte SHA-256</th><th>Intégrité</th><th></th></tr>
        </thead>
        <tbody>
          @for (d of docs(); track d.id) {
            <tr>
              <td><b>{{ d.name }}</b><small>importé le {{ d.uploadedAt | date:'dd/MM/yyyy HH:mm' }} par {{ d.uploadedBy }}</small></td>
              <td><span class="lvl" [attr.data-l]="d.accessLevel">{{ levelLabel(d.accessLevel) }}</span></td>
              <td class="num">{{ d.pages }}</td>
              <td class="num">{{ d.chunks }}</td>
              <td><code [title]="d.sha256">{{ short(d.sha256) }}</code></td>
              <td>
                <span class="integ" [attr.data-i]="d.integrity">{{ integrityLabel(d.integrity) }}</span>
                @if (d.lastCheckedAt) { <small>{{ d.lastCheckedAt | date:'HH:mm:ss' }}</small> }
              </td>
              <td class="actions">
                <button class="btn small" (click)="verify(d)">Vérifier</button>
                @if (isAdmin()) { <button class="btn small danger" (click)="remove(d)">Supprimer</button> }
              </td>
            </tr>
            @if (detail()[d.id]; as v) {
              @if (v.integrity === 'ALTERE') {
                <tr class="alert-row"><td colspan="7">
                  <b>Le fichier stocké ne correspond plus à l'original.</b>
                  Attendu <code>{{ short(v.expected) }}</code>, obtenu <code>{{ short(v.actual) }}</code>.
                  Ce document est exclu des réponses de l'assistant.
                </td></tr>
              }
            }
          } @empty {
            <tr><td colspan="7" class="muted">Aucun document accessible avec votre rôle.</td></tr>
          }
        </tbody>
      </table>
    </div>
  </div>
  `
})
export class DocumentsComponent implements OnInit {
  docs = signal<DocumentInfo[]>([]);
  detail = signal<Record<number, VerifyResult>>({});
  checking = signal(false);
  uploading = signal(false);
  message = signal('');
  file: File | null = null;
  level: AccessLevel = 'PUBLIC';

  isAdmin = computed(() => this.auth.role() === 'ADMIN');
  canUpload = computed(() => this.auth.role() === 'ADMIN' || this.auth.role() === 'RH');
  levels = computed<AccessLevel[]>(() => (this.isAdmin() ? ['PUBLIC', 'RH', 'CONFIDENTIEL'] : ['PUBLIC', 'RH']));

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.documents().subscribe(d => this.docs.set(d));
  }

  pick(e: Event): void {
    this.file = (e.target as HTMLInputElement).files?.[0] ?? null;
    this.message.set('');
  }

  upload(): void {
    if (!this.file) return;
    this.uploading.set(true);
    this.api.upload(this.file, this.level).subscribe({
      next: d => {
        this.message.set(`« ${d.name} » importé : ${d.pages} page(s), ${d.chunks} passage(s) indexés.`);
        this.uploading.set(false);
        this.file = null;
        this.load();
      },
      error: err => {
        this.message.set(err?.error?.message ?? 'Import impossible.');
        this.uploading.set(false);
      }
    });
  }

  verify(d: DocumentInfo): void {
    this.api.verify(d.id).subscribe(v => this.apply([v]));
  }

  verifyAll(): void {
    this.checking.set(true);
    this.api.verifyAll().subscribe({
      next: list => { this.apply(list); this.checking.set(false); },
      error: () => this.checking.set(false)
    });
  }

  remove(d: DocumentInfo): void {
    this.api.remove(d.id).subscribe(() => this.load());
  }

  levelLabel(l: AccessLevel): string { return LEVEL_LABEL[l]; }
  integrityLabel(i: DocumentInfo['integrity']): string { return INTEGRITY_LABEL[i]; }
  short(h: string): string { return shortHash(h); }

  private apply(list: VerifyResult[]): void {
    const map = { ...this.detail() };
    const now = new Date().toISOString();
    for (const v of list) map[v.id] = v;
    this.detail.set(map);
    this.docs.update(ds => ds.map(d => {
      const v = list.find(x => x.id === d.id);
      return v ? { ...d, integrity: v.integrity, lastCheckedAt: now } : d;
    }));
  }
}
