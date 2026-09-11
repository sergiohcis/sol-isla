import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';
import { Cart, emptyCart } from '../models/cart.model';

/** Guest cart state — identified server-side by an httpOnly cookie (CartController), never a
 *  logged-in user. This service just mirrors whatever the backend returns after each call; it
 *  never computes totals itself (the backend is authoritative on price — CLAUDE.md rule 2). */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);

  private readonly cartSignal = signal<Cart>(emptyCart());
  readonly cart = this.cartSignal.asReadonly();
  readonly totalQuantity = computed(() => this.cartSignal().totalQuantity);

  /** Called once at app bootstrap (see provideAppInitializer in app.config.ts) so the header
   *  badge is correct before the shopper visits any cart-aware page. */
  refresh(): Observable<Cart> {
    return this.http.get<Cart>('/api/cart').pipe(
      tap((cart) => this.cartSignal.set(cart)),
      catchError(() => of(emptyCart())),
    );
  }

  addItem(productId: string, quantity: number): Observable<Cart> {
    return this.http
      .post<Cart>('/api/cart/items', { productId, quantity })
      .pipe(tap((cart) => this.cartSignal.set(cart)));
  }

  updateQuantity(itemId: string, quantity: number): Observable<Cart> {
    return this.http
      .put<Cart>(`/api/cart/items/${itemId}`, { quantity })
      .pipe(tap((cart) => this.cartSignal.set(cart)));
  }

  removeItem(itemId: string): Observable<Cart> {
    return this.http.delete<Cart>(`/api/cart/items/${itemId}`).pipe(tap((cart) => this.cartSignal.set(cart)));
  }
}
