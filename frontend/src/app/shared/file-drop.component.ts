import { ChangeDetectionStrategy, Component, computed, forwardRef, input, signal } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ACCEPTED_TYPES, MAX_FILE_BYTES, formatBytes, validateFile } from './file-validation';

/**
 * Drag-and-drop file picker that works as a form control: `formControlName="document"` gives
 * the parent the selected File and the validation errors.
 */
@Component({
  selector: 'app-file-drop',
  imports: [MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FileDropComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => FileDropComponent), multi: true },
  ],
  template: `
    <div
      class="drop"
      [class.dragging]="dragging()"
      [class.invalid]="error()"
      (dragover)="onDragOver($event)"
      (dragleave)="dragging.set(false)"
      (drop)="onDrop($event)"
    >
      @if (file(); as selected) {
        <div class="selected">
          <span class="name">{{ selected.name }}</span>
          <span class="size">{{ size() }}</span>
        </div>
        <button mat-button type="button" (click)="clear()" [disabled]="disabled()">Remove</button>
      } @else {
        <p>Drag a file here, or</p>
        <button mat-stroked-button type="button" (click)="picker.click()" [disabled]="disabled()">
          Choose a file
        </button>
        <p class="hint">PDF, PNG or JPEG, up to {{ maxSize() }}</p>
      }
      <input #picker type="file" hidden [attr.accept]="accept().join(',')" (change)="onSelect($event)" />
    </div>
    @if (error(); as errors) {
      <p class="error" role="alert">
        @if (errors['fileType']) {
          This file type is not accepted.
        } @else if (errors['fileSize']) {
          The file is larger than {{ maxSize() }}.
        }
      </p>
    }
  `,
  styles: `
    .drop {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      padding: 24px 16px;
      border: 2px dashed #b0b7c3;
      border-radius: 12px;
      background: #fff;
      text-align: center;
      transition: border-color 120ms, background 120ms;
    }
    .drop.dragging { border-color: #1a73e8; background: #eef4fe; }
    .drop.invalid { border-color: #c5221f; }
    .drop p { margin: 4px 0; }
    .selected { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
    .name { font-weight: 500; word-break: break-all; }
    .size, .hint { color: #5f6368; font-size: 0.85rem; }
    .error { color: #c5221f; margin: 6px 0 0; font-size: 0.85rem; }
  `,
})
export class FileDropComponent implements ControlValueAccessor, Validator {
  readonly accept = input<readonly string[]>(ACCEPTED_TYPES);
  readonly maxBytes = input(MAX_FILE_BYTES);

  readonly file = signal<File | null>(null);
  readonly dragging = signal(false);
  readonly disabled = signal(false);
  readonly error = computed(() => validateFile(this.file(), this.accept(), this.maxBytes()));
  readonly size = computed(() => formatBytes(this.file()?.size ?? 0));
  readonly maxSize = computed(() => formatBytes(this.maxBytes()));

  private onChange: (file: File | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  writeValue(value: File | null): void {
    this.file.set(value ?? null);
  }

  registerOnChange(fn: (file: File | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  validate(_control: AbstractControl): ValidationErrors | null {
    return this.error();
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.disabled()) {
      this.dragging.set(true);
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const dropped = event.dataTransfer?.files.item(0);
    if (dropped && !this.disabled()) {
      this.select(dropped);
    }
  }

  onSelect(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    const chosen = inputElement.files?.item(0);
    if (chosen) {
      this.select(chosen);
    }
    inputElement.value = '';
  }

  clear(): void {
    this.select(null);
  }

  private select(file: File | null): void {
    this.file.set(file);
    this.onChange(file);
    this.onTouched();
  }
}
