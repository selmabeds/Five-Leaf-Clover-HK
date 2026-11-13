
import QueueBand from './QueueBand';
import { queueBand } from './queueBandRule';

/**
 * DASH UI layer: city map (Spec DASH › Map view; Phase III 5.4).
 *
 * - R-DASH-02: all restaurants and on-duty couriers, positioned by lat/lon within the dataset bounds.
 * - Map Canvas: "canvas scaled to [minLon, maxLon] × [minLat, maxLat]" (shown on the axes).
 * - Restaurant markers: colour = preparation-queue band (R-DASH-03, queueBandRule.js).
 * - Courier markers: badge = courier state (I = idle, A = assigned, C = carrying).
 * - Clicking a restaurant opens a details panel: small metrics, the order queue with states
 *   (own restaurant staff only, Spec Appendix A) and the quick actions the role may use (R-DASH-01).
 * - R-DASH-06: an empty map is handled gracefully.
 * No route planning, travel time or live map services (Spec scope note).
 */

// SVG drawing area (the plot sits inside margins so the axis labels fit)
const WIDTH = 1000;
const HEIGHT = 560;
const LEFT = 80;
const RIGHT = 30;
const TOP = 40;
const BOTTOM = 40;
const GRID_LINES = 4;

const COURIER_BADGE = { IDLE: 'I', ASSIGNED: 'A', CARRYING: 'C' };

// Initials shown inside a restaurant marker, e.g. "CARE Eats Test Restaurant" → "CE", "Nut Today!" → "NT"
function initials(name) {
  const words = (name ?? '').replace(/[^\p{L}\p{N}\s]/gu, '').split(/\s+/).filter(Boolean);
  if (words.length === 0) return '?';
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
}

// Smallest and largest value; a zero span is widened slightly so a single point stays drawable
function range(values) {
  let min = Math.min(...values);
  let max = Math.max(...values);
  if (max - min < 1e-9) {
    min -= 0.005;
    max += 0.005;
  }
  return { min, max };
}

export default function MapView({
  restaurants,
  couriers,
  queueSummary,
  selectedId,
  onSelect,
  role,
  ownRestaurantId,
  ownOrders,
  busy,
  onViewMenu,
  onRestaurantStatus
}) {
  const points = [
    ...restaurants.map(r => ({ lat: r.latitude, lon: r.longitude })),
    ...couriers.map(c => ({ lat: c.latitude, lon: c.longitude }))
  ];

  // R-DASH-06: empty map
  if (points.length === 0) {
    return (
      <div className="map-empty" role="status">
        The map is empty: there are no restaurants or on-duty couriers to show.
      </div>
    );
  }

  // Dataset bounds [minLon, maxLon] × [minLat, maxLat]
  const lon = range(points.map(p => p.lon));
  const lat = range(points.map(p => p.lat));
  const plotWidth = WIDTH - LEFT - RIGHT;
  const plotHeight = HEIGHT - TOP - BOTTOM;
  const x = value => LEFT + ((value - lon.min) / (lon.max - lon.min)) * plotWidth;
  const y = value => TOP + ((lat.max - value) / (lat.max - lat.min)) * plotHeight; // north at the top

  const selected = restaurants.find(r => r.restaurantId === selectedId);
  const stats = selected ? queueSummary[selected.restaurantId] : undefined;
  const isOwnRestaurant = role === 'RESTAURANT' && selected?.restaurantId === ownRestaurantId;
  const canPause = selected && selected.status !== 'CLOSED'
    && (role === 'OPERATOR' || isOwnRestaurant);

  return (
    <div className="map-container">
      {/* Legend: what the marker colours (R-DASH-03) and courier badges mean */}
      <div className="map-legend" aria-label="Map legend">
        <span className="legend-item"><span className="legend-dot marker-short" /> Short queue (0–2)</span>
        <span className="legend-item"><span className="legend-dot marker-building" /> Building (3–5)</span>
        <span className="legend-item"><span className="legend-dot marker-long" /> Long (6+) or paused</span>
        <span className="legend-item"><span className="legend-courier">I</span> Courier idle</span>
        <span className="legend-item"><span className="legend-courier">A</span> Courier assigned</span>
        <span className="legend-item"><span className="legend-courier">C</span> Courier carrying</span>
      </div>

      <svg
        className="map-canvas"
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        role="img"
        aria-label="City map of restaurants and on-duty couriers"
      >
        {/* Grid */}
        {Array.from({ length: GRID_LINES + 1 }, (_, i) => {
          const gx = LEFT + (i / GRID_LINES) * plotWidth;
          const gy = TOP + (i / GRID_LINES) * plotHeight;
          return (
            <g key={i} className="map-grid">
              <line x1={gx} y1={TOP} x2={gx} y2={TOP + plotHeight} />
              <line x1={LEFT} y1={gy} x2={LEFT + plotWidth} y2={gy} />
            </g>
          );
        })}

        {/* Axes: dataset bounds */}
        <text className="map-axis" x={LEFT} y={TOP - 14} textAnchor="start">{lon.min.toFixed(4)}</text>
        <text className="map-axis" x={LEFT + plotWidth} y={TOP - 14} textAnchor="end">{lon.max.toFixed(4)}</text>
        <text className="map-axis" x={LEFT - 10} y={TOP + 5} textAnchor="end">{lat.max.toFixed(4)}</text>
        <text className="map-axis" x={LEFT - 10} y={TOP + plotHeight} textAnchor="end">{lat.min.toFixed(4)}</text>

        {/* Restaurant markers: colour = queue band (R-DASH-03) */}
        {restaurants.map(restaurant => {
          const queued = queueSummary[restaurant.restaurantId]?.queued ?? 0;
          const band = queueBand(queued, restaurant.status);
          const isSelected = restaurant.restaurantId === selectedId;
          const cx = x(restaurant.longitude);
          const cy = y(restaurant.latitude);
          return (
            <g
              key={'r' + restaurant.restaurantId}
              className={'map-restaurant marker-' + band.level + (isSelected ? ' selected' : '')}
              tabIndex={0}
              role="button"
              aria-label={restaurant.name}
              onClick={() => onSelect(restaurant.restaurantId)}
              onKeyDown={event => { if (event.key === 'Enter') onSelect(restaurant.restaurantId); }}
            >
              <circle cx={cx} cy={cy} r={isSelected ? 15 : 13} />
              <text x={cx} y={cy + 4} textAnchor="middle">{initials(restaurant.name)}</text>
              <title>{`${restaurant.name} — ${restaurant.status} — ${band.label} · ${queued} in preparation queue`}</title>
            </g>
          );
        })}

        {/* On-duty courier markers: badge = courier state */}
        {couriers.map(courier => (
          <g key={'c' + courier.id} className="map-courier">
            <circle cx={x(courier.longitude)} cy={y(courier.latitude)} r={9} />
            <text x={x(courier.longitude)} y={y(courier.latitude) + 4} textAnchor="middle">
              {COURIER_BADGE[courier.state] ?? '?'}
            </text>
            <title>{`Courier #${courier.id} — ${courier.state}`}</title>
          </g>
        ))}
      </svg>

      {/* Details panel (pop-up over the map) */}
      {selected && (
        <div className="map-details" role="dialog" aria-label={'Details: ' + selected.name}>
          <div className="map-details-header">
            <h4>{selected.name}</h4>
            <button type="button" className="map-details-close" onClick={() => onSelect(null)}>
              Close
            </button>
          </div>

          {/* Small metrics (counts only, Spec Appendix A) */}
          <p>Status: {selected.status}</p>
          <p><QueueBand queued={stats?.queued ?? 0} restaurantStatus={selected.status} /></p>
          <p>
            {stats?.queued ?? 0} in preparation queue · {stats?.preparing ?? 0} preparing · {stats?.ready ?? 0} ready
          </p>

          {/* Order queue with states: only the restaurant's own staff may see its orders */}
          {isOwnRestaurant && (
            <ul className="map-details-orders">
              {ownOrders.length === 0 && <li>No orders yet.</li>}
              {ownOrders.map(order => (
                <li key={order.orderId}>Order #{order.orderId} — {order.status}</li>
              ))}
            </ul>
          )}

          {/* Quick actions the logged-in role is permitted to use (R-DASH-01) */}
          <div className="map-details-actions">
            {role === 'CUSTOMER' && (
              <button type="button" disabled={busy} onClick={() => onViewMenu(selected.restaurantId)}>
                View Menu
              </button>
            )}
            {canPause && (
              <button type="button" disabled={busy} onClick={() => onRestaurantStatus(selected)}>
                {selected.status === 'OPEN' ? 'Pause' : 'Reopen'}
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
