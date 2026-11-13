import { api } from './api';
import type {
  AppNotification,
  IngestNotificationPayload,
  NotificationLevel,
  PaginatedResponse,
} from '../types/notification';

export interface NotificationFilterParams {
  source?: string;
  read?: boolean;
  level?: NotificationLevel;
  page?: number;
  size?: number;
}

export const notificationService = {
  async listNotifications(params: NotificationFilterParams = {}): Promise<PaginatedResponse<AppNotification>> {
    const { data } = await api.get<PaginatedResponse<AppNotification>>('/notifications', {
      params: {
        source: params.source || undefined,
        read: typeof params.read === 'boolean' ? params.read : undefined,
        level: params.level || undefined,
        page: params.page ?? 0,
        size: params.size ?? 30,
      },
    });
    return data;
  },

  async listSources(): Promise<string[]> {
    const { data } = await api.get<string[]>('/notifications/sources');
    return data;
  },

  async getUnreadCount(): Promise<number> {
    const { data } = await api.get<{ unreadCount: number }>('/notifications/unread-count');
    return data.unreadCount;
  },

  async markRead(id: number): Promise<AppNotification> {
    const { data } = await api.patch<AppNotification>(`/notifications/${id}/read`);
    return data;
  },

  async markAllRead(): Promise<void> {
    await api.post('/notifications/read-all');
  },

  async deleteNotification(id: number): Promise<void> {
    await api.delete(`/notifications/${id}`);
  },

  async sendTestNotification(payload: IngestNotificationPayload, sourceName?: string): Promise<AppNotification> {
    const headers: Record<string, string> = {};
    if (sourceName) {
      headers['X-Admin-Test-Source'] = sourceName;
    }
    const { data } = await api.post<AppNotification>('/notifications', payload, { headers });
    return data;
  },
};
