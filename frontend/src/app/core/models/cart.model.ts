export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  productSlug: string;
  primaryImageUrl: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  availableStock: number;
}

export interface Cart {
  id: string | null;
  items: CartItem[];
  totalQuantity: number;
  subtotal: number;
  currency: string | null;
}

export function emptyCart(): Cart {
  return { id: null, items: [], totalQuantity: 0, subtotal: 0, currency: null };
}
