import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import EmptyState from '../../components/EmptyState.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import { bookingApi } from '../../api/bookingApi.js';
import { showApi } from '../../api/showApi.js';
import { useBookingStore } from '../../store/bookingStore.js';
import { useToastStore } from '../../store/toastStore.js';
import { formatCurrency, formatDateTime } from '../../utils/formatters.js';

const GST_RATE = 0.18;

function LegendItem({ colorClass, label }) {
  return (
    <div className="flex items-center gap-2">
      <div className={`w-5 h-5 rounded ${colorClass} border border-white/10`} />
      <div className="text-sm text-slate-300">{label}</div>
    </div>
  );
}

function seatStatusLabel(status) {
  if (!status) return 'Unknown';
  return {
    AVAILABLE: 'Available',
    LOCKED: 'Locked',
    BOOKED: 'Booked',
  }[status] ?? status;
}

function SeatButton({ seat, isSelected, onToggle }) {
  const disabled = !seat || seat.seatStatus === 'BOOKED' || seat.seatStatus === 'LOCKED';
  const base = 'w-10 h-10 md:w-11 md:h-11 rounded-md flex items-center justify-center text-xs md:text-sm shadow-sm transform transition';
  const color = seat
    ? isSelected
      ? 'bg-yellow-400 text-black ring-2 ring-yellow-300'
      : seat.seatStatus === 'BOOKED'
      ? 'bg-red-600 text-white'
      : seat.seatStatus === 'LOCKED'
      ? 'bg-gray-500 text-white'
      : seat.seatType === 'PLATINUM'
      ? 'bg-gradient-to-br from-amber-300 to-amber-400 text-black'
      : seat.seatType === 'GOLD'
      ? 'bg-gradient-to-br from-yellow-200 to-amber-200 text-black'
      : 'bg-green-200 text-black'
    : 'bg-transparent';

  const title = seat ? `${seat.rowLabel}${seat.seatNumber}\n₹${seat.price}\n${seatStatusLabel(seat.seatStatus)}` : '';

  return (
    <button
      type="button"
      title={title}
      aria-disabled={disabled}
      onClick={() => !disabled && onToggle(seat)}
      className={`${base} ${disabled ? 'opacity-80 cursor-not-allowed' : 'cursor-pointer hover:scale-105'} ${color}`}
      disabled={disabled}
    >
      {seat ? `${seat.rowLabel}${seat.seatNumber}` : ''}
    </button>
  );
}

export default function SeatSelectionPage() {
  const { showId } = useParams();
  const navigate = useNavigate();
  const toast = useToastStore((s) => s.push);
  const { selectedSeatIds, toggleSeat, clearSelection, setLastBooking } = useBookingStore();

  const [show, setShow] = useState(null);
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError(null);
    Promise.all([showApi.get(showId), showApi.seats(showId)])
      .then(([s, seatList]) => {
        if (!mounted) return;
        setShow(s);
        setSeats(Array.isArray(seatList) ? seatList : []);
        clearSelection();
      })
      .catch((err) => {
        console.error('Error loading seats', err);
        setError('Unable to load seats');
      })
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [showId]);

  // Build row -> seatNumber map using real DB seats
  const rows = useMemo(() => {
    const map = new Map();
    seats.forEach((s) => {
      const r = s.rowLabel || 'A';
      if (!map.has(r)) map.set(r, new Map());
      map.get(r).set(Number(s.seatNumber), s);
    });
    // sort rows by row label lexicographically (A, B, C...) but keep numeric order if labels are numbers
    const sortedRows = Array.from(map.keys()).sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
    const rowsArr = sortedRows.map((r) => {
      const seatsMap = map.get(r);
      const maxSeat = Math.max(...Array.from(seatsMap.keys()));
      const rowSeats = [];
      for (let i = 1; i <= maxSeat; i++) {
        rowSeats.push(seatsMap.get(i) ?? null);
      }
      return { label: r, seats: rowSeats };
    });
    return rowsArr;
  }, [seats]);

  const totalSeats = seats.length;
  const availableSeatsCount = seats.filter((s) => s.seatStatus === 'AVAILABLE').length;
  const bookedSeatsCount = seats.filter((s) => s.seatStatus === 'BOOKED').length;
  const lockedSeatsCount = seats.filter((s) => s.seatStatus === 'LOCKED').length;
  const selectedSeats = useMemo(() => seats.filter((s) => selectedSeatIds.includes(s.id)), [seats, selectedSeatIds]);

  const subtotal = selectedSeats.reduce((sum, s) => sum + Number(s.price || 0), 0);
  const gst = subtotal * GST_RATE;
  const grandTotal = subtotal + gst;

  async function confirmBooking() {
    if (selectedSeatIds.length === 0) {
      toast({ type: 'error', message: 'Select at least one seat' });
      return;
    }
    setSubmitting(true);
    try {
      const booking = await bookingApi.create({ showId: Number(showId), showSeatIds: selectedSeatIds });
      setLastBooking(booking);
      toast({ type: 'success', message: 'Seats locked. Confirm payment to finish.' });
      navigate(`/booking/${booking.id}/confirm`);
    } catch (err) {
      console.error('Booking error', err);
      toast({ type: 'error', message: err?.message || 'Unable to lock seats' });
    } finally {
      setSubmitting(false);
    }
  }

  function handleToggleSeat(seat) {
    if (!seat) return;
    if (seat.seatStatus !== 'AVAILABLE') return;
    toggleSeat(seat.id);
  }

  if (loading) return <LoadingSkeleton rows={8} />;
  if (error) return <EmptyState title="Unable to load seats" message={error} />;

  return (
    <AnimatedPage className="grid gap-6 lg:grid-cols-[1fr_380px] px-4 md:px-8">
      <section className="space-y-6">
        <div>
          <p className="text-xs font-black uppercase tracking-wider text-amber-300">Seat selection</p>
          <h1 className="mt-2 text-3xl md:text-4xl font-extrabold text-white">{show?.movieTitle}</h1>
          <p className="mt-1 text-sm text-slate-400">{show?.theaterName} · {show?.screenName} · {formatDateTime(show?.startsAt)}</p>
        </div>

        <div className="mx-auto w-full max-w-5xl">
          <div className="bg-slate-800/40 rounded-md p-3 text-center text-sm md:text-base text-amber-100/90 font-semibold tracking-wider shadow-md glass">
            <div className="border-t border-b border-slate-700 py-2">— — — — — — — — — — — — — — — —</div>
            <div className="mt-2 text-amber-100">SCREEN</div>
            <div className="mt-2 border-t border-slate-700 pt-2"> </div>
          </div>

          <div className="flex items-center justify-between mt-4 mb-2">
            <div className="flex gap-4">
              <LegendItem colorClass="bg-green-200 text-black" label="Available" />
              <LegendItem colorClass="bg-red-600 text-white" label="Booked" />
              <LegendItem colorClass="bg-yellow-400 text-black" label="Selected" />
              <LegendItem colorClass="bg-gray-500 text-white" label="Locked" />
            </div>
            <div className="text-sm text-slate-400">Total: {totalSeats} · Available: {availableSeatsCount} · Booked: {bookedSeatsCount}</div>
          </div>

          <div className="bg-slate-900/30 p-4 rounded-lg glass shadow-lg overflow-auto">
            <div className="space-y-3">
              {rows.map((row) => (
                <div key={row.label} className="flex items-center gap-4">
                  <div className="w-8 md:w-10 text-slate-300 text-sm font-semibold">{row.label}</div>
                  <div className="flex gap-2 md:gap-3 flex-wrap">
                    {row.seats.map((s, idx) => (
                      <SeatButton key={`${row.label}-${idx}`} seat={s} isSelected={s && selectedSeatIds.includes(s.id)} onToggle={handleToggleSeat} />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <aside className="panel h-max p-5 glass rounded-lg shadow-xl">
        <h2 className="text-xl md:text-2xl font-extrabold text-white">Booking summary</h2>
        <div className="mt-4 text-sm text-slate-300 space-y-3">
          <div>
            <div className="text-slate-400 text-xs">Selected seats</div>
            <div className="mt-1 text-white font-semibold">{selectedSeats.length ? selectedSeats.map((s) => `${s.rowLabel}${s.seatNumber}`).join(', ') : 'None'}</div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="text-slate-400 text-xs">Seat count</div>
            <div className="text-white text-right">{selectedSeats.length}</div>

            <div className="text-slate-400 text-xs">Subtotal</div>
            <div className="text-white text-right">{formatCurrency(subtotal)}</div>

            <div className="text-slate-400 text-xs">GST (18%)</div>
            <div className="text-white text-right">{formatCurrency(gst)}</div>

            <div className="text-slate-400 text-xs">Grand total</div>
            <div className="text-amber-200 text-right font-bold">{formatCurrency(grandTotal)}</div>
          </div>

          <div className="mt-4">
            <Button className="w-full" disabled={submitting || selectedSeats.length === 0} onClick={confirmBooking}>
              {submitting ? 'Locking seats...' : `Confirm ${selectedSeats.length} seat${selectedSeats.length !== 1 ? 's' : ''}`}
            </Button>
          </div>

          <div className="mt-4 text-xs text-slate-500">Tip: Click seats to select. Booked or locked seats are disabled.</div>
        </div>
      </aside>
    </AnimatedPage>
  );
}
