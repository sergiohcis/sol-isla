import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CheckoutRequest, OrderResponse } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  private readonly http = inject(HttpClient);

  /** {@code idempotencyKey} should be generated once per checkout attempt by the caller and
   *  reused across retries of that same attempt (design doc §23) — never a fresh key per call. */
  checkout(request: CheckoutRequest, idempotencyKey: string): Observable<OrderResponse> {
    return this.http.post<OrderResponse>('/api/checkout', request, {
      headers: { 'Idempotency-Key': idempotencyKey },
    });
  }

  getByOrderNumber(orderNumber: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/orders/track/${orderNumber}`);
  }
}
