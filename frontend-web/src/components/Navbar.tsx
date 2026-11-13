import { Bell, Key, LogOut, Radio, Send, BellRing } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import type { SseStatus } from '../hooks/useSseStream';

interface NavbarProps {
  activeTab: 'notifications' | 'tokens';
  onSelectTab: (tab: 'notifications' | 'tokens') => void;
  unreadCount: number;
  streamStatus: SseStatus;
  onOpenTestModal: () => void;
  onEnablePush: () => void;
  pushEnabled: boolean;
}

export function Navbar({
  activeTab,
  onSelectTab,
  unreadCount,
  streamStatus,
  onOpenTestModal,
  onEnablePush,
  pushEnabled,
}: NavbarProps) {
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-40 border-b border-[var(--app-border)] bg-[var(--app-bg)]/80 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-500/10 text-[var(--accent-color)] border border-indigo-500/20 shadow-sm">
              <Bell size={20} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold tracking-tight text-white text-base">Notification Center</span>
                <span className="rounded-full bg-neutral-800 px-2 py-0.5 text-[10px] font-semibold text-neutral-400 border border-neutral-700/50">
                  v1.0
                </span>
              </div>
              <div className="flex items-center gap-1.5 mt-0.5">
                <span
                  className={`h-2 w-2 rounded-full transition-colors ${
                    streamStatus === 'connected'
                      ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.6)] animate-pulse'
                      : streamStatus === 'connecting'
                      ? 'bg-amber-400'
                      : 'bg-neutral-600'
                  }`}
                />
                <span className="text-[11px] text-[var(--app-text-muted)] font-medium capitalize">
                  {streamStatus === 'connected' ? 'Live Stream Active' : streamStatus}
                </span>
              </div>
            </div>
          </div>

          <nav className="hidden sm:flex items-center gap-1 ml-4 border-l border-[var(--app-border)] pl-4">
            <button
              type="button"
              onClick={() => onSelectTab('notifications')}
              className={`flex items-center gap-2 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
                activeTab === 'notifications'
                  ? 'bg-indigo-500/15 text-[var(--accent-color)] border border-indigo-500/25 shadow-sm'
                  : 'text-[var(--app-text-muted)] hover:bg-neutral-800/60 hover:text-white'
              }`}
            >
              <Radio size={14} />
              <span>Notifications</span>
              {unreadCount > 0 && (
                <span className="flex h-4 min-w-[16px] items-center justify-center rounded-full bg-indigo-500 px-1 text-[10px] font-bold text-white">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('tokens')}
              className={`flex items-center gap-2 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
                activeTab === 'tokens'
                  ? 'bg-indigo-500/15 text-[var(--accent-color)] border border-indigo-500/25 shadow-sm'
                  : 'text-[var(--app-text-muted)] hover:bg-neutral-800/60 hover:text-white'
              }`}
            >
              <Key size={14} />
              <span>API Tokens</span>
            </button>
          </nav>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={onOpenTestModal}
            className="flex items-center gap-1.5 rounded-lg border border-[var(--app-border)] bg-neutral-900/80 px-2.5 py-1.5 text-xs font-medium text-neutral-300 transition-all hover:bg-neutral-800 hover:text-white hover:border-neutral-700"
            title="Dispatch a test notification"
          >
            <Send size={13} className="text-indigo-400" />
            <span className="hidden md:inline">Test Alert</span>
          </button>

          <button
            type="button"
            onClick={onEnablePush}
            className={`flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-medium transition-all ${
              pushEnabled
                ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400'
                : 'border-[var(--app-border)] bg-neutral-900/80 text-neutral-400 hover:text-white hover:border-neutral-700'
            }`}
            title={pushEnabled ? 'Web Push is active' : 'Enable Web Push notifications'}
          >
            <BellRing size={13} />
            <span className="hidden md:inline">{pushEnabled ? 'Push Active' : 'Enable Push'}</span>
          </button>

          {user && (
            <div className="flex items-center gap-2 pl-2 border-l border-[var(--app-border)]">
              <span className="hidden lg:inline text-xs font-medium text-neutral-400">
                {user.username}
              </span>
              <button
                type="button"
                onClick={() => void logout()}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-400 transition-colors hover:bg-neutral-800 hover:text-rose-400"
                title="Log out"
                aria-label="Log out"
              >
                <LogOut size={15} />
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="flex sm:hidden border-t border-[var(--app-border)] px-4 py-2 gap-2 bg-neutral-950/60">
        <button
          type="button"
          onClick={() => onSelectTab('notifications')}
          className={`flex-1 flex items-center justify-center gap-2 rounded-lg py-1.5 text-xs font-medium ${
            activeTab === 'notifications'
              ? 'bg-indigo-500/20 text-indigo-300 font-semibold'
              : 'text-neutral-400'
          }`}
        >
          <Radio size={13} />
          <span>Notifications</span>
          {unreadCount > 0 && (
            <span className="flex h-4 min-w-[16px] items-center justify-center rounded-full bg-indigo-500 px-1 text-[9px] font-bold text-white">
              {unreadCount}
            </span>
          )}
        </button>

        <button
          type="button"
          onClick={() => onSelectTab('tokens')}
          className={`flex-1 flex items-center justify-center gap-2 rounded-lg py-1.5 text-xs font-medium ${
            activeTab === 'tokens'
              ? 'bg-indigo-500/20 text-indigo-300 font-semibold'
              : 'text-neutral-400'
          }`}
        >
          <Key size={13} />
          <span>API Tokens</span>
        </button>
      </div>
    </header>
  );
}
