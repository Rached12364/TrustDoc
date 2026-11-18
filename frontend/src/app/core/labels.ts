import { AccessLevel, Integrity, Role } from './models';

export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Administrateur',
  RH: 'Ressources humaines',
  EMPLOYE: 'Collaborateur'
};

export const LEVEL_LABEL: Record<AccessLevel, string> = {
  PUBLIC: 'Public',
  RH: 'RH',
  CONFIDENTIEL: 'Confidentiel'
};

export const INTEGRITY_LABEL: Record<Integrity, string> = {
  OK: 'Intègre',
  ALTERE: 'Modifié',
  NON_VERIFIE: 'Non vérifié'
};

export const MODE_LABEL: Record<string, string> = {
  ollama: 'IA locale (Ollama)',
  ollama_off: 'Ollama non lancé · mode extractif',
  openai: 'API externe',
  extractif: 'Mode extractif (sans LLM)'
};

export function shortHash(h: string): string {
  return h ? `${h.slice(0, 10)}…${h.slice(-6)}` : '';
}
