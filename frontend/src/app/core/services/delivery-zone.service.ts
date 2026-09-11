import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DeliveryZone } from '../models/delivery-zone.model';

@Injectable({ providedIn: 'root' })
export class DeliveryZoneService {
  private readonly http = inject(HttpClient);

  /** Public, active-only zones — for the checkout form. */
  listPublic(): Observable<DeliveryZone[]> {
    return this.http.get<DeliveryZone[]>('/api/delivery/zones');
  }
}
