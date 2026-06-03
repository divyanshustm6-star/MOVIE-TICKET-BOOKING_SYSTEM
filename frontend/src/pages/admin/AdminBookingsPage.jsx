import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { bookingApi } from '../../api/bookingApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { useToastStore } from '../../store/toastStore.js';
import { bookingStatuses } from '../../utils/constants.js';
import { compactStatus, formatCurrency } from '../../utils/formatters.js';

export default function AdminBookingsPage() {
  const toast = useToastStore((state) => state.push);
  const { data: bookings = [], loading, reload } = useAsync(() => bookingApi.list(), []);

  async function updateStatus(id, bookingStatus) {
    await bookingApi.update(id, { bookingStatus });
    toast({ type: 'success', message: 'Booking updated' });
    await reload();
  }

  async function remove(id) {
    if (!window.confirm('Delete this booking?')) return;
    await bookingApi.remove(id);
    toast({ type: 'success', message: 'Booking deleted' });
    await reload();
  }

  return (
    <AnimatedPage className="grid gap-6">
      <PageHeader title="Manage bookings" eyebrow="Admin" />
      {loading ? <LoadingSkeleton rows={6} /> : (
        <div className="grid gap-3">
          {bookings.map((booking) => (
            <div key={booking.id} className="panel grid gap-4 p-4 xl:grid-cols-[1fr_220px_auto] xl:items-center">
              <div>
                <p className="font-black text-white">{booking.movieTitle}</p>
                <p className="text-sm text-slate-400">{booking.bookingReference} · {formatCurrency(booking.totalAmount)} · {compactStatus(booking.bookingStatus)}</p>
              </div>
              <select className="field" value={booking.bookingStatus} onChange={(e) => updateStatus(booking.id, e.target.value)}>
                {bookingStatuses.map((status) => <option key={status}>{status}</option>)}
              </select>
              <Button variant="danger" onClick={() => remove(booking.id)}>Delete</Button>
            </div>
          ))}
        </div>
      )}
    </AnimatedPage>
  );
}
