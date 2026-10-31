import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PapersService } from '../../core/services/papers.service';
import { Paper } from '../../core/models';

const TOPIC_OPTIONS = ['Banco de Dados', 'Inteligência Artificial', 'Machine Learning Tradicional'];

@Component({
  selector: 'app-leitura',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './leitura.component.html',
  styleUrl: './leitura.component.scss'
})
export class LeituraComponent implements OnInit {
  readonly topicOptions = TOPIC_OPTIONS;
  selectedTopic: string | null = null;
  papers: Paper[] = [];
  loading = false;

  constructor(private papersService: PapersService) {}

  ngOnInit(): void {
    this.load();
  }

  selectTopic(topic: string | null): void {
    this.selectedTopic = topic;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.papersService.getFeed(this.selectedTopic ?? undefined).subscribe({
      next: (papers) => {
        this.papers = papers;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
