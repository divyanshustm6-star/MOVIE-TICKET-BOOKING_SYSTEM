import AnimatedPage from '../../components/AnimatedPage.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import StatCard from '../../components/StatCard.jsx';
import { bookingApi } from '../../api/bookingApi.js';
import { movieApi } from '../../api/movieApi.js';
import { paymentApi } from '../../api/paymentApi.js';
import { showApi } from '../../api/showApi.js';
import { userApi } from '../../api/userApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { formatCurrency } from '../../utils/formatters.js';

export default function AdminDashboardPage() {
  const { data, loading } = useAsync(async () => {
    const [movies, shows, bookings, payments, users] = await Promise.all([
      movieApi.list(),
      showApi.list(),
      bookingApi.list(),
      paymentApi.list(),
      userApi.list(),
    ]);
    return { movies, shows, bookings, payments, users };
  }, []);

  if (loading) return <LoadingSkeleton rows={6} className="md:grid-cols-3" />;

  const revenue = (data.payments || []).filter((payment) => payment.paymentStatus === 'SUCCESS').reduce((sum, payment) => sum + Number(payment.amount || 0), 0);

  return (
    <AnimatedPage className="grid gap-6">
      <div>
        <p className="text-xs font-black uppercase tracking-[0.3em] text-ember-400">Operations</p>
        <h1 className="mt-2 text-4xl font-black text-white">Admin dashboard</h1>
      </div>
      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <StatCard label="Movies" value={data.movies.length} />
        <StatCard label="Shows" value={data.shows.length} />
        <StatCard label="Bookings" value={data.bookings.length} />
        <StatCard label="Users" value={data.users.length} />
        <StatCard label="Revenue" value={formatCurrency(revenue)} />
      </section>
      <section className="panel overflow-hidden">
        <div className="border-b border-white/10 p-5">
          <h2 className="text-xl font-black text-white">Recent bookings</h2>
        </div>
        <div className="divide-y divide-white/10">
          {data.bookings.slice(0, 8).map((booking) => (
            <div key={booking.id} className="grid gap-2 p-5 md:grid-cols-[1fr_auto]">
              <div>
                <p className="font-black text-white">{booking.movieTitle}</p>
                <p className="text-sm text-slate-400">{booking.bookingReference}</p>
              </div>
              <p className="font-black text-white">{formatCurrency(booking.totalAmount)}</p>
            </div>
          ))}
        </div>
      </section>
    </AnimatedPage>
  );
}
