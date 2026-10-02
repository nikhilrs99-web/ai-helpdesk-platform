import { useState } from 'react';
import { Send } from 'lucide-react';
import { assistantApi } from '../api/assistant';
import { ApiRequestError } from '../api/client';

interface Message {
  role: 'user' | 'assistant';
  text: string;
}

// Chat with ai-service's tool-calling agent. The agent acts with *your* token, so it can only
// see and do what you could already do yourself; escalations it creates still wait for a
// human to approve them (see the ticket's Escalations panel).
export default function Assistant() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);

  async function send() {
    const text = input.trim();
    if (!text || sending) return;
    setMessages((m) => [...m, { role: 'user', text }]);
    setInput('');
    setSending(true);
    try {
      const reply = await assistantApi.chat(text);
      setMessages((m) => [...m, { role: 'assistant', text: reply }]);
    } catch (err) {
      const msg = err instanceof ApiRequestError ? err.message : 'The assistant is unavailable.';
      setMessages((m) => [...m, { role: 'assistant', text: msg }]);
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="p-6 md:p-8 lg:p-10 w-full max-w-3xl mx-auto flex flex-col h-[calc(100vh-2rem)]">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">AI Assistant</h1>
      <p className="text-sm text-slate-500 mb-4">
        Ask about a ticket&apos;s status or SLA, search the knowledge base, or request an escalation.
      </p>

      <div className="flex-1 overflow-y-auto bg-white rounded-lg border border-slate-200 p-4 space-y-3" aria-live="polite">
        {messages.length === 0 && <p className="text-slate-400 text-sm">Say hello to get started.</p>}
        {messages.map((m, i) => (
          <div key={i} className={m.role === 'user' ? 'text-right' : 'text-left'}>
            <span
              className={`inline-block max-w-[85%] whitespace-pre-wrap rounded-lg px-3 py-2 text-sm ${
                m.role === 'user' ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-800'
              }`}
            >
              {m.text}
            </span>
          </div>
        ))}
        {sending && <div className="text-sm text-slate-400 animate-pulse">Thinking...</div>}
      </div>

      <form
        className="flex gap-2 mt-3"
        onSubmit={(e) => {
          e.preventDefault();
          void send();
        }}
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask the assistant..."
          className="flex-1 border border-slate-300 rounded-lg px-3 py-2 text-sm"
        />
        <button
          type="submit"
          disabled={sending || !input.trim()}
          className="bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white px-4 py-2 rounded-lg flex items-center gap-1 text-sm"
        >
          <Send size={14} /> Send
        </button>
      </form>
    </div>
  );
}
