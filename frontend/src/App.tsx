import React from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import TicketList from './components/TicketList';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-100 flex">
        <nav className="w-64 bg-slate-800 text-white p-4">
          <h2 className="text-xl font-bold mb-6">AI Helpdesk</h2>
          <ul>
            <li className="mb-2"><Link to="/" className="hover:text-blue-300">Dashboard</Link></li>
            <li className="mb-2"><Link to="/tickets" className="hover:text-blue-300">Tickets</Link></li>
            <li className="mb-2"><Link to="/kb" className="hover:text-blue-300">Knowledge Base</Link></li>
          </ul>
        </nav>
        <main className="flex-1">
          <Routes>
            <Route path="/" element={<div className="p-6">Dashboard (Analytics UI)</div>} />
            <Route path="/tickets" element={<TicketList />} />
            <Route path="/kb" element={<div className="p-6">KB Search Page</div>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
