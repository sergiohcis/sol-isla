import { DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { CartItem } from '../../core/models/cart.model';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-cart',
  imports: [FormsModule, RouterLink, Button, InputNumber, Toast, DecimalPipe],
  providers: [MessageService],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})
export class Cart {
  private readonly cartService = inject(CartService);
  private readonly messageService = inject(MessageService);

  readonly cart = this.cartService.cart;

  updateQuantity(item: CartItem, quantity: number): void {
    if (quantity === item.quantity) {
      return;
    }
    this.cartService.updateQuantity(item.id, quantity).subscribe({
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Could not update quantity', detail: err.error?.detail });
        this.cartService.refresh().subscribe(); // revert the optimistic input value
      },
    });
  }

  removeItem(item: CartItem): void {
    this.cartService.removeItem(item.id).subscribe({
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Could not remove item', detail: err.error?.detail }),
    });
  }
}
