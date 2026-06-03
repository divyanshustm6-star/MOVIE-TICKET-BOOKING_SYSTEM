import { Link } from 'react-router-dom';

export default function ErrorPage({ code = '404', title = 'Page not found', message = 'The page you requested is not available.' }) {
  return (
    <main className="grid min-h-screen place-items-center bg-cinema-radial px-4">
      <section className="panel max-w-lg p-8 text-center">
        <p className="text-6xl font-black text-ember-400">{code}</p>
        <h1 className="mt-3 text-3xl font-black text-white">{title}</h1>
        <p className="mt-3 text-slate-400">{message}</p>
        <Link className="btn-primary mt-6" to="/">Back home</Link>
      </section>
    </main>
  );
}
