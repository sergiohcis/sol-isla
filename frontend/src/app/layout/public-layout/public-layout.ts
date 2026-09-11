import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { CartService } from '../../core/services/cart.service';

/** Chrome for the guest-facing storefront (catalog, cart, checkout). No auth here — guest
 *  checkout is the MVP default (CLAUDE.md rule 11). */
@Component({
  selector: 'app-public-layout',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayout {
  private readonly cartService = inject(CartService);

  readonly currentYear = new Date().getFullYear();
  readonly cartQuantity = this.cartService.totalQuantity;
}
