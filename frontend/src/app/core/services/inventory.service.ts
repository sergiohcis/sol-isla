import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface InventoryResponse {
  productId: string;
  availableQuantity: number;
  reservedQuantity: number;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);

  adjust(productId: string, delta: number, reason: string): Observable<InventoryResponse> {
    return this.http.post<InventoryResponse>(`/api/admin/inventory/${productId}/adjust`, { delta, reason });
  }
}
