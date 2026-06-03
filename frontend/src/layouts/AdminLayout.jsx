import { NavLink, Outlet } from 'react-router-dom';

const adminLinks = [
  { to: '/admin', label: 'Overview', end: true },
  { to: '/admin/movies', label: 'Movies' },
  { to: '/admin/shows', label: 'Shows' },
  { to: '/admin/bookings', label: 'Bookings' },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/theaters', label: 'Theaters' },
];

export default function AdminLayout() {
  return (
    <div className="grid gap-6 lg:grid-cols-[240px_1fr]">
      <aside className="panel h-max p-3">
        <p className="px-3 py-2 text-xs font-black uppercase tracking-[0.25em] text-ember-400">Admin</p>
        <nav className="grid gap-1">
          {adminLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) => `rounded-xl px-3 py-3 text-sm font-bold ${isActive ? 'bg-white text-cinema-950' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <Outlet />
    </div>
  );
}
