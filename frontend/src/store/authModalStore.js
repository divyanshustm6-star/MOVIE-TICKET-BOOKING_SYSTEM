import { create } from 'zustand';

export const useAuthModalStore = create((set) => ({
  isOpen: false,
  tab: 'login', // 'login' | 'register'
  onSuccess: null,

  openLogin: (onSuccess = null) => set({ isOpen: true, tab: 'login', onSuccess }),
  openRegister: (onSuccess = null) => set({ isOpen: true, tab: 'register', onSuccess }),
  close: () => set({ isOpen: false, onSuccess: null }),
}));

export const useLoginRequiredStore = create((set) => ({
  isOpen: false,
  onConfirm: null,

  open: (onConfirm = null) => set({ isOpen: true, onConfirm }),
  close: () => set({ isOpen: false, onConfirm: null }),
}));
