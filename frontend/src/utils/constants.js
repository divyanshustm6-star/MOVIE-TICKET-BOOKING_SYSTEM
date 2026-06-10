export const API_BASE_URL ='https://movie-ticket-booking-system-2jjx.onrender.com';

export const STORAGE_KEYS = {
  token: 'moviebooking.token',
  user: 'moviebooking.user',
};

export const roles = {
  admin: 'ROLE_ADMIN',
  user: 'ROLE_USER',
};

export const movieStatuses = ['UPCOMING', 'NOW_SHOWING', 'ARCHIVED'];
export const showStatuses = ['SCHEDULED', 'BOOKING_OPEN', 'SOLD_OUT', 'CANCELLED', 'COMPLETED'];
export const bookingStatuses = ['PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED'];
export const paymentMethods = ['CARD', 'UPI', 'NET_BANKING', 'WALLET', 'CASH'];
export const screenTypeOptions = [
  { label: 'STANDARD', value: 'STANDARD' },
  { label: 'IMAX', value: 'IMAX' },
  { label: 'DOLBY', value: 'DOLBY' },
  { label: '3D', value: 'THREE_D' },
  { label: '4DX', value: 'FOUR_DX' },
];

export function screenTypeLabel(value) {
  if (!value) return '';
  const opt = screenTypeOptions.find((o) => o.value === value);
  return opt ? opt.label : value;
}

export const seatTypes = ['REGULAR', 'PREMIUM', 'RECLINER', 'ACCESSIBLE'];
