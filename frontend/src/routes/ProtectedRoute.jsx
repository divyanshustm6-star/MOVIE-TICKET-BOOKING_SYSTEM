import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/authStore.js';

export default function ProtectedRoute({ children, roles }) {
  const location = useLocation();
  const { token, user, bootstrapped } = useAuthStore();

  if (!bootstrapped) {
    return <div className="grid min-h-screen place-items-center bg-cinema-950 text-white">Loading session...</div>;
  }

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles?.length && !roles.some((role) => user?.roles?.includes(role))) {
    return <Navigate to="/403" replace />;
  }

  return children ? children : <Outlet />;
}
