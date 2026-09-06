import React, { useState, useEffect } from 'react';

export default function AgentPresence() {
  const [isOnline, setIsOnline] = useState(false);

  useEffect(() => {
    // Ping the backend presence API on mount and every 60 seconds
    const pingPresence = async () => {
      try {
        // Mocking API call to /api/agents/ping
        // await fetch('/api/agents/ping', { method: 'POST', headers: { Authorization: 'Bearer ...' } });
        setIsOnline(true);
      } catch (e) {
        setIsOnline(false);
      }
    };

    pingPresence();
    const interval = setInterval(pingPresence, 60000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-700 rounded-full w-max">
      <div className={`w-2.5 h-2.5 rounded-full ${isOnline ? 'bg-green-400' : 'bg-red-400'}`} />
      <span className="text-sm font-medium text-slate-200">
        {isOnline ? 'Online (Receiving Routing)' : 'Offline'}
      </span>
    </div>
  );
}
