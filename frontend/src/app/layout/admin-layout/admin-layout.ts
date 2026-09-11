import { Component, inject, signal } from '@angular/core';
import { NavigationStart, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
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
  { label: 'Categories', path: '/admin/categories', icon: 'pi pi-tags' },
];

/** Persistent chrome (sidebar nav + topbar) wrapping every authenticated admin route. The
 *  public storefront is a separate experience and is not routed through this layout.
 *  Below 768px the sidebar becomes an off-canvas drawer (opened via the topbar hamburger)
 *  and a bottom tab bar takes over primary navigation, since the admin is regularly used
 *  from a phone. */
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
  readonly drawerOpen = signal(false);

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.drawerOpen.set(false);
      }
    });
  }

  toggleDrawer(): void {
    this.drawerOpen.update((open) => !open);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  logout(): void {
    this.currentUserService.logout().subscribe(() => this.router.navigateByUrl('/admin/login'));
  }
}
