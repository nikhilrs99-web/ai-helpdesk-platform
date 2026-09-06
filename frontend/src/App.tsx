import React from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import TicketList from './components/TicketList';
import Dashboard from './components/Dashboard';
import AgentPresence from './components/AgentPresence';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-slate-50 flex flex-col md:flex-row">
        <nav className="w-full md:w-64 bg-slate-900 text-white flex flex-col">
          <div className="p-6">
            <h2 className="text-xl font-bold mb-2 text-blue-400">AI Helpdesk</h2>
            <AgentPresence />
          </div>
          <ul className="flex-1 px-4 mt-4 space-y-2">
            <li>
              <Link to="/" className="block py-2.5 px-4 rounded transition duration-200 hover:bg-slate-800 hover:text-white">
                Analytics Dashboard
              </Link>
            </li>
            <li>
              <Link to="/tickets" className="block py-2.5 px-4 rounded transition duration-200 hover:bg-slate-800 hover:text-white">
                Support Tickets
              </Link>
            </li>
            <li>
              <Link to="/kb" className="block py-2.5 px-4 rounded transition duration-200 hover:bg-slate-800 hover:text-white">
                Knowledge Base
              </Link>
            </li>
          </ul>
        </nav>
        <main className="flex-1 overflow-x-hidden overflow-y-auto bg-slate-50">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/tickets" element={<TicketList />} />
            <Route path="/kb" element={<div className="p-8">KB Search Placeholder</div>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;

export default App;
