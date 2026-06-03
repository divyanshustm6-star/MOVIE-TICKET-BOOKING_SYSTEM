import { create } from 'zustand';

export const useToastStore = create((set) => ({
  toasts: [],
  push(toast) {
    const id = crypto.randomUUID();
    set((state) => ({ toasts: [...state.toasts, { id, type: 'info', ...toast }] }));
    window.setTimeout(() => {
      set((state) => ({ toasts: state.toasts.filter((item) => item.id !== id) }));
    }, toast.duration || 4200);
  },
  remove(id) {
    set((state) => ({ toasts: state.toasts.filter((item) => item.id !== id) }));
  },
}));
