import type { TicketStatus } from '../api/types';

const STATUS_STYLES: Record<TicketStatus, string> = {
  OPEN: 'bg-blue-100 text-blue-700',
  AI_TRIAGED: 'bg-purple-100 text-purple-700',
  ASSIGNED: 'bg-indigo-100 text-indigo-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-700',
  WAITING_FOR_CUSTOMER: 'bg-orange-100 text-orange-700',
  RESOLVED: 'bg-emerald-100 text-emerald-700',
  CLOSED: 'bg-slate-200 text-slate-600',
};

export default function StatusBadge({ status }: { status: TicketStatus }) {
  return (
    <span className={`inline-block px-2.5 py-1 rounded-full text-xs font-medium ${STATUS_STYLES[status]}`}>
      {status.replaceAll('_', ' ')}
    </span>
  );
}
