
import { queueBand } from './queueBandRule';

/**
 * DASH UI layer: preparation-queue colour band (R-DASH-03).
 * The rule itself lives in queueBandRule.js so every view uses exactly the same thresholds.
 */

// Coloured label, also readable as text: e.g. "Building · 4 in preparation queue"
// (queued = ACCEPTED orders waiting in the preparation queue, not PLACED / PREPARING / READY)
export default function QueueBand({ queued, restaurantStatus }) {
  const band = queueBand(queued, restaurantStatus);

  return (
    <span
      className={'queue-band queue-' + band.level}
      title="Preparation queue (R-DASH-03): red when 6+ queued or paused, amber at 3-5, green otherwise"
    >
      {band.label} · {queued} in preparation queue
    </span>
  );
}
