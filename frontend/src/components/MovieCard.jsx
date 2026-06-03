import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { compactStatus, formatDate } from '../utils/formatters.js';

export default function MovieCard({ movie }) {
  return (
    <motion.article whileHover={{ y: -5 }} className="group panel overflow-hidden">
      <Link to={`/movies/${movie.id}`} className="block">
        <div className="relative aspect-[16/10] overflow-hidden bg-cinema-700">
          {movie.posterUrl && !movie.posterUrl.includes('example.com') ? (
            <img src={movie.posterUrl} alt={movie.title} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
          ) : (
            <div className="grid h-full place-items-center bg-gradient-to-br from-cinema-700 to-black text-6xl font-black text-white/25">
              {movie.title?.[0] || 'M'}
            </div>
          )}
          <span className="absolute left-4 top-4 rounded-full bg-black/70 px-3 py-1 text-xs font-black text-ember-300 backdrop-blur">
            {compactStatus(movie.status)}
          </span>
        </div>
        <div className="grid gap-3 p-5">
          <div>
            <h2 className="line-clamp-1 text-xl font-black text-white">{movie.title}</h2>
            <p className="mt-1 text-sm text-slate-400">{movie.language} · {movie.durationMinutes} min · {formatDate(movie.releaseDate)}</p>
          </div>
          <p className="line-clamp-2 min-h-12 text-sm leading-6 text-slate-400">{movie.description || 'No description available.'}</p>
          <div className="flex flex-wrap gap-2">
            {(movie.genres || []).slice(0, 3).map((genre) => <span className="badge" key={genre}>{genre}</span>)}
          </div>
        </div>
      </Link>
    </motion.article>
  );
}
