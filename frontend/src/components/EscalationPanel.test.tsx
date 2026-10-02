import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EscalationPanel from './EscalationPanel';
import type { EscalationResponse } from '../api/types';

const mocks = vi.hoisted(() => ({
  list: vi.fn(),
  create: vi.fn(),
  approve: vi.fn(),
  reject: vi.fn(),
  hasRole: vi.fn(),
}));

vi.mock('../api/escalations', () => ({
  escalationsApi: { list: mocks.list, create: mocks.create, approve: mocks.approve, reject: mocks.reject },
}));
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ hasRole: mocks.hasRole }),
}));

const pending: EscalationResponse = {
  id: 'e1',
  ticketId: 't1',
  reason: 'Customer is very upset',
  requestedBy: 'cust-1',
  status: 'PENDING',
  decidedBy: null,
  createdAt: new Date().toISOString(),
};

describe('EscalationPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.list.mockResolvedValue([pending]);
  });

  it('lets an agent approve a pending escalation and reloads the list', async () => {
    mocks.hasRole.mockImplementation((r: string) => r === 'agent');
    mocks.approve.mockResolvedValue({ ...pending, status: 'APPROVED' });

    render(<EscalationPanel ticketId="t1" />);
    await userEvent.click(await screen.findByText('Approve'));

    await waitFor(() => expect(mocks.approve).toHaveBeenCalledWith('t1', 'e1'));
    expect(mocks.list).toHaveBeenCalledTimes(2);
  });

  it('hides approve/reject from customers', async () => {
    mocks.hasRole.mockReturnValue(false);

    render(<EscalationPanel ticketId="t1" />);

    expect(await screen.findByText('Customer is very upset')).toBeInTheDocument();
    expect(screen.queryByText('Approve')).not.toBeInTheDocument();
    expect(screen.queryByText('Reject')).not.toBeInTheDocument();
  });

  it('creates an escalation with the typed reason', async () => {
    mocks.hasRole.mockReturnValue(false);
    mocks.list.mockResolvedValue([]);
    mocks.create.mockResolvedValue(pending);

    render(<EscalationPanel ticketId="t1" />);
    await userEvent.type(await screen.findByPlaceholderText(/need escalating/i), 'Billing error');
    await userEvent.click(screen.getByText('Request escalation'));

    await waitFor(() => expect(mocks.create).toHaveBeenCalledWith('t1', 'Billing error'));
  });
});
