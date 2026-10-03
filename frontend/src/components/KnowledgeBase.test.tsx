import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import KnowledgeBase from './KnowledgeBase';

const search = vi.hoisted(() => vi.fn());
vi.mock('../api/articles', () => ({ articlesApi: { search } }));
vi.mock('../auth/keycloak', () => ({ default: { token: 'x' } }));

const article = {
  id: 'a1',
  title: 'Reset your password',
  body: 'Click forgot password',
  category: 'ACCESS' as const,
  createdAt: '',
  updatedAt: '',
};

describe('KnowledgeBase', () => {
  it('searches and lists matching articles', async () => {
    search.mockResolvedValue({ content: [article] });
    render(<KnowledgeBase />);
    await userEvent.type(screen.getByPlaceholderText('Search articles...'), 'password');
    await userEvent.click(screen.getByText('Search'));
    expect(await screen.findByText('Reset your password')).toBeInTheDocument();
    expect(search).toHaveBeenCalledWith('password', 0);
  });

  it('shows an empty state when nothing matches', async () => {
    search.mockResolvedValue({ content: [] });
    render(<KnowledgeBase />);
    await userEvent.type(screen.getByPlaceholderText('Search articles...'), 'zzz');
    await userEvent.click(screen.getByText('Search'));
    expect(await screen.findByText('No articles found.')).toBeInTheDocument();
  });
});
