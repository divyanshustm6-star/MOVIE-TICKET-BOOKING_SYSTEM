import { Navigate, Route, Routes } from 'react-router-dom';
import { useEffect } from 'react';
import AuthLayout from './layouts/AuthLayout.jsx';
import AppLayout from './layouts/AppLayout.jsx';
import AdminLayout from './layouts/AdminLayout.jsx';
import ProtectedRoute from './routes/ProtectedRoute.jsx';
import LoginPage from './pages/auth/LoginPage.jsx';
import RegisterPage from './pages/auth/RegisterPage.jsx';
import HomePage from './pages/HomePage.jsx';
import MoviesPage from './pages/movies/MoviesPage.jsx';
import MovieDetailsPage from './pages/movies/MovieDetailsPage.jsx';
import ShowsPage from './pages/shows/ShowsPage.jsx';
import SeatSelectionPage from './pages/booking/SeatSelectionPage.jsx';
import BookingConfirmationPage from './pages/booking/BookingConfirmationPage.jsx';
import BookingHistoryPage from './pages/booking/BookingHistoryPage.jsx';
import BookingDetailsPage from './pages/booking/BookingDetailsPage.jsx';
import ProfilePage from './pages/user/ProfilePage.jsx';
import AdminDashboardPage from './pages/admin/AdminDashboardPage.jsx';
import AdminMoviesPage from './pages/admin/AdminMoviesPage.jsx';
import AdminShowsPage from './pages/admin/AdminShowsPage.jsx';
import AdminBookingsPage from './pages/admin/AdminBookingsPage.jsx';
import AdminUsersPage from './pages/admin/AdminUsersPage.jsx';
import AdminTheatersPage from './pages/admin/AdminTheatersPage.jsx';
import ErrorPage from './pages/errors/ErrorPage.jsx';
import { roles } from './utils/constants.js';
import { useAuthStore } from './store/authStore.js';

export default function App() {
  const bootstrap = useAuthStore((state) => state.bootstrap);
  const bootstrapped = useAuthStore((state) => state.bootstrapped);
  const token = useAuthStore((state) => state.token);

  useEffect(() => {
    bootstrap();
  }, [bootstrap]);

  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/login" element={token && bootstrapped ? <Navigate to="/" replace /> : <LoginPage />} />
        <Route path="/register" element={token && bootstrapped ? <Navigate to="/" replace /> : <RegisterPage />} />
      </Route>

      <Route
        element={
          <ProtectedRoute roles={[roles.user, roles.admin]}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="/movies" element={<MoviesPage />} />
        <Route path="/movies/:id" element={<MovieDetailsPage />} />
        <Route path="/shows" element={<ShowsPage />} />
        <Route path="/book/:showId" element={<SeatSelectionPage />} />
        <Route path="/booking/:bookingId/confirm" element={<BookingConfirmationPage />} />
        <Route path="/bookings" element={<BookingHistoryPage />} />
        <Route path="/bookings/:bookingId" element={<BookingDetailsPage />} />
        <Route path="/profile" element={<ProfilePage />} />

        <Route
          path="/admin"
          element={
            <ProtectedRoute roles={[roles.admin]}>
              <AdminLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<AdminDashboardPage />} />
          <Route path="movies" element={<AdminMoviesPage />} />
          <Route path="shows" element={<AdminShowsPage />} />
          <Route path="bookings" element={<AdminBookingsPage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="theaters" element={<AdminTheatersPage />} />
        </Route>
      </Route>

      <Route path="/403" element={<ErrorPage code="403" title="Access denied" message="Your account does not have permission to open this page." />} />
      <Route path="*" element={<ErrorPage />} />
    </Routes>
  );
}
