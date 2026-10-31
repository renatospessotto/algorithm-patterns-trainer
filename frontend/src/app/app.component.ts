import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatIconModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  readonly navLinks = [
    { path: '/hoje', label: 'Hoje', icon: 'today' },
    { path: '/explorar', label: 'Explorar', icon: 'explore' },
    { path: '/stats', label: 'Stats', icon: 'insights' },
    { path: '/leitura', label: 'Leitura', icon: 'auto_stories' },
    { path: '/historico', label: 'Histórico', icon: 'history' }
  ];
}
