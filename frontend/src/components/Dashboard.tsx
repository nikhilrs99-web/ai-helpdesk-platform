import React, { useEffect, useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const dummyData = [
  { name: 'Mon', tickets: 40, resolved: 24 },
  { name: 'Tue', tickets: 30, resolved: 13 },
  { name: 'Wed', tickets: 20, resolved: 38 },
  { name: 'Thu', tickets: 27, resolved: 39 },
  { name: 'Fri', tickets: 18, resolved: 48 },
  { name: 'Sat', tickets: 23, resolved: 38 },
  { name: 'Sun', tickets: 34, resolved: 43 },
];

export default function Dashboard() {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Simulate API fetch delay
    setTimeout(() => setLoading(false), 800);
  }, []);

  if (loading) {
    return <div className="p-6 text-gray-500 animate-pulse">Loading dashboard metrics...</div>;
  }

  return (
    <div className="p-6 md:p-8 lg:p-10 w-full max-w-7xl mx-auto">
      <h1 className="text-2xl font-bold mb-6 text-slate-800">Analytics Dashboard</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white p-6 rounded-lg shadow border border-slate-100">
          <h3 className="text-slate-500 text-sm font-medium">SLA Compliance</h3>
          <p className="text-3xl font-bold text-green-600 mt-2">94.2%</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow border border-slate-100">
          <h3 className="text-slate-500 text-sm font-medium">AI Auto-Resolution</h3>
          <p className="text-3xl font-bold text-blue-600 mt-2">28.5%</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow border border-slate-100">
          <h3 className="text-slate-500 text-sm font-medium">Draft Acceptance Rate</h3>
          <p className="text-3xl font-bold text-indigo-600 mt-2">76.1%</p>
        </div>
      </div>

      <div className="bg-white p-6 rounded-lg shadow border border-slate-100 h-96">
        <h3 className="text-lg font-semibold mb-4 text-slate-800">Weekly Ticket Volume</h3>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={dummyData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
            <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#64748b' }} />
            <YAxis axisLine={false} tickLine={false} tick={{ fill: '#64748b' }} />
            <Tooltip contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
            <Legend />
            <Line type="monotone" dataKey="tickets" name="Incoming" stroke="#3b82f6" strokeWidth={3} activeDot={{ r: 8 }} />
            <Line type="monotone" dataKey="resolved" name="Resolved" stroke="#10b981" strokeWidth={3} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
