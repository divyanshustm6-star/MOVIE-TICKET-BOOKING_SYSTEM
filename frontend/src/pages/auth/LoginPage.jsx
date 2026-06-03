import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import FormField from '../../components/FormField.jsx';
import { useAuthStore } from '../../store/authStore.js';
import { useToastStore } from '../../store/toastStore.js';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, loading } = useAuthStore();
  const toast = useToastStore((state) => state.push);
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError('');
    if (!form.email.includes('@') || form.password.length < 6) {
      setError('Enter a valid email and a password with at least 6 characters.');
      return;
    }
    try {
      await login(form);
      toast({ type: 'success', message: 'Logged in successfully' });
      navigate(location.state?.from?.pathname || '/', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid email or password');
    }
  }

  return (
    <AnimatedPage className="panel p-6 md:p-8">
      <h2 className="text-3xl font-black text-white">Welcome back</h2>
      <p className="mt-2 text-sm leading-6 text-slate-400">Login to browse shows and manage your bookings.</p>
      {error && <p className="mt-5 rounded-xl border border-red-400/30 bg-red-500/15 p-3 text-sm font-semibold text-red-100">{error}</p>}
      <form onSubmit={submit} className="mt-6 grid gap-5">
        <FormField label="Email">
          <input className="field" type="email" value={form.email} onChange={(e) => update('email', e.target.value)} required />
        </FormField>
        <FormField label="Password">
          <input className="field" type="password" value={form.password} onChange={(e) => update('password', e.target.value)} minLength={6} required />
        </FormField>
        <Button type="submit" disabled={loading} className="w-full">{loading ? 'Logging in...' : 'Login'}</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-400">
        New customer? <Link to="/register" className="font-bold text-ember-300">Create an account</Link>
      </p>
    </AnimatedPage>
  );
}
