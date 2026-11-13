
/**
 * R-DASH-03 preparation-queue rule, shared by every DASH view (cards, Restaurant Orders, map markers):
 *
 * "DASH shall encode each restaurant's preparation queue with three levels:
 *  long (red) at six or more queued orders, or when the restaurant is paused;
 *  building (amber) at three to five; short (green) otherwise."
 *
 * Queued = ACCEPTED orders waiting in the preparation queue (R-ORD-03).
 */
export function queueBand(queued, restaurantStatus) {
  // Paused stays at the red "long" level (R-DASH-03); the label says why, so "1 queued" doesn't look wrong
  if (restaurantStatus === 'PAUSED') {
    return { level: 'long', label: 'Paused' };
  }
  if (queued >= 6) {
    return { level: 'long', label: 'Long' };
  }
  if (queued >= 3) {
    return { level: 'building', label: 'Building' };
  }
  return { level: 'short', label: 'Short' };
}
