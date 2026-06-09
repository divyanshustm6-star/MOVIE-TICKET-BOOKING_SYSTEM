import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import EmptyState from '../../components/EmptyState.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { showApi } from '../../api/showApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { compactStatus, formatCurrency, formatDateTime } from '../../utils/formatters.js';
import { useAuthStore } from '../../store/authStore.js';
import { useLoginRequiredStore } from '../../store/authModalStore.js';

export default function ShowsPage() {
  const [date, setDate] = useState('');
  const navigate = useNavigate();
  const { token } = useAuthStore();
  const loginRequired = useLoginRequiredStore();

  const handleChooseSeats = (showId) => {
    if (!token) {
      loginRequired.open(() => {
        navigate(`/book/${showId}`);
      });
    } else {
      navigate(`/book/${showId}`);
    }
  };

  const { data: showsRaw, loading, error, reload } = useAsync(() => showApi.list(date ? { date } : {}), [date]);
  const shows = showsRaw ?? [];

  return (
    <AnimatedPage className="grid gap-8">
      <PageHeader title="Show listings" eyebrow="Book seats" description="Shows come from the backend `/api/shows` endpoint and can be filtered by show date." />
      <div className="panel flex flex-col gap-3 p-4 sm:flex-row">
        <input className="field sm:max-w-xs" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <button className="btn-secondary" type="button" onClick={() => { setDate(''); reload().catch(() => {}); }}>Reset</button>
      </div>
      {loading && <LoadingSkeleton rows={5} />}
      {error && <EmptyState title="Unable to load shows" message={error} />}
      {!loading && !error && shows.length === 0 && <EmptyState title="No shows available" />}
      <div className="grid gap-4">
        {(shows || []).map((show) => (
          <div key={show?.id} className="panel grid gap-4 p-5 lg:grid-cols-[1fr_auto] lg:items-center">
            <div>
              <p className="text-xl font-black text-white">{show?.movieTitle}</p>
              <p className="mt-2 text-sm text-slate-400">{show?.theaterName} · {show?.screenName} · {formatDateTime(show?.startsAt)}</p>
              <div className="mt-3 flex flex-wrap gap-2">
                <span className="badge">{compactStatus(show?.status)}</span>
                <span className="badge">{formatCurrency(show?.price ?? 0)}</span>
              </div>
            </div>
            <button
              type="button"
              onClick={() => handleChooseSeats(show?.id)}
              className="btn-primary"
            >
              Choose seats
            </button>
          </div>
        ))}
      </div>
    </AnimatedPage>
  );
}
