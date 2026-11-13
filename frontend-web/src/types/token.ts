export interface ApiToken {
  id: number;
  name: string;
  tokenPrefix: string;
  source: string;
  scopes: string[];
  createdAt: string;
  lastUsedAt?: string | null;
  revokedAt?: string | null;
  active: boolean;
}

export interface CreateTokenPayload {
  name: string;
  source: string;
  scopes: string[];
}

export interface CreatedTokenResult {
  token: ApiToken;
  rawToken: string;
}
