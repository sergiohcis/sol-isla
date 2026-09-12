import { DecimalPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Skeleton } from 'primeng/skeleton';
import { Toast } from 'primeng/toast';
import { OrderService } from '../../../../core/services/order.service';
import { CurrentUserService } from '../../../../core/auth/current-user.service';
import {
  ORDER_HAPPY_PATH,
  ORDER_STATUS_LABELS,
  ORDER_TERMINAL_BRANCH_STATUSES,
  OrderResponse,
  OrderStatus,
} from '../../../../core/models/order.model';

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

type StepState = 'done' | 'current' | 'upcoming';

@Component({
  selector: 'app-order-detail',
  imports: [FormsModule, RouterLink, Button, InputText, Toast, ConfirmDialog, Skeleton, DecimalPipe, DatePipe],
  providers: [MessageService, ConfirmationService],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss',
})
export class OrderDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly currentUserService = inject(CurrentUserService);

  readonly order = signal<OrderResponse | null>(null);
  readonly loading = signal(false);
  readonly updating = signal(false);
  readonly cancelling = signal(false);
  readonly isAdmin = this.currentUserService.isAdmin;
  readonly happyPath = ORDER_HAPPY_PATH;

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

  get isOnHappyPath(): boolean {
    const current = this.order()?.orderStatus;
    return current !== undefined && !ORDER_TERMINAL_BRANCH_STATUSES.includes(current);
  }

  get hasActions(): boolean {
    return this.availableStatusTransitions.length > 0 || (this.isAdmin() && this.canCancel);
  }

  ngOnInit(): void {
    this.load();
  }

  labelFor(status: OrderStatus): string {
    return ORDER_STATUS_LABELS[status];
  }

  stepState(status: OrderStatus): StepState {
    const current = this.order()?.orderStatus;
    if (!current) {
      return 'upcoming';
    }
    const currentIndex = this.happyPath.indexOf(current);
    const stepIndex = this.happyPath.indexOf(status);
    if (stepIndex < currentIndex) {
      return 'done';
    }
    return stepIndex === currentIndex ? 'current' : 'upcoming';
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

  confirmCancel(): void {
    if (!this.cancelReason.trim()) {
      return;
    }
    const orderNumber = this.order()?.orderNumber;
    this.confirmationService.confirm({
      header: 'Cancel order',
      message: `Cancel order ${orderNumber}? This restocks its inventory and cannot be undone.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Cancel order',
      acceptButtonProps: { severity: 'danger' },
      rejectLabel: 'Keep order',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () => this.cancel(),
    });
  }

  private cancel(): void {
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
