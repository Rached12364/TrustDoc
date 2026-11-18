import { Component, input } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
  <span class="logo" [class.light]="light()">
    <svg viewBox="0 0 32 32" width="30" height="30" aria-hidden="true">
      <path d="M16 2 4 7v8c0 7.5 5.1 13.6 12 15 6.9-1.4 12-7.5 12-15V7L16 2z" class="logo-shield"/>
      <path d="m10.5 16 4 4 7-8" fill="none" class="logo-check" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
    <span class="logo-word">Trust<b>Doc</b></span>
  </span>
  `
})
export class LogoComponent {
  light = input(false);
}
