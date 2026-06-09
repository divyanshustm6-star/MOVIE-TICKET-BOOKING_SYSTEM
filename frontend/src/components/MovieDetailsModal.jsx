import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore.js';
import { useMovieDetailsModalStore } from '../store/movieDetailsModalStore.js';
import { useLoginRequiredStore } from '../store/authModalStore.js';
import { formatDate } from '../utils/formatters.js';
import Button from './Button.jsx';

export default function MovieDetailsModal() {
  const { isOpen, movie, close } = useMovieDetailsModalStore();
  const { token } = useAuthStore();
  const loginRequired = useLoginRequiredStore();
  const navigate = useNavigate();

  if (!isOpen || !movie) return null;

  function getYoutubeId(url) {
    if (!url) return null;
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]*).*/;
    const match = url.match(regExp);
    return match && match[2].length === 11 ? match[2] : null;
  }

  const youtubeId = getYoutubeId(movie.trailerUrl);

  function handleBooking() {
    close();
    if (!token) {
      loginRequired.open(() => {
        navigate(`/movies/${movie.id}`);
      });
    } else {
      navigate(`/movies/${movie.id}`);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4 overflow-y-auto animate-fade-in">
      <div className="relative w-full max-w-4xl rounded-3xl border border-white/10 bg-cinema-900 p-6 md:p-8 shadow-panel my-8 animate-scale-up">
        {/* Close Button */}
        <button
          type="button"
          onClick={close}
          className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-white/10 hover:text-white transition z-10"
          aria-label="Close modal"
        >
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <div className="grid gap-6 md:grid-cols-[300px_1fr] items-start">
          {/* Movie Poster */}
          <div className="mx-auto w-full max-w-[300px] aspect-[2/3] overflow-hidden rounded-2xl bg-cinema-800 shadow-xl border border-white/5">
            {movie.posterUrl && !movie.posterUrl.includes('example.com') ? (
              <img src={movie.posterUrl} alt={movie.title} className="h-full w-full object-cover" />
            ) : (
              <div className="grid h-full place-items-center bg-gradient-to-br from-cinema-700 to-black text-6xl font-black text-white/25">
                {movie.title?.[0] || 'M'}
              </div>
            )}
          </div>

          {/* Movie Details */}
          <div className="flex flex-col h-full justify-between">
            <div>
              <h2 className="text-3xl font-black text-white md:text-4xl">{movie.title}</h2>
              <div className="mt-2 flex flex-wrap gap-2">
                {(movie.genres || []).map((genre) => (
                  <span className="badge" key={genre}>
                    {genre}
                  </span>
                ))}
              </div>

              <p className="mt-4 text-sm leading-6 text-slate-300 max-h-48 overflow-y-auto pr-2">
                {movie.description || 'No description available.'}
              </p>

              <div className="mt-6 grid gap-4 text-sm text-slate-400 sm:grid-cols-2">
                <div className="flex items-center gap-2">
                  <span className="text-amber-500">🗣️</span>
                  <span>Language: <strong className="text-white">{movie.language}</strong></span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-amber-500">⏱️</span>
                  <span>Duration: <strong className="text-white">{movie.durationMinutes} minutes</strong></span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-amber-500">📅</span>
                  <span>Release Date: <strong className="text-white">{formatDate(movie.releaseDate)}</strong></span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-amber-500">🛡️</span>
                  <span>Certification: <strong className="text-white">{movie.certification || 'NA'}</strong></span>
                </div>
              </div>
            </div>

            {/* Embed Video or link */}
            <div className="mt-6">
              {youtubeId ? (
                <div className="w-full aspect-video rounded-2xl overflow-hidden border border-white/10 shadow-lg">
                  <iframe
                    className="w-full h-full"
                    src={`https://www.youtube.com/embed/${youtubeId}`}
                    title="Movie Trailer"
                    frameBorder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowFullScreen
                  />
                </div>
              ) : movie.trailerUrl ? (
                <a
                  href={movie.trailerUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 text-sm font-semibold text-amber-400 hover:text-amber-300 transition"
                >
                  📺 Watch Trailer External Link
                </a>
              ) : null}
            </div>

            {/* Action Buttons */}
            <div className="mt-6 flex flex-wrap gap-3">
              <Button type="button" onClick={handleBooking} className="flex-1 min-h-12 text-base font-black">
                Book Tickets / View Shows
              </Button>
              <Button type="button" variant="secondary" onClick={close} className="px-6 min-h-12 text-base font-semibold">
                Close
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
