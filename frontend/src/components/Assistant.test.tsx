import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Assistant from './Assistant';
import { ApiRequestError } from '../api/client';

const chat = vi.hoisted(() => vi.fn());
vi.mock('../api/assistant', () => ({ assistantApi: { chat } }));
vi.mock('../auth/keycloak', () => ({ default: { token: 'x' } }));

describe('Assistant', () => {
  it('shows the user message and the assistant reply', async () => {
    chat.mockResolvedValueOnce('Ticket TKT-1 is IN_PROGRESS');

    render(<Assistant />);
    await userEvent.type(screen.getByPlaceholderText('Ask the assistant...'), 'status of TKT-1?');
    await userEvent.click(screen.getByText('Send'));

    expect(await screen.findByText('Ticket TKT-1 is IN_PROGRESS')).toBeInTheDocument();
    expect(screen.getByText('status of TKT-1?')).toBeInTheDocument();
    expect(chat).toHaveBeenCalledWith('status of TKT-1?');
  });

  it('surfaces an API error as an assistant message', async () => {
    chat.mockRejectedValueOnce(new ApiRequestError('The AI service is temporarily unavailable', 503));

    render(<Assistant />);
    await userEvent.type(screen.getByPlaceholderText('Ask the assistant...'), 'hi');
    await userEvent.click(screen.getByText('Send'));

    expect(await screen.findByText('The AI service is temporarily unavailable')).toBeInTheDocument();
  });
});
