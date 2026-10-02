import { useEffect, useState } from 'react';
import { agentsApi } from '../api/agents';
import { useAuth } from '../auth/AuthContext';

export default function AgentPresence() {
  const { hasRole } = useAuth();
  const [isOnline, setIsOnline] = useState(false);
  const isAgent = hasRole('agent') || hasRole('admin');

  useEffect(() => {
    if (!isAgent) return;

    // AgentController.pingPresence (agent/admin only) marks this agent online for a TTL
    // window in Redis (AgentPresenceService) - there's no separate "get status" endpoint,
    // so a successful ping *is* the online signal, repeated before the TTL can expire.
    let cancelled = false;
    const ping = async () => {
      try {
        await agentsApi.ping();
        if (!cancelled) setIsOnline(true);
      } catch {
        if (!cancelled) setIsOnline(false);
      }
    };

    ping();
    const interval = setInterval(ping, 60000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [isAgent]);

  if (!isAgent) return null;

  return (
    <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-700 rounded-full w-max">
      <div className={`w-2.5 h-2.5 rounded-full ${isOnline ? 'bg-green-400' : 'bg-red-400'}`} />
      <span className="text-sm font-medium text-slate-200">
        {isOnline ? 'Online (Receiving Routing)' : 'Offline'}
      </span>
    </div>
  );
}
