export interface Category {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  parentId: string | null;
  active: boolean;
  sortOrder: number;
}

export interface CategoryCreateRequest {
  name: string;
  description: string | null;
  parentId: string | null;
  sortOrder: number;
}

export interface CategoryUpdateRequest extends CategoryCreateRequest {
  active: boolean;
}
