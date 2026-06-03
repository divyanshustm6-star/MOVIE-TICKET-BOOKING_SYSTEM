import { Outlet } from 'react-router-dom';

export default function AuthLayout() {
  return (
    <main className="min-h-screen bg-cinema-radial px-4 py-8">
      <div className="mx-auto grid min-h-[calc(100vh-64px)] w-full max-w-6xl items-center gap-10 lg:grid-cols-[1fr_460px]">
        <section className="hidden lg:block">
          <p className="text-xs font-black uppercase tracking-[0.4em] text-ember-400">Movie Booking</p>
          <h1 className="mt-5 max-w-3xl text-6xl font-black leading-[0.95] tracking-tight text-white">
            Reserve the best seats before the lights go down.
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-8 text-slate-300">
            Browse movies, choose shows, lock seats, and manage bookings through a secure JWT session.
          </p>
        </section>
        <Outlet />
      </div>
    </main>
  );
}
