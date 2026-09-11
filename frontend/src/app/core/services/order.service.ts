import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../models/page.model';
import { OrderResponse, OrderStatus, OrderSummary } from '../models/order.model';

export interface OrderSearchParams {
  status?: OrderStatus;
  q?: string;
  page?: number;
  size?: number;
}

function toHttpParams(params: OrderSearchParams): HttpParams {
  let httpParams = new HttpParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  }
  return httpParams;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  searchAdmin(params: OrderSearchParams): Observable<Page<OrderSummary>> {
    return this.http.get<Page<OrderSummary>>('/api/admin/orders', { params: toHttpParams(params) });
  }

  getAdminById(orderId: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/admin/orders/${orderId}`);
  }

  changeStatus(orderId: string, status: OrderStatus): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`/api/admin/orders/${orderId}/status`, { status });
  }

  cancel(orderId: string, reason: string): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`/api/admin/orders/${orderId}/cancel`, { reason });
  }
}
