import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusChipComponent } from './status-chip.component';

describe('StatusChipComponent', () => {
  let fixture: ComponentFixture<StatusChipComponent>;

  beforeEach(() => {
    fixture = TestBed.createComponent(StatusChipComponent);
  });

  it('shows a readable label and tone for an application status', () => {
    fixture.componentRef.setInput('status', 'IN_REVIEW');
    fixture.detectChanges();

    const chip: HTMLElement = fixture.nativeElement.querySelector('.chip');
    expect(chip.textContent?.trim()).toBe('In review');
    expect(chip.dataset['tone']).toBe('info');
  });

  it('flags a background-check hit as a warning', () => {
    fixture.componentRef.setInput('status', 'HIT');
    fixture.detectChanges();

    const chip: HTMLElement = fixture.nativeElement.querySelector('.chip');
    expect(chip.textContent?.trim()).toBe('Check hit');
    expect(chip.dataset['tone']).toBe('warning');
  });
});
