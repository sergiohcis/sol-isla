import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderResponse } from '../../core/models/order.model';
import { CheckoutService } from '../../core/services/checkout.service';

@Component({
  selector: 'app-order-confirmation',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './order-confirmation.html',
  styleUrl: './order-confirmation.scss',
})
export class OrderConfirmation implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly checkoutService = inject(CheckoutService);

  readonly order = signal<OrderResponse | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);

  ngOnInit(): void {
    const orderNumber = this.route.snapshot.paramMap.get('orderNumber');
    if (!orderNumber) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }
    this.checkoutService.getByOrderNumber(orderNumber).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }
}
