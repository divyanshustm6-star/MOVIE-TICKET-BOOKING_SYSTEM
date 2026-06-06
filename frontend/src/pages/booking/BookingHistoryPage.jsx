import AnimatedPage from '../../components/AnimatedPage.jsx';
import EmptyState from '../../components/EmptyState.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { Link } from 'react-router-dom';
import { bookingApi } from '../../api/bookingApi.js';
import { paymentApi } from '../../api/paymentApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { compactStatus, formatCurrency } from '../../utils/formatters.js';

export default function BookingHistoryPage() {
  const { data: bookingsRaw, loading, error } = useAsync(() => bookingApi.history(), []);
  const bookings = bookingsRaw ?? [];

  async function downloadTicket(bookingId, bookingRef) {
    try {
      const data = await paymentApi.downloadTicket(bookingId);
      const blob = new Blob([data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `ticket-${bookingRef || bookingId}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Failed to download ticket', e);
      alert('Unable to download ticket');
    }
  }

  return (
    <AnimatedPage className="grid gap-6">
      <PageHeader title="Booking history" eyebrow="Your account" description="All bookings created by the signed-in user." />
      {loading && <LoadingSkeleton rows={5} />}
      {error && <EmptyState title="Unable to load bookings" message={error} />}
      {!loading && !error && bookings.length === 0 && <EmptyState title="No bookings yet" message="Book a show to see it here." />}
      <div className="grid gap-4">
        {(bookings || []).map((booking) => (
          <article key={booking?.id} className="panel p-5">
            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="text-xl font-black text-white">{booking?.movieTitle}</p>
                <p className="mt-1 text-sm text-slate-400">{booking?.bookingReference} · {booking?.seatsCount ?? 0} seats</p>
              </div>
              <div className="text-left md:text-right">
                <span className="badge">{compactStatus(booking?.bookingStatus)}</span>
                <p className="mt-2 text-lg font-black text-white">{formatCurrency(booking?.totalAmount ?? 0)}</p>
                <div className="mt-2 flex items-center gap-3 justify-end">
                  <Link className="inline-flex text-sm font-bold text-ember-300" to={`/bookings/${booking?.id}`}>View details</Link>
                  <button className="btn-mini" onClick={() => downloadTicket(booking?.id, booking?.bookingReference)}>Download ticket</button>
                </div>
              </div>
            </div>
          </article>
        ))}
      </div>
    </AnimatedPage>
  );
}
