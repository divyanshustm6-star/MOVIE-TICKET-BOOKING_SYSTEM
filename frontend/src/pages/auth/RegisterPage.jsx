import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import FormField from '../../components/FormField.jsx';
import { useAuthStore } from '../../store/authStore.js';
import { useToastStore } from '../../store/toastStore.js';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { register, loading } = useAuthStore();
  const toast = useToastStore((state) => state.push);
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [error, setError] = useState('');

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError('');
    if (!form.fullName.trim() || !form.email.includes('@') || form.password.length < 6) {
      setError('Name, valid email, and a 6 character password are required.');
      return;
    }
    try {
      await register({ ...form, phone: form.phone || null });
      toast({ type: 'success', message: 'Account created successfully' });
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to register');
    }
  }

  return (
    <AnimatedPage className="panel p-6 md:p-8">
      <h2 className="text-3xl font-black text-white">Create account</h2>
      <p className="mt-2 text-sm leading-6 text-slate-400">Your account receives the USER role automatically.</p>
      {error && <p className="mt-5 rounded-xl border border-red-400/30 bg-red-500/15 p-3 text-sm font-semibold text-red-100">{error}</p>}
      <form onSubmit={submit} className="mt-6 grid gap-5">
        <FormField label="Full name">
          <input className="field" value={form.fullName} onChange={(e) => update('fullName', e.target.value)} required />
        </FormField>
        <FormField label="Email">
          <input className="field" type="email" value={form.email} onChange={(e) => update('email', e.target.value)} required />
        </FormField>
        <FormField label="Phone">
          <input className="field" value={form.phone} onChange={(e) => update('phone', e.target.value)} />
        </FormField>
        <FormField label="Password">
          <input className="field" type="password" value={form.password} onChange={(e) => update('password', e.target.value)} minLength={6} required />
        </FormField>
        <Button type="submit" disabled={loading} className="w-full">{loading ? 'Creating...' : 'Register'}</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-400">
        Already registered? <Link to="/login" className="font-bold text-ember-300">Login</Link>
      </p>
    </AnimatedPage>
  );
}
