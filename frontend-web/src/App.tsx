import { useCallback, useEffect, useState } from 'react';
import { Bell, LoaderCircle } from 'lucide-react';
import { useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { LoginPage } from './components/LoginPage';
import { NotificationCard } from './components/NotificationCard';
import { NotificationFilters } from './components/NotificationFilters';
import { TestDispatchModal } from './components/TestDispatchModal';
import { TokenManagerView } from './components/TokenManagerView';
import { notificationService } from './services/notificationService';
import { pushService } from './services/pushService';
import { useSseStream } from './hooks/useSseStream';
import type { AppNotification, NotificationLevel } from './types/notification';

export function App() {
  const { user, loading: authLoading } = useAuth();
  const [activeTab, setActiveTab] = useState<'notifications' | 'tokens'>('notifications');

  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [sources, setSources] = useState<string[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const [selectedSource, setSelectedSource] = useState('');
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [selectedLevel, setSelectedLevel] = useState<NotificationLevel | ''>('');

  const [testModalOpen, setTestModalOpen] = useState(false);
  const [pushEnabled, setPushEnabled] = useState(false);

  const fetchUnreadCount = useCallback(async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
    } catch {
      // Ignore count fetch error
    }
  }, []);

  const fetchSources = useCallback(async () => {
    try {
      const list = await notificationService.listSources();
      setSources(list);
    } catch {
      // Ignore sources fetch error
    }
  }, []);

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const data = await notificationService.listNotifications({
        source: selectedSource || undefined,
        read: unreadOnly ? false : undefined,
        level: selectedLevel || undefined,
      });
      setNotifications(data.content);
    } catch {
      // Ignore fetch error
    } finally {
      setLoading(false);
    }
  }, [selectedSource, unreadOnly, selectedLevel]);

  useEffect(() => {
    if (user?.authenticated) {
      void fetchNotifications();
      void fetchUnreadCount();
      void fetchSources();
      void pushService.isSubscribed().then(setPushEnabled);
    }
  }, [user?.authenticated, fetchNotifications, fetchUnreadCount, fetchSources]);

  const { status: streamStatus } = useSseStream(
    {
      onNewNotification: (newNotif) => {
        setNotifications((prev) => [newNotif, ...prev.filter((n) => n.id !== newNotif.id)]);
        setUnreadCount((c) => c + 1);
        setSources((prev) => (prev.includes(newNotif.source) ? prev : [...prev, newNotif.source]));
      },
      onNotificationRead: ({ id }) => {
        setNotifications((prev) =>
          prev.map((n) => (n.id === id ? { ...n, read: true } : n))
        );
      },
      onAllRead: () => {
        setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
        setUnreadCount(0);
      },
      onNotificationDeleted: ({ id }) => {
        setNotifications((prev) => prev.filter((n) => n.id !== id));
      },
      onUnreadCount: (count) => {
        setUnreadCount(count);
      },
    },
    Boolean(user?.authenticated)
  );

  const handleMarkRead = async (id: number) => {
    try {
      await notificationService.markRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
      void fetchUnreadCount();
    } catch {
      // Ignore error
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // Ignore error
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await notificationService.deleteNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      void fetchUnreadCount();
    } catch {
      // Ignore error
    }
  };

  const handleEnablePush = async () => {
    const success = await pushService.enablePush();
    setPushEnabled(success);
  };

  if (authLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--app-bg)] text-neutral-400">
        <LoaderCircle size={28} className="animate-spin text-indigo-400" />
      </div>
    );
  }

  if (!user?.authenticated) {
    return <LoginPage />;
  }

  return (
    <div className="min-h-screen bg-[var(--app-bg)] text-[var(--app-text)] flex flex-col">
      <Navbar
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        unreadCount={unreadCount}
        streamStatus={streamStatus}
        onOpenTestModal={() => setTestModalOpen(true)}
        onEnablePush={() => void handleEnablePush()}
        pushEnabled={pushEnabled}
      />

      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6">
        {activeTab === 'notifications' ? (
          <div className="space-y-4">
            <NotificationFilters
              sources={sources}
              selectedSource={selectedSource}
              onSelectSource={setSelectedSource}
              unreadOnly={unreadOnly}
              onToggleUnreadOnly={() => setUnreadOnly((prev) => !prev)}
              selectedLevel={selectedLevel}
              onSelectLevel={setSelectedLevel}
              onMarkAllRead={() => void handleMarkAllRead()}
              onRefresh={() => {
                void fetchNotifications();
                void fetchUnreadCount();
              }}
              loading={loading}
              unreadCount={unreadCount}
            />

            {loading && notifications.length === 0 ? (
              <div className="flex items-center justify-center gap-2 rounded-2xl border border-[var(--app-border)] bg-[var(--app-card-bg)] p-12 text-xs text-[var(--app-text-muted)]">
                <LoaderCircle size={16} className="animate-spin text-indigo-400" />
                <span>Loading notifications...</span>
              </div>
            ) : notifications.length === 0 ? (
              <div className="rounded-2xl border border-[var(--app-border)] bg-[var(--app-card-bg)] p-12 text-center shadow-sm">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-500/10 text-indigo-400 mb-3 border border-indigo-500/20">
                  <Bell size={22} />
                </div>
                <h3 className="text-sm font-bold text-white">All caught up</h3>
                <p className="mt-1 text-xs text-neutral-400 max-w-sm mx-auto">
                  No notifications match your current filters. New alerts will show up here in real time.
                </p>
              </div>
            ) : (
              <ul className="space-y-3">
                {notifications.map((item) => (
                  <NotificationCard
                    key={item.id}
                    notification={item}
                    onMarkRead={(id) => void handleMarkRead(id)}
                    onDelete={(id) => void handleDelete(id)}
                  />
                ))}
              </ul>
            )}
          </div>
        ) : (
          <TokenManagerView />
        )}
      </main>

      <TestDispatchModal
        isOpen={testModalOpen}
        onClose={() => setTestModalOpen(false)}
        onSuccess={() => {
          void fetchNotifications();
          void fetchUnreadCount();
          void fetchSources();
        }}
      />
    </div>
  );
}
