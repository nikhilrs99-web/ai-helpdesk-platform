import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { ticketsApi } from '../api/tickets';
import { ApiRequestError } from '../api/client';
import type { TicketResponse } from '../api/types';
import StatusBadge from './StatusBadge';

export default function TicketList() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<{ content: TicketResponse[]; totalPages: number; first: boolean; last: boolean } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    ticketsApi
      .list(page)
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiRequestError ? err.message : 'Failed to load tickets.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page]);

  return (
    <div className="p-6 md:p-8 lg:p-10 w-full max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Support Tickets</h1>
        <Link
          to="/tickets/new"
          className="inline-flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition"
        >
          <Plus size={16} /> New Ticket
        </Link>
      </div>

      {loading && <div className="text-slate-500 animate-pulse">Loading tickets...</div>}
      {error && (
        <div className="bg-red-50 text-red-700 border border-red-200 rounded-lg p-4">{error}</div>
      )}

      {!loading && !error && data && (
        <>
          {data.content.length === 0 ? (
            <div className="bg-white rounded-lg shadow p-8 text-center text-slate-500 border border-slate-100">
              No tickets yet.
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow border border-slate-100 divide-y divide-slate-100 overflow-hidden">
              {data.content.map((ticket) => (
                <Link
                  key={ticket.id}
                  to={`/tickets/${ticket.id}`}
                  className="flex items-center justify-between gap-4 p-4 hover:bg-slate-50 transition"
                >
                  <div className="min-w-0">
                    <p className="font-medium text-slate-800 truncate">{ticket.subject}</p>
                    <p className="text-sm text-slate-500">
                      {ticket.category.replaceAll('_', ' ')} &middot; {ticket.routedTeam}
                    </p>
                  </div>
                  <StatusBadge status={ticket.status} />
                </Link>
              ))}
            </div>
          )}

          <div className="flex items-center justify-between mt-4 text-sm">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={data.first}
              className="px-3 py-1.5 rounded border border-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-100"
            >
              Previous
            </button>
            <span className="text-slate-500">
              Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={data.last}
              className="px-3 py-1.5 rounded border border-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-100"
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
}
