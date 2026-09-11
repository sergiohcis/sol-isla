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

  /** The order number alone isn't a valid credential (our order numbers are sequential) — the
   *  phone number the order was placed with is required too (design doc §53). POST, not GET,
   *  so the phone number never lands in a URL/browser history/server log. */
  trackOrder(orderNumber: string, phone: string): Observable<OrderResponse> {
    return this.http.post<OrderResponse>('/api/orders/track', { orderNumber, phone });
  }
}
