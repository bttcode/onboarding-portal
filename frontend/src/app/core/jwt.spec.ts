import { AccessTokenClaims, decodeJwtPayload } from './jwt';

function token(payload: object): string {
  const json = new TextEncoder().encode(JSON.stringify(payload));
  const base64 = btoa(String.fromCharCode(...json));
  const base64Url = base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `header.${base64Url}.signature`;
}

describe('decodeJwtPayload', () => {
  it('reads the roles of a Keycloak token', () => {
    const claims = decodeJwtPayload<AccessTokenClaims>(
      token({ sub: '42', preferred_username: 'carol', realm_access: { roles: ['caseworker'] } }),
    );

    expect(claims?.preferred_username).toBe('carol');
    expect(claims?.realm_access?.roles).toEqual(['caseworker']);
  });

  it('keeps non-ASCII characters in names', () => {
    const claims = decodeJwtPayload<AccessTokenClaims>(token({ sub: '1', name: 'Trần Thái Bảo' }));

    expect(claims?.name).toBe('Trần Thái Bảo');
  });

  it('returns null for something that is not a JWT', () => {
    expect(decodeJwtPayload('not-a-token')).toBeNull();
    expect(decodeJwtPayload('a.%%%.c')).toBeNull();
  });
});
