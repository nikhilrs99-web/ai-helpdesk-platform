import { useEffect, useState } from 'react';
import { slaApi } from '../api/escalations';
import type { SlaStatusResponse } from '../api/types';

export default function SlaPanel({ ticketId }: { ticketId: string }) {
  const [sla, setSla] = useState<SlaStatusResponse | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    slaApi
      .get(ticketId)
      .then(setSla)
      .catch(() => setFailed(true));
  }, [ticketId]);

  if (failed) return <div className="text-sm text-slate-400">SLA status unavailable.</div>;
  if (!sla) return <div className="text-sm text-slate-400 animate-pulse">Loading SLA...</div>;
  if (!sla.targetConfigured) {
    return <div className="text-sm text-slate-500">No SLA target is configured for this category.</div>;
  }

  const tone = sla.breached ? 'bg-red-50 text-red-700 border-red-200' : 'bg-green-50 text-green-700 border-green-200';
  return (
    <div className={`text-sm border rounded-lg px-3 py-2 ${tone}`} data-testid="sla-panel">
      <span className="font-medium">First-response SLA ({sla.targetMinutes} min): </span>
      {sla.breached
        ? 'BREACHED'
        : `on track, ${sla.minutesRemaining} min remaining`}
      {sla.deadline && <span className="opacity-75"> &middot; deadline {new Date(sla.deadline).toLocaleString()}</span>}
    </div>
  );
}
