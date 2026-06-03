import { AnimatePresence, motion } from 'framer-motion';
import { useToastStore } from '../store/toastStore.js';

export default function ToastHost() {
  const { toasts, remove } = useToastStore();

  return (
    <div className="fixed right-4 top-4 z-50 grid w-[min(380px,calc(100vw-32px))] gap-3">
      <AnimatePresence>
        {toasts.map((toast) => (
          <motion.button
            key={toast.id}
            type="button"
            initial={{ opacity: 0, x: 40 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 40 }}
            onClick={() => remove(toast.id)}
            className={[
              'rounded-2xl border px-4 py-3 text-left text-sm font-semibold shadow-panel backdrop-blur',
              toast.type === 'success' ? 'border-emerald-400/30 bg-emerald-500/15 text-emerald-100' : '',
              toast.type === 'error' ? 'border-red-400/30 bg-red-500/15 text-red-100' : '',
              toast.type === 'info' ? 'border-white/10 bg-white/10 text-white' : '',
            ].join(' ')}
          >
            {toast.message}
          </motion.button>
        ))}
      </AnimatePresence>
    </div>
  );
}
