export type Role = 'ADMIN' | 'RH' | 'EMPLOYE';
export type AccessLevel = 'PUBLIC' | 'RH' | 'CONFIDENTIEL';
export type Integrity = 'OK' | 'ALTERE' | 'NON_VERIFIE';

export interface Session {
  token: string;
  username: string;
  displayName: string;
  role: Role;
}

export interface DocumentInfo {
  id: number;
  name: string;
  accessLevel: AccessLevel;
  pages: number;
  chunks: number;
  sha256: string;
  uploadedAt: string;
  uploadedBy: string;
  integrity: Integrity;
  lastCheckedAt: string | null;
}

export interface VerifyResult {
  id: number;
  integrity: Integrity;
  expected: string;
  actual: string;
}

export interface Source {
  n: number;
  documentId: number;
  documentName: string;
  page: number;
  excerpt: string;
  score: number;
}

export interface ChatAnswer {
  answer: string;
  grounded: boolean;
  sources: Source[];
  mode: string;
  latencyMs: number;
}

export interface AuditEntry {
  id: number;
  time: string;
  username: string;
  role: Role;
  action: string;
  detail: string;
  grounded: boolean | null;
  sourceCount: number;
}

export interface Status {
  llmMode: string;
  model: string;
  documents: number;
  chunks: number;
}

export interface ChatTurn {
  question: string;
  answer?: ChatAnswer;
  error?: string;
  pending: boolean;
}
