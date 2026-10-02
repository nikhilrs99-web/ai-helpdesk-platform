import { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { analyticsApi } from '../api/analytics';
import { ApiRequestError } from '../api/client';
import type { DashboardMetrics } from '../api/types';

export default function Dashboard() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    analyticsApi
      .getDashboardMetrics()
      .then(setMetrics)
      .catch((err) => setError(err instanceof ApiRequestError ? err.message : 'Failed to load metrics.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="p-6 text-gray-500 animate-pulse">Loading dashboard metrics...</div>;
  }
  if (error || !metrics) {
    return <div className="p-6 text-red-700">{error ?? 'No data available.'}</div>;
  }

  // Only two rates come out of analytics-service today (see AnalyticsController.getDashboardMetrics) -
  // there's no time-series endpoint, so this stays a same-snapshot comparison rather than a
  // fabricated weekly trend.
  const chartData = [
    { name: 'SLA Compliance', value: metrics.slaCompliancePercentage, fill: '#10b981' },
    { name: 'AI Auto-Resolution', value: metrics.aiResolutionPercentage, fill: '#3b82f6' },
  ];

  return (
    <div className="p-6 md:p-8 lg:p-10 w-full max-w-7xl mx-auto">
      <h1 className="text-2xl font-bold mb-6 text-slate-800">Analytics Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white p-6 rounded-lg shadow border border-slate-100">
          <h3 className="text-slate-500 text-sm font-medium">Total Tickets</h3>
          <p className="text-3xl font-bold text-slate-800 mt-2">{metrics.totalTickets}</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow border border-slate-100">
          <h3 className="text-slate-500 text-sm font-medium">SLA Compliance</h3>
          <p className="text-3xl font-bold text-green-600 mt-2">{metrics.slaCompliancePercentage}%</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow border border-slate-100">
          <h3 className="text-slate-500 text-sm font-medium">AI Auto-Resolution</h3>
          <p className="text-3xl font-bold text-blue-600 mt-2">{metrics.aiResolutionPercentage}%</p>
        </div>
      </div>

      <div className="bg-white p-6 rounded-lg shadow border border-slate-100 h-80">
        <h3 className="text-lg font-semibold mb-4 text-slate-800">Current Rates</h3>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
            <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#64748b' }} />
            <YAxis unit="%" axisLine={false} tickLine={false} tick={{ fill: '#64748b' }} />
            <Tooltip formatter={(value: number) => `${value}%`} contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
            <Bar dataKey="value" radius={[6, 6, 0, 0]}>
              {chartData.map((entry) => (
                <Cell key={entry.name} fill={entry.fill} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
