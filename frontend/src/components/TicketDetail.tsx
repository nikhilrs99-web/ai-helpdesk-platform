import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ticketsApi } from '../api/tickets';
import { ApiRequestError } from '../api/client';
import { TICKET_STATUSES, type TicketResponse, type TicketStatus } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import StatusBadge from './StatusBadge';

export default function TicketDetail() {
  const { id } = useParams<{ id: string }>();
  const { hasRole, subject } = useAuth();
  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editing, setEditing] = useState(false);
  const [editSubject, setEditSubject] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [nextStatus, setNextStatus] = useState<TicketStatus | ''>('');
  const [saving, setSaving] = useState(false);

  const isAgent = hasRole('agent') || hasRole('admin');

  function load() {
    if (!id) return;
    setLoading(true);
    setError(null);
    ticketsApi
      .get(id)
      .then((t) => {
        setTicket(t);
        setEditSubject(t.subject);
        setEditDescription(t.description);
      })
      .catch((err) => setError(err instanceof ApiRequestError ? err.message : 'Failed to load ticket.'))
      .finally(() => setLoading(false));
  }

  useEffect(load, [id]);

  async function saveEdit() {
    if (!id) return;
    setSaving(true);
    try {
      const updated = await ticketsApi.update(id, { subject: editSubject, description: editDescription });
      setTicket(updated);
      setEditing(false);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Failed to save changes.');
    } finally {
      setSaving(false);
    }
  }

  async function applyStatus() {
    if (!id || !nextStatus) return;
    setSaving(true);
    try {
      const updated = await ticketsApi.changeStatus(id, nextStatus);
      setTicket(updated);
      setNextStatus('');
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Failed to change status.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="p-8 text-slate-500 animate-pulse">Loading ticket...</div>;
  if (error && !ticket) return <div className="p-8 text-red-700">{error}</div>;
  if (!ticket) return null;

  const canEdit = isAgent || ticket.requesterId === subject;

  return (
    <div className="p-6 md:p-8 lg:p-10 w-full max-w-3xl mx-auto">
      <Link to="/tickets" className="text-sm text-blue-600 hover:underline">
        &larr; Back to tickets
      </Link>

      <div className="bg-white rounded-lg shadow border border-slate-100 p-6 mt-4">
        <div className="flex items-start justify-between gap-4 mb-4">
          {editing ? (
            <input
              value={editSubject}
              onChange={(e) => setEditSubject(e.target.value)}
              className="text-xl font-bold text-slate-800 border border-slate-300 rounded px-2 py-1 flex-1"
            />
          ) : (
            <h1 className="text-xl font-bold text-slate-800">{ticket.subject}</h1>
          )}
          <StatusBadge status={ticket.status} />
        </div>

        <p className="text-sm text-slate-500 mb-4">
          {ticket.category.replaceAll('_', ' ')} &middot; routed to {ticket.routedTeam} &middot; created{' '}
          {new Date(ticket.createdAt).toLocaleString()}
        </p>

        {editing ? (
          <textarea
            value={editDescription}
            onChange={(e) => setEditDescription(e.target.value)}
            rows={5}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm mb-4"
          />
        ) : (
          <p className="text-slate-700 whitespace-pre-wrap mb-4">{ticket.description}</p>
        )}

        {Object.keys(ticket.metadata).length > 0 && (
          <div className="text-sm text-slate-600 border-t border-slate-100 pt-3 mb-4">
            {Object.entries(ticket.metadata).map(([k, v]) => (
              <div key={k}>
                <span className="font-medium">{k}:</span> {v}
              </div>
            ))}
          </div>
        )}

        {error && <div className="bg-red-50 text-red-700 border border-red-200 rounded-lg p-3 text-sm mb-4">{error}</div>}

        {canEdit && (
          <div className="flex gap-2 border-t border-slate-100 pt-4">
            {editing ? (
              <>
                <button
                  onClick={saveEdit}
                  disabled={saving}
                  className="bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg"
                >
                  Save
                </button>
                <button
                  onClick={() => setEditing(false)}
                  className="border border-slate-300 text-sm font-medium px-4 py-2 rounded-lg hover:bg-slate-50"
                >
                  Cancel
                </button>
              </>
            ) : (
              <button
                onClick={() => setEditing(true)}
                className="border border-slate-300 text-sm font-medium px-4 py-2 rounded-lg hover:bg-slate-50"
              >
                Edit
              </button>
            )}
          </div>
        )}

        {isAgent && (
          <div className="flex items-center gap-2 border-t border-slate-100 pt-4 mt-4">
            <select
              value={nextStatus}
              onChange={(e) => setNextStatus(e.target.value as TicketStatus)}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
            >
              <option value="">Change status to...</option>
              {TICKET_STATUSES.filter((s) => s !== ticket.status).map((s) => (
                <option key={s} value={s}>
                  {s.replaceAll('_', ' ')}
                </option>
              ))}
            </select>
            <button
              onClick={applyStatus}
              disabled={!nextStatus || saving}
              className="bg-slate-800 hover:bg-slate-900 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg"
            >
              Apply
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
