import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

interface BlogListItem {
  id: string;
  _id?: string;
  title: string;
  images?: string[];
  author?: string;
  authorId?: string;
  createdAt?: string;
  likeCount?: number;
}

interface BlogListResponse {
  data: BlogListItem[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

@Component({
  selector: 'app-blog-list',
  templateUrl: './blog-list.component.html',
  styleUrls: ['./blog-list.component.scss']
})
export class BlogListComponent implements OnInit {
  private readonly apiBase = 'http://localhost:8082/api';

  blogs: BlogListItem[] = [];
  loading = false;
  error = '';

  page = 1;
  limit = 10;
  totalPages = 1;


  constructor(
    private http: HttpClient,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadBlogs();
  }

  loadBlogs(): void {
    this.loading = true;
    this.error = '';

    let params = new HttpParams()
      .set('page', this.page)
      .set('limit', this.limit);


    const headers = this.buildAuthHeaders();
    if (!headers) {
      this.error = 'No token found';
      this.loading = false;
      return;
    }

    this.http.get<BlogListResponse>(`${this.apiBase}/blogs`, { params, headers }).subscribe({
      next: (response) => {
        const items = response.data || [];
        this.blogs = items.map((blog) => ({
          ...blog,
          id: blog.id || blog._id || ''
        }));
        this.totalPages = response.totalPages || 1;
        this.page = response.page || this.page;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'Failed to load blogs';
        this.loading = false;
      }
    });
  }


  goToBlog(blog: BlogListItem): void {
    const blogId = blog.id || blog._id;
    if (!blogId) {
      this.error = 'Blog id is missing';
      return;
    }
    this.router.navigate(['/blogs', blogId]);
  }

  previousPage(): void {
    if (this.page > 1) {
      this.page -= 1;
      this.loadBlogs();
    }
  }

  nextPage(): void {
    if (this.page < this.totalPages) {
      this.page += 1;
      this.loadBlogs();
    }
  }

  formatDate(value?: string): string {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    return isNaN(date.getTime()) ? value : date.toLocaleDateString();
  }

  private buildAuthHeaders(): HttpHeaders | null {
    const token = this.authService.getToken();
    if (!token) {
      return null;
    }
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }
}
