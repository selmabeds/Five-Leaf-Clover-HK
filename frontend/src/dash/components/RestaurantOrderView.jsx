
/**
 * DASH UI layer: Restaurant Orders.
 *
 * Restaurant staff can accept, reject (with a reason), start preparation
 * and mark orders as ready. This view only displays the orders and reports
 * clicks; App.jsx keeps the state and calls orderService.js (application layer).
 */

// When an order became ACCEPTED, read from its transition history (R-ORD-05 event records)
function acceptedTime(order) {
  const accepted = (order.transitionHistory ?? [])
    .filter(record => record.newStatus === 'ACCEPTED')
    .at(-1);

  if (!accepted) return Number.MAX_SAFE_INTEGER;

  const time = Date.parse(accepted.timestamp);
  return Number.isNaN(time) ? Number.MAX_SAFE_INTEGER : time;
}

export default function RestaurantOrderView({
  orders,
  ordersLoading,
  pendingAction,
  rejectingOrderId,
  rejectionReason,
  onRejectionReasonChange,
  onRefresh,
  onAccept,
  onStartReject,
  onConfirmReject,
  onCancelReject,
  onStartPreparation,
  onMarkReady
}) {
  const busy = ordersLoading || pendingAction !== null;

  // R-ORD-03 / UC-ORD-02 step 6: accepted orders are prepared in arrival order (first in, first out).
  // The order whose turn it is = the ACCEPTED order that was accepted first (ties: lowest order number).
  const nextToPrepareId = orders
    .filter(order => order.status === 'ACCEPTED')
    .sort((a, b) => acceptedTime(a) - acceptedTime(b) || a.orderId - b.orderId)[0]?.orderId ?? null;

  return (
    <section aria-busy={busy}>
      <h3>Restaurant Orders</h3>
      {pendingAction && <p role="status">{pendingAction} in progress...</p>}

      <button type="button" disabled={busy} onClick={onRefresh}>
        {ordersLoading ? 'Loading orders...' : 'Refresh Orders'}
      </button>

      {!ordersLoading && orders.length === 0 && (
        <p className="orders-empty">No orders for this restaurant yet.</p>
      )}

      <div className="order-list">
        {orders.map(order => (
          <div className="order-card" key={order.orderId}>
            <div className="order-card-header">
              <span className="order-number">Order #{order.orderId}</span>
              <span className="order-status">{order.status}</span>
            </div>

            <div className="order-actions">
              {order.status === 'PLACED' && rejectingOrderId !== order.orderId && (
                <>
                  <button type="button" disabled={busy} onClick={() => onAccept(order.orderId)}>
                    Accept
                  </button>

                  <button type="button" disabled={busy} onClick={() => onStartReject(order.orderId)}>
                    Reject
                  </button>
                </>
              )}

              {order.status === 'PLACED' && rejectingOrderId === order.orderId && (
                <div className="reject-form">
                  <input
                    type="text"
                    aria-label={'Reason for rejecting order ' + order.orderId}
                    placeholder="Reason for rejecting this order"
                    value={rejectionReason}
                    onChange={event => onRejectionReasonChange(event.target.value)}
                    autoFocus
                  />
                  <button
                    type="button"
                    disabled={busy || !rejectionReason.trim()}
                    onClick={() => onConfirmReject(order.orderId)}
                  >
                    Confirm Reject
                  </button>
                  <button type="button" disabled={busy} onClick={onCancelReject}>
                    Cancel
                  </button>
                </div>
              )}

              {order.status === 'ACCEPTED' && order.orderId === nextToPrepareId && (
                <button type="button" disabled={busy} onClick={() => onStartPreparation(order)}>
                  Start Preparation
                </button>
              )}

              {order.status === 'ACCEPTED' && order.orderId !== nextToPrepareId && (
                <p className="order-note">Waiting in preparation queue</p>
              )}

              {order.status === 'PREPARING' && (
                <button type="button" disabled={busy} onClick={() => onMarkReady(order.orderId)}>
                  Mark Ready
                </button>
              )}

              {order.status === 'READY' && (
                <p className="order-note">Waiting for courier pickup</p>
              )}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
