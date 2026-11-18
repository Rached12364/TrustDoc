import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AccessLevel, AuditEntry, ChatAnswer, DocumentInfo, Status, VerifyResult } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  status(): Observable<Status> {
    return this.http.get<Status>('/api/status');
  }

  ask(question: string): Observable<ChatAnswer> {
    return this.http.post<ChatAnswer>('/api/chat', { question });
  }

  documents(): Observable<DocumentInfo[]> {
    return this.http.get<DocumentInfo[]>('/api/documents');
  }

  upload(file: File, accessLevel: AccessLevel): Observable<DocumentInfo> {
    const form = new FormData();
    form.append('file', file);
    form.append('accessLevel', accessLevel);
    return this.http.post<DocumentInfo>('/api/documents', form);
  }

  verify(id: number): Observable<VerifyResult> {
    return this.http.post<VerifyResult>(`/api/documents/${id}/verify`, {});
  }

  verifyAll(): Observable<VerifyResult[]> {
    return this.http.post<VerifyResult[]>('/api/documents/verify', {});
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/documents/${id}`);
  }

  audit(): Observable<AuditEntry[]> {
    return this.http.get<AuditEntry[]>('/api/audit');
  }
}
