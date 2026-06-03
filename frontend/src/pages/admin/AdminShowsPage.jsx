import { useState } from 'react';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { movieApi } from '../../api/movieApi.js';
import { showApi } from '../../api/showApi.js';
import { theaterApi } from '../../api/theaterApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { useToastStore } from '../../store/toastStore.js';
import { showStatuses } from '../../utils/constants.js';
import { formatCurrency, formatDateTime } from '../../utils/formatters.js';

const emptyShow = { movieId: '', screenId: '', showDate: '', startTime: '', endTime: '', status: 'SCHEDULED' };

function localDateTimeFromDateAndTime(date, time) {
  if (!date || !time) return '';
  // time expected 'HH:MM' or 'HH:MM:SS'
  const t = time.length === 5 ? time + ':00' : time;
  return `${date}T${t}`;
}

export default function AdminShowsPage() {
  const toast = useToastStore((state) => state.push);
  const { data, loading, reload } = useAsync(async () => {
    const [shows, movies, theaters] = await Promise.all([showApi.list(), movieApi.list(), theaterApi.list()]);
    const screensNested = await Promise.all(theaters.map((theater) => theaterApi.screens(theater.id).catch(() => [])));
    return { shows, movies, theaters, screens: screensNested.flat() };
  }, []);
  const [form, setForm] = useState(emptyShow);
  const [editingId, setEditingId] = useState(null);
  const [movieDurationMinutes, setMovieDurationMinutes] = useState(120);
  const [manualEndOverride, setManualEndOverride] = useState(false);

  const [editLoading, setEditLoading] = useState(false);

  function extractTime(value) {
    // accepts HH:MM:SS or HH:MM or ISO datetime
    if (!value) return '';
    if (value.includes('T')) {
      try {
        const dt = new Date(value);
        const pad = (n) => String(n).padStart(2, '0');
        return `${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
      } catch (err) {
        // fallthrough
      }
    }
    const parts = value.split(':');
    if (parts.length >= 2) return `${parts[0].padStart(2,'0')}:${parts[1].padStart(2,'0')}`;
    return '';
  }

  function calculateEndFrom(date, startTime, durationMinutes) {
    if (!date || !startTime) return '';
    const [hh, mm] = startTime.split(':').map((v) => parseInt(v, 10));
    const dt = new Date(`${date}T${String(hh).padStart(2,'0')}:${String(mm).padStart(2,'0')}:00`);
    dt.setMinutes(dt.getMinutes() + Number(durationMinutes));
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
  }

  async function edit(show) {
    setEditingId(show.id);
    setEditLoading(true);
    try {
      console.log('Edit requested for show id=', show.id);
      const full = await showApi.get(show.id);
      console.log('Show API Response', full);
      console.log('Selected Screen', full.screenId);
      console.log('Start Time', full.startTime || full.startsAt || full.startsAt);
      console.log('End Time', full.endTime || full.endsAt || full.endsAt);

      const movieId = full.movieId || full.movie?.id || show.movieId;
      const screenId = full.screenId || full.screen?.id || show.screenId;
      const showDate = full.showDate || (full.startsAt ? full.startsAt.slice(0, 10) : show.showDate);
      const startTime = extractTime(full.startTime || full.startsAt || show.startsAt);
      const endTime = extractTime(full.endTime || full.endsAt || show.endsAt);
      const status = full.status || show.status;

      // ensure screens are loaded before selecting - screens available in data.screens
      console.log('Available screens count:', data.screens.length);

      setForm({
        movieId: String(movieId),
        screenId: String(screenId),
        showDate,
        startTime,
        endTime,
        status,
      });

      // set movie duration if available
      const m = data.movies.find((mv) => String(mv.id) === String(movieId));
      if (m) setMovieDurationMinutes(m.durationMinutes || m.runtime || m.lengthMinutes || m.duration || 120);
    } catch (err) {
      console.error('Failed to fetch show details', err);
      toast({ type: 'error', message: 'Unable to load show details for editing' });
      setEditingId(null);
    } finally {
      setEditLoading(false);
    }
  }

  // when movie changes, fetch movie details to get duration
  async function onMovieChange(movieId) {
    setForm({ ...form, movieId });
    if (!movieId) return;
    try {
      const md = await movieApi.get(Number(movieId));
      const dur = md.durationMinutes || md.runtime || md.lengthMinutes || md.duration || 120;
      setMovieDurationMinutes(Number(dur) || 120);
      // if start time already set and end not manually overridden, auto-calc endTime
      if (form.startTime && !manualEndOverride) {
        const calc = calculateEndFrom(form.showDate || '', form.startTime, Number(dur) || 120);
        setForm((f) => ({ ...f, endTime: calc }));
      }
    } catch (err) {
      // fallback
      setMovieDurationMinutes(120);
    }
  }



  async function save(event) {
    event.preventDefault();
    // validations
    if (!form.movieId) return toast({ type: 'error', message: 'Please select a movie' });
    if (!form.screenId) return toast({ type: 'error', message: 'Please select a screen' });
    if (!form.showDate) return toast({ type: 'error', message: 'Please select a show date' });
    if (!form.startTime) return toast({ type: 'error', message: 'Please select start time' });
    if (!form.endTime) return toast({ type: 'error', message: 'Please select end time' });
    const now = new Date();
    const start = new Date(localDateTimeFromDateAndTime(form.showDate, form.startTime));
    const end = new Date(localDateTimeFromDateAndTime(form.showDate, form.endTime));
    if (start < now) return toast({ type: 'error', message: 'Start time cannot be in the past' });
    if (end <= start) return toast({ type: 'error', message: 'End time must be after start time' });

    // check overlapping shows on same screen
    const overlapping = data.shows.find((s) => {
      if (Number(s.screenId) !== Number(form.screenId)) return false;
      const sStart = new Date(localDateTimeFromDateAndTime(s.showDate || s.startsAt?.slice(0,10), s.startTime || s.startsAt?.slice(11,16)));
      const sEnd = new Date(localDateTimeFromDateAndTime(s.showDate || s.endsAt?.slice(0,10), s.endTime || s.endsAt?.slice(11,16)));
      return (start < sEnd && end > sStart) && (!editingId || s.id !== editingId);
    });
    if (overlapping) return toast({ type: 'error', message: `Overlapping show detected: ${overlapping.movieTitle} ${formatDateTime(overlapping.startsAt || localDateTimeFromDateAndTime(overlapping.showDate, overlapping.startTime))} - ${formatDateTime(overlapping.endsAt || localDateTimeFromDateAndTime(overlapping.showDate, overlapping.endTime))}` });

    const payload = { ...form, movieId: Number(form.movieId), screenId: Number(form.screenId), showDate: form.showDate, startTime: form.startTime, endTime: form.endTime, startsAt: localDateTimeFromDateAndTime(form.showDate, form.startTime), endsAt: localDateTimeFromDateAndTime(form.showDate, form.endTime) };
    console.log('Saving show payload', { editingId, payload });
    console.log('Creating show payload', { editingId, payload });
    if (editingId) {
      try {
        await showApi.update(editingId, payload);
        toast({ type: 'success', message: 'Show updated' });
        // reset only on success
        setForm(emptyShow);
        setEditingId(null);
        setManualEndOverride(false);
        await reload();
      } catch (err) {
        console.error('Show update failed:', err?.response?.data || err);
        toast({ type: 'error', message: err?.response?.data?.message || 'Failed to update show' });
        // do not clear form on failure
      }
    } else {
      try {
        await showApi.create(payload);
        toast({ type: 'success', message: 'Show created and seats generated' });
        // reset only on success
        setForm(emptyShow);
        setManualEndOverride(false);
        await reload();
      } catch (err) {
        console.error('Show creation failed:', err?.response?.data || err);
        toast({ type: 'error', message: err?.response?.data?.message || 'Failed to create show' });
        // do not clear form on failure
      }
    }
  }

  async function remove(id) {
    if (!window.confirm('Delete this show and related bookings?')) return;
    await showApi.remove(id);
    toast({ type: 'success', message: 'Show deleted' });
    await reload();
  }

  if (loading) return <LoadingSkeleton rows={6} />;

  return (
    <AnimatedPage className="grid gap-6">
      <PageHeader title="Manage shows" eyebrow="Admin" description="Create and update scheduled shows. Backend generates show seats from active screen seats." />
      <form onSubmit={save} className="panel grid gap-4 p-5 md:grid-cols-2">
        <div>
          <label className="text-xs text-slate-400">Movie</label>
          <select className="field" value={form.movieId} onChange={(e) => onMovieChange(e.target.value)} required>
            <option value="">Select a movie</option>
            {data.movies.map((movie) => <option key={movie.id} value={String(movie.id)}>{movie.title}</option>)}
          </select>
        </div>

        <div>
          <label className="text-xs text-slate-400">Screen</label>
          <select className="field" value={form.screenId} onChange={(e) => setForm({ ...form, screenId: e.target.value })} required>
            <option value="">Select a screen</option>
            {data.screens.map((screen) => <option key={screen.id} value={String(screen.id)}>Screen {screen.name} · Theater #{screen.theaterId}</option>)}
          </select>
        </div>

        <div>
          <label className="text-xs text-slate-400">Show Date <span title="The date on which the movie will be shown." className="ml-1 text-xs">ℹ️</span></label>
          <input className="field" type="date" value={form.showDate} onChange={(e) => setForm({ ...form, showDate: e.target.value })} required />
        </div>

        <div>
          <label className="text-xs text-slate-400">Start Time <span title="When the movie begins." className="ml-1 text-xs">ℹ️</span></label>
          <input className="field" type="time" value={form.startTime} onChange={(e) => { setForm({ ...form, startTime: e.target.value }); if (!manualEndOverride && movieDurationMinutes) { const calc = calculateEndFrom(form.showDate || '', e.target.value, movieDurationMinutes); setForm((f) => ({ ...f, endTime: calc })); } }} required />
        </div>

        <div>
          <label className="text-xs text-slate-400">End Time <span title="When the movie ends. Auto-calculated from movie duration; override if needed." className="ml-1 text-xs">ℹ️</span></label>
          <input className="field" type="time" value={form.endTime} onChange={(e) => { setManualEndOverride(true); setForm({ ...form, endTime: e.target.value }); }} required />
          <div className="text-xs text-slate-400">Auto-calculated from movie duration ({movieDurationMinutes} minutes). Edit to override.</div>
        </div>


        <div>
          <label className="text-xs text-slate-400">Show Status</label>
          <select className="field" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>{['SCHEDULED','ACTIVE','COMPLETED','CANCELLED'].map((item) => <option key={item}>{item}</option>)}</select>
        </div>

        <div className="md:col-span-2">
          <div className="mb-2 text-sm font-semibold text-slate-200">Show summary preview</div>
          <div className="bg-slate-900 p-3 rounded text-sm text-slate-200">
            <div>Movie: {data.movies.find(m=>m.id===Number(form.movieId))?.title || '-'}</div>
            <div>Screen: {data.screens.find(s=>s.id===Number(form.screenId))?.name || '-'}</div>
            <div>Date: {form.showDate || '-'}</div>
            <div>Start Time: {form.startTime ? form.startTime : '-'}</div>
            <div>End Time: {form.endTime ? form.endTime : '-'}</div>
            <div>Status: {form.status}</div>
          </div>
        </div>

        <div className="flex gap-3 md:col-span-2">
          <Button type="submit">{editingId ? 'Update show' : 'Add show'}</Button>
          {editingId && <Button type="button" variant="secondary" onClick={() => { setEditingId(null); setForm(emptyShow); }}>Cancel</Button>}
        </div>
      </form>
      <div className="grid gap-3">
        {data.shows.map((show) => (
          <div key={show.id} className="panel flex flex-col gap-4 p-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="font-black text-white">{show.movieTitle}</p>
              <p className="text-sm text-slate-400">{show.theaterName} · {formatDateTime(show.startsAt)} · {formatCurrency(show.price)}</p>
            </div>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => edit(show)}>Edit</Button>
              <Button variant="danger" onClick={() => remove(show.id)}>Delete</Button>
            </div>
          </div>
        ))}
      </div>
    </AnimatedPage>
  );
}
