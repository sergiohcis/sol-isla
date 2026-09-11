import { DecimalPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Tag } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { OrderService } from '../../../../core/services/order.service';
import { CurrentUserService } from '../../../../core/auth/current-user.service';
import { OrderResponse, OrderStatus } from '../../../../core/models/order.model';

/** Mirrors the backend's ALLOWED_TRANSITIONS in OrderServiceImpl — kept in sync manually, the
 *  backend is authoritative and re-validates regardless (CLAUDE.md rule 9). REJECTED/CANCELLED/
 *  RETURNED are terminal and restock inventory on entry. */
const STATUS_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING_CONFIRMATION: ['CONFIRMED', 'REJECTED'],
  CONFIRMED: ['PREPARING'],
  PREPARING: ['READY_FOR_DELIVERY'],
  READY_FOR_DELIVERY: ['OUT_FOR_DELIVERY'],
  OUT_FOR_DELIVERY: ['DELIVERED', 'FAILED_DELIVERY'],
  FAILED_DELIVERY: ['OUT_FOR_DELIVERY', 'RETURNED'],
  DELIVERED: ['RETURNED'],
  CANCELLED: [],
  REJECTED: [],
  RETURNED: [],
};

/** The one transition that needs ORDER_CANCEL rather than ORDER_UPDATE_STATUS (RolePermissions —
 *  ADMIN-only by default), so it's gated by currentUserService.isAdmin() rather than always shown;
 *  the backend independently enforces this regardless of what the UI shows. */
const CANCELLABLE_FROM: OrderStatus[] = ['PENDING_CONFIRMATION', 'CONFIRMED', 'PREPARING', 'READY_FOR_DELIVERY', 'OUT_FOR_DELIVERY'];

const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING_CONFIRMATION: 'Pending confirmation',
  CONFIRMED: 'Confirmed',
  PREPARING: 'Preparing',
  READY_FOR_DELIVERY: 'Ready for delivery',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
  REJECTED: 'Rejected',
  FAILED_DELIVERY: 'Failed delivery',
  RETURNED: 'Returned',
};

@Component({
  selector: 'app-order-detail',
  imports: [FormsModule, RouterLink, Button, InputText, Tag, Toast, DecimalPipe, DatePipe],
  providers: [MessageService],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss',
})
export class OrderDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);
  private readonly messageService = inject(MessageService);
  private readonly currentUserService = inject(CurrentUserService);

  readonly order = signal<OrderResponse | null>(null);
  readonly loading = signal(false);
  readonly updating = signal(false);
  readonly cancelling = signal(false);
  readonly isAdmin = this.currentUserService.isAdmin;

  cancelReason = '';

  get orderId(): string {
    return this.route.snapshot.paramMap.get('id')!;
  }

  get availableStatusTransitions(): OrderStatus[] {
    const current = this.order()?.orderStatus;
    return current ? STATUS_TRANSITIONS[current] : [];
  }

  get canCancel(): boolean {
    const current = this.order()?.orderStatus;
    return current !== undefined && CANCELLABLE_FROM.includes(current);
  }

  ngOnInit(): void {
    this.load();
  }

  labelFor(status: OrderStatus): string {
    return STATUS_LABELS[status];
  }

  private load(): void {
    this.loading.set(true);
    this.orderService.getAdminById(this.orderId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  changeStatus(status: OrderStatus): void {
    this.updating.set(true);
    this.orderService.changeStatus(this.orderId, status).subscribe({
      next: (order) => {
        this.order.set(order);
        this.updating.set(false);
        this.messageService.add({ severity: 'success', summary: `Status changed to ${this.labelFor(status)}` });
      },
      error: (err) => {
        this.updating.set(false);
        this.messageService.add({ severity: 'error', summary: 'Status change failed', detail: err.error?.detail });
      },
    });
  }

  cancel(): void {
    if (!this.cancelReason.trim()) {
      return;
    }
    this.cancelling.set(true);
    this.orderService.cancel(this.orderId, this.cancelReason).subscribe({
      next: (order) => {
        this.order.set(order);
        this.cancelling.set(false);
        this.cancelReason = '';
        this.messageService.add({ severity: 'success', summary: 'Order cancelled' });
      },
      error: (err) => {
        this.cancelling.set(false);
        this.messageService.add({ severity: 'error', summary: 'Cancel failed', detail: err.error?.detail });
      },
    });
  }
}
