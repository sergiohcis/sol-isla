import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../models/page.model';
import {
  Product,
  ProductCreateRequest,
  ProductImage,
  ProductStatus,
  ProductSummary,
  ProductUpdateRequest,
} from '../models/product.model';

export interface ProductSearchParams {
  q?: string;
  category?: string;
  categoryId?: string;
  status?: ProductStatus;
  minPrice?: number;
  maxPrice?: number;
  featured?: boolean;
  page?: number;
  size?: number;
}

function toHttpParams(params: ProductSearchParams): HttpParams {
  let httpParams = new HttpParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  }
  return httpParams;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  searchPublic(params: ProductSearchParams): Observable<Page<ProductSummary>> {
    return this.http.get<Page<ProductSummary>>('/api/products', { params: toHttpParams(params) });
  }

  getPublicBySlug(slug: string): Observable<Product> {
    return this.http.get<Product>(`/api/products/${slug}`);
  }

  searchAdmin(params: ProductSearchParams): Observable<Page<ProductSummary>> {
    return this.http.get<Page<ProductSummary>>('/api/admin/products', { params: toHttpParams(params) });
  }

  getAdminById(productId: string): Observable<Product> {
    return this.http.get<Product>(`/api/admin/products/${productId}`);
  }

  create(request: ProductCreateRequest): Observable<Product> {
    return this.http.post<Product>('/api/admin/products', request);
  }

  update(productId: string, request: ProductUpdateRequest): Observable<Product> {
    return this.http.put<Product>(`/api/admin/products/${productId}`, request);
  }

  changeStatus(productId: string, status: ProductStatus): Observable<Product> {
    return this.http.patch<Product>(`/api/admin/products/${productId}/status`, { status });
  }

  uploadImage(productId: string, file: File, altText?: string): Observable<ProductImage> {
    const formData = new FormData();
    formData.append('file', file);
    if (altText) {
      formData.append('altText', altText);
    }
    return this.http.post<ProductImage>(`/api/admin/products/${productId}/images`, formData);
  }

  deleteImage(productId: string, imageId: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/products/${productId}/images/${imageId}`);
  }

  setPrimaryImage(productId: string, imageId: string): Observable<void> {
    return this.http.post<void>(`/api/admin/products/${productId}/images/${imageId}/primary`, {});
  }
}
