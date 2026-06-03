import AnimatedPage from '../../components/AnimatedPage.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { useAuthStore } from '../../store/authStore.js';

export default function ProfilePage() {
  const user = useAuthStore((state) => state.user);

  return (
    <AnimatedPage className="grid gap-6">
      <PageHeader title="Profile" eyebrow="Account" description="Your backend exposes `/auth/me` for profile reads. Profile update is admin-only through `/api/users/{id}` in the current backend." />
      <div className="panel grid gap-5 p-6 md:grid-cols-2">
        <label className="label">
          Full name
          <input className="field" value={user?.fullName || ''} readOnly />
        </label>
        <label className="label">
          Email
          <input className="field" value={user?.email || ''} readOnly />
        </label>
        <label className="label">
          Phone
          <input className="field" value={user?.phone || ''} readOnly />
        </label>
        <label className="label">
          Roles
          <input className="field" value={(user?.roles || []).join(', ')} readOnly />
        </label>
      </div>
    </AnimatedPage>
  );
}
