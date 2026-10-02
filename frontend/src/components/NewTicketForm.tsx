import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ticketsApi } from '../api/tickets';
import { ApiRequestError } from '../api/client';
import { TICKET_CATEGORIES, type TicketCategory } from '../api/types';

// Required metadata keys per category mirror services/ticket-service/.../tickettype/
// *TicketHandler.java exactly - the server rejects the request (400) if these are
// missing, so the form only needs to ask for what that category actually requires.
const CATEGORY_FIELDS: Partial<Record<TicketCategory, { key: string; label: string }[]>> = {
  BUG: [
    { key: 'browser', label: 'Browser' },
    { key: 'appVersion', label: 'App version' },
  ],
  BILLING: [{ key: 'invoiceId', label: 'Invoice ID' }],
};

export default function NewTicketForm() {
  const navigate = useNavigate();
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<TicketCategory>('HOW_TO');
  const [metadata, setMetadata] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const extraFields = CATEGORY_FIELDS[category] ?? [];

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const created = await ticketsApi.create({ subject, description, category, metadata });
      navigate(`/tickets/${created.id}`);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Failed to create ticket.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="p-6 md:p-8 lg:p-10 w-full max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6 text-slate-800">New Ticket</h1>
      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow border border-slate-100 p-6 space-y-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Category</label>
          <select
            value={category}
            onChange={(e) => {
              setCategory(e.target.value as TicketCategory);
              setMetadata({});
            }}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
          >
            {TICKET_CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c.replaceAll('_', ' ')}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Subject</label>
          <input
            required
            maxLength={200}
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Description</label>
          <textarea
            required
            maxLength={5000}
            rows={5}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>

        {extraFields.map((field) => (
          <div key={field.key}>
            <label className="block text-sm font-medium text-slate-700 mb-1">{field.label}</label>
            <input
              required
              value={metadata[field.key] ?? ''}
              onChange={(e) => setMetadata((m) => ({ ...m, [field.key]: e.target.value }))}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
        ))}

        {error && <div className="bg-red-50 text-red-700 border border-red-200 rounded-lg p-3 text-sm">{error}</div>}

        <button
          type="submit"
          disabled={submitting}
          className="bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg transition"
        >
          {submitting ? 'Submitting...' : 'Submit Ticket'}
        </button>
      </form>
    </div>
  );
}
