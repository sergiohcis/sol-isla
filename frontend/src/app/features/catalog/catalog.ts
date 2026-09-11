import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { InputText } from 'primeng/inputtext';
import { Paginator, PaginatorState } from 'primeng/paginator';
import { Skeleton } from 'primeng/skeleton';
import { CategoryService } from '../../core/services/category.service';
import { ProductService } from '../../core/services/product.service';
import { Category } from '../../core/models/category.model';
import { ProductSummary } from '../../core/models/product.model';

/** Public storefront home + category browsing (design doc §30: mobile-first grid of cards). Also
 *  mounted at `/category/:slug` — same component, category comes from the route param. */
@Component({
  selector: 'app-catalog',
  imports: [FormsModule, RouterLink, InputText, Paginator, Skeleton, DecimalPipe],
  templateUrl: './catalog.html',
  styleUrl: './catalog.scss',
})
export class Catalog implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly categoryService = inject(CategoryService);
  private readonly productService = inject(ProductService);

  readonly categories = signal<Category[]>([]);
  readonly products = signal<ProductSummary[]>([]);
  readonly loading = signal(true);
  readonly totalRecords = signal(0);
  readonly pageSize = 12;
  private page = 0;

  q = '';
  selectedCategorySlug: string | null = null;

  ngOnInit(): void {
    this.categoryService.listPublic().subscribe((categories) => this.categories.set(categories));
    this.route.paramMap.subscribe((params) => {
      this.selectedCategorySlug = params.get('slug');
      this.page = 0;
      this.load();
    });
  }

  selectCategory(slug: string | null): void {
    this.selectedCategorySlug = slug;
    this.page = 0;
    this.load();
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  onPageChange(event: PaginatorState): void {
    this.page = event.page ?? 0;
    this.load();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private load(): void {
    this.loading.set(true);
    this.productService
      .searchPublic({
        q: this.q || undefined,
        category: this.selectedCategorySlug ?? undefined,
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
}
