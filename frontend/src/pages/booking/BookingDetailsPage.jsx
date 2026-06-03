import { Link, useParams } from 'react-router-dom';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import EmptyState from '../../components/EmptyState.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import { bookingApi } from '../../api/bookingApi.js';
import { paymentApi } from '../../api/paymentApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { useAuthStore } from '../../store/authStore.js';
import { compactStatus, formatCurrency } from '../../utils/formatters.js';

export default function BookingDetailsPage() {
  const { bookingId } = useParams();
  const isAdmin = useAuthStore((state) => state.isAdmin);
  const { data, loading, error } = useAsync(async () => {
    const booking = isAdmin()
      ? await bookingApi.get(bookingId)
      : await bookingApi.history().then((items) => items.find((item) => String(item.id) === String(bookingId)));
    const payments = booking ? await paymentApi.byBooking(booking.id).catch(() => []) : [];
    return { booking, payments };
  }, [bookingId]);

  if (loading) return <LoadingSkeleton rows={4} />;
  if (error || !data?.booking) return <EmptyState title="Booking not found" message={error || 'This booking is not available for your account.'} />;

  const { booking, payments } = data;

  return (
    <AnimatedPage className="mx-auto grid max-w-4xl gap-6">
      <div className="panel p-6">
        <p className="text-xs font-black uppercase tracking-[0.3em] text-ember-400">Booking details</p>
        <h1 className="mt-3 text-4xl font-black text-white">{booking.movieTitle}</h1>
        <p className="mt-2 text-slate-400">{booking.bookingReference}</p>
        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
            <p className="text-sm text-slate-400">Status</p>
            <p className="mt-1 text-lg font-black text-white">{compactStatus(booking.bookingStatus)}</p>
          </div>
          <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
            <p className="text-sm text-slate-400">Total</p>
            <p className="mt-1 text-lg font-black text-white">{formatCurrency(booking.totalAmount)}</p>
          </div>
          <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
            <p className="text-sm text-slate-400">Seats</p>
            <p className="mt-1 text-lg font-black text-white">{booking.seats?.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(', ') || booking.seatsCount}</p>
          </div>
          <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
            <p className="text-sm text-slate-400">Show ID</p>
            <p className="mt-1 text-lg font-black text-white">{booking.showId}</p>
          </div>
        </div>
        {booking.bookingStatus === 'PENDING' && (
          <Link className="btn-primary mt-6" to={`/booking/${booking.id}/confirm`}>Complete payment</Link>
        )}
      </div>
      <div className="panel p-6">
        <h2 className="text-xl font-black text-white">Payments</h2>
        {payments.length === 0 ? (
          <p className="mt-3 text-sm text-slate-400">No payment records found.</p>
        ) : (
          <div className="mt-4 grid gap-3">
            {payments.map((payment) => (
              <div key={payment.id} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <p className="font-black text-white">{payment.paymentReference}</p>
                <p className="mt-1 text-sm text-slate-400">{payment.provider} · {payment.paymentMethod} · {compactStatus(payment.paymentStatus)} · {formatCurrency(payment.amount)}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </AnimatedPage>
  );
}
