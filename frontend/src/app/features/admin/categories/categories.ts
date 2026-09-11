import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { MessageService, PrimeTemplate } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { CategoryService } from '../../../core/services/category.service';
import { Category } from '../../../core/models/category.model';

interface CategoryFormState {
  name: string;
  description: string;
  parentId: string | null;
  sortOrder: number;
  active: boolean;
}

const EMPTY_FORM: CategoryFormState = { name: '', description: '', parentId: null, sortOrder: 0, active: true };

@Component({
  selector: 'app-categories',
  imports: [FormsModule, Button, Dialog, InputText, InputNumber, Textarea, ToggleSwitch, Select, TableModule, Tag, Toast, PrimeTemplate],
  providers: [MessageService],
  templateUrl: './categories.html',
  styleUrl: './categories.scss',
})
export class Categories implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly messageService = inject(MessageService);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(false);
  readonly dialogVisible = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<string | null>(null);

  form: CategoryFormState = { ...EMPTY_FORM };

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.categoryService.listAdmin().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form = { ...EMPTY_FORM, sortOrder: this.categories().length };
    this.dialogVisible.set(true);
  }

  openEdit(category: Category): void {
    this.editingId.set(category.id);
    this.form = {
      name: category.name,
      description: category.description ?? '',
      parentId: category.parentId,
      sortOrder: category.sortOrder,
      active: category.active,
    };
    this.dialogVisible.set(true);
  }

  save(): void {
    this.saving.set(true);
    const editingId = this.editingId();
    const request$ = editingId
      ? this.categoryService.update(editingId, { ...this.form })
      : this.categoryService.create({
          name: this.form.name,
          description: this.form.description,
          parentId: this.form.parentId,
          sortOrder: this.form.sortOrder,
        });

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({ severity: 'success', summary: editingId ? 'Category updated' : 'Category created' });
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.messageService.add({ severity: 'error', summary: 'Save failed', detail: err.error?.detail });
      },
    });
  }

  parentName(parentId: string | null): string {
    if (!parentId) {
      return '—';
    }
    return this.categories().find((c) => c.id === parentId)?.name ?? '—';
  }

  get otherCategories(): Category[] {
    const editingId = this.editingId();
    return this.categories().filter((c) => c.id !== editingId);
  }
}
