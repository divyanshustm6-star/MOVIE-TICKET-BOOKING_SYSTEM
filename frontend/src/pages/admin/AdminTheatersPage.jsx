import { useState } from 'react';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { theaterApi } from '../../api/theaterApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { useToastStore } from '../../store/toastStore.js';
import { screenTypeOptions, screenTypeLabel, seatTypes } from '../../utils/constants.js';

const emptyTheater = { name: '', addressLine1: '', addressLine2: '', city: '', state: '', postalCode: '', country: 'India', contactPhone: '', status: 'ACTIVE' };
const emptyScreen = { theaterId: '', name: '', screenType: 'STANDARD', totalSeats: 50, status: 'ACTIVE' };
const emptySeat = { screenId: '', rowLabel: 'A', seatNumber: 1, seatType: 'REGULAR', status: 'ACTIVE' };

export default function AdminTheatersPage() {
  const toast = useToastStore((state) => state.push);
  const { data, loading, reload } = useAsync(async () => {
    const theaters = await theaterApi.list();
    const screens = (await Promise.all(theaters.map((theater) => theaterApi.screens(theater.id).catch(() => [])))).flat();
    return { theaters, screens };
  }, []);
  const [theater, setTheater] = useState(emptyTheater);
  const [screen, setScreen] = useState(emptyScreen);
  const [seat, setSeat] = useState(emptySeat);

  // generate seats form state
  const [genScreenId, setGenScreenId] = useState('');
  const [genStartRow, setGenStartRow] = useState('A');
  const [genEndRow, setGenEndRow] = useState('E');
  const [genSeatsPerRow, setGenSeatsPerRow] = useState(10);
  const [genSeatType, setGenSeatType] = useState('REGULAR');
  const [genSeatPrice, setGenSeatPrice] = useState(150);
  const [genExistingCount, setGenExistingCount] = useState(0);
  const [genPreview, setGenPreview] = useState([]);
  const [genPreviewOpen, setGenPreviewOpen] = useState(false);

  async function createTheater(event) {
    event.preventDefault();
    if (!theater.name || theater.name.trim().length === 0) return toast({ type: 'error', message: 'Please enter theater name.' });
    if (!theater.addressLine1 || theater.addressLine1.trim().length === 0) return toast({ type: 'error', message: 'Please enter theater address.' });
    await theaterApi.create(theater);
    toast({ type: 'success', message: 'Theater created' });
    setTheater(emptyTheater);
    await reload();
  }

  function clearTheaterForm() {
    setTheater(emptyTheater);
  }

  function clearScreenForm() {
    setScreen(emptyScreen);
  }

  async function createScreen(event) {
    event.preventDefault();
    if (!screen.theaterId) return toast({ type: 'error', message: 'Please select a theater before creating a screen.' });
    if (!screen.name || String(screen.name).trim().length === 0) return toast({ type: 'error', message: 'Please enter a screen name.' });
    if (!screen.totalSeats || Number(screen.totalSeats) < 1) return toast({ type: 'error', message: 'Screen capacity must be at least 1.' });
    // validate screen type against supported backend enum values
    const allowedTypes = screenTypeOptions.map((o) => o.value);
    if (!allowedTypes.includes(screen.screenType)) return toast({ type: 'error', message: 'Unsupported screen type selected.' });
    await theaterApi.createScreen({ ...screen, theaterId: Number(screen.theaterId), totalSeats: Number(screen.totalSeats) });
    toast({ type: 'success', message: 'Screen created' });
    setScreen(emptyScreen);
    await reload();
  }

  // bulk generate seats using individual createSeat calls (backend should provide a bulk endpoint ideally)
  async function generateSeats(event) {
    event && event.preventDefault();
    if (!genScreenId) return toast({ type: 'error', message: 'Please select a screen before generating seats.' });
    const start = (genStartRow || 'A').toUpperCase();
    const end = (genEndRow || start).toUpperCase();
    if (start.charCodeAt(0) > end.charCodeAt(0)) return toast({ type: 'error', message: 'Ending Row must be after Starting Row.' });
    const perRow = Number(genSeatsPerRow) || 0;
    if (perRow < 1) return toast({ type: 'error', message: 'Seats Per Row must be greater than 0.' });
    const price = Number(genSeatPrice) || 0;
    if (price <= 0) return toast({ type: 'error', message: 'Please enter a valid seat price (> 0).' });

    const screensMap = (data && data.screens) ? Object.fromEntries(data.screens.map(s => [String(s.id), s])) : {};
    const screenObj = screensMap[String(genScreenId)];
    if (!screenObj) return toast({ type: 'error', message: 'Selected screen not found' });

    const rows = [];
    for (let c = start.charCodeAt(0); c <= end.charCodeAt(0); c++) rows.push(String.fromCharCode(c));
    const totalToCreate = rows.length * perRow;
    if ((screenObj.totalSeats || 0) < (genExistingCount + totalToCreate)) {
      return toast({ type: 'error', message: `Generated seats (${totalToCreate}) exceed screen capacity (${screenObj.totalSeats}).` });
    }

    let created = 0;
    let skipped = 0;
    for (const row of rows) {
      for (let i = 1; i <= perRow; i++) {
        try {
          await theaterApi.createSeat({ screenId: Number(genScreenId), rowLabel: row, seatNumber: i, seatType: genSeatType, price: price });
          created++;
        } catch (err) {
          // assume duplicate or validation error; skip and continue
          skipped++;
        }
      }
    }
    toast({ type: 'success', message: `Generation complete. Created: ${created}, Skipped: ${skipped}` });
    // reset form
    setGenPreview([]);
    setGenScreenId('');
    setGenExistingCount(0);
    setGenSeatPrice(150);
    await reload();
  }

  // helper to build preview whenever inputs change
  async function updatePreviewFor(screenId = genScreenId, start = genStartRow, end = genEndRow, perRow = genSeatsPerRow) {
    const s = (start || 'A').toUpperCase();
    const e = (end || s).toUpperCase();
    const rows = [];
    for (let c = s.charCodeAt(0); c <= e.charCodeAt(0); c++) rows.push(String.fromCharCode(c));
    const per = Number(perRow) || 0;
    const preview = rows.map((r) => ({ row: r, seats: per }));
    setGenPreview(preview);
    // fetch existing seats count for capacity check
    if (screenId) {
      try {
        const seats = await theaterApi.seats(screenId);
        setGenExistingCount(seats.length || 0);
      } catch (err) {
        setGenExistingCount(0);
      }
    }
  }

  // compute generation statistics for preview and validation
  function computeGenStats(screenId = genScreenId, start = genStartRow, end = genEndRow, perRow = genSeatsPerRow) {
    const s = (start || 'A').toUpperCase();
    const e = (end || s).toUpperCase();
    const rows = [];
    for (let c = s.charCodeAt(0); c <= e.charCodeAt(0); c++) rows.push(String.fromCharCode(c));
    const per = Number(perRow) || 0;
    const total = rows.length * per;
    const screenObj = (data && data.screens) ? data.screens.find((sc) => String(sc.id) === String(screenId)) : null;
    const capacity = screenObj ? (screenObj.totalSeats || 0) : null;
    const willExceed = capacity != null && (genExistingCount + total) > capacity;
    return { rows, per, total, capacity, willExceed };
  }

  const genStats = computeGenStats();

  if (loading) return <LoadingSkeleton rows={6} />;

  return (
    <AnimatedPage className="grid gap-6">
      <PageHeader title="Manage theaters" eyebrow="Admin" description="Create theaters, screens, and seats used by show scheduling." />
      <div className="grid gap-4 grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
        <form onSubmit={createTheater} className="panel grid gap-3 p-4 h-full">
          <h2 className="text-xl font-black text-white">Add theater</h2>
          <input className="field" placeholder="Name (e.g. Phoenix Mall Cinema)" value={theater.name} onChange={(e) => setTheater({ ...theater, name: e.target.value })} required />
          <input className="field" placeholder="Address (Street, Building)" value={theater.addressLine1} onChange={(e) => setTheater({ ...theater, addressLine1: e.target.value })} required />
          <input className="field" placeholder="City (e.g. Mumbai)" value={theater.city} onChange={(e) => setTheater({ ...theater, city: e.target.value })} required />
          <input className="field" placeholder="State (e.g. Maharashtra)" value={theater.state} onChange={(e) => setTheater({ ...theater, state: e.target.value })} required />
          <input className="field" placeholder="Postal code (e.g. 400001)" value={theater.postalCode} onChange={(e) => setTheater({ ...theater, postalCode: e.target.value })} required />
          <div className="text-xs text-slate-400">Helper: provide the official address so customers see correct location.</div>
          <div className="flex gap-2">
            <Button type="submit">Create theater</Button>
            <Button type="button" variant="secondary" onClick={clearTheaterForm}>Clear Form</Button>
          </div>
        </form>

        <form onSubmit={createScreen} className="panel grid gap-3 p-4 h-full">
          <h2 className="text-xl font-black text-white">Add screen</h2>
          <label className="text-xs text-slate-400">Select Theater</label>
          <select className="field" value={screen.theaterId} onChange={(e) => setScreen({ ...screen, theaterId: e.target.value })} required>
            <option value="">Choose a theater</option>
            {data.theaters.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
          <input className="field" placeholder="Example: Screen 1, Audi 1, IMAX Hall" value={screen.name} onChange={(e) => setScreen({ ...screen, name: e.target.value })} required />
          <label className="text-xs text-slate-400">Screen Type</label>
          <select className="field" value={screen.screenType} onChange={(e) => setScreen({ ...screen, screenType: e.target.value })}>
            {screenTypeOptions.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
          </select>
          <input className="field" type="number" min="1" value={screen.totalSeats} onChange={(e) => setScreen({ ...screen, totalSeats: e.target.value })} placeholder="Enter total number of seats (e.g. 50, 100, 250)" required />
          <div className="text-xs text-slate-400">This is the maximum number of seats allowed in this screen.</div>
          <div className="flex gap-2">
            <Button type="submit">Create screen</Button>
            <Button type="button" variant="secondary" onClick={clearScreenForm}>Clear</Button>
          </div>
        </form>

        <form onSubmit={generateSeats} className="panel grid gap-4 p-5 h-full">
          <h2 className="text-xl font-black text-white">Generate seats (bulk)</h2>

          <label className="text-sm font-semibold text-slate-300">Select Screen</label>
          <select className="field mb-3" value={genScreenId} onChange={(e) => { setGenScreenId(e.target.value); updatePreviewFor(e.target.value); }} required>
          <option value="">Choose a screen</option>
          {data.screens.map((item) => <option key={item.id} value={item.id}>{item.name} · Theater #{item.theaterId} · Capacity: {item.totalSeats}</option>)}
          </select>

          <div className="grid grid-cols-3 gap-3 items-end">
          <div>
            <label className="text-sm font-semibold text-slate-300">Starting Row <span title="First row to generate seats from." className="ml-1 text-xs">ℹ️</span></label>
            <input className="field h-12 text-lg text-center" placeholder="A" value={genStartRow} onChange={(e) => { setGenStartRow(e.target.value.toUpperCase()); updatePreviewFor(undefined, e.target.value.toUpperCase(), genEndRow, genSeatsPerRow); }} />
          </div>

          <div>
            <label className="text-sm font-semibold text-slate-300">Ending Row <span title="Last row to generate seats to." className="ml-1 text-xs">ℹ️</span></label>
            <input className="field h-12 text-lg text-center" placeholder="E" value={genEndRow} onChange={(e) => { setGenEndRow(e.target.value.toUpperCase()); updatePreviewFor(undefined, genStartRow, e.target.value.toUpperCase(), genSeatsPerRow); }} />
          </div>

          <div>
            <label className="text-sm font-semibold text-slate-300">Seats Per Row <span title="Number of seats that will be created in every row." className="ml-1 text-xs">ℹ️</span></label>
            <input className="field h-12 text-lg text-center" type="number" min="1" placeholder="10" value={genSeatsPerRow} onChange={(e) => { setGenSeatsPerRow(e.target.value); updatePreviewFor(undefined, genStartRow, genEndRow, e.target.value); }} />
          </div>
          </div>

          <div className="mt-4">
          <div className="border-l-4 border-orange-400 bg-slate-900 p-3 rounded-md">
            <div className="text-sm text-slate-300">Rows: <span className="font-semibold">{genStats.rows.length}</span></div>
            <div className="text-sm text-slate-300">Seats/Row: <span className="font-semibold">{genStats.per}</span></div>
            <div className="text-sm text-orange-300">Total Seats: <span className="font-semibold text-white">{genStats.total}</span></div>
          </div>
          </div>

          <div className="grid grid-cols-2 gap-3 mt-4">
          <div>
            <label className="text-sm font-semibold text-slate-300">Seat Type</label>
            <select className="field" value={genSeatType} onChange={(e) => setGenSeatType(e.target.value)}>
              {['REGULAR','PREMIUM','VIP','RECLINER','LOUNGER','COUPLE','EXECUTIVE','PLATINUM'].map((st) => <option key={st} value={st}>{st}</option>)}
            </select>
          </div>
          <div>
            <label className="text-sm font-semibold text-slate-300">Seat Price (₹)</label>
            <input className="field h-12 text-lg" type="number" min="0" value={genSeatPrice} onChange={(e) => setGenSeatPrice(e.target.value)} placeholder="500" />
            <div className="text-xs text-slate-400 mt-1">This price will be applied to all seats generated in this batch.</div>
          </div>
          </div>

          <div className="mt-3 text-xs text-slate-400">
          <button type="button" className="text-sm underline" onClick={() => setGenPreviewOpen(!genPreviewOpen)}>{genPreviewOpen ? '▼ Preview Seat Layout' : '▶ Preview Seat Layout'}</button>
          </div>

          {genPreviewOpen && (
          <div className="mt-2 bg-slate-900 p-3 rounded text-sm text-slate-200 overflow-auto max-h-40">
            {genPreview.length === 0 && <div>No preview yet. Choose rows and seats per row to see layout.</div>}
            {genPreview.length > 0 && (
              <div className="space-y-2">
                {genPreview.map((p) => (
                  <div key={p.row} className="flex gap-2 flex-wrap">
                    <div className="w-12 font-semibold">{p.row}</div>
                    <div className="flex gap-1 flex-wrap">
                      {Array.from({ length: p.seats }).map((_, i) => (
                        <div key={i} className="px-2 py-1 bg-slate-800 rounded">{p.row}{i + 1}</div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
          )}

          <div className="mt-4 flex gap-3">
          <Button type="submit" variant="primary" className="flex-1" disabled={!genScreenId || genStats.per < 1 || genStats.willExceed}>Generate Seats</Button>
          <Button type="button" variant="secondary" className="flex-1" onClick={() => { setGenPreview([]); setGenScreenId(''); setGenStartRow('A'); setGenEndRow('E'); setGenSeatsPerRow(10); setGenSeatPrice(150); }}>Reset</Button>
          </div>
        </form>
      </div>

      {/* theaters list with expandable screens and seats */}
      <div className="grid gap-3">
        {data.theaters.map((theater) => (
          <TheaterCard key={theater.id} theater={theater} screens={data.screens.filter(s => s.theaterId === theater.id)} reload={reload} />
        ))}
      </div>
    </AnimatedPage>
  );
}

function TheaterCard({ theater, screens, reload }) {
  const [expanded, setExpanded] = useState(false);
  const [screenSeats, setScreenSeats] = useState({}); // map screenId -> seats array
  const [loadingSeats, setLoadingSeats] = useState({});
  const toast = useToastStore((s) => s.push);

  async function toggleExpand() {
    setExpanded(!expanded);
    if (!expanded) {
      // prefetch seats for all screens
      for (const screen of screens) {
        await loadSeats(screen.id);
      }
    }
  }

  async function loadSeats(screenId) {
    if (screenSeats[screenId]) return;
    setLoadingSeats((l) => ({ ...l, [screenId]: true }));
    try {
      const seats = await theaterApi.seats(screenId);
      setScreenSeats((m) => ({ ...m, [screenId]: seats }));
    } catch (err) {
      console.error('Failed to load seats', err);
      toast({ type: 'error', message: 'Unable to load seats for screen ' + screenId });
    } finally {
      setLoadingSeats((l) => ({ ...l, [screenId]: false }));
    }
  }

  return (
    <div className="panel p-4">
      <div className="flex items-start justify-between">
        <div>
          <p className="font-black text-white">{theater.name}</p>
          <p className="text-sm text-slate-400">{theater.addressLine1}, {theater.city}, {theater.state}</p>
          <div className="mt-2 text-xs text-slate-400">Screens: {screens.length} · Seats: {screens.reduce((sum, s) => sum + (s.totalSeats || 0), 0)}</div>
        </div>
        <div className="flex gap-2">
          <button className="btn" title="View theater details" onClick={() => window.location.href = `/admin/theaters/${theater.id}`}>View</button>
          <button className="btn" title="Edit theater" onClick={async () => { const name = prompt('Theater name', theater.name); if (name) { await theaterApi.update(theater.id, { ...theater, name }); reload(); } }}>Edit</button>
          <button className="btn btn-danger" title="Delete theater" onClick={async () => { if (confirm('Delete this theater? This action cannot be undone.')) { await theaterApi.remove(theater.id); reload(); } }}>Delete</button>
          <button className="btn" onClick={toggleExpand}>{expanded ? 'Collapse' : 'Expand'}</button>
        </div>
      </div>

      {expanded && (
        <div className="mt-4">
          {screens.length === 0 && <div className="text-sm text-slate-400">No screens found</div>}
          {screens.map((screen) => (
            <div key={screen.id} className="mt-3 border-t border-white/5 pt-3">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-white">{screen.name} <span className="text-xs text-slate-400">({screenTypeLabel(screen.screenType)})</span></p>
                  <div className="text-sm text-slate-400">Seats: {screen.totalSeats}</div>
                </div>
                <div className="flex gap-2">
                  <button className="btn" title="View seats" onClick={async () => { await loadSeats(screen.id); window.location.href = `/admin/screens/${screen.id}`; }}>View Seats</button>
                  <button className="btn" title="Generate seats" onClick={() => { window.location.href = `/admin/screens/${screen.id}`; }}>Generate Seats</button>
                  <button className="btn" title="Edit screen" onClick={async () => { const name = prompt('Screen name', screen.name); if (name) { await theaterApi.updateScreen(screen.id, { theaterId: screen.theaterId, name, screenType: screen.screenType, totalSeats: screen.totalSeats }); reload(); } }}>Edit Screen</button>
                  <button className="btn btn-danger" title="Delete screen" onClick={async () => { if (confirm('Delete screen?')) { await theaterApi.removeScreen(screen.id); reload(); } }}>Delete Screen</button>
                </div>
              </div>

               <div className="mt-3">
              <div className="text-sm text-slate-300 font-semibold">Seats</div>
              {loadingSeats[screen.id] && <div className="text-sm text-slate-400">Loading seats...</div>}
              {!loadingSeats[screen.id] && (!screenSeats[screen.id] || screenSeats[screen.id].length === 0) && <div className="text-sm text-slate-400">No seats found</div>}
              {!loadingSeats[screen.id] && screenSeats[screen.id] && (
                   <div className="mt-2 grid grid-cols-6 gap-2">
                     {screenSeats[screen.id].map((seat) => (
                       <div key={seat.id} className={`p-3 rounded text-xs ${seat.status === 'ACTIVE' ? 'bg-slate-800 text-white' : 'bg-gray-700 text-slate-300'}`}>
                         <div className="font-semibold">{seat.rowLabel}{seat.seatNumber}</div>
                         <div className="text-xs text-slate-400">{seat.seatType || '-'}</div>
                         {seat.price != null && <div className="text-xs text-slate-300">₹{seat.price}</div>}
                       </div>
                     ))}
                   </div>
              )}
               </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
