import { useState } from 'react';
import { AlertCircle, Bell, LoaderCircle, Lock } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../services/api';

export function LoginPage() {
  const { login } = useAuth();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) return;

    setLoading(true);
    setError(null);
    try {
      await login({ username: username.trim(), password });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-4 bg-[var(--app-bg)]">
      <div className="w-full max-w-sm rounded-3xl border border-[var(--app-border)] bg-[var(--app-card-bg)] p-6 sm:p-8 shadow-2xl animate-rise">
        <div className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-500/10 text-[var(--accent-color)] border border-indigo-500/20 shadow-md">
            <Bell size={24} />
          </div>
          <h1 className="mt-4 text-lg font-bold text-white tracking-tight">Notification Center</h1>
          <p className="mt-1 text-xs text-[var(--app-text-muted)]">
            Admin access to manage notifications and API tokens
          </p>
        </div>

        {error && (
          <div className="mt-5 flex items-start gap-2.5 rounded-xl bg-rose-500/10 border border-rose-500/20 p-3 text-xs text-rose-400">
            <AlertCircle size={15} className="shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1.5">
              Username
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3.5 py-2.5 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none transition-colors"
              required
              autoFocus
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1.5">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3.5 py-2.5 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none transition-colors"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 flex items-center justify-center gap-2 rounded-xl bg-indigo-600 py-2.5 text-xs font-semibold text-white hover:bg-indigo-500 transition-all disabled:opacity-50 shadow-md shadow-indigo-600/20"
          >
            {loading ? (
              <LoaderCircle size={15} className="animate-spin" />
            ) : (
              <>
                <Lock size={13} />
                <span>Sign In</span>
              </>
            )}
          </button>
        </form>

        <p className="mt-6 text-center text-[11px] text-neutral-500">
          Protected single-user administration
        </p>
      </div>
    </div>
  );
}
