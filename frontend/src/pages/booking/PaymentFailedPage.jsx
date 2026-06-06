import { useNavigate } from 'react-router-dom';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';

export default function PaymentFailedPage() {
  const navigate = useNavigate();
  return (
    <AnimatedPage className="mx-auto max-w-2xl">
      <div className="panel p-8 text-center">
        <h1 className="text-4xl font-black text-white">Payment failed</h1>
        <p className="mt-3 text-slate-300">Your payment could not be completed. Try again or contact support.</p>
        <div className="mt-6 flex justify-center gap-4">
          <Button onClick={() => navigate(-1)}>Try again</Button>
          <Button onClick={() => navigate('/bookings')}>View bookings</Button>
        </div>
      </div>
    </AnimatedPage>
  );
}
