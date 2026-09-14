import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import TicketList from './components/TicketList';
import TicketDetail from './components/TicketDetail';
import NewTicketForm from './components/NewTicketForm';
import Dashboard from './components/Dashboard';
import KnowledgeBase from './components/KnowledgeBase';
import AgentPresence from './components/AgentPresence';
import { useAuth } from './auth/AuthContext';

function App() {
  const { username, logout } = useAuth();

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
          <div className="p-4 border-t border-slate-800 flex items-center justify-between">
            <span className="text-sm text-slate-300 truncate">{username}</span>
            <button
              onClick={logout}
              title="Sign out"
              className="text-slate-400 hover:text-white p-1.5 rounded hover:bg-slate-800 transition"
            >
              <LogOut size={16} />
            </button>
          </div>
        </nav>
        <main className="flex-1 overflow-x-hidden overflow-y-auto bg-slate-50">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/tickets" element={<TicketList />} />
            <Route path="/tickets/new" element={<NewTicketForm />} />
            <Route path="/tickets/:id" element={<TicketDetail />} />
            <Route path="/kb" element={<KnowledgeBase />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
