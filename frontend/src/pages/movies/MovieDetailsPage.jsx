import { useParams, useNavigate } from 'react-router-dom';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import EmptyState from '../../components/EmptyState.jsx';
import { movieApi } from '../../api/movieApi.js';
import { showApi } from '../../api/showApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { compactStatus, formatCurrency, formatDate, formatDateTime } from '../../utils/formatters.js';
import { useAuthStore } from '../../store/authStore.js';
import { useLoginRequiredStore } from '../../store/authModalStore.js';

export default function MovieDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { token } = useAuthStore();
  const loginRequired = useLoginRequiredStore();

  const handleChooseSeats = (showId) => {
    if (!token) {
      loginRequired.open(() => {
        navigate(`/book/${showId}`);
      });
    } else {
      navigate(`/book/${showId}`);
    }
  };
  const { data, loading, error } = useAsync(async () => {
    const [movie, shows] = await Promise.all([movieApi.get(id), showApi.list({ movieId: id }).catch(() => [])]);
    return { movie, shows };
  }, [id]);

  if (loading) return <LoadingSkeleton rows={4} />;
  if (error) return <EmptyState title="Movie unavailable" message={error} />;

  const { movie, shows } = data;

  return (
    <AnimatedPage className="grid gap-8">
      <section className="panel overflow-hidden">
        <div className="grid lg:grid-cols-[420px_1fr]">
          <div className="aspect-[4/3] bg-cinema-700 lg:aspect-auto">
            {movie.posterUrl && !movie.posterUrl.includes('example.com') ? (
              <img src={movie.posterUrl} alt={movie.title} className="h-full w-full object-cover" />
            ) : (
              <div className="grid h-full place-items-center bg-gradient-to-br from-cinema-700 to-black text-8xl font-black text-white/20">{movie.title?.[0]}</div>
            )}
          </div>
          <div className="p-6 md:p-9">
            <span className="badge">{compactStatus(movie.status)}</span>
            <h1 className="mt-5 text-4xl font-black text-white md:text-6xl">{movie.title}</h1>
            <p className="mt-4 text-lg leading-8 text-slate-300">{movie.description}</p>
            <div className="mt-6 grid gap-3 text-sm font-semibold text-slate-300 sm:grid-cols-2">
              <p>Language: {movie.language}</p>
              <p>Duration: {movie.durationMinutes} min</p>
              <p>Release: {formatDate(movie.releaseDate)}</p>
              <p>Certification: {movie.certification || 'NA'}</p>
            </div>
            <div className="mt-6 flex flex-wrap gap-2">{(movie.genres || []).map((genre) => <span className="badge" key={genre}>{genre}</span>)}</div>
          </div>
        </div>
      </section>

      <section>
        <h2 className="mb-4 text-2xl font-black text-white">Available shows</h2>
        {shows.length === 0 ? <EmptyState title="No shows scheduled" /> : (
          <div className="grid gap-4">
            {shows.map((show) => (
              <div key={show.id} className="panel flex flex-col gap-4 p-5 md:flex-row md:items-center md:justify-between">
                <div>
                  <p className="text-lg font-black text-white">{show.theaterName} · {show.screenName}</p>
                  <p className="mt-1 text-sm text-slate-400">{formatDateTime(show.startsAt)} · {compactStatus(show.status)} · {formatCurrency(show.price)}</p>
                </div>
                <button
                  type="button"
                  onClick={() => handleChooseSeats(show.id)}
                  className="btn-primary"
                >
                  Select seats
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </AnimatedPage>
  );
}
