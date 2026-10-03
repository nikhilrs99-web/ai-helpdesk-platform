import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import TicketList from './TicketList';
import { ApiRequestError } from '../api/client';
import type { TicketResponse } from '../api/types';

const list = vi.hoisted(() => vi.fn());
vi.mock('../api/tickets', () => ({ ticketsApi: { list } }));
vi.mock('../auth/keycloak', () => ({ default: { token: 'x' } }));

const ticket = (id: string, subject: string): TicketResponse => ({
  id,
  subject,
  description: 'd',
  category: 'HOW_TO',
  status: 'OPEN',
  requesterId: 'u',
  assignedAgentId: null,
  routedTeam: 'support',
  metadata: {},
  createdAt: '2026-10-01T00:00:00Z',
  updatedAt: '2026-10-01T00:00:00Z',
});
const page = (items: TicketResponse[], opts = {}) => ({
  content: items,
  totalElements: items.length,
  totalPages: 2,
  number: 0,
  size: 10,
  first: true,
  last: false,
  ...opts,
});

const renderList = () =>
  render(
    <MemoryRouter>
      <TicketList />
    </MemoryRouter>,
  );

describe('TicketList', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders tickets with their category and team', async () => {
    list.mockResolvedValue(page([ticket('1', 'Cannot log in'), ticket('2', 'Invoice wrong')]));
    renderList();
    expect(await screen.findByText('Cannot log in')).toBeInTheDocument();
    expect(screen.getByText('Invoice wrong')).toBeInTheDocument();
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
  });

  it('shows an empty state', async () => {
    list.mockResolvedValue(page([], { totalPages: 0, last: true }));
    renderList();
    expect(await screen.findByText('No tickets yet.')).toBeInTheDocument();
  });

  it('pages forward when Next is clicked', async () => {
    list.mockResolvedValue(page([ticket('1', 'First page')]));
    renderList();
    await userEvent.click(await screen.findByText('Next'));
    await waitFor(() => expect(list).toHaveBeenCalledWith(1));
  });

  it('shows the API error message', async () => {
    list.mockRejectedValue(new ApiRequestError('You do not have permission to perform this action.', 403));
    renderList();
    expect(await screen.findByText(/do not have permission/)).toBeInTheDocument();
  });
});
