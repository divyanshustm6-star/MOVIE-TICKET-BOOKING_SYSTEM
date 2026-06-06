import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore.js';
import ToastHost from '../components/ToastHost.jsx';
import BackButton from '../components/BackButton.jsx';

const links = [
  { to: '/', label: 'Home' },
  { to: '/movies', label: 'Movies' },
  { to: '/shows', label: 'Shows' },
  { to: '/bookings', label: 'Bookings' },
  { to: '/profile', label: 'Profile' },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const { user, logout, isAdmin } = useAuthStore();

  return (
    <div className="min-h-screen bg-cinema-radial">
      <ToastHost />
      <header className="sticky top-0 z-40 border-b border-white/10 bg-cinema-950/85 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-4 md:flex-row md:items-center md:justify-between">
          <button type="button" onClick={() => navigate('/')} className="text-left text-xl font-black text-white">
            Divyanshu Movies
          </button>
          <nav className="flex flex-wrap gap-2">
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) => `rounded-xl px-3 py-2 text-sm font-bold transition ${isActive ? 'bg-ember-500 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}
              >
                {link.label}
              </NavLink>
            ))}
            {isAdmin() && (
              <NavLink to="/admin" className={({ isActive }) => `rounded-xl px-3 py-2 text-sm font-bold transition ${isActive ? 'bg-ember-500 text-white' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}>
                Admin
              </NavLink>
            )}
          </nav>
          <div className="flex items-center gap-3">
            <span className="hidden max-w-[180px] truncate text-sm font-semibold text-slate-400 sm:inline">{user?.email}</span>
            <button type="button" onClick={() => logout()} className="btn-secondary min-h-10 px-4">Logout</button>
          </div>
        </div>
      </header>
      <main className="relative mx-auto max-w-7xl px-4 py-8 md:py-10">
        <BackButton />
        <Outlet />
      </main>
    </div>
  );
}
