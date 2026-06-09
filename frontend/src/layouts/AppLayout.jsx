import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore.js';
import ToastHost from '../components/ToastHost.jsx';
import BackButton from '../components/BackButton.jsx';
import AuthModal from '../components/AuthModal.jsx';
import LoginRequiredModal from '../components/LoginRequiredModal.jsx';
import MovieDetailsModal from '../components/MovieDetailsModal.jsx';
import { useAuthModalStore } from '../store/authModalStore.js';

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout, isAdmin } = useAuthStore();
  const openLogin = useAuthModalStore((state) => state.openLogin);

  const handleNavClick = (to) => {
    if (to.startsWith('/#')) {
      const targetId = to.substring(2);
      if (location.pathname !== '/') {
        navigate('/');
        setTimeout(() => {
          const element = document.getElementById(targetId);
          if (element) element.scrollIntoView({ behavior: 'smooth' });
        }, 150);
      } else {
        const element = document.getElementById(targetId);
        if (element) element.scrollIntoView({ behavior: 'smooth' });
      }
    } else {
      navigate(to);
    }
  };

  return (
    <div className="min-h-screen bg-cinema-radial">
      <ToastHost />
      <AuthModal />
      <LoginRequiredModal />
      <MovieDetailsModal />

      <header className="sticky top-0 z-40 border-b border-white/10 bg-cinema-950/85 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-4 md:flex-row md:items-center md:justify-between">
          <button type="button" onClick={() => navigate('/')} className="text-left text-xl font-black text-white">
            Divyanshu Movies
          </button>
          
          <nav className="flex flex-wrap gap-2">
            <NavLink
              to="/"
              className={({ isActive }) => `rounded-xl px-3 py-2 text-sm font-bold transition ${isActive && location.pathname === '/' ? 'bg-ember-500 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}
            >
              Home
            </NavLink>
            <NavLink
              to="/movies"
              className={({ isActive }) => `rounded-xl px-3 py-2 text-sm font-bold transition ${isActive ? 'bg-ember-500 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}
            >
              Movies
            </NavLink>
            <button
              type="button"
              onClick={() => handleNavClick('/#about')}
              className="rounded-xl px-3 py-2 text-sm font-bold text-slate-300 hover:bg-white/10 hover:text-white transition"
            >
              About
            </button>
            <button
              type="button"
              onClick={() => handleNavClick('/#contact')}
              className="rounded-xl px-3 py-2 text-sm font-bold text-slate-300 hover:bg-white/10 hover:text-white transition"
            >
              Contact
            </button>
            {isAdmin() && (
              <NavLink to="/admin" className={({ isActive }) => `rounded-xl px-3 py-2 text-sm font-bold transition ${isActive ? 'bg-ember-500 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}>
                Admin
              </NavLink>
            )}
          </nav>

          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3">
                <NavLink to="/bookings" className="rounded-xl px-3 py-2 text-sm font-bold text-slate-300 hover:bg-white/10 hover:text-white transition">
                  Bookings
                </NavLink>
                <NavLink to="/profile" className="rounded-xl px-3 py-2 text-sm font-bold text-slate-300 hover:bg-white/10 hover:text-white transition">
                  Profile
                </NavLink>
                <span className="hidden max-w-[180px] truncate text-sm font-semibold text-slate-400 sm:inline">
                  {user.fullName || user.email}
                </span>
                <button type="button" onClick={() => logout()} className="btn-secondary min-h-10 px-4">
                  Logout
                </button>
              </div>
            ) : (
              <button type="button" onClick={() => openLogin()} className="btn-primary min-h-10 px-6">
                Login
              </button>
            )}
          </div>
        </div>
      </header>
      
      <main className="relative mx-auto max-w-7xl px-4 py-8 md:py-10">
        {location.pathname !== '/' && <BackButton />}
        <Outlet />
      </main>
    </div>
  );
}
