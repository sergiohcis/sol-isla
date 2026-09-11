import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { DatePicker } from 'primeng/datepicker';
import { FileUpload, FileUploadHandlerEvent } from 'primeng/fileupload';
import { Tag } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { ProductService } from '../../../../core/services/product.service';
import { CategoryService } from '../../../../core/services/category.service';
import { InventoryService } from '../../../../core/services/inventory.service';
import { Category } from '../../../../core/models/category.model';
import { DiscountType, Product, ProductFormValue, ProductStatus } from '../../../../core/models/product.model';

const DISCOUNT_TYPE_OPTIONS: { label: string; value: DiscountType }[] = [
  { label: 'No discount', value: 'NONE' },
  { label: 'Percentage off', value: 'PERCENTAGE' },
  { label: 'Fixed amount off', value: 'FIXED_AMOUNT' },
];

/** design doc §32: ACTIVE -> INACTIVE -> ARCHIVED (ARCHIVED is terminal) — mirrors the backend's
 *  ALLOWED_TRANSITIONS in ProductServiceImpl. Kept in sync manually; the backend is authoritative
 *  and re-validates regardless (CLAUDE.md rule 9). */
const STATUS_TRANSITIONS: Record<ProductStatus, ProductStatus[]> = {
  DRAFT: ['ACTIVE', 'ARCHIVED'],
  ACTIVE: ['INACTIVE', 'ARCHIVED'],
  INACTIVE: ['ACTIVE', 'ARCHIVED'],
  ARCHIVED: [],
};

function emptyForm(): ProductFormValue {
  return {
    sku: '',
    name: '',
    categoryId: '',
    description: '',
    basePrice: 0,
    currency: 'USD',
    discountType: 'NONE',
    discountValue: null,
    discountEffectiveFrom: null,
    discountEffectiveTo: null,
    featured: false,
  };
}

@Component({
  selector: 'app-product-form',
  imports: [
    FormsModule, RouterLink, Button, InputText, InputNumber, Textarea, Select, ToggleSwitch, DatePicker, FileUpload,
    Tag, Toast,
  ],
  providers: [MessageService],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss',
})
export class ProductForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly inventoryService = inject(InventoryService);
  private readonly messageService = inject(MessageService);

  readonly categories = signal<Category[]>([]);
  readonly product = signal<Product | null>(null);
  readonly saving = signal(false);
  readonly discountTypeOptions = DISCOUNT_TYPE_OPTIONS;

  form: ProductFormValue = emptyForm();
  /** p-datepicker binds to Date objects; the wire format (ProductFormValue.discountEffectiveFrom/To)
   *  is an ISO string, so these are converted at load/save time rather than bound directly. */
  discountEffectiveFromDate: Date | null = null;
  discountEffectiveToDate: Date | null = null;
  initialStockQuantity = 0;
  stockDelta = 0;
  stockReason = '';

  get productId(): string | null {
    return this.route.snapshot.paramMap.get('id');
  }

  get isEditMode(): boolean {
    return this.productId !== null;
  }

  get availableStatusTransitions(): ProductStatus[] {
    const current = this.product()?.status;
    return current ? STATUS_TRANSITIONS[current] : [];
  }

  ngOnInit(): void {
    this.categoryService.listAdmin().subscribe((categories) => this.categories.set(categories));
    if (this.isEditMode) {
      this.loadProduct();
    }
  }

  private loadProduct(): void {
    this.productService.getAdminById(this.productId!).subscribe((product) => {
      this.product.set(product);
      this.form = {
        sku: product.sku,
        name: product.name,
        categoryId: product.category.id,
        description: product.description ?? '',
        basePrice: product.basePrice,
        currency: product.currency,
        discountType: product.discountType,
        discountValue: product.discountValue,
        discountEffectiveFrom: product.discountEffectiveFrom,
        discountEffectiveTo: product.discountEffectiveTo,
        featured: product.featured,
      };
      this.discountEffectiveFromDate = product.discountEffectiveFrom ? new Date(product.discountEffectiveFrom) : null;
      this.discountEffectiveToDate = product.discountEffectiveTo ? new Date(product.discountEffectiveTo) : null;
    });
  }

  save(): void {
    this.saving.set(true);
    const payload: ProductFormValue = {
      ...this.form,
      discountEffectiveFrom: this.discountEffectiveFromDate?.toISOString() ?? null,
      discountEffectiveTo: this.discountEffectiveToDate?.toISOString() ?? null,
    };
    const request$ = this.isEditMode
      ? this.productService.update(this.productId!, payload)
      : this.productService.create({ ...payload, initialStockQuantity: this.initialStockQuantity });

    request$.subscribe({
      next: (product) => {
        this.saving.set(false);
        this.messageService.add({ severity: 'success', summary: this.isEditMode ? 'Product updated' : 'Product created' });
        if (!this.isEditMode) {
          this.router.navigate(['/admin/products', product.id, 'edit']);
        } else {
          this.product.set(product);
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.messageService.add({ severity: 'error', summary: 'Save failed', detail: err.error?.detail });
      },
    });
  }

  changeStatus(status: ProductStatus): void {
    this.productService.changeStatus(this.productId!, status).subscribe({
      next: (product) => {
        this.product.set(product);
        this.messageService.add({ severity: 'success', summary: `Status changed to ${status}` });
      },
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Status change failed', detail: err.error?.detail }),
    });
  }

  adjustStock(): void {
    if (this.stockDelta === 0 || !this.stockReason.trim()) {
      return;
    }
    this.inventoryService.adjust(this.productId!, this.stockDelta, this.stockReason).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Stock adjusted' });
        this.stockDelta = 0;
        this.stockReason = '';
        this.loadProduct();
      },
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Adjustment failed', detail: err.error?.detail }),
    });
  }

  uploadImage(event: FileUploadHandlerEvent, fileUpload: FileUpload): void {
    const file = event.files[0];
    if (!file) {
      return;
    }
    this.productService.uploadImage(this.productId!, file).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Image uploaded' });
        fileUpload.clear();
        this.loadProduct();
      },
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Upload failed', detail: err.error?.detail }),
    });
  }

  deleteImage(imageId: string): void {
    this.productService.deleteImage(this.productId!, imageId).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Image removed' });
        this.loadProduct();
      },
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Delete failed', detail: err.error?.detail }),
    });
  }

  setPrimaryImage(imageId: string): void {
    this.productService.setPrimaryImage(this.productId!, imageId).subscribe({
      next: () => this.loadProduct(),
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Could not set primary image', detail: err.error?.detail }),
    });
  }
}
