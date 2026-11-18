import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';
import { LoginComponent } from './pages/login.component';
import { ShellComponent } from './pages/shell.component';
import { ChatComponent } from './pages/chat.component';
import { DocumentsComponent } from './pages/documents.component';
import { AuditComponent } from './pages/audit.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'chat', component: ChatComponent },
      { path: 'documents', component: DocumentsComponent },
      { path: 'audit', component: AuditComponent, canActivate: [roleGuard('ADMIN')] },
      { path: '', pathMatch: 'full', redirectTo: 'chat' }
    ]
  },
  { path: '**', redirectTo: 'chat' }
];
