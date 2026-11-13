import { CheckCheck, Filter, RotateCw } from 'lucide-react';
import type { NotificationLevel } from '../types/notification';

interface NotificationFiltersProps {
  sources: string[];
  selectedSource: string;
  onSelectSource: (source: string) => void;
  unreadOnly: boolean;
  onToggleUnreadOnly: () => void;
  selectedLevel: NotificationLevel | '';
  onSelectLevel: (level: NotificationLevel | '') => void;
  onMarkAllRead: () => void;
  onRefresh: () => void;
  loading: boolean;
  unreadCount: number;
}

export function NotificationFilters({
  sources,
  selectedSource,
  onSelectSource,
  unreadOnly,
  onToggleUnreadOnly,
  selectedLevel,
  onSelectLevel,
  onMarkAllRead,
  onRefresh,
  loading,
  unreadCount,
}: NotificationFiltersProps) {
  return (
    <div className="flex flex-col gap-2.5 sm:gap-3 rounded-2xl border border-[var(--app-border)] bg-[var(--app-card-bg)] p-3 sm:p-4 shadow-sm">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 sm:gap-3">
        <div className="flex items-center justify-between sm:justify-start gap-2 flex-wrap">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-neutral-400 mr-1">
            <Filter size={14} />
            <span>Filters</span>
          </div>

          <button
            type="button"
            onClick={onToggleUnreadOnly}
            className={`rounded-lg px-2.5 py-1.5 sm:py-1 text-xs font-medium border transition-colors ${
              unreadOnly
                ? 'border-indigo-500/40 bg-indigo-500/20 text-indigo-300'
                : 'border-[var(--app-border)] bg-neutral-900/60 text-neutral-400 hover:text-white'
            }`}
          >
            Unread only
          </button>

          <div className="flex sm:hidden items-center gap-1.5 ml-auto">
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={onMarkAllRead}
                disabled={loading}
                className="flex items-center gap-1 rounded-lg border border-[var(--app-border)] bg-neutral-900/80 px-2 py-1 text-[11px] font-medium text-neutral-300 hover:bg-neutral-800 hover:text-white transition-colors disabled:opacity-50"
              >
                <CheckCheck size={13} className="text-emerald-400" />
                <span>Mark read</span>
              </button>
            )}

            <button
              type="button"
              onClick={onRefresh}
              disabled={loading}
              className="flex h-7 w-7 items-center justify-center rounded-lg border border-[var(--app-border)] bg-neutral-900/80 text-neutral-400 hover:bg-neutral-800 hover:text-white transition-colors disabled:opacity-50"
              title="Refresh notifications"
              aria-label="Refresh notifications"
            >
              <RotateCw size={13} className={loading ? 'animate-spin' : ''} />
            </button>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-2 sm:flex sm:items-center sm:gap-2">
          <select
            value={selectedLevel}
            onChange={(e) => onSelectLevel(e.target.value as NotificationLevel | '')}
            className="w-full sm:w-auto rounded-lg border border-[var(--app-border)] bg-neutral-900/80 px-2.5 py-1.5 sm:py-1 text-xs font-medium text-neutral-300 focus:border-indigo-500 focus:outline-none"
            aria-label="Filter by level"
          >
            <option value="">All Levels</option>
            <option value="INFO">Info</option>
            <option value="SUCCESS">Success</option>
            <option value="WARNING">Warning</option>
            <option value="ERROR">Error</option>
          </select>

          <select
            value={selectedSource}
            onChange={(e) => onSelectSource(e.target.value)}
            className="w-full sm:w-auto rounded-lg border border-[var(--app-border)] bg-neutral-900/80 px-2.5 py-1.5 sm:py-1 text-xs font-medium text-neutral-300 focus:border-indigo-500 focus:outline-none capitalize"
            aria-label="Filter by source"
          >
            <option value="">All Sources</option>
            {sources.map((src) => (
              <option key={src} value={src}>
                {src}
              </option>
            ))}
          </select>
        </div>

        <div className="hidden sm:flex items-center gap-2">
          {unreadCount > 0 && (
            <button
              type="button"
              onClick={onMarkAllRead}
              disabled={loading}
              className="flex items-center gap-1.5 rounded-lg border border-[var(--app-border)] bg-neutral-900/80 px-3 py-1.5 text-xs font-medium text-neutral-300 hover:bg-neutral-800 hover:text-white transition-colors disabled:opacity-50"
            >
              <CheckCheck size={14} className="text-emerald-400" />
              <span>Mark all read</span>
            </button>
          )}

          <button
            type="button"
            onClick={onRefresh}
            disabled={loading}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-[var(--app-border)] bg-neutral-900/80 text-neutral-400 hover:bg-neutral-800 hover:text-white transition-colors disabled:opacity-50"
            title="Refresh notifications"
            aria-label="Refresh notifications"
          >
            <RotateCw size={14} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>
    </div>
  );
}
