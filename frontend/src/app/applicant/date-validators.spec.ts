import { FormControl } from '@angular/forms';
import { minimumAge, notInPast } from './date-validators';

describe('date validators', () => {
  const now = () => new Date(2026, 9, 26); // 26 October 2026, local time

  it('accepts someone who turns 18 today', () => {
    expect(minimumAge(18, now)(new FormControl('2008-10-26'))).toBeNull();
  });

  it('rejects someone who turns 18 tomorrow', () => {
    expect(minimumAge(18, now)(new FormControl('2008-10-27'))).toEqual({ minimumAge: { years: 18 } });
  });

  it('leaves an empty date to the required validator', () => {
    expect(minimumAge(18, now)(new FormControl(''))).toBeNull();
    expect(notInPast(now)(new FormControl(''))).toBeNull();
  });

  it('accepts today and rejects yesterday as a start date', () => {
    expect(notInPast(now)(new FormControl('2026-10-26'))).toBeNull();
    expect(notInPast(now)(new FormControl('2026-10-25'))).toEqual({ inPast: true });
  });
});
