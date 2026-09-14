import { useState, type FormEvent } from 'react';
import { Search } from 'lucide-react';
import { articlesApi } from '../api/articles';
import { ApiRequestError } from '../api/client';
import type { ArticleResponse } from '../api/types';

export default function KnowledgeBase() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<ArticleResponse[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSearch(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const page = await articlesApi.search(query, 0);
      setResults(page.content);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Search failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-6 md:p-8 lg:p-10 w-full max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-6 text-slate-800">Knowledge Base</h1>

      <form onSubmit={handleSearch} className="flex gap-2 mb-6">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search articles..."
          className="flex-1 border border-slate-300 rounded-lg px-3 py-2 text-sm"
        />
        <button
          type="submit"
          disabled={loading}
          className="inline-flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg"
        >
          <Search size={16} /> Search
        </button>
      </form>

      {loading && <div className="text-slate-500 animate-pulse">Searching...</div>}
      {error && <div className="bg-red-50 text-red-700 border border-red-200 rounded-lg p-3 text-sm">{error}</div>}

      {results && !loading && (
        results.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center text-slate-500 border border-slate-100">
            No articles found.
          </div>
        ) : (
          <div className="space-y-3">
            {results.map((article) => (
              <div key={article.id} className="bg-white rounded-lg shadow border border-slate-100 p-4">
                <div className="flex items-center justify-between mb-1">
                  <h3 className="font-semibold text-slate-800">{article.title}</h3>
                  <span className="text-xs text-slate-400">{article.category.replaceAll('_', ' ')}</span>
                </div>
                <p className="text-sm text-slate-600 line-clamp-3">{article.body}</p>
              </div>
            ))}
          </div>
        )
      )}
    </div>
  );
}
