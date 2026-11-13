import { useEffect, useState } from 'react';
import { AlertCircle, Check, Copy, Key, Plus, ShieldAlert, Trash2, X } from 'lucide-react';
import { tokenService } from '../services/tokenService';
import type { ApiToken, CreatedTokenResult } from '../types/token';
import { getErrorMessage } from '../services/api';
import { formatRelativeTime } from '../utils/format';

export function TokenManagerView() {
  const [tokens, setTokens] = useState<ApiToken[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createdResult, setCreatedResult] = useState<CreatedTokenResult | null>(null);

  const [name, setName] = useState('');
  const [source, setSource] = useState('');
  const [scopeNotify, setScopeNotify] = useState(true);
  const [scopeRead, setScopeRead] = useState(false);
  const [creating, setCreating] = useState(false);
  const [copied, setCopied] = useState(false);

  const loadTokens = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await tokenService.listTokens();
      setTokens(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadTokens();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !source.trim()) return;

    const scopes: string[] = [];
    if (scopeNotify) scopes.push('notify');
    if (scopeRead) scopes.push('read');

    if (scopes.length === 0) {
      setError('Please select at least one scope');
      return;
    }

    setCreating(true);
    setError(null);
    try {
      const res = await tokenService.createToken({
        name: name.trim(),
        source: source.trim().toLowerCase(),
        scopes,
      });
      setCreatedResult(res);
      setCreateModalOpen(false);
      setName('');
      setSource('');
      setScopeNotify(true);
      setScopeRead(false);
      void loadTokens();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (id: number) => {
    try {
      await tokenService.revokeToken(id);
      void loadTokens();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await tokenService.deleteToken(id);
      void loadTokens();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const copyToClipboard = async (text: string) => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-white">API Tokens</h1>
          <p className="mt-1 text-xs text-[var(--app-text-muted)]">
            Services authenticate with Bearer tokens to ingest or read notifications.
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setError(null);
            setCreateModalOpen(true);
          }}
          className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-indigo-500 transition-colors shadow-sm shadow-indigo-600/20"
        >
          <Plus size={15} />
          <span>Generate New Token</span>
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl bg-rose-500/10 border border-rose-500/20 p-3 text-xs text-rose-400">
          <AlertCircle size={15} className="shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {createdResult && (
        <div className="rounded-2xl border border-indigo-500/30 bg-gradient-to-r from-indigo-950/30 to-[var(--app-card-bg)] p-5 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-indigo-400 font-semibold text-sm">
              <ShieldAlert size={18} />
              <span>Token Generated Successfully</span>
            </div>
            <button
              type="button"
              onClick={() => setCreatedResult(null)}
              className="text-neutral-400 hover:text-white"
            >
              <X size={16} />
            </button>
          </div>

          <p className="text-xs text-neutral-300">
            Copy this token now for <strong className="text-white">{createdResult.token.name}</strong>. You will not be able to see it again!
          </p>

          <div className="flex items-center gap-2 rounded-xl border border-indigo-500/30 bg-neutral-950 px-3 py-2 font-mono text-xs text-indigo-300 select-all overflow-x-auto">
            <span className="flex-1 truncate">{createdResult.rawToken}</span>
            <button
              type="button"
              onClick={() => void copyToClipboard(createdResult.rawToken)}
              className="flex items-center gap-1.5 rounded-lg bg-indigo-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-indigo-500 transition-colors shrink-0"
            >
              {copied ? <Check size={13} /> : <Copy size={13} />}
              <span>{copied ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
        </div>
      )}

      <div className="rounded-2xl border border-[var(--app-border)] bg-[var(--app-card-bg)] overflow-hidden shadow-sm">
        {loading && tokens.length === 0 ? (
          <div className="p-8 text-center text-xs text-[var(--app-text-muted)]">
            Loading tokens...
          </div>
        ) : tokens.length === 0 ? (
          <div className="p-12 text-center">
            <Key size={28} className="mx-auto text-neutral-600 mb-3" />
            <p className="text-sm font-semibold text-white">No API Tokens generated</p>
            <p className="mt-1 text-xs text-neutral-400">
              Generate a token for StudyZen, Proxmox, NUT, or Home to start sending alerts.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-[var(--app-border)] bg-neutral-900/50 text-neutral-400 font-semibold">
                <tr>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Source Tag</th>
                  <th className="px-4 py-3">Token Prefix</th>
                  <th className="px-4 py-3">Scopes</th>
                  <th className="px-4 py-3">Last Used</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--app-border)]">
                {tokens.map((token) => (
                  <tr key={token.id} className="hover:bg-neutral-800/20 transition-colors">
                    <td className="px-4 py-3 font-semibold text-white">{token.name}</td>
                    <td className="px-4 py-3">
                      <span className="rounded-md bg-neutral-800 px-2 py-0.5 text-[11px] font-mono text-neutral-300">
                        {token.source}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-mono text-neutral-400">{token.tokenPrefix}...</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
                        {token.scopes.map((s) => (
                          <span
                            key={s}
                            className="rounded bg-indigo-500/10 border border-indigo-500/20 px-1.5 py-0.5 text-[10px] font-medium text-indigo-400"
                          >
                            {s}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-neutral-400">
                      {token.lastUsedAt ? formatRelativeTime(token.lastUsedAt) : 'Never'}
                    </td>
                    <td className="px-4 py-3">
                      {token.active ? (
                        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-400">
                          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
                          Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-rose-400">
                          <span className="h-1.5 w-1.5 rounded-full bg-rose-400" />
                          Revoked
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        {token.active && (
                          <button
                            type="button"
                            onClick={() => void handleRevoke(token.id)}
                            className="rounded-lg px-2 py-1 text-[11px] font-medium text-neutral-400 hover:bg-neutral-800 hover:text-amber-400 transition-colors"
                          >
                            Revoke
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => void handleDelete(token.id)}
                          className="flex h-7 w-7 items-center justify-center rounded-lg text-neutral-400 hover:bg-rose-500/15 hover:text-rose-400 transition-colors"
                          title="Delete token"
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {createModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-xs">
          <div className="w-full max-w-md rounded-2xl border border-[var(--app-border)] bg-[var(--app-card-bg)] shadow-2xl overflow-hidden animate-rise">
            <header className="flex items-center justify-between border-b border-[var(--app-border)] px-5 py-4">
              <div className="flex items-center gap-2">
                <Key size={18} className="text-indigo-400" />
                <h2 className="text-sm font-bold text-white">Generate API Token</h2>
              </div>
              <button
                type="button"
                onClick={() => setCreateModalOpen(false)}
                className="rounded-lg p-1 text-neutral-400 hover:bg-neutral-800 hover:text-white"
              >
                <X size={16} />
              </button>
            </header>

            <form onSubmit={handleCreate} className="p-5 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-neutral-300 mb-1">
                  Token Name
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. StudyZen Exam Notifier"
                  className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3 py-2 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-300 mb-1">
                  Source Identifier
                </label>
                <input
                  type="text"
                  value={source}
                  onChange={(e) => setSource(e.target.value)}
                  placeholder="e.g. studyzen, proxmox, nut, home"
                  className="w-full rounded-xl border border-[var(--app-border)] bg-neutral-900/80 px-3 py-2 text-xs text-white placeholder-neutral-500 focus:border-indigo-500 focus:outline-none font-mono"
                  required
                />
                <p className="mt-1 text-[11px] text-neutral-500">
                  Every alert sent with this token will have its source set to this value automatically.
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-300 mb-2">
                  Permissions (Scopes)
                </label>
                <div className="space-y-2">
                  <label className="flex items-center gap-2.5 text-xs text-neutral-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={scopeNotify}
                      onChange={(e) => setScopeNotify(e.target.checked)}
                      className="rounded border-neutral-700 bg-neutral-900 text-indigo-600 focus:ring-0"
                    />
                    <span>
                      <strong>notify</strong> — Ingest new notifications (<code className="text-neutral-400">POST /notifications</code>)
                    </span>
                  </label>

                  <label className="flex items-center gap-2.5 text-xs text-neutral-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={scopeRead}
                      onChange={(e) => setScopeRead(e.target.checked)}
                      className="rounded border-neutral-700 bg-neutral-900 text-indigo-600 focus:ring-0"
                    />
                    <span>
                      <strong>read</strong> — Read notifications and listen to SSE stream (<code className="text-neutral-400">Home dashboard</code>)
                    </span>
                  </label>
                </div>
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setCreateModalOpen(false)}
                  className="rounded-xl border border-[var(--app-border)] px-4 py-2 text-xs font-medium text-neutral-400 hover:bg-neutral-800 hover:text-white transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-500 transition-colors disabled:opacity-50"
                >
                  <Key size={13} />
                  <span>{creating ? 'Generating...' : 'Generate Token'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
