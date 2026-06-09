import { create } from 'zustand';

export const useMovieDetailsModalStore = create((set) => ({
  isOpen: false,
  movie: null,
  open: (movie) => set({ isOpen: true, movie }),
  close: () => set({ isOpen: false, movie: null }),
}));
