
/**
 * DASH order communication service.
 *
 * Communicates with the ORD backend.
 * ORD owns restaurant and order business rules.
 */

// Human-readable error from ORD: the backend answers {"error": "..."}, so keep only the message
async function readError(response) {
  const text = await response.text();
  try {
    return JSON.parse(text).error || text || 'Request failed (' + response.status + ')';
  } catch {
    return text || 'Request failed (' + response.status + ')';
  }
}

// GET request to ORD; throws `errorMessage` (or the server's message) if the request fails
async function getJson(url, errorMessage) {
  const response = await fetch(url, { credentials: 'include' });
  if (!response.ok) throw new Error(errorMessage ?? await readError(response));
  return response.json();
}

// POST command to ORD (needs the CSRF token from the login session)
async function postJson(url, csrfToken, body) {
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': csrfToken
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {})
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json();
}

// All restaurants with their status and position (R-ORD-01)
export function getRestaurants() {
  return getJson('/api/restaurants', 'Could not load restaurants');
}

// UC-ORD-01 Browse Restaurant Menu
export function getMenu(restaurantId) {
  return getJson(`/api/restaurants/${restaurantId}/menu`, 'Could not load menu');
}

// UC-ORD-02 Order Food: orderData = { restaurantId, orderLines: [{ menuItemId, quantity }] }
// ORD checks R-ORD-02 (open restaurant, available items) before creating the order
export function placeOrder(orderData, csrfToken) {
  return postJson('/api/orders', csrfToken, orderData);
}

// Orders of the logged-in staff member's restaurant (Spec Appendix A; UC-ORD-08 checks it)
export function getRestaurantOrders(restaurantId) {
  return getJson(`/api/restaurants/${restaurantId}/orders`);
}

// UC-ORD-04 Respond to Order: accept
export function acceptOrder(orderId, csrfToken) {
  return postJson(`/api/orders/${orderId}/accept`, csrfToken);
}

// UC-ORD-04 Respond to Order: reject with a reason
export function rejectOrder(orderId, reason, csrfToken) {
  return postJson(`/api/orders/${orderId}/reject`, csrfToken, { reason });
}

// UC-ORD-02 step 6: Start Preparation of the next order in the preparation queue (R-ORD-03)
export function startPreparation(restaurantId, csrfToken) {
  return postJson(`/api/restaurants/${restaurantId}/start-next`, csrfToken);
}

// UC-ORD-02 steps 7-8: mark a prepared order ready
export function markOrderReady(orderId, csrfToken) {
  return postJson(`/api/orders/${orderId}/ready`, csrfToken);
}

// UC-ORD-07 View Order Status: the logged-in customer's own orders only
export function getCustomerOrders() {
  return getJson('/api/orders/customer');
}

// UC-ORD-03 Cancel Order: a customer may cancel only while the order is PLACED
export function cancelOrder(orderId, csrfToken) {
  return postJson(`/api/orders/${orderId}/cancel`, csrfToken);
}

// UC-ORD-05 Manage Restaurant Menu: item availability
export function markItemSoldOut(restaurantId, itemId, csrfToken) {
  return postJson(`/api/restaurants/${restaurantId}/menu/${itemId}/sold-out`, csrfToken);
}

export function markItemAvailable(restaurantId, itemId, csrfToken) {
  return postJson(`/api/restaurants/${restaurantId}/menu/${itemId}/available`, csrfToken);
}

// UC-ORD-06 Manage Restaurant Status
export function pauseRestaurant(restaurantId, csrfToken) {
  return postJson(`/api/restaurants/${restaurantId}/pause`, csrfToken);
}

export function reopenRestaurant(restaurantId, csrfToken) {
  return postJson(`/api/restaurants/${restaurantId}/reopen`, csrfToken);
}
