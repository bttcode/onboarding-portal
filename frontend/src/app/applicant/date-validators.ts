import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Today's date as yyyy-MM-dd in the user's time zone, the format of <input type="date">. */
export function todayIso(now: Date = new Date()): string {
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
}

/** The person must be at least `years` old today. */
export function minimumAge(years: number, now: () => Date = () => new Date()): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string | null;
    if (!value) {
      return null;
    }
    const today = now();
    const latestBirthDate = new Date(today.getFullYear() - years, today.getMonth(), today.getDate());
    return value > todayIso(latestBirthDate) ? { minimumAge: { years } } : null;
  };
}

/** The date must be today or later. */
export function notInPast(now: () => Date = () => new Date()): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string | null;
    return value && value < todayIso(now()) ? { inPast: true } : null;
  };
}
