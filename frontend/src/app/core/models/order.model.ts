export type PaymentMethod = 'CARD' | 'CASH_ON_DELIVERY';
export type PaymentStatus = 'NOT_REQUIRED' | 'PENDING' | 'AUTHORIZED' | 'PAID' | 'FAILED' | 'CANCELLED' | 'REFUNDED';
export type OrderStatus =
  | 'PENDING_CONFIRMATION'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY_FOR_DELIVERY'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'FAILED_DELIVERY'
  | 'RETURNED';

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
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

/** The normal fulfillment path an order walks absent a cancellation/rejection/failed delivery —
 *  what the admin order-detail page's status stepper renders. Mirrors OrderServiceImpl's
 *  ALLOWED_TRANSITIONS forward chain (kept in sync manually, same tradeoff as order-detail.ts's
 *  STATUS_TRANSITIONS map — the backend is authoritative regardless). */
export const ORDER_HAPPY_PATH: OrderStatus[] = [
  'PENDING_CONFIRMATION',
  'CONFIRMED',
  'PREPARING',
  'READY_FOR_DELIVERY',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
];

/** Branch states off the happy path — a stepper implies linear progress, which is misleading
 *  once an order has landed in one of these, so the detail page shows a banner instead. */
export const ORDER_TERMINAL_BRANCH_STATUSES: OrderStatus[] = ['CANCELLED', 'REJECTED', 'FAILED_DELIVERY', 'RETURNED'];

export interface OrderItemResponse {
  skuSnapshot: string;
  productNameSnapshot: string;
  unitPrice: number;
  discountAmount: number;
  finalUnitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface OrderSummary {
  id: string;
  orderNumber: string;
  customerName: string;
  customerPhone: string;
  grandTotal: number;
  currency: string;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  orderStatus: OrderStatus;
  createdAt: string;
}

export interface OrderResponse {
  id: string;
  orderNumber: string;
  customerName: string;
  customerPhone: string;
  customerEmail: string | null;
  deliveryAddress: string;
  deliveryCity: string;
  deliveryPostalCode: string | null;
  deliveryNotes: string | null;
  items: OrderItemResponse[];
  subtotal: number;
  discountTotal: number;
  deliveryFee: number;
  grandTotal: number;
  currency: string;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  orderStatus: OrderStatus;
  createdAt: string;
}

export interface CheckoutRequest {
  customerName: string;
  customerPhone: string;
  customerEmail: string | null;
  deliveryAddress: string;
  deliveryCity: string;
  deliveryPostalCode: string | null;
  deliveryNotes: string | null;
  deliveryZoneId: string;
  paymentMethod: PaymentMethod;
}
