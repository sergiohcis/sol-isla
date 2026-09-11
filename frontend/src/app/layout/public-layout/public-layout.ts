import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/** Chrome for the guest-facing storefront (catalog, cart, checkout). No auth here — guest
 *  checkout is the MVP default (CLAUDE.md rule 11). */
@Component({
  selector: 'app-public-layout',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
})
export class PublicLayout {
  readonly currentYear = new Date().getFullYear();
}
