import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import AgentPresence from './AgentPresence';

const mocks = vi.hoisted(() => ({ ping: vi.fn(), hasRole: vi.fn() }));
vi.mock('../api/agents', () => ({ agentsApi: { ping: mocks.ping } }));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ hasRole: mocks.hasRole }) }));

describe('AgentPresence', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders nothing for customers and never pings', () => {
    mocks.hasRole.mockReturnValue(false);
    const { container } = render(<AgentPresence />);
    expect(container).toBeEmptyDOMElement();
    expect(mocks.ping).not.toHaveBeenCalled();
  });

  it('shows Online after a successful ping for agents', async () => {
    mocks.hasRole.mockImplementation((r: string) => r === 'agent');
    mocks.ping.mockResolvedValue(undefined);
    render(<AgentPresence />);
    expect(await screen.findByText(/Online/)).toBeInTheDocument();
  });

  it('shows Offline when the ping fails', async () => {
    mocks.hasRole.mockImplementation((r: string) => r === 'agent');
    mocks.ping.mockRejectedValue(new Error('down'));
    render(<AgentPresence />);
    await waitFor(() => expect(mocks.ping).toHaveBeenCalled());
    expect(screen.getByText('Offline')).toBeInTheDocument();
  });
});
