import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { OrderResponse } from '../../core/models/order.model';
import { CheckoutService } from '../../core/services/checkout.service';

/**
 * Shows an order's status. Reached two ways:
 *  1. Straight from checkout, which passes the order it just got back via router state — no
 *     need to make the customer re-enter their phone number to see the order they just placed.
 *  2. Directly (bookmarked link, or the "Track your order" page) with no state — the order
 *     number alone isn't a valid credential (design doc §53: order numbers are sequential), so
 *     this falls back to asking for the phone number the order was placed with.
 */
@Component({
  selector: 'app-order-confirmation',
  imports: [FormsModule, RouterLink, Button, InputText, Message, DecimalPipe],
  templateUrl: './order-confirmation.html',
  styleUrl: './order-confirmation.scss',
})
export class OrderConfirmation implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly checkoutService = inject(CheckoutService);

  readonly order = signal<OrderResponse | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly needsPhoneVerification = signal(false);
  readonly verifying = signal(false);
  readonly verifyError = signal<string | null>(null);

  orderNumber = '';
  phone = '';

  ngOnInit(): void {
    const orderNumber = this.route.snapshot.paramMap.get('orderNumber');
    if (!orderNumber) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }
    this.orderNumber = orderNumber;

    const passedOrder = (history.state as { order?: OrderResponse } | null)?.order;
    if (passedOrder && passedOrder.orderNumber === orderNumber) {
      this.order.set(passedOrder);
    } else {
      this.needsPhoneVerification.set(true);
    }
    this.loading.set(false);
  }

  verifyPhone(): void {
    if (!this.phone.trim()) {
      return;
    }
    this.verifying.set(true);
    this.verifyError.set(null);
    this.checkoutService.trackOrder(this.orderNumber, this.phone).subscribe({
      next: (order) => {
        this.order.set(order);
        this.needsPhoneVerification.set(false);
        this.verifying.set(false);
      },
      error: () => {
        this.verifying.set(false);
        this.verifyError.set("We couldn't find that order for that phone number.");
      },
    });
  }
}
