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
import { formatCurrency } from '../../utils/formatters.js';
import { paymentMethods } from '../../utils/constants.js';

export default function BookingConfirmationPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const toast = useToastStore((state) => state.push);
  const [method, setMethod] = useState('UPI');
  const [upiId, setUpiId] = useState('');
  const [upiError, setUpiError] = useState('');

  // card
  const [cardNumber, setCardNumber] = useState('');
  const [cardHolder, setCardHolder] = useState('');
  const [cardExpiryMonth, setCardExpiryMonth] = useState('');
  const [cardExpiryYear, setCardExpiryYear] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  const [cardError, setCardError] = useState('');

  // net banking
  const [bankName, setBankName] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [referenceNumber, setReferenceNumber] = useState('');

  // wallet
  const [walletProvider, setWalletProvider] = useState('Paytm');
  const [walletMobile, setWalletMobile] = useState('');

  const [paying, setPaying] = useState(false);
  const { data: booking, loading, error } = useAsync(() => bookingApi.get(bookingId).catch(() => bookingApi.history().then((items) => items.find((item) => String(item.id) === String(bookingId)))), [bookingId]);

  function validateUpi(value) {
    if (!value) return false;
    const re = /^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}$/;
    return re.test(value.trim());
  }

  function validateCardNumber(value) {
    const digits = (value || '').replace(/\D/g, '');
    return digits.length >= 12 && digits.length <= 19;
  }

  function validateExpiry(month, year) {
    const m = Number(month);
    const y = Number(year);
    if (!m || !y) return false;
    if (m < 1 || m > 12) return false;
    const now = new Date();
    const exp = new Date(y, m - 1, 1);
    return exp >= new Date(now.getFullYear(), now.getMonth(), 1);
  }

  function validateCvv(value) {
    return /^\d{3,4}$/.test(value);
  }

  function validateWalletMobile(value) {
    return /^\d{10}$/.test((value || '').trim());
  }

  function canPay() {
    switch (method) {
      case 'UPI':
        return validateUpi(upiId);
      case 'CARD':
        return validateCardNumber(cardNumber) && cardHolder.trim() && validateExpiry(cardExpiryMonth, cardExpiryYear) && validateCvv(cardCvv);
      case 'NET_BANKING':
        return bankName.trim() && accountHolderName.trim() && referenceNumber.trim();
      case 'WALLET':
        return walletProvider && validateWalletMobile(walletMobile);
      case 'CASH':
        return true;
      default:
        return false;
    }
  }

  async function pay() {
    // client-side validation
    if (method === 'UPI' && !validateUpi(upiId)) {
      setUpiError('Enter a valid UPI ID (example@upi)');
      return;
    }
    if (method === 'CARD') {
      if (!validateCardNumber(cardNumber) || !validateExpiry(cardExpiryMonth, cardExpiryYear) || !validateCvv(cardCvv) || !cardHolder.trim()) {
        setCardError('Enter valid card details');
        return;
      }
    }
    if (method === 'NET_BANKING') {
      if (!bankName.trim() || !accountHolderName.trim() || !referenceNumber.trim()) {
        return;
      }
    }
    if (method === 'WALLET' && !validateWalletMobile(walletMobile)) {
      return;
    }

    setPaying(true);
    try {
      const payload = {
        bookingId: Number(bookingId),
        paymentMethod: method,
        provider: method === 'UPI' ? 'Razorpay UPI' : 'CinemaPay',
        providerTransactionId: `WEB-${Date.now()}`,
      };
      if (method === 'UPI') payload.upiId = upiId.trim();
      if (method === 'CARD') {
        payload.cardNumber = cardNumber.replace(/\s+/g, '');
        payload.cardHolderName = cardHolder.trim();
        payload.cardExpiryMonth = Number(cardExpiryMonth);
        payload.cardExpiryYear = Number(cardExpiryYear);
        // do NOT send CVV to backend
      }
      if (method === 'NET_BANKING') {
        payload.bankName = bankName.trim();
        payload.accountHolderName = accountHolderName.trim();
        payload.referenceNumber = referenceNumber.trim();
      }
      if (method === 'WALLET') {
        payload.walletProvider = walletProvider;
        payload.walletMobile = walletMobile.trim();
      }

      await paymentApi.create(payload);
      toast({ type: 'success', message: 'Payment successful. Booking confirmed.' });
      navigate('/bookings');
    } finally {
      setPaying(false);
    }
  }

  if (loading) return <LoadingSkeleton rows={4} />;
  if (error || !booking) return <EmptyState title="Booking not found" message={error || 'Unable to read booking details.'} />;

  return (
    <AnimatedPage className="mx-auto max-w-3xl">
      <div className="panel p-6 md:p-8">
        <p className="text-xs font-black uppercase tracking-[0.3em] text-ember-400">Confirmation</p>
        <h1 className="mt-3 text-4xl font-black text-white">Confirm payment</h1>
        <div className="mt-6 grid gap-4 rounded-2xl border border-white/10 bg-black/20 p-5">
          <p className="flex justify-between text-slate-300"><span>Reference</span><b className="text-white">{booking.bookingReference}</b></p>
          <p className="flex justify-between text-slate-300"><span>Movie</span><b className="text-white">{booking.movieTitle}</b></p>
          <p className="flex justify-between text-slate-300"><span>Seats</span><b className="text-white">{booking.seatsCount}</b></p>
          <p className="flex justify-between border-t border-white/10 pt-4 text-xl text-white"><span>Total</span><b>{formatCurrency(booking.totalAmount)}</b></p>
        </div>
        <label className="label mt-6">
          Payment method
          <select className="field" value={method} onChange={(e) => { setMethod(e.target.value); setUpiError(''); setCardError(''); }}>
            {paymentMethods.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>

        {/* Summary of selected payment method */}
        <div className="mt-3 p-3 rounded border border-white/5 bg-black/10">
          <div className="text-sm text-slate-300">Selected method: <span className="text-white font-semibold">{method}</span></div>
          {method === 'UPI' && upiId && <div className="text-sm text-slate-400 mt-1">UPI ID: <span className="text-white">{upiId}</span></div>}
          {method === 'CARD' && cardNumber && <div className="text-sm text-slate-400 mt-1">Card: <span className="text-white">**** **** **** {cardNumber.replace(/\D/g,'').slice(-4)}</span></div>}
          {method === 'NET_BANKING' && bankName && <div className="text-sm text-slate-400 mt-1">Bank: <span className="text-white">{bankName}</span> · Ref: <span className="text-white">{referenceNumber}</span></div>}
          {method === 'WALLET' && walletProvider && <div className="text-sm text-slate-400 mt-1">Wallet: <span className="text-white">{walletProvider}</span> · Mobile: <span className="text-white">{walletMobile}</span></div>}
        </div>

        {/* Dynamic payment fields */}
        {method === 'UPI' && (
          <label className="label">
            UPI ID
            <input className="field" placeholder="example@upi" value={upiId} onChange={(e) => { setUpiId(e.target.value); setUpiError(''); }} />
            {upiError && <div className="text-rose-400 text-sm mt-1">{upiError}</div>}
          </label>
        )}

        {method === 'CARD' && (
          <div className="grid gap-3">
            <label className="label">
              Card number
              <input className="field" placeholder="1234 5678 9012 3456" value={cardNumber} onChange={(e) => { setCardNumber(e.target.value); setCardError(''); }} />
            </label>
            <label className="label">
              Card holder name
              <input className="field" placeholder="Name on card" value={cardHolder} onChange={(e) => setCardHolder(e.target.value)} />
            </label>
            <div className="flex gap-2">
              <label className="label flex-1">
                Expiry month
                <input className="field" placeholder="MM" value={cardExpiryMonth} onChange={(e) => setCardExpiryMonth(e.target.value)} />
              </label>
              <label className="label flex-1">
                Expiry year
                <input className="field" placeholder="YYYY" value={cardExpiryYear} onChange={(e) => setCardExpiryYear(e.target.value)} />
              </label>
              <label className="label w-32">
                CVV
                <input className="field" placeholder="123" value={cardCvv} onChange={(e) => setCardCvv(e.target.value)} />
              </label>
            </div>
            {cardError && <div className="text-rose-400 text-sm">{cardError}</div>}
          </div>
        )}

        {method === 'NET_BANKING' && (
          <div className="grid gap-3">
            <label className="label">
              Bank
              <input className="field" placeholder="Bank name" value={bankName} onChange={(e) => setBankName(e.target.value)} />
            </label>
            <label className="label">
              Account holder name
              <input className="field" placeholder="Account holder" value={accountHolderName} onChange={(e) => setAccountHolderName(e.target.value)} />
            </label>
            <label className="label">
              Reference number
              <input className="field" placeholder="Transaction ref" value={referenceNumber} onChange={(e) => setReferenceNumber(e.target.value)} />
            </label>
          </div>
        )}

        {method === 'WALLET' && (
          <div className="grid gap-3">
            <label className="label">
              Wallet provider
              <select className="field" value={walletProvider} onChange={(e) => setWalletProvider(e.target.value)}>
                <option>Paytm</option>
                <option>PhonePe</option>
                <option>Amazon Pay</option>
                <option>Mobikwik</option>
              </select>
            </label>
            <label className="label">
              Linked mobile number
              <input className="field" placeholder="10 digit mobile" value={walletMobile} onChange={(e) => setWalletMobile(e.target.value)} />
            </label>
          </div>
        )}

        {/* Cash requires no extra fields */}

        <Button className="mt-6 w-full" onClick={pay} disabled={paying || !canPay()}>{paying ? 'Processing...' : 'Pay and confirm'}</Button>
      </div>
    </AnimatedPage>
  );
}
