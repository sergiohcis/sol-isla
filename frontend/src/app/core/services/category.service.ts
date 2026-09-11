import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Category, CategoryCreateRequest, CategoryUpdateRequest } from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);

  /** Public, active-only categories (storefront filter chips). */
  listPublic(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  listAdmin(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/admin/categories');
  }

  create(request: CategoryCreateRequest): Observable<Category> {
    return this.http.post<Category>('/api/admin/categories', request);
  }

  update(categoryId: string, request: CategoryUpdateRequest): Observable<Category> {
    return this.http.put<Category>(`/api/admin/categories/${categoryId}`, request);
  }
}
