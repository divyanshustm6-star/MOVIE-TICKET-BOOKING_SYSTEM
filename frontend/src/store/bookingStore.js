import { create } from 'zustand';

export const useBookingStore = create((set, get) => ({
  show: null,
  seats: [],
  selectedSeatIds: [],
  lastBooking: null,
  setShow(show) {
    set({ show });
  },
  setSeats(seats) {
    set({ seats });
  },
  toggleSeat(id) {
    const selected = get().selectedSeatIds;
    set({
      selectedSeatIds: selected.includes(id)
        ? selected.filter((seatId) => seatId !== id)
        : [...selected, id],
    });
  },
  clearSelection() {
    set({ selectedSeatIds: [] });
  },
  setLastBooking(lastBooking) {
    set({ lastBooking });
  },
  reset() {
    set({ show: null, seats: [], selectedSeatIds: [], lastBooking: null });
  },
}));
