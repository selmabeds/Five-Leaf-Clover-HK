
/**
 * DASH delivery read service.
 *
 * Communicates with the DLV backend (read only).
 * DLV owns courier state and position; DASH only displays them on the map.
 */

// R-DASH-02: on-duty couriers only (DLV returns couriers whose state is not OFFLINE)
// Each courier: { id, latitude, longitude, state } with state IDLE | ASSIGNED | CARRYING
export async function getOnDutyCouriers() {
  const response = await fetch('/api/delivery/couriers', { credentials: 'include' });
  if (!response.ok) throw new Error('Could not load couriers');
  return response.json();
}
