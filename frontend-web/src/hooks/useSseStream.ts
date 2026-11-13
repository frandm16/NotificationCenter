import { useEffect, useState } from 'react';
import type { AppNotification } from '../types/notification';

export type SseStatus = 'connecting' | 'connected' | 'disconnected';

interface SseHandlers {
  onNewNotification?: (notification: AppNotification) => void;
  onNotificationRead?: (data: { id: number; read: boolean }) => void;
  onAllRead?: () => void;
  onNotificationDeleted?: (data: { id: number }) => void;
  onUnreadCount?: (count: number) => void;
}

export function useSseStream(handlers: SseHandlers, enabled: boolean = true) {
  const [status, setStatus] = useState<SseStatus>('disconnected');

  useEffect(() => {
    if (!enabled) {
      setStatus('disconnected');
      return;
    }

    setStatus('connecting');
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1';
    const eventSource = new EventSource(`${baseUrl}/stream`, { withCredentials: true });

    eventSource.addEventListener('open', () => {
      setStatus('connected');
    });

    eventSource.addEventListener('connected', () => {
      setStatus('connected');
    });

    eventSource.addEventListener('notification:new', (event) => {
      try {
        const notification = JSON.parse(event.data) as AppNotification;
        handlers.onNewNotification?.(notification);
      } catch {
        // Ignore parse error
      }
    });

    eventSource.addEventListener('notification:read', (event) => {
      try {
        const data = JSON.parse(event.data) as { id: number; read: boolean };
        handlers.onNotificationRead?.(data);
      } catch {
        // Ignore parse error
      }
    });

    eventSource.addEventListener('notification:read-all', () => {
      handlers.onAllRead?.();
    });

    eventSource.addEventListener('notification:deleted', (event) => {
      try {
        const data = JSON.parse(event.data) as { id: number };
        handlers.onNotificationDeleted?.(data);
      } catch {
        // Ignore parse error
      }
    });

    eventSource.addEventListener('unread-count', (event) => {
      try {
        const count = Number(event.data);
        if (!Number.isNaN(count)) {
          handlers.onUnreadCount?.(count);
        }
      } catch {
        // Ignore parse error
      }
    });

    eventSource.addEventListener('error', () => {
      setStatus('disconnected');
    });

    return () => {
      eventSource.close();
      setStatus('disconnected');
    };
  }, [enabled]);

  return { status };
}
