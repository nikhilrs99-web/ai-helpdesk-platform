import React from 'react';

export default function TicketList() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Support Tickets</h1>
      <div className="bg-white rounded shadow p-4">
        <p className="text-gray-500">Ticket list is wired to API Gateway /api/tickets</p>
      </div>
    </div>
  );
}
