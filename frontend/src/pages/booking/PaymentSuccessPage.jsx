import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { bookingApi } from '../../api/bookingApi.js';
import { paymentApi } from '../../api/paymentApi.js';
import { formatCurrency } from '../../utils/formatters.js';
import RazorpayLogo from '../../components/razorpay-logo.svg';

export default function PaymentSuccessPage() {
  const navigate = useNavigate();

  const { data: bookings, loading } = useAsync(() => bookingApi.history().catch(() => []), []);
  const latest = Array.isArray(bookings) && bookings.length ? bookings[0] : null;

  useEffect(() => {
    // load confetti script and fire confetti once
    (async () => {
      try {
        if (!window.confetti) {
          const s = document.createElement('script');
          s.src = 'https://cdn.jsdelivr.net/npm/canvas-confetti@1.5.1/dist/confetti.browser.min.js';
          document.head.appendChild(s);
          await new Promise((r) => (s.onload = r));
        }
        // small confetti burst
        if (window.confetti) {
          window.confetti({
            particleCount: 120,
            spread: 70,
            origin: { y: 0.2 },
            colors: ['#f59e0b', '#ffffff', '#ffd79e'],
          });
        }
      } catch (e) {
        // ignore confetti errors
        // console.debug('confetti load failed', e);
      }
    })();
  }, []);

  async function downloadTicket() {
    try {
      const bookingId = latest?.id;
      if (!bookingId) return;
      const data = await paymentApi.downloadTicket(bookingId);
      const blob = new Blob([data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `ticket-${latest.bookingReference || bookingId}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Failed to download ticket', e);
      alert('Unable to download ticket');
    }
  }

  if (loading) return <AnimatedPage className="mx-auto max-w-2xl"><LoadingSkeleton rows={6} /></AnimatedPage>;

  return (
    <AnimatedPage className="mx-auto max-w-3xl px-4 md:px-8">
      <div className="panel p-8 grid gap-6 text-center">
        <div className="mx-auto flex flex-col items-center gap-4">
          <div className="rounded-full bg-[rgba(21,128,61,0.12)] w-20 h-20 flex items-center justify-center shadow-lg">
            <svg className="h-10 w-10 text-emerald-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20 6L9 17l-5-5" strokeLinecap="round" strokeLinejoin="round"/></svg>
          </div>

          <h1 className="text-3xl font-black text-white">Payment successful</h1>
          <p className="text-slate-300">Your booking is confirmed. A copy of the ticket has been sent to your email if available.</p>

          {latest && (
            <div className="mt-4 p-4 rounded-2xl bg-[rgba(255,255,255,0.03)] border border-[rgba(255,255,255,0.06)] backdrop-blur-sm shadow-md text-left w-full md:w-2/3">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-xs text-slate-400">Booking Reference</div>
                  <div className="text-lg font-black text-white">{latest.bookingReference}</div>
                </div>
                <img src={RazorpayLogo} alt="Razorpay" className="h-6" />
              </div>

              <div className="mt-3 grid gap-2 text-sm text-slate-300">
                <div className="flex justify-between"><span>Movie</span><span className="text-white">{latest.movieTitle}</span></div>
                <div className="flex justify-between"><span>Seats</span><span className="text-white">{(latest.seatNumbers && latest.seatNumbers.length) ? latest.seatNumbers.join(', ') : (latest.seats?.map((s) => `${s.rowLabel}${s.seatNumber}`).join(', ') || latest.seatLabels || 'N/A')}</span></div>
                <div className="flex justify-between"><span>Amount Paid</span><span className="text-white">{formatCurrency(latest.totalAmount)}</span></div>
              </div>
            </div>
          )}

          <div className="mt-4 flex flex-col md:flex-row gap-3">
            <Button className="btn-primary" onClick={downloadTicket} disabled={!latest}>Download Ticket</Button>
            <Button className="btn-secondary" onClick={() => navigate('/bookings')}>View Bookings</Button>
            <Button className="" onClick={() => navigate('/')}>Back to Home</Button>
          </div>
        </div>
      </div>
    </AnimatedPage>
  );
}
