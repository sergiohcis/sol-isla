import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { Category } from '../../../core/models/category.model';
import { ProductStatus, ProductSummary } from '../../../core/models/product.model';

const STATUS_OPTIONS: { label: string; value: ProductStatus | null }[] = [
  { label: 'All statuses', value: null },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Archived', value: 'ARCHIVED' },
];

const STATUS_SEVERITY: Record<ProductStatus, 'success' | 'secondary' | 'warn' | 'danger'> = {
  ACTIVE: 'success',
  DRAFT: 'secondary',
  INACTIVE: 'warn',
  ARCHIVED: 'danger',
};

/** PRODUCT_VIEW to list, PRODUCT_CREATE/PRODUCT_UPDATE gate the create/edit form itself (product-form.ts). */
@Component({
  selector: 'app-admin-products',
  imports: [FormsModule, RouterLink, Button, InputText, Select, TableModule, Tag, Tooltip, Paginator, Toast, PrimeTemplate, DecimalPipe],
  providers: [MessageService],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class Products implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly messageService = inject(MessageService);

  readonly products = signal<ProductSummary[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(false);
  readonly totalRecords = signal(0);
  readonly pageSize = 20;
  private page = 0;

  readonly statusOptions = STATUS_OPTIONS;

  q = '';
  categoryId: string | null = null;
  status: ProductStatus | null = null;

  ngOnInit(): void {
    this.categoryService.listAdmin().subscribe((categories) => this.categories.set(categories));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.productService
      .searchAdmin({
        q: this.q || undefined,
        categoryId: this.categoryId ?? undefined,
        status: this.status ?? undefined,
        page: this.page,
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          this.products.set(result.items);
          this.totalRecords.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  onPageChange(event: PaginatorState): void {
    this.page = event.page ?? 0;
    this.load();
  }

  severityFor(status: ProductStatus): 'success' | 'secondary' | 'warn' | 'danger' {
    return STATUS_SEVERITY[status];
  }

  archive(product: ProductSummary): void {
    this.productService.changeStatus(product.id, 'ARCHIVED').subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Product archived' });
        this.load();
      },
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Could not archive', detail: err.error?.detail }),
    });
  }
}
