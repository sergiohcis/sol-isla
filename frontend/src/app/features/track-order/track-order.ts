import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { CheckoutService } from '../../core/services/checkout.service';

/** Standalone entry point for a customer who wants to check an order's status later (not
 *  right after checkout, where OrderConfirmation already has the order via router state). */
@Component({
  selector: 'app-track-order',
  imports: [FormsModule, Button, InputText, Message],
  templateUrl: './track-order.html',
  styleUrl: './track-order.scss',
})
export class TrackOrder {
  private readonly router = inject(Router);
  private readonly checkoutService = inject(CheckoutService);

  orderNumber = '';
  phone = '';
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  get formValid(): boolean {
    return !!(this.orderNumber.trim() && this.phone.trim());
  }

  submit(): void {
    if (!this.formValid) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.checkoutService.trackOrder(this.orderNumber.trim(), this.phone).subscribe({
      next: (order) => {
        this.loading.set(false);
        this.router.navigate(['/orders', order.orderNumber, 'confirmation'], { state: { order } });
      },
      error: () => {
        this.loading.set(false);
        this.error.set("We couldn't find that order for that phone number.");
      },
    });
  }
}
