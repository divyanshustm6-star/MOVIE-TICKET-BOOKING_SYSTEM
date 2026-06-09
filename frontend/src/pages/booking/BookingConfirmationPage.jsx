import { useNavigate, useParams } from 'react-router-dom';
import { useState } from 'react';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import EmptyState from '../../components/EmptyState.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import { bookingApi } from '../../api/bookingApi.js';
import { paymentApi } from '../../api/paymentApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { useToastStore } from '../../store/toastStore.js';
import { formatCurrency, formatDateTime } from '../../utils/formatters.js';
import RazorpayLogo from '../../components/razorpay-logo.svg';

// load script helper
function loadScript(src) {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) return resolve();
    const s = document.createElement('script');
    s.src = src;
    s.onload = () => resolve();
    s.onerror = () => reject(new Error('Failed to load script ' + src));
    document.body.appendChild(s);
  });
}

export default function BookingConfirmationPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const toast = useToastStore((state) => state.push);
  const [paying, setPaying] = useState(false);
  const { data: booking, loading, error } = useAsync(() => bookingApi.get(bookingId).catch(() => bookingApi.history().then((items) => items.find((item) => String(item.id) === String(bookingId)))), [bookingId]);

  const seats = booking?.seatNumbers ?? (booking?.seats ? booking.seats.map(s => `${s.rowLabel}${s.seatNumber}`) : []);
  const seatCount = booking?.seatsCount || seats.length || booking?.seatCount || 1;
  const totalAmount = Number(booking?.totalAmount || 0);
  const convenience = Number(booking?.convenienceFee || booking?.convenience || 0);
  const gst = Number(booking?.gstAmount ?? Math.round(((totalAmount - convenience) * 0.18) * 100) / 100);
  const ticketSubtotal = Math.max(0, totalAmount - convenience - gst);
  const perSeat = seatCount ? Math.round((ticketSubtotal / seatCount) * 100) / 100 : ticketSubtotal;

  async function pay() {
    setPaying(true);
    try {
      const orderResp = await paymentApi.createOrder({ bookingId: Number(bookingId) });
      await loadScript('https://checkout.razorpay.com/v1/checkout.js');

      const options = {
        key: orderResp.key,
        amount: orderResp.amount,
        currency: orderResp.currency,
        name: booking?.movieTitle || 'Cinema Booking',
        description: `Booking ${booking?.bookingReference || ''}`,
        order_id: orderResp.orderId,
        handler: async function (response) {
          try {
            await paymentApi.verifyPayment({
              bookingId: Number(bookingId),
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            toast({ type: 'success', message: 'Payment successful. Booking confirmed.' });
            navigate('/payment-success');
          } catch (err) {
            console.error('Verification failed', err);
            toast({ type: 'error', message: err?.response?.data?.message || 'Payment verification failed' });
            navigate('/payment-failed');
          }
        },
        modal: {
          ondismiss: function () {
            toast({ type: 'info', message: 'Payment cancelled' });
          },
        },
        prefill: {
          name: booking?.userName || '',
          email: booking?.userEmail || '',
        },
        notes: { bookingId: bookingId },
        theme: { color: '#f59e0b' },
      };

      const rzp = new window.Razorpay(options);
      rzp.open();
    } catch (e) {
      console.error(e);
      toast({ type: 'error', message: e?.response?.data?.message || 'Failed to start payment' });
    } finally {
      setPaying(false);
    }
  }

  if (loading) return <LoadingSkeleton rows={6} />;
  if (error || !booking) return <EmptyState title="Booking not found" message={error || 'Unable to read booking details.'} />;

  return (
    <AnimatedPage className="mx-auto max-w-6xl px-4 md:px-8">
      <div className="grid gap-6 md:grid-cols-[1fr_420px] items-start">
        <section className="space-y-4">
          <div className="flex items-start gap-4">
            {/* Poster thumbnail */}
            {booking?.posterUrl || booking?.moviePoster || booking?.image ? (
              // eslint-disable-next-line jsx-a11y/img-redundant-alt
              <img src={booking.posterUrl || booking.moviePoster || booking.image} alt={`${booking.movieTitle} poster`} className="w-24 h-36 rounded-lg object-cover shadow-xl" />
            ) : (
              <div className="w-24 h-36 rounded-lg bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center text-slate-500">Poster</div>
            )}

            <div>
              <h1 className="text-2xl md:text-3xl font-extrabold text-white">{booking.movieTitle}</h1>
              <p className="mt-1 text-sm text-slate-400">{booking.theaterName} · {booking.screenName}</p>
              <p className="mt-1 text-sm text-slate-400">{formatDateTime(booking.startsAt || booking.showStartsAt || booking.showTime)}</p>
            </div>
          </div>

          <div className="panel p-4 md:p-6 rounded-2xl bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.06)] backdrop-blur-sm shadow-md">
            <h2 className="text-lg font-black text-white">Booking summary</h2>
            <div className="mt-3 grid gap-2 text-sm text-slate-300">
              <div className="flex justify-between"><span>Reference</span><strong className="text-white">{booking.bookingReference}</strong></div>
              <div className="flex justify-between"><span>Movie</span><span className="text-white">{booking.movieTitle}</span></div>
              <div className="flex justify-between"><span>Theater</span><span className="text-white">{booking.theaterName}</span></div>
              <div className="flex justify-between"><span>Seats</span><span className="text-white">{(seats && seats.length) ? (Array.isArray(seats) ? seats.join(', ') : seats) : booking.seatLabels || 'N/A'}</span></div>
              <div className="flex justify-between"><span>Seat count</span><span className="text-white">{seatCount}</span></div>
              <div className="flex justify-between"><span>Ticket price</span><span className="text-white">{formatCurrency(perSeat)}</span></div>
              <div className="flex justify-between"><span>GST (18%)</span><span className="text-white">{formatCurrency(gst)}</span></div>
              <div className="flex justify-between"><span>Convenience fee</span><span className="text-white">{formatCurrency(convenience)}</span></div>
              <div className="flex justify-between border-t border-white/10 pt-3 text-xl text-white"><span className="font-black">Total</span><span className="font-black">{formatCurrency(totalAmount)}</span></div>
            </div>
          </div>

          <div className="mt-4">
            <div className="panel p-4 rounded-2xl bg-[rgba(0,0,0,0.35)] border border-[rgba(255,255,255,0.04)] backdrop-blur-sm shadow-sm flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="text-2xl">🔒</div>
                <div>
                  <div className="text-sm text-slate-300">100% Secure payment</div>
                  <div className="text-xs text-slate-400">Powered by Razorpay</div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <img src={RazorpayLogo} alt="Razorpay" className="h-6" />
              </div>
            </div>

            <div className="mt-4 flex items-center gap-3">
              <Button className="btn-primary flex-1" onClick={pay} disabled={paying}>
                {paying ? 'Processing payment...' : 'Pay Securely'}
              </Button>
              <Button className="btn-secondary" onClick={() => navigate(-1)}>Cancel</Button>
            </div>

            <div className="mt-3 text-xs text-slate-400">We will never ask for your card details on this site. You'll be redirected to Razorpay's secure checkout.</div>
          </div>
        </section>

        <aside className="">
          <div className="panel p-5 rounded-2xl bg-[rgba(255,255,255,0.02)] border border-[rgba(255,255,255,0.04)] backdrop-blur-sm shadow-md">
            <h3 className="text-lg font-black text-white">Payment methods</h3>
            <div className="mt-3 grid gap-3">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-[rgba(255,255,255,0.03)] flex items-center justify-center text-amber-400">💳</div>
                <div>
                  <div className="text-white font-semibold">Cards</div>
                  <div className="text-xs text-slate-400">Credit & Debit cards</div>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-[rgba(255,255,255,0.03)] flex items-center justify-center text-amber-400">🏦</div>
                <div>
                  <div className="text-white font-semibold">Net Banking</div>
                  <div className="text-xs text-slate-400">Choose your bank</div>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-[rgba(255,255,255,0.03)] flex items-center justify-center text-amber-400">📱</div>
                <div>
                  <div className="text-white font-semibold">UPI</div>
                  <div className="text-xs text-slate-400">Google Pay, PhonePe, Paytm & more</div>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-[rgba(255,255,255,0.03)] flex items-center justify-center text-amber-400">👛</div>
                <div>
                  <div className="text-white font-semibold">Wallets</div>
                  <div className="text-xs text-slate-400">Paytm Wallet, Mobikwik</div>
                </div>
              </div>
            </div>

            <div className="mt-6 text-xs text-slate-400">
              <div className="flex items-center gap-2"><span className="text-amber-400">•</span> Secure checkout powered by Razorpay</div>
              <div className="flex items-center gap-2 mt-2"><span className="text-amber-400">•</span> 256-bit SSL encryption</div>
            </div>
          </div>
        </aside>
      </div>
    </AnimatedPage>
  );
}
