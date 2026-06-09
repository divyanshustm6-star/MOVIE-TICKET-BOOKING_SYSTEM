import { useAuthModalStore, useLoginRequiredStore } from '../store/authModalStore.js';
import Button from './Button.jsx';

export default function LoginRequiredModal() {
  const { isOpen, onConfirm, close } = useLoginRequiredStore();
  const authModal = useAuthModalStore();

  if (!isOpen) return null;

  function handleLogin() {
    close();
    authModal.openLogin(onConfirm);
  }

  function handleRegister() {
    close();
    authModal.openRegister(onConfirm);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4 animate-fade-in">
      <div className="relative w-full max-w-sm overflow-hidden rounded-3xl border border-white/10 bg-cinema-900 p-6 md:p-8 shadow-panel text-center animate-scale-up">
        {/* Icon */}
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-amber-500/10 text-amber-500 text-3xl">
          🔒
        </div>

        {/* Text */}
        <h3 className="text-2xl font-black text-white">Login Required</h3>
        <p className="mt-3 text-sm leading-6 text-slate-300">
          Please log in or create an account to view available shows, select seats, or book tickets.
        </p>

        {/* Buttons */}
        <div className="mt-6 flex flex-col gap-3">
          <Button type="button" onClick={handleLogin} className="w-full">
            Login
          </Button>
          <Button type="button" variant="secondary" onClick={handleRegister} className="w-full">
            Create Account
          </Button>
          <button
            type="button"
            onClick={close}
            className="mt-2 text-sm font-semibold text-slate-400 hover:text-white transition"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
