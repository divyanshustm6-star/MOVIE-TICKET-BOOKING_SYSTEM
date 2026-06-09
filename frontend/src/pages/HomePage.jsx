import { useState } from 'react';
import AnimatedPage from '../components/AnimatedPage.jsx';
import MovieCard from '../components/MovieCard.jsx';
import LoadingSkeleton from '../components/LoadingSkeleton.jsx';
import { movieApi } from '../api/movieApi.js';
import { useAsync } from '../hooks/useAsync.js';

export default function HomePage() {
  const { data: movies, loading } = useAsync(async () => {
    return await movieApi.publicList();
  }, []);

  const [searchQuery, setSearchQuery] = useState('');

  const filteredMovies = (movies || []).filter((movie) => {
    const query = searchQuery.toLowerCase();
    const titleMatch = movie.title?.toLowerCase().includes(query);
    const genreMatch = (movie.genres || []).some((g) => g.toLowerCase().includes(query));
    const langMatch = movie.language?.toLowerCase().includes(query);
    return titleMatch || genreMatch || langMatch;
  });

  const scrollToMovies = () => {
    const element = document.getElementById('movies-list');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <AnimatedPage className="grid gap-12">
      {/* Hero Section */}
      <section className="relative min-h-[500px] flex items-center justify-center overflow-hidden rounded-3xl border border-white/10 bg-[linear-gradient(to_bottom,rgba(5,6,8,0.7),rgba(5,6,8,0.9)),url('https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1800&q=80')] bg-cover bg-center p-6 shadow-panel md:p-12 text-center">
        <div className="max-w-3xl z-10 flex flex-col items-center">
          <p className="text-xs font-black uppercase tracking-[0.35em] text-amber-500">Divyanshu Movies</p>
          <h1 className="mt-4 text-4xl font-black leading-tight text-white md:text-6xl">
            A premium way to book your cinema nights.
          </h1>
          <p className="mt-5 max-w-2xl text-base md:text-lg leading-relaxed text-slate-300">
            Browse through the latest movies, pick your favorite shows, select your premium seats, and get instant ticket confirmation.
          </p>

          {/* Search Box */}
          <div className="mt-8 flex w-full max-w-lg flex-col gap-3 sm:flex-row items-center bg-white/5 border border-white/10 rounded-2xl p-2 backdrop-blur-md">
            <div className="relative flex-1 w-full flex items-center">
              <span className="absolute left-3 text-slate-400">🔍</span>
              <input
                type="text"
                placeholder="Search movies by name, genre or language..."
                className="w-full bg-transparent pl-10 pr-4 py-2.5 text-sm text-white focus:outline-none placeholder:text-slate-400"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <button type="button" onClick={scrollToMovies} className="btn-primary w-full sm:w-auto px-6 py-2.5 text-sm font-bold whitespace-nowrap">
              Browse Movies
            </button>
          </div>
        </div>
      </section>

      {/* Movie Catalog / Featured Movies */}
      <section id="movies-list" className="scroll-mt-24">
        <div className="mb-8 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.25em] text-amber-500">Currently Booking</p>
            <h2 className="mt-2 text-3xl font-black text-white">Featured Movies</h2>
          </div>
          {searchQuery && (
            <p className="text-sm text-slate-400">
              Found {filteredMovies.length} matching {filteredMovies.length === 1 ? 'movie' : 'movies'}
            </p>
          )}
        </div>

        {loading ? (
          <LoadingSkeleton rows={3} className="grid-cols-1 md:grid-cols-3" />
        ) : filteredMovies.length === 0 ? (
          <div className="panel p-10 text-center rounded-2xl bg-white/5 border border-white/5">
            <span className="text-4xl">🎬</span>
            <h3 className="mt-3 text-lg font-bold text-white">No movies found</h3>
            <p className="mt-1 text-sm text-slate-400">Try adjusting your search criteria.</p>
          </div>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {filteredMovies.map((movie) => (
              <MovieCard key={movie.id} movie={movie} />
            ))}
          </div>
        )}
      </section>

      {/* Why Choose Us */}
      <section className="grid gap-6 md:grid-cols-3">
        <div className="panel p-6 rounded-2xl bg-white/5 border border-white/5 text-center flex flex-col items-center">
          <div className="text-3xl mb-3">⚡</div>
          <h3 className="text-lg font-bold text-white">Easy Booking</h3>
          <p className="mt-2 text-sm text-slate-400">
            Select your preferred movie, screen, and premium recliner seats in just a few quick taps.
          </p>
        </div>
        <div className="panel p-6 rounded-2xl bg-white/5 border border-white/5 text-center flex flex-col items-center">
          <div className="text-3xl mb-3">🛡️</div>
          <h3 className="text-lg font-bold text-white">Secure Payments</h3>
          <p className="mt-2 text-sm text-slate-400">
            Process payments seamlessly and securely with credit cards, wallets, and UPI through Razorpay.
          </p>
        </div>
        <div className="panel p-6 rounded-2xl bg-white/5 border border-white/5 text-center flex flex-col items-center">
          <div className="text-3xl mb-3">🎟️</div>
          <h3 className="text-lg font-bold text-white">Instant Confirmation</h3>
          <p className="mt-2 text-sm text-slate-400">
            Receive your booking confirmation receipt and details sent straight to your email instantly.
          </p>
        </div>
      </section>

      {/* About Section */}
      <section id="about" className="panel p-8 md:p-12 rounded-3xl bg-white/5 border border-white/5 scroll-mt-24">
        <div className="max-w-3xl">
          <p className="text-xs font-black uppercase tracking-[0.25em] text-amber-500">About Us</p>
          <h2 className="mt-2 text-3xl font-black text-white">Divyanshu Movies Experience</h2>
          <p className="mt-6 text-sm md:text-base leading-relaxed text-slate-300">
            Welcome to Divyanshu Movies, where we aim to redefine your cinematic journeys. We offer a modern, intuitive, and extremely secure web application designed to bring the latest movies and shows straight to your screens. Whether you are looking for blockbuster actions, horror thrillers, or romantic comedies, our platform provides a hassle-free seat reservation system and real-time updates on scheduled screenings.
          </p>
        </div>
      </section>

      {/* Contact Section */}
      <section id="contact" className="panel p-8 md:p-12 rounded-3xl bg-white/5 border border-white/5 scroll-mt-24">
        <p className="text-xs font-black uppercase tracking-[0.25em] text-amber-500">Get in touch</p>
        <h2 className="mt-2 text-3xl font-black text-white">Contact & Support</h2>
        <div className="mt-8 grid gap-8 md:grid-cols-2">
          <div>
            <p className="text-sm leading-relaxed text-slate-300">
              Have questions about booking tickets or show listings? Reach out to our dedicated support team, and we will get back to you within 24 hours.
            </p>
            <div className="mt-6 space-y-3 text-sm text-slate-300">
              <p className="flex items-center gap-2">📍 <span className="text-white">123 Cinema Boulevard, Entertainment City</span></p>
              <p className="flex items-center gap-2">✉️ <span className="text-white">support@divyanshumovies.com</span></p>
              <p className="flex items-center gap-2">📞 <span className="text-white">+1 (234) 567-890</span></p>
            </div>
          </div>
          <div className="flex flex-col justify-end gap-3 text-sm text-slate-400">
            <span className="font-bold text-white">Follow Us</span>
            <div className="flex gap-4">
              <a href="#facebook" className="hover:text-amber-500 transition">Facebook</a>
              <a href="#twitter" className="hover:text-amber-500 transition">Twitter</a>
              <a href="#instagram" className="hover:text-amber-500 transition">Instagram</a>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-white/10 pt-8 pb-4 text-center text-xs text-slate-500">
        <p>&copy; {new Date().getFullYear()} Divyanshu Movies. All rights reserved.</p>
      </footer>
    </AnimatedPage>
  );
}
