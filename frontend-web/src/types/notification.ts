export type NotificationLevel = 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';

export interface AppNotification {
  id: number;
  source: string;
  title: string;
  body: string;
  level: NotificationLevel;
  url?: string | null;
  dedupKey?: string | null;
  read: boolean;
  createdAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface IngestNotificationPayload {
  title: string;
  body: string;
  level?: NotificationLevel;
  url?: string;
  dedupKey?: string;
}
