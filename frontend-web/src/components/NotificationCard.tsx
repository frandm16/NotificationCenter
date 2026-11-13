import { Check, ExternalLink, Trash2 } from 'lucide-react';
import type { AppNotification } from '../types/notification';
import { formatRelativeTime, getLevelBadgeStyle } from '../utils/format';

interface NotificationCardProps {
  notification: AppNotification;
  onMarkRead: (id: number) => void;
  onDelete: (id: number) => void;
}

export function NotificationCard({ notification, onMarkRead, onDelete }: NotificationCardProps) {
  const badge = getLevelBadgeStyle(notification.level);

  return (
    <li
      className={`group relative flex flex-col rounded-2xl border transition-all duration-200 p-3.5 sm:p-4.5 ${
        notification.read
          ? 'border-[var(--app-border)] bg-[var(--app-card-bg)]/80 hover:bg-[var(--app-card-hover)]'
          : 'border-indigo-500/30 bg-gradient-to-r from-indigo-950/25 via-[var(--app-card-bg)] to-[var(--app-card-bg)] shadow-[0_0_15px_rgba(99,102,241,0.08)]'
      }`}
    >
      <div className="flex items-start justify-between gap-2.5">
        <div className="flex flex-wrap items-center gap-1.5 sm:gap-2">
          {!notification.read && (
            <span
              className="h-2 w-2 rounded-full bg-[var(--accent-color)] shadow-[0_0_6px_var(--accent-color)]"
              title="Unread"
              aria-label="Unread notification"
            />
          )}

          <span
            className={`inline-flex items-center gap-1.5 rounded-md px-2 py-0.5 text-[10px] sm:text-[11px] font-semibold uppercase tracking-wider border ${badge.bg} ${badge.text} ${badge.border}`}
          >
            <span className={`h-1.5 w-1.5 rounded-full ${badge.dot}`} />
            {notification.level}
          </span>

          <span className="rounded-md bg-neutral-800/90 px-2 py-0.5 text-[10px] sm:text-[11px] font-medium text-neutral-300 border border-neutral-700/60 font-mono">
            {notification.source}
          </span>
        </div>

        <div className="flex items-center gap-1 sm:gap-2 shrink-0">
          <time
            dateTime={notification.createdAt}
            className="text-[10px] sm:text-[11px] text-[var(--app-text-muted)] whitespace-nowrap"
          >
            {formatRelativeTime(notification.createdAt)}
          </time>

          <div className="flex items-center gap-0.5 sm:gap-1 opacity-100 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity">
            {!notification.read && (
              <button
                type="button"
                onClick={() => onMarkRead(notification.id)}
                className="flex h-7 w-7 sm:h-7 sm:w-7 items-center justify-center rounded-lg text-neutral-400 hover:bg-indigo-500/15 hover:text-indigo-300 transition-colors"
                title="Mark as read"
                aria-label="Mark as read"
              >
                <Check size={14} />
              </button>
            )}

            <button
              type="button"
              onClick={() => onDelete(notification.id)}
              className="flex h-7 w-7 sm:h-7 sm:w-7 items-center justify-center rounded-lg text-neutral-400 hover:bg-rose-500/15 hover:text-rose-400 transition-colors"
              title="Delete notification"
              aria-label="Delete notification"
            >
              <Trash2 size={14} />
            </button>
          </div>
        </div>
      </div>

      <div className="mt-2 sm:mt-2.5">
        <h3 className="text-sm font-semibold text-white tracking-tight">{notification.title}</h3>
        <p className="mt-1 text-xs leading-relaxed text-neutral-300 whitespace-pre-wrap break-words">
          {notification.body}
        </p>
      </div>

      {notification.url && (
        <div className="mt-2.5 pt-2 border-t border-neutral-800/60 flex items-center justify-between">
          <a
            href={notification.url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-indigo-400 hover:text-indigo-300 hover:underline transition-colors py-0.5"
          >
            <span>Open Link</span>
            <ExternalLink size={12} />
          </a>
        </div>
      )}
    </li>
  );
}
