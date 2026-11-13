import { useEffect, useRef, useState } from 'react';
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
  const handlersRef = useRef(handlers);
  handlersRef.current = handlers;

  useEffect(() => {
    if (!enabled) {
      setStatus('disconnected');
      return;
    }

    setStatus('connecting');
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1';
    const eventSource = new EventSource(`${baseUrl}/stream`, { withCredentials: true });

    const markConnected = () => {
      setStatus('connected');
    };

    eventSource.onopen = markConnected;
    eventSource.addEventListener('open', markConnected);
    eventSource.addEventListener('connected', markConnected);
    eventSource.addEventListener('ping', markConnected);

    eventSource.addEventListener('notification:new', (event) => {
      markConnected();
      try {
        const notification = JSON.parse(event.data) as AppNotification;
        handlersRef.current.onNewNotification?.(notification);
      } catch {
        // Ignore parse error
      }
    });

    eventSource.addEventListener('notification:read', (event) => {
      markConnected();
      try {
        const data = JSON.parse(event.data) as { id: number; read: boolean };
        handlersRef.current.onNotificationRead?.(data);
      } catch {
        // Ignore parse error
      }
    });

    eventSource.addEventListener('notification:read-all', () => {
      markConnected();
      handlersRef.current.onAllRead?.();
    });

    eventSource.addEventListener('notification:deleted', (event) => {
      markConnected();
      try {
        const data = JSON.parse(event.data) as { id: number };
        handlersRef.current.onNotificationDeleted?.(data);
      } catch {
        // Ignore parse error
      }
    });

    eventSource.addEventListener('unread-count', (event) => {
      markConnected();
      try {
        const count = Number(event.data);
        if (!Number.isNaN(count)) {
          handlersRef.current.onUnreadCount?.(count);
        }
      } catch {
        // Ignore parse error
      }
    });

    eventSource.onerror = () => {
      if (eventSource.readyState === EventSource.CONNECTING) {
        setStatus('connecting');
      } else {
        setStatus('disconnected');
      }
    };

    return () => {
      eventSource.close();
      setStatus('disconnected');
    };
  }, [enabled]);

  return { status };
}
