import { DecimalPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { PrimeTemplate } from 'primeng/api';
import { OrderService } from '../../../core/services/order.service';
import { ORDER_STATUS_LABELS, OrderStatus, OrderSummary } from '../../../core/models/order.model';

const STATUS_OPTIONS: { label: string; value: OrderStatus | null }[] = [
  { label: 'All statuses', value: null },
  { label: 'Pending confirmation', value: 'PENDING_CONFIRMATION' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Preparing', value: 'PREPARING' },
  { label: 'Ready for delivery', value: 'READY_FOR_DELIVERY' },
  { label: 'Out for delivery', value: 'OUT_FOR_DELIVERY' },
  { label: 'Delivered', value: 'DELIVERED' },
  { label: 'Cancelled', value: 'CANCELLED' },
  { label: 'Rejected', value: 'REJECTED' },
  { label: 'Failed delivery', value: 'FAILED_DELIVERY' },
  { label: 'Returned', value: 'RETURNED' },
];

const STATUS_SEVERITY: Record<OrderStatus, 'success' | 'secondary' | 'info' | 'warn' | 'danger'> = {
  PENDING_CONFIRMATION: 'warn',
  CONFIRMED: 'info',
  PREPARING: 'info',
  READY_FOR_DELIVERY: 'info',
  OUT_FOR_DELIVERY: 'info',
  DELIVERED: 'success',
  CANCELLED: 'danger',
  REJECTED: 'danger',
  FAILED_DELIVERY: 'danger',
  RETURNED: 'secondary',
};

/** ORDER_VIEW to list; ORDER_UPDATE_STATUS/ORDER_CANCEL gate the actions on order-detail.ts. */
@Component({
  selector: 'app-admin-orders',
  imports: [FormsModule, RouterLink, Button, InputText, Select, TableModule, Tag, Paginator, PrimeTemplate, DecimalPipe, DatePipe],
  templateUrl: './orders.html',
  styleUrl: './orders.scss',
})
export class Orders implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly orders = signal<OrderSummary[]>([]);
  readonly loading = signal(false);
  readonly totalRecords = signal(0);
  readonly pageSize = 20;
  private page = 0;

  readonly statusOptions = STATUS_OPTIONS;

  q = '';
  status: OrderStatus | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.orderService
      .searchAdmin({
        q: this.q || undefined,
        status: this.status ?? undefined,
        page: this.page,
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          this.orders.set(result.items);
          this.totalRecords.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  onPageChange(event: PaginatorState): void {
    this.page = event.page ?? 0;
    this.load();
  }

  severityFor(status: OrderStatus): 'success' | 'secondary' | 'info' | 'warn' | 'danger' {
    return STATUS_SEVERITY[status];
  }

  labelFor(status: OrderStatus): string {
    return ORDER_STATUS_LABELS[status];
  }
}
