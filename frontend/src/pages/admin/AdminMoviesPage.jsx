import { useState } from 'react';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { movieApi } from '../../api/movieApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { useToastStore } from '../../store/toastStore.js';
import { movieStatuses } from '../../utils/constants.js';

const emptyMovie = {
  title: '',
  description: '',
  language: 'English',
  durationMinutes: 120,
  releaseDate: '',
  certification: 'UA',
  posterUrl: '',
  trailerUrl: '',
  status: 'UPCOMING',
  genres: '',
};

function toPayload(form) {
  return {
    ...form,
    durationMinutes: Number(form.durationMinutes),
    releaseDate: form.releaseDate || null,
    genres: form.genres.split(',').map((item) => item.trim()).filter(Boolean),
  };
}

export default function AdminMoviesPage() {
  const toast = useToastStore((state) => state.push);
  const { data: movies = [], loading, reload } = useAsync(() => movieApi.list(), []);
  const [form, setForm] = useState(emptyMovie);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);

  function edit(movie) {
    setEditingId(movie.id);
    setForm({ ...movie, releaseDate: movie.releaseDate || '', genres: (movie.genres || []).join(', ') });
  }

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    try {
      if (editingId) {
        await movieApi.update(editingId, toPayload(form));
        toast({ type: 'success', message: 'Movie updated' });
      } else {
        await movieApi.create(toPayload(form));
        toast({ type: 'success', message: 'Movie added' });
      }
      setForm(emptyMovie);
      setEditingId(null);
      await reload();
    } finally {
      setSaving(false);
    }
  }

  async function remove(id) {
    if (!window.confirm('Delete this movie?')) return;
    await movieApi.remove(id);
    toast({ type: 'success', message: 'Movie deleted' });
    await reload();
  }

  return (
    <AnimatedPage className="grid gap-6">
      <PageHeader title="Manage movies" eyebrow="Admin" description="Create, update, and delete movie records through `/movies`." />
      <form onSubmit={save} className="panel grid gap-4 p-5 md:grid-cols-2">
        <input className="field" placeholder="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
        <input className="field" placeholder="Language" value={form.language} onChange={(e) => setForm({ ...form, language: e.target.value })} required />
        <input className="field" type="number" min="1" placeholder="Duration" value={form.durationMinutes} onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })} required />
        <input className="field" type="date" value={form.releaseDate} onChange={(e) => setForm({ ...form, releaseDate: e.target.value })} />
        <input className="field" placeholder="Certification" value={form.certification} onChange={(e) => setForm({ ...form, certification: e.target.value })} />
        <select className="field" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>{movieStatuses.map((item) => <option key={item}>{item}</option>)}</select>
        <input className="field md:col-span-2" placeholder="Poster URL" value={form.posterUrl} onChange={(e) => setForm({ ...form, posterUrl: e.target.value })} />
        <input className="field md:col-span-2" placeholder="Trailer URL" value={form.trailerUrl} onChange={(e) => setForm({ ...form, trailerUrl: e.target.value })} />
        <input className="field md:col-span-2" placeholder="Genres comma separated" value={form.genres} onChange={(e) => setForm({ ...form, genres: e.target.value })} />
        <textarea className="field min-h-28 md:col-span-2" placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <div className="flex gap-3 md:col-span-2">
          <Button type="submit" disabled={saving}>{editingId ? 'Update movie' : 'Add movie'}</Button>
          {editingId && <Button type="button" variant="secondary" onClick={() => { setEditingId(null); setForm(emptyMovie); }}>Cancel</Button>}
        </div>
      </form>
      {loading ? <LoadingSkeleton rows={5} /> : (
        <div className="grid gap-3">
          {movies.map((movie) => (
            <div key={movie.id} className="panel flex flex-col gap-4 p-4 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="font-black text-white">{movie.title}</p>
                <p className="text-sm text-slate-400">{movie.language} · {movie.status}</p>
              </div>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => edit(movie)}>Edit</Button>
                <Button variant="danger" onClick={() => remove(movie.id)}>Delete</Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </AnimatedPage>
  );
}
