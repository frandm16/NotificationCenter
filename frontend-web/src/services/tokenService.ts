import { api } from './api';
import type { ApiToken, CreateTokenPayload, CreatedTokenResult } from '../types/token';

export const tokenService = {
  async listTokens(): Promise<ApiToken[]> {
    const { data } = await api.get<ApiToken[]>('/tokens');
    return data;
  },

  async createToken(payload: CreateTokenPayload): Promise<CreatedTokenResult> {
    const { data } = await api.post<CreatedTokenResult>('/tokens', payload);
    return data;
  },

  async revokeToken(id: number): Promise<void> {
    await api.patch(`/tokens/${id}/revoke`);
  },

  async deleteToken(id: number): Promise<void> {
    await api.delete(`/tokens/${id}`);
  },
};
