import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import TicketDetail from './TicketDetail';
import type { TicketResponse } from '../api/types';

const mocks = vi.hoisted(() => ({ get: vi.fn(), changeStatus: vi.fn(), update: vi.fn(), hasRole: vi.fn() }));
vi.mock('../api/tickets', () => ({
  ticketsApi: { get: mocks.get, changeStatus: mocks.changeStatus, update: mocks.update },
}));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ hasRole: mocks.hasRole, subject: 'cust-1' }) }));
// The SLA and escalation panels have their own tests and their own API calls.
vi.mock('./SlaPanel', () => ({ default: () => <div>sla-panel</div> }));
vi.mock('./EscalationPanel', () => ({ default: () => <div>escalation-panel</div> }));

const ticket: TicketResponse = {
  id: 't1',
  subject: 'Cannot log in',
  description: 'Password reset link never arrives',
  category: 'ACCESS',
  status: 'OPEN',
  requesterId: 'cust-1',
  assignedAgentId: null,
  routedTeam: 'access-team',
  metadata: { browser: 'Firefox' },
  createdAt: '2026-10-01T00:00:00Z',
  updatedAt: '2026-10-01T00:00:00Z',
};

const renderDetail = () =>
  render(
    <MemoryRouter initialEntries={['/tickets/t1']}>
      <Routes>
        <Route path="/tickets/:id" element={<TicketDetail />} />
      </Routes>
    </MemoryRouter>,
  );

describe('TicketDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.get.mockResolvedValue(ticket);
  });

  it('shows the ticket, its metadata and the SLA and escalation panels', async () => {
    mocks.hasRole.mockReturnValue(false);
    renderDetail();
    expect(await screen.findByText('Cannot log in')).toBeInTheDocument();
    expect(screen.getByText('Password reset link never arrives')).toBeInTheDocument();
    expect(screen.getByText('Firefox')).toBeInTheDocument();
    expect(screen.getByText('sla-panel')).toBeInTheDocument();
    expect(screen.getByText('escalation-panel')).toBeInTheDocument();
  });

  it('hides the status control from customers', async () => {
    mocks.hasRole.mockReturnValue(false);
    renderDetail();
    await screen.findByText('Cannot log in');
    expect(screen.queryByText('Change status to...')).not.toBeInTheDocument();
  });

  it('lets an agent change the status', async () => {
    mocks.hasRole.mockImplementation((r: string) => r === 'agent');
    mocks.changeStatus.mockResolvedValue({ ...ticket, status: 'AI_TRIAGED' });
    renderDetail();
    await screen.findByText('Cannot log in');
    await userEvent.selectOptions(screen.getAllByRole('combobox')[0], 'AI_TRIAGED');
    await userEvent.click(screen.getByText('Apply'));
    await waitFor(() => expect(mocks.changeStatus).toHaveBeenCalledWith('t1', 'AI_TRIAGED'));
  });

  it('lets the requester edit their own ticket', async () => {
    mocks.hasRole.mockReturnValue(false);
    mocks.update.mockResolvedValue({ ...ticket, subject: 'Updated subject' });
    renderDetail();
    await userEvent.click(await screen.findByText('Edit'));
    const input = screen.getByDisplayValue('Cannot log in');
    await userEvent.clear(input);
    await userEvent.type(input, 'Updated subject');
    await userEvent.click(screen.getByText('Save'));
    await waitFor(() =>
      expect(mocks.update).toHaveBeenCalledWith('t1', {
        subject: 'Updated subject',
        description: 'Password reset link never arrives',
      }),
    );
  });
});
