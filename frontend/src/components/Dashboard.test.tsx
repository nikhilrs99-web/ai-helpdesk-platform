import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import Dashboard from './Dashboard';

const mocks = vi.hoisted(() => ({ metrics: vi.fn(), volume: vi.fn() }));

vi.mock('../api/analytics', () => ({
  analyticsApi: { getDashboardMetrics: mocks.metrics, getTicketVolume: mocks.volume },
}));
vi.mock('../auth/keycloak', () => ({ default: { token: 'x' } }));

// recharts' ResponsiveContainer measures the DOM, which jsdom doesn't lay out - render children directly.
vi.mock('recharts', async (importOriginal) => {
  const actual = await importOriginal<typeof import('recharts')>();
  return { ...actual, ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div> };
});

const metrics = { totalTickets: 12, slaCompliancePercentage: 91.5, aiResolutionPercentage: 25 };

describe('Dashboard', () => {
  it('shows the metrics and the ticket-volume chart', async () => {
    mocks.metrics.mockResolvedValue(metrics);
    mocks.volume.mockResolvedValue([
      { date: '2026-10-01', count: 2 },
      { date: '2026-10-02', count: 5 },
    ]);

    render(<Dashboard />);

    expect(await screen.findByText('12')).toBeInTheDocument();
    expect(screen.getByText('91.5%')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTestId('volume-chart')).toBeInTheDocument());
    expect(mocks.volume).toHaveBeenCalledWith(7);
  });

  it('still renders metrics when the volume endpoint fails', async () => {
    mocks.metrics.mockResolvedValue(metrics);
    mocks.volume.mockRejectedValue(new Error('boom'));

    render(<Dashboard />);

    expect(await screen.findByText('12')).toBeInTheDocument();
    expect(screen.queryByTestId('volume-chart')).not.toBeInTheDocument();
  });
});
