import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-blog-form',
  templateUrl: './blog-form.component.html',
  styleUrls: ['./blog-form.component.scss']
})
export class BlogFormComponent {
  private readonly apiBase = environment.apiBase + '/api';

  title = '';
  description = '';
  images: string[] = [''];

  loading = false;
  error = '';
  success = '';

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  addImageField(): void {
    this.images.push('');
  }

  removeImageField(index: number): void {
    this.images.splice(index, 1);
    if (this.images.length === 0) {
      this.images.push('');
    }
  }

  submit(): void {
    this.error = '';
    this.success = '';

    if (!this.title.trim() || !this.description.trim()) {
      this.error = 'Title and description are required.';
      return;
    }

    const payload = {
      title: this.title.trim(),
      description: this.description.trim(),
      images: this.images.map((img) => img.trim()).filter((img) => img.length > 0)
    };

    this.loading = true;

    this.http.post(`${this.apiBase}/blogs`, payload).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/blogs']);
      },
      error: (err) => {
        this.error = err?.error?.error || 'Failed to create blog';
        this.loading = false;
      }
    });
  }
}
