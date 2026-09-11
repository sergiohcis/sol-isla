/** Mirrors the backend's PageResponse<T> envelope (common/api/PageResponse.java). */
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
