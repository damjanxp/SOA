import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import { AuthService } from '../../auth/auth.service';

interface BlogComment {
  id: string;
  _id?: string;
  text: string;
  username?: string;
  userId?: string;
  authorId?: string;
  createdAt?: string;
}

interface BlogDetail {
  id: string;
  _id?: string;
  title: string;
  description?: string;
  images?: string[];
  likes?: string[];
  likeCount?: number;
  comments?: BlogComment[];
  liked?: boolean;
}

@Component({
  selector: 'app-blog-detail',
  templateUrl: './blog-detail.component.html',
  styleUrls: ['./blog-detail.component.scss']
})
export class BlogDetailComponent implements OnInit {
  private readonly apiBase = 'http://localhost:8082/api';

  blog?: BlogDetail;
  renderedDescription: SafeHtml = '';

  loading = false;
  error = '';

  commentText = '';
  editingCommentId: string | null = null;
  editingText = '';

  currentUserId = '';
  isLiked = false;

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private sanitizer: DomSanitizer,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.getCurrentUserId();
    this.fetchBlog();
  }

  fetchBlog(silent = false): void {
    const blogId = this.route.snapshot.paramMap.get('id');
    if (!blogId) {
      this.error = 'Blog not found';
      return;
    }

    if (!silent) {
      this.loading = true;
    }
    this.error = '';

    const headers = this.buildAuthHeaders();
    if (!headers) {
      this.error = 'No token found';
      this.loading = false;
      return;
    }

    this.http.get<{ data: BlogDetail }>(`${this.apiBase}/blogs/${blogId}`, { headers }).subscribe({
      next: (response) => {
        const incoming = response.data;
        const comments = (incoming.comments || []).map((comment) => ({
          ...comment,
          id: comment.id || comment._id || ''
        }));
        this.blog = {
          ...incoming,
          id: incoming.id || incoming._id || '',
          comments
        };
        const likes = this.blog.likes || [];
        this.isLiked = likes.includes(this.currentUserId);
        this.renderedDescription = this.sanitizer.bypassSecurityTrustHtml(
          marked.parse(this.blog?.description || '') as string
        );
        if (!silent) {
          this.loading = false;
        }
      },
      error: (err) => {
        this.error = err?.error?.error || 'Failed to load blog';
        if (!silent) {
          this.loading = false;
        }
      }
    });
  }

  toggleLike(): void {
    if (!this.blog) {
      return;
    }

    const headers = this.buildAuthHeaders();
    if (!headers) {
      this.error = 'No token found';
      return;
    }

    const blogId = this.blog.id || this.blog._id;
    if (!blogId) {
      this.error = 'Blog id is missing';
      return;
    }

    const request = this.isLiked
      ? this.http.delete(`${this.apiBase}/blogs/${blogId}/like`, { headers })
      : this.http.post(`${this.apiBase}/blogs/${blogId}/like`, {}, { headers });

    request.subscribe({
      next: () => {
        this.isLiked = !this.isLiked;
        this.fetchBlog(true);
      },
      error: (err) => {
        this.error = err?.error?.error || 'Failed to update like';
      }
    });
  }

  submitComment(): void {
    if (!this.blog || !this.commentText.trim()) {
      return;
    }

    const headers = this.buildAuthHeaders();
    if (!headers) {
      this.error = 'No token found';
      return;
    }

    const blogId = this.blog.id || this.blog._id;
    if (!blogId) {
      this.error = 'Blog id is missing';
      return;
    }

    const payload = { text: this.commentText.trim() };

    this.http.post(`${this.apiBase}/blogs/${blogId}/comments`, payload, { headers }).subscribe({
      next: () => {
        this.commentText = '';
        this.fetchBlog(true);
      },
      error: (err) => {
        this.error = err?.error?.error || 'Failed to add comment';
      }
    });
  }

  startEdit(comment: BlogComment): void {
    this.editingCommentId = comment.id;
    this.editingText = comment.text;
  }

  cancelEdit(): void {
    this.editingCommentId = null;
    this.editingText = '';
  }

  updateComment(comment: BlogComment): void {
    if (!this.blog || !this.editingText.trim()) {
      return;
    }

    const headers = this.buildAuthHeaders();
    if (!headers) {
      this.error = 'No token found';
      return;
    }

    const blogId = this.blog.id || this.blog._id;
    const commentId = comment.id || comment._id;
    if (!blogId || !commentId) {
      this.error = 'Comment id is missing';
      return;
    }

    const payload = { text: this.editingText.trim() };

    this.http.put(`${this.apiBase}/blogs/${blogId}/comments/${commentId}`, payload, { headers }).subscribe({
      next: () => {
        this.cancelEdit();
        this.fetchBlog(true);
      },
      error: (err) => {
        this.error = err?.error?.error || 'Failed to update comment';
      }
    });
  }

  isOwnComment(comment: BlogComment): boolean {
    const ownerId = comment.userId || comment.authorId;
    return !!ownerId && ownerId === this.currentUserId;
  }

  formatDate(value?: string): string {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    return isNaN(date.getTime()) ? value : date.toLocaleString();
  }

  private getCurrentUserId(): string {
    const token = localStorage.getItem('token');
    if (!token) {
      return '';
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload?.userId || '';
    } catch {
      return '';
    }
  }

  private buildAuthHeaders(): HttpHeaders | null {
    const token = this.authService.getToken();
    if (!token) {
      return null;
    }
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }
}
