
/**
 * DASH UI layer: My Orders (customer).
 *
 * The logged-in customer follows the progress of their own orders
 * (UC-ORD-07 View Order Status) and may cancel an order while it is
 * still PLACED (UC-ORD-03 Cancel Order, R-DASH-01). After that,
 * cancellation is an operator action, so no Cancel button is shown.
 * After delivery the customer can view the order summary with its computed
 * cost (R-PRC-02, UC-PRC-02, sequence diagram displayOrderSummary).
 */
export default function CustomerOrderView({ orders, loading, busy, onRefresh, onCancel, onViewSummary }) {
  return (
    <section aria-busy={loading || busy}>
      <h3>My Orders</h3>

      <button type="button" disabled={loading || busy} onClick={onRefresh}>
        {loading ? 'Loading orders...' : 'Refresh Orders'}
      </button>

      {!loading && orders.length === 0 && (
        <p className="orders-empty">You have no orders yet.</p>
      )}

      <div className="order-list">
        {orders.map(order => (
          <div className="order-card" key={order.orderId}>
            <div className="order-card-header">
              <span className="order-number">Order #{order.orderId}</span>
              <span className="order-status">{order.status}</span>
            </div>

            <div className="order-actions">
              {order.status === 'PLACED' && (
                <button type="button" disabled={busy} onClick={() => onCancel(order.orderId)}>
                  Cancel Order
                </button>
              )}

              {/* After delivery (a refunded order was delivered first) */}
              {(order.status === 'DELIVERED' || order.status === 'REFUNDED') && (
                <button type="button" disabled={busy} onClick={() => onViewSummary(order.orderId)}>
                  View Summary
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
