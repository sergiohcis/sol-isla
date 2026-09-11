import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/public-layout/public-layout').then((m) => m.PublicLayout),
    children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('./features/catalog/catalog').then((m) => m.Catalog) },
      {
        path: 'category/:slug',
        loadComponent: () => import('./features/catalog/catalog').then((m) => m.Catalog),
      },
      {
        path: 'products/:slug',
        loadComponent: () => import('./features/product-detail/product-detail').then((m) => m.ProductDetail),
      },
      { path: 'cart', loadComponent: () => import('./features/cart/cart').then((m) => m.Cart) },
      { path: 'checkout', loadComponent: () => import('./features/checkout/checkout').then((m) => m.Checkout) },
      { path: 'track', loadComponent: () => import('./features/track-order/track-order').then((m) => m.TrackOrder) },
      {
        path: 'orders/:orderNumber/confirmation',
        loadComponent: () =>
          import('./features/order-confirmation/order-confirmation').then((m) => m.OrderConfirmation),
      },
    ],
  },
  {
    path: 'admin/login',
    loadComponent: () => import('./features/admin/login/admin-login').then((m) => m.AdminLogin),
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/admin-layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'orders' },
      { path: 'orders', loadComponent: () => import('./features/admin/orders/orders').then((m) => m.Orders) },
      {
        path: 'orders/:id',
        loadComponent: () => import('./features/admin/orders/order-detail/order-detail').then((m) => m.OrderDetail),
      },
      { path: 'products', loadComponent: () => import('./features/admin/products/products').then((m) => m.Products) },
      {
        path: 'products/new',
        loadComponent: () => import('./features/admin/products/product-form/product-form').then((m) => m.ProductForm),
      },
      {
        path: 'products/:id/edit',
        loadComponent: () => import('./features/admin/products/product-form/product-form').then((m) => m.ProductForm),
      },
      { path: 'categories', loadComponent: () => import('./features/admin/categories/categories').then((m) => m.Categories) },
    ],
  },
];
