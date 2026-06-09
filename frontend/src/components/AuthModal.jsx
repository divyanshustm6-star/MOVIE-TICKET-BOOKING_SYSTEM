import { useState, useEffect } from 'react';
import { useAuthStore } from '../store/authStore.js';
import { useAuthModalStore, useLoginRequiredStore } from '../store/authModalStore.js';
import { useToastStore } from '../store/toastStore.js';
import Button from './Button.jsx';
import FormField from './FormField.jsx';

export default function AuthModal() {
  const { isOpen, tab, onSuccess, close, openLogin, openRegister } = useAuthModalStore();
  const loginRequired = useLoginRequiredStore();
  const { login, register, loading } = useAuthStore();
  const toast = useToastStore((state) => state.push);

  const [loginForm, setLoginForm] = useState({ email: '', password: '' });
  const [registerForm, setRegisterForm] = useState({ fullName: '', email: '', phone: '', password: '', confirmPassword: '' });
  const [error, setError] = useState('');

  // Clear errors and forms on open/tab change
  useEffect(() => {
    setError('');
  }, [isOpen, tab]);

  if (!isOpen) return null;

  function updateLogin(field, value) {
    setLoginForm((prev) => ({ ...prev, [field]: value }));
  }

  function updateRegister(field, value) {
    setRegisterForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleLogin(e) {
    e.preventDefault();
    setError('');
    if (!loginForm.email.includes('@') || loginForm.password.length < 6) {
      setError('Please enter a valid email and a password of at least 6 characters.');
      return;
    }
    try {
      await login(loginForm);
      toast({ type: 'success', message: 'Logged in successfully' });
      close();
      loginRequired.close();
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid email or password');
    }
  }

  async function handleRegister(e) {
    e.preventDefault();
    setError('');
    if (!registerForm.fullName.trim() || !registerForm.email.includes('@')) {
      setError('Please fill in your name and a valid email address.');
      return;
    }
    if (registerForm.password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }
    if (registerForm.password !== registerForm.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    try {
      await register({
        fullName: registerForm.fullName,
        email: registerForm.email,
        phone: registerForm.phone || null,
        password: registerForm.password,
      });
      toast({ type: 'success', message: 'Account created successfully' });
      close();
      loginRequired.close();
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-fade-in">
      <div className="relative w-full max-w-md overflow-hidden rounded-3xl border border-white/10 bg-cinema-900 p-6 md:p-8 shadow-panel animate-scale-up">
        {/* Close Button */}
        <button
          type="button"
          onClick={close}
          className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-white/10 hover:text-white transition"
          aria-label="Close modal"
        >
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        {/* Tab Headers */}
        <div className="flex border-b border-white/10 mb-6">
          <button
            type="button"
            onClick={() => openLogin(onSuccess)}
            className={`flex-1 pb-3 text-lg font-black transition ${
              tab === 'login' ? 'border-b-2 border-amber-500 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            Login
          </button>
          <button
            type="button"
            onClick={() => openRegister(onSuccess)}
            className={`flex-1 pb-3 text-lg font-black transition ${
              tab === 'register' ? 'border-b-2 border-amber-500 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            Create Account
          </button>
        </div>

        {error && (
          <div className="mb-4 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm font-semibold text-red-200">
            {error}
          </div>
        )}

        {tab === 'login' ? (
          <form onSubmit={handleLogin} className="grid gap-5">
            <FormField label="Email address">
              <input
                className="field"
                type="email"
                placeholder="you@example.com"
                value={loginForm.email}
                onChange={(e) => updateLogin('email', e.target.value)}
                required
              />
            </FormField>
            <FormField label="Password">
              <input
                className="field"
                type="password"
                placeholder="••••••••"
                value={loginForm.password}
                onChange={(e) => updateLogin('password', e.target.value)}
                minLength={6}
                required
              />
            </FormField>
            <Button type="submit" disabled={loading} className="w-full mt-2">
              {loading ? 'Logging in...' : 'Login'}
            </Button>
          </form>
        ) : (
          <form onSubmit={handleRegister} className="grid gap-4">
            <FormField label="Full name">
              <input
                className="field"
                type="text"
                placeholder="John Doe"
                value={registerForm.fullName}
                onChange={(e) => updateRegister('fullName', e.target.value)}
                required
              />
            </FormField>
            <FormField label="Email address">
              <input
                className="field"
                type="email"
                placeholder="john@example.com"
                value={registerForm.email}
                onChange={(e) => updateRegister('email', e.target.value)}
                required
              />
            </FormField>
            <FormField label="Phone number (optional)">
              <input
                className="field"
                type="tel"
                placeholder="1234567890"
                value={registerForm.phone}
                onChange={(e) => updateRegister('phone', e.target.value)}
              />
            </FormField>
            <FormField label="Password">
              <input
                className="field"
                type="password"
                placeholder="At least 6 characters"
                value={registerForm.password}
                onChange={(e) => updateRegister('password', e.target.value)}
                minLength={6}
                required
              />
            </FormField>
            <FormField label="Confirm Password">
              <input
                className="field"
                type="password"
                placeholder="Repeat password"
                value={registerForm.confirmPassword}
                onChange={(e) => updateRegister('confirmPassword', e.target.value)}
                minLength={6}
                required
              />
            </FormField>
            <Button type="submit" disabled={loading} className="w-full mt-2">
              {loading ? 'Creating...' : 'Register'}
            </Button>
          </form>
        )}
      </div>
    </div>
  );
}
