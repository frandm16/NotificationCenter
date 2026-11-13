import { api } from './api';
import type { AuthStatus, LoginPayload } from '../types/auth';

export const authService = {
  async login(payload: LoginPayload): Promise<AuthStatus> {
    const { data } = await api.post<AuthStatus>('/auth/login', payload);
    return data;
  },

  async logout(): Promise<void> {
    await api.post('/auth/logout');
  },

  async checkMe(): Promise<AuthStatus> {
    try {
      const { data } = await api.get<AuthStatus>('/auth/me');
      return data;
    } catch {
      return { authenticated: false, username: null };
    }
  },
};
