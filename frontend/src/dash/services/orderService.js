
/**
 * DASH order communication service.
 *
 * Communicates with the ORD backend.
 * ORD owns restaurant and order business rules.
 */

// GET request to ORD; throws the server's message if the request fails
async function getJson(url) {
  const response = await fetch(url, { credentials: 'include' });
  if (!response.ok) throw new Error(await response.text());
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
  if (!response.ok) throw new Error(await response.text());
  return response.json();
}

export async function getMenu(restaurantId) {
  // TODO: Request the restaurant menu from ORD.
  throw new Error("Menu integration not implemented yet.");
}

export async function placeOrder(orderData) {
  // TODO: Send a new order request to ORD.
  throw new Error("Order placement not implemented yet.");
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
