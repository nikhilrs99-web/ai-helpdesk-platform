import { useEffect, useState } from 'react';
import { escalationsApi } from '../api/escalations';
import { ApiRequestError } from '../api/client';
import type { EscalationResponse } from '../api/types';
import { useAuth } from '../auth/AuthContext';

const STATUS_STYLES: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-slate-200 text-slate-700',
};

// Escalations are a human-in-the-loop gate: anyone with access to the ticket can request
// one, but only agents/admins can approve or reject it (enforced server-side too).
export default function EscalationPanel({ ticketId }: { ticketId: string }) {
  const { hasRole } = useAuth();
  const isAgent = hasRole('agent') || hasRole('admin');
  const [items, setItems] = useState<EscalationResponse[]>([]);
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function load() {
    escalationsApi
      .list(ticketId)
      .then(setItems)
      .catch((err) => setError(err instanceof ApiRequestError ? err.message : 'Failed to load escalations.'));
  }

  useEffect(load, [ticketId]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      load();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Action failed.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="border-t border-slate-100 pt-4 mt-4">
      <h2 className="text-sm font-semibold text-slate-700 mb-2">Escalations</h2>
      {error && <div className="bg-red-50 text-red-700 border border-red-200 rounded-lg p-2 text-sm mb-2">{error}</div>}

      {items.length === 0 && <p className="text-sm text-slate-400 mb-2">No escalations.</p>}
      <ul className="space-y-2 mb-3">
        {items.map((e) => (
          <li key={e.id} className="text-sm border border-slate-200 rounded-lg p-3 flex items-start justify-between gap-3">
            <div>
              <div className="text-slate-800">{e.reason}</div>
              <div className="text-xs text-slate-500">
                requested by {e.requestedBy}
                {e.decidedBy ? ` · decided by ${e.decidedBy}` : ''}
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <span className={`text-xs font-medium px-2 py-0.5 rounded ${STATUS_STYLES[e.status]}`}>{e.status}</span>
              {isAgent && e.status === 'PENDING' && (
                <>
                  <button
                    disabled={busy}
                    onClick={() => run(() => escalationsApi.approve(ticketId, e.id))}
                    className="text-xs bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white px-2 py-1 rounded"
                  >
                    Approve
                  </button>
                  <button
                    disabled={busy}
                    onClick={() => run(() => escalationsApi.reject(ticketId, e.id))}
                    className="text-xs border border-slate-300 hover:bg-slate-50 disabled:opacity-50 px-2 py-1 rounded"
                  >
                    Reject
                  </button>
                </>
              )}
            </div>
          </li>
        ))}
      </ul>

      <div className="flex gap-2">
        <input
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Why does this need escalating?"
          className="flex-1 border border-slate-300 rounded-lg px-3 py-2 text-sm"
        />
        <button
          disabled={busy || !reason.trim()}
          onClick={() =>
            run(async () => {
              await escalationsApi.create(ticketId, reason.trim());
              setReason('');
            })
          }
          className="bg-slate-800 hover:bg-slate-900 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg"
        >
          Request escalation
        </button>
      </div>
    </div>
  );
}
