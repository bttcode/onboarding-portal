import { formatBytes, validateFile } from './file-validation';

describe('validateFile', () => {
  const pdf = (bytes: number) => new File([new Uint8Array(bytes)], 'id.pdf', { type: 'application/pdf' });

  it('accepts a small PDF', () => {
    expect(validateFile(pdf(1000))).toBeNull();
  });

  it('accepts no file, because the document is optional', () => {
    expect(validateFile(null)).toBeNull();
  });

  it('rejects an unsupported type', () => {
    const exe = new File([new Uint8Array(10)], 'tool.exe', { type: 'application/x-msdownload' });

    expect(validateFile(exe)).toEqual({ fileType: { actual: 'application/x-msdownload' } });
  });

  it('rejects a file over the limit', () => {
    expect(validateFile(pdf(2048), undefined, 1024)).toEqual({ fileSize: { max: 1024, actual: 2048 } });
  });
});

describe('formatBytes', () => {
  it('uses readable units', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2 KB');
    expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB');
  });
});
