import { useState } from 'react';
import { Send, X, AlertCircle } from 'lucide-react';
import { notificationService } from '../services/notificationService';
import type { NotificationLevel } from '../types/notification';
import { getErrorMessage } from '../services/api';

interface TestDispatchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function TestDispatchModal({ isOpen, onClose, onSuccess }: TestDispatchModalProps) {
  const [source, setSource] = useState('studyzen');
  const [title, setTitle] = useState('Approaching Exam: Distributed Systems');
  const [body, setBody] = useState('You have an upcoming exam scheduled tomorrow at 09:00 in Room A-12.');
  const [level, setLevel] = useState<NotificationLevel>('WARNING');
  const [url, setUrl] = useState('https://studyzen.frandm.dev/calendar');
  const [dedupKey, setDedupKey] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !body.trim()) return;

    setSubmitting(true);
    setError(null);
    try {
      await notificationService.sendTestNotification(
        {
          title: title.trim(),
          body: body.trim(),
          level,
          url: url.trim() || undefined,
          dedupKey: dedupKey.trim() || undefined,
        },
        source.trim() || undefined
      );
      onSuccess();
      onClose();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-xs">
      <div className="w-full max-w-md rounded-2xl border border-[var(--app-border)] bg-[var(--app-card-bg)] shadow-2xl overflow-hidden animate-rise">
        <header className="flex items-center justify-between border-b border-[var(--app-border)] px-5 py-4">
          <div className="flex items-center gap-2">
            <Send size={18} className="text-indigo-400" />
            <h2 className="text-sm font-bold text-white">Dispatch Test Notification</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-neutral-400 hover:bg-neutral-800 hover:text-white"
            aria-label="Close"
          >
            <X size={16} />
          </button>
        </header>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {error && (
            <div className="flex items-center gap-2 rounded-lg bg-rose-500/10 border border-rose-500/20 px-3 py-2 text-xs text-rose-400">
              <AlertCircle size={14} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-neutral-300 mb-1">
                Source Tag
              </label>
              <input
                type="text"
                value={source}
                onChange={(e) => setSource(e.target.value)}
                placeholder="studyzen, proxmox, nut..."
                className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3 py-2 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-300 mb-1">
                Severity Level
              </label>
              <select
                value={level}
                onChange={(e) => setLevel(e.target.value as NotificationLevel)}
                className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3 py-2 text-xs text-white focus:border-indigo-500 focus:outline-none"
              >
                <option value="INFO">INFO</option>
                <option value="SUCCESS">SUCCESS</option>
                <option value="WARNING">WARNING</option>
                <option value="ERROR">ERROR</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">
              Title
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Study session finished"
              className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3 py-2 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">
              Body Message
            </label>
            <textarea
              rows={3}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Details about this alert..."
              className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3 py-2 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none resize-none"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">
              URL (Optional)
            </label>
            <input
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://studyzen.frandm.dev"
              className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3 py-2 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1">
              Deduplication Key (Optional)
            </label>
            <input
              type="text"
              value={dedupKey}
              onChange={(e) => setDedupKey(e.target.value)}
              placeholder="e.g. exam-alert-101"
              className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3 py-2 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div className="pt-2 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-[var(--app-border)] px-4 py-2 text-xs font-medium text-neutral-400 hover:bg-neutral-800 hover:text-white transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-500 transition-colors disabled:opacity-50 shadow-md shadow-indigo-600/20"
            >
              <Send size={13} />
              <span>{submitting ? 'Sending...' : 'Send Alert'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
