
import QueueBand from './QueueBand';

/**
 * DASH UI layer: information area (Spec DASH › Goal).
 *
 * "An information area showing: the currently logged-in user and their role;
 *  the selected restaurant name and quick stats (orders queued, in preparation, ready)".
 * Counts only, never order details (Spec Appendix A). Reset and the map label
 * toggle belong here too and are added with those items.
 */
export default function InfoArea({ user, email, restaurant, stats }) {
  return (
    <div className="info-area">
      <p className="info-user">
        Logged in as <strong>{user?.name ?? email}</strong> ({email}) · Role: <strong>{user?.role?.toLowerCase()}</strong>
      </p>

      {restaurant && (
        <div className="info-restaurant">
          <span className="info-restaurant-name">{restaurant.name}</span>
          <span>Status: {restaurant.status}</span>
          <span>{stats?.queued ?? 0} queued</span>
          <span>{stats?.preparing ?? 0} in preparation</span>
          <span>{stats?.ready ?? 0} ready</span>
          <QueueBand queued={stats?.queued ?? 0} restaurantStatus={restaurant.status} />
        </div>
      )}
    </div>
  );
}
