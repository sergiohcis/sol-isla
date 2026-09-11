import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Button } from 'primeng/button';
import { CurrentUserService } from '../../core/auth/current-user.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Orders', path: '/admin/orders', icon: 'pi pi-shopping-bag' },
  { label: 'Products', path: '/admin/products', icon: 'pi pi-box' },
];

/** Persistent chrome (sidebar nav + topbar) wrapping every authenticated admin route. The
 *  public storefront is a separate experience and is not routed through this layout. */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, Button],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout {
  private readonly currentUserService = inject(CurrentUserService);
  private readonly router = inject(Router);

  readonly currentUser = this.currentUserService.currentUser;
  readonly navItems = NAV_ITEMS;

  logout(): void {
    this.currentUserService.logout().subscribe(() => this.router.navigateByUrl('/admin/login'));
  }
}
