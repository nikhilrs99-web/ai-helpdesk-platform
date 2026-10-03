import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import NewTicketForm from './NewTicketForm';
import { ApiRequestError } from '../api/client';

const create = vi.hoisted(() => vi.fn());
const navigate = vi.hoisted(() => vi.fn());
vi.mock('../api/tickets', () => ({ ticketsApi: { create } }));
vi.mock('../auth/keycloak', () => ({ default: { token: 'x' } }));
vi.mock('react-router-dom', async (orig) => ({
  ...(await orig<typeof import('react-router-dom')>()),
  useNavigate: () => navigate,
}));

const renderForm = () =>
  render(
    <MemoryRouter>
      <NewTicketForm />
    </MemoryRouter>,
  );

describe('NewTicketForm', () => {
  beforeEach(() => vi.clearAllMocks());

  it('submits a HOW_TO ticket without extra metadata and navigates to it', async () => {
    create.mockResolvedValue({ id: 't-1' });
    renderForm();
    const [subject, description] = screen.getAllByRole('textbox');
    await userEvent.type(subject, 'Help');
    await userEvent.type(description, 'Please');
    await userEvent.click(screen.getByText('Submit Ticket'));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        subject: 'Help',
        description: 'Please',
        category: 'HOW_TO',
        metadata: {},
      }),
    );
    expect(navigate).toHaveBeenCalledWith('/tickets/t-1');
  });

  it('asks for the category-specific fields the server requires (BUG)', async () => {
    renderForm();
    await userEvent.selectOptions(screen.getByRole('combobox'), 'BUG');
    expect(screen.getByText('Browser')).toBeInTheDocument();
    expect(screen.getByText('App version')).toBeInTheDocument();
  });

  it('asks for the invoice id on BILLING tickets', async () => {
    renderForm();
    await userEvent.selectOptions(screen.getByRole('combobox'), 'BILLING');
    expect(screen.getByText('Invoice ID')).toBeInTheDocument();
  });

  it('shows the server error and does not navigate', async () => {
    create.mockRejectedValue(new ApiRequestError('Too many requests - please slow down and try again shortly.', 429));
    renderForm();
    const [subject, description] = screen.getAllByRole('textbox');
    await userEvent.type(subject, 'x');
    await userEvent.type(description, 'y');
    await userEvent.click(screen.getByText('Submit Ticket'));
    expect(await screen.findByText(/Too many requests/)).toBeInTheDocument();
    expect(navigate).not.toHaveBeenCalled();
  });
});
