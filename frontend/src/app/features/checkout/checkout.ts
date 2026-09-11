import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { RadioButton } from 'primeng/radiobutton';
import { Message } from 'primeng/message';
import { CartService } from '../../core/services/cart.service';
import { CheckoutService } from '../../core/services/checkout.service';
import { DeliveryZoneService } from '../../core/services/delivery-zone.service';
import { DeliveryZone } from '../../core/models/delivery-zone.model';
import { CheckoutRequest, PaymentMethod } from '../../core/models/order.model';

/** design doc §13/§14: collect only what's needed, server recalculates everything (CLAUDE.md
 *  rule 2) — this form never sends a price, only what the customer chose. */
@Component({
  selector: 'app-checkout',
  imports: [FormsModule, Button, InputText, Textarea, Select, RadioButton, Message, DecimalPipe],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
})
export class Checkout implements OnInit {
  private readonly cartService = inject(CartService);
  private readonly checkoutService = inject(CheckoutService);
  private readonly deliveryZoneService = inject(DeliveryZoneService);
  private readonly router = inject(Router);

  readonly cart = this.cartService.cart;
  readonly zones = signal<DeliveryZone[]>([]);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  /** Generated once per page visit and reused across retries of the same submission — never
   *  regenerated per HTTP call (design doc §23). */
  private readonly idempotencyKey = crypto.randomUUID();

  form = {
    customerName: '',
    customerPhone: '',
    customerEmail: '',
    deliveryAddress: '',
    deliveryCity: '',
    deliveryPostalCode: '',
    deliveryNotes: '',
    deliveryZoneId: '',
    paymentMethod: 'CASH_ON_DELIVERY' as PaymentMethod,
  };

  ngOnInit(): void {
    this.deliveryZoneService.listPublic().subscribe((zones) => {
      this.zones.set(zones);
      if (zones.length > 0) {
        this.form.deliveryZoneId = zones[0].id;
      }
    });
  }

  get selectedZoneFee(): number {
    return this.zones().find((z) => z.id === this.form.deliveryZoneId)?.fee ?? 0;
  }

  get estimatedTotal(): number {
    return this.cart().subtotal + this.selectedZoneFee;
  }

  get formValid(): boolean {
    return !!(
      this.form.customerName &&
      this.form.customerPhone &&
      this.form.deliveryAddress &&
      this.form.deliveryCity &&
      this.form.deliveryZoneId
    );
  }

  submit(): void {
    if (!this.formValid || this.cart().items.length === 0) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);

    const request: CheckoutRequest = {
      customerName: this.form.customerName,
      customerPhone: this.form.customerPhone,
      customerEmail: this.form.customerEmail || null,
      deliveryAddress: this.form.deliveryAddress,
      deliveryCity: this.form.deliveryCity,
      deliveryPostalCode: this.form.deliveryPostalCode || null,
      deliveryNotes: this.form.deliveryNotes || null,
      deliveryZoneId: this.form.deliveryZoneId,
      paymentMethod: this.form.paymentMethod,
    };

    this.checkoutService.checkout(request, this.idempotencyKey).subscribe({
      next: (order) => {
        this.cartService.refresh().subscribe();
        // Pass the order we already have via router state so the confirmation page can show it
        // immediately, without asking the customer to re-enter their phone number to look up
        // the order they just placed (see OrderConfirmation's fallback for when this is absent).
        this.router.navigate(['/orders', order.orderNumber, 'confirmation'], { state: { order } });
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err.error?.detail ?? 'Checkout failed. Please try again.');
      },
    });
  }
}
