import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AnyStatus, STATUS_PRESENTATION } from '../core/labels';

/** Coloured label for an application or background-check status. */
@Component({
  selector: 'app-status-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="chip" [attr.data-tone]="presentation().tone">{{ presentation().label }}</span>`,
  styles: `
    .chip {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 999px;
      font-size: 0.8rem;
      font-weight: 500;
      line-height: 1.6;
      white-space: nowrap;
    }
    .chip[data-tone='neutral'] { background: #eceff1; color: #37474f; }
    .chip[data-tone='info'] { background: #e3f2fd; color: #0d47a1; }
    .chip[data-tone='success'] { background: #e6f4ea; color: #1e6b34; }
    .chip[data-tone='warning'] { background: #fff4e0; color: #8a4b00; }
    .chip[data-tone='danger'] { background: #fde7e9; color: #8c1d18; }
  `,
})
export class StatusChipComponent {
  readonly status = input.required<AnyStatus>();
  readonly presentation = computed(() => STATUS_PRESENTATION[this.status()]);
}
