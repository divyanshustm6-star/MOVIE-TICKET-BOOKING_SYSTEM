import { Link } from 'react-router-dom';
import AnimatedPage from '../components/AnimatedPage.jsx';
import MovieCard from '../components/MovieCard.jsx';
import StatCard from '../components/StatCard.jsx';
import LoadingSkeleton from '../components/LoadingSkeleton.jsx';
import { movieApi } from '../api/movieApi.js';
import { showApi } from '../api/showApi.js';
import { bookingApi } from '../api/bookingApi.js';
import { useAsync } from '../hooks/useAsync.js';

export default function HomePage() {
  const { data, loading } = useAsync(async () => {
    const [movies, shows, bookings] = await Promise.all([
      movieApi.list(),
      showApi.list().catch(() => []),
      bookingApi.history().catch(() => []),
    ]);
    return { movies, shows, bookings };
  }, []);

  const movies = data?.movies || [];

  return (
    <AnimatedPage className="grid gap-8">
      <section className="grid min-h-[440px] items-end overflow-hidden rounded-3xl border border-white/10 bg-[linear-gradient(120deg,rgba(5,6,8,.75),rgba(5,6,8,.35)),url('https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1800&q=80')] bg-cover bg-center p-6 shadow-panel md:p-10">
        <div className="max-w-3xl">
          <p className="text-xs font-black uppercase tracking-[0.35em] text-ember-300">Now booking</p>
          <h1 className="mt-4 text-5xl font-black leading-none text-white md:text-7xl">A sharper way to book cinema nights.</h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-slate-200">Explore movies, pick a live show, reserve seats, and confirm payment through your secure account.</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/movies" className="btn-primary">Browse movies</Link>
            <Link to="/shows" className="btn-secondary">Find shows</Link>
          </div>
        </div>
      </section>

      {loading ? (
        <LoadingSkeleton rows={3} className="grid-cols-1 md:grid-cols-3" />
      ) : (
        <>
          <section className="grid gap-4 md:grid-cols-3">
            <StatCard label="Movies" value={movies.length} detail="Available in catalog" />
            <StatCard label="Shows" value={data?.shows?.length || 0} detail="Scheduled screenings" />
            <StatCard label="Bookings" value={data?.bookings?.length || 0} detail="Your booking history" />
          </section>
          <section>
            <div className="mb-5 flex items-end justify-between">
              <div>
                <p className="text-xs font-black uppercase tracking-[0.25em] text-ember-400">Featured</p>
                <h2 className="mt-2 text-3xl font-black text-white">Movies to watch</h2>
              </div>
              <Link to="/movies" className="text-sm font-bold text-ember-300">View all</Link>
            </div>
            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
              {movies.slice(0, 3).map((movie) => <MovieCard key={movie.id} movie={movie} />)}
            </div>
          </section>
        </>
      )}
    </AnimatedPage>
  );
}
