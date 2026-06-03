import { useMemo, useState } from 'react';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import EmptyState from '../../components/EmptyState.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import MovieCard from '../../components/MovieCard.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { movieApi } from '../../api/movieApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { movieStatuses } from '../../utils/constants.js';
import { uniqueValues } from '../../utils/formatters.js';

export default function MoviesPage() {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [language, setLanguage] = useState('');
  const { data: movies = [], loading, error } = useAsync(() => movieApi.list(), []);

  const languages = useMemo(() => uniqueValues(movies || [], 'language'), [movies]);
  const filtered = useMemo(() => (movies || []).filter((movie) => {
    const text = `${movie.title} ${movie.description} ${(movie.genres || []).join(' ')}`.toLowerCase();
    return (!query || text.includes(query.toLowerCase()))
      && (!status || movie.status === status)
      && (!language || movie.language === language);
  }), [movies, query, status, language]);

  return (
    <AnimatedPage className="grid gap-8">
      <PageHeader title="Movie catalog" eyebrow="Browse" description="Search and filter the backend movie catalog. Movie reads require a USER or ADMIN token." />
      <div className="panel grid gap-4 p-4 md:grid-cols-[1fr_220px_220px]">
        <input className="field" placeholder="Search title, genre, description..." value={query} onChange={(e) => setQuery(e.target.value)} />
        <select className="field" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {movieStatuses.map((item) => <option key={item} value={item}>{item}</option>)}
        </select>
        <select className="field" value={language} onChange={(e) => setLanguage(e.target.value)}>
          <option value="">All languages</option>
          {languages.map((item) => <option key={item} value={item}>{item}</option>)}
        </select>
      </div>
      {loading && <LoadingSkeleton rows={6} className="md:grid-cols-2 xl:grid-cols-3" />}
      {error && <EmptyState title="Unable to load movies" message={error} />}
      {!loading && !error && filtered.length === 0 && <EmptyState title="No movies match your filters" />}
      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {filtered.map((movie) => <MovieCard key={movie.id} movie={movie} />)}
      </div>
    </AnimatedPage>
  );
}
