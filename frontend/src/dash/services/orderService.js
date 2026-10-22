
/**
 * DASH order communication service.
 *
 * Communicates with the ORD backend.
 * ORD owns restaurant and order business rules.
 */

export async function getMenu(restaurantId) {
  // TODO: Request the restaurant menu from ORD.
  throw new Error("Menu integration not implemented yet.");
}

export async function placeOrder(orderData) {
  // TODO: Send a new order request to ORD.
  throw new Error("Order placement not implemented yet.");
}

export async function acceptOrder(orderId) {
  // TODO: Request order acceptance through ORD.
  throw new Error("Order acceptance not implemented yet.");
}

export async function rejectOrder(orderId, reason) {
  // TODO: Request order rejection through ORD.
  throw new Error("Order rejection not implemented yet.");
}

export async function markOrderReady(orderId) {
  // TODO: Request the ready transition through ORD.
  throw new Error("Mark ready not implemented yet.");
}
