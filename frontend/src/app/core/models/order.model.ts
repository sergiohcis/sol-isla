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
