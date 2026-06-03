import { compactStatus } from '../utils/formatters.js';

export default function SeatGrid({ seats, selectedSeatIds, onToggle }) {
  const grouped = seats.reduce((acc, seat) => {
    const row = seat.rowLabel || 'A';
    acc[row] = [...(acc[row] || []), seat];
    return acc;
  }, {});

  return (
    <div className="panel p-5">
      <div className="mx-auto mb-8 h-2 max-w-xl rounded-full bg-gradient-to-r from-transparent via-ember-400 to-transparent shadow-glow" />
      <div className="grid gap-4">
        {Object.entries(grouped).map(([row, rowSeats]) => (
          <div key={row} className="grid grid-cols-[32px_1fr] items-center gap-3">
            <span className="text-sm font-black text-slate-400">{row}</span>
            <div className="flex flex-wrap gap-2">
              {rowSeats.sort((a, b) => a.seatNumber - b.seatNumber).map((seat) => {
                const unavailable = seat.seatStatus !== 'AVAILABLE';
                const selected = selectedSeatIds.includes(seat.id);
                return (
                  <button
                    key={seat.id}
                    type="button"
                    disabled={unavailable}
                    title={`${row}${seat.seatNumber} · ${compactStatus(seat.seatStatus)}`}
                    onClick={() => onToggle(seat.id)}
                    className={[
                      'h-10 w-10 rounded-lg border text-xs font-black transition',
                      selected ? 'border-ember-400 bg-ember-500 text-white shadow-glow' : 'border-white/10 bg-white/10 text-slate-200 hover:bg-white/20',
                      unavailable ? 'cursor-not-allowed border-red-400/20 bg-red-950/40 text-red-300 opacity-60' : '',
                    ].join(' ')}
                  >
                    {seat.seatNumber}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
      <div className="mt-6 flex flex-wrap gap-4 text-xs font-bold text-slate-400">
        <span><b className="mr-2 inline-block h-3 w-3 rounded bg-white/15" />Available</span>
        <span><b className="mr-2 inline-block h-3 w-3 rounded bg-ember-500" />Selected</span>
        <span><b className="mr-2 inline-block h-3 w-3 rounded bg-red-950" />Unavailable</span>
      </div>
    </div>
  );
}
