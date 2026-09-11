export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
export type DiscountType = 'NONE' | 'PERCENTAGE' | 'FIXED_AMOUNT';

export interface ProductImage {
  id: string;
  url: string;
  altText: string | null;
  sortOrder: number;
  primary: boolean;
}

export interface CategorySummary {
  id: string;
  name: string;
  slug: string;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  slug: string;
  description: string | null;
  category: CategorySummary;
  basePrice: number;
  currency: string;
  discountType: DiscountType;
  discountValue: number | null;
  discountEffectiveFrom: string | null;
  discountEffectiveTo: string | null;
  effectivePrice: number;
  status: ProductStatus;
  featured: boolean;
  stockQuantity: number;
  images: ProductImage[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductSummary {
  id: string;
  sku: string;
  name: string;
  slug: string;
  primaryImageUrl: string | null;
  basePrice: number;
  effectivePrice: number;
  currency: string;
  featured: boolean;
  status: ProductStatus;
  categoryName: string | null;
}

export interface ProductFormValue {
  sku: string;
  name: string;
  categoryId: string;
  description: string | null;
  basePrice: number;
  currency: string;
  discountType: DiscountType;
  discountValue: number | null;
  discountEffectiveFrom: string | null;
  discountEffectiveTo: string | null;
  featured: boolean;
}

export interface ProductCreateRequest extends ProductFormValue {
  initialStockQuantity: number;
}

export type ProductUpdateRequest = ProductFormValue;
