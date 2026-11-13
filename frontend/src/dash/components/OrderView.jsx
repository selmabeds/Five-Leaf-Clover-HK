
/**
 * DASH UI layer: Place an Order (UC-ORD-02 Order Food).
 *
 * Customers select a quantity for one menu item and submit their order to ORD.
 * R-DASH-04: Place Order is disabled (not hidden) when ordering isn't allowed.
 */
export default function OrderView({ item, quantity, disabled, onQuantityChange, onPlaceOrder }) {
  return (
    <>
      <input
        type="number"
        min="1"
        step="1"
        aria-label={'Quantity of ' + item.name}
        value={quantity}
        disabled={disabled}
        onChange={event => onQuantityChange(item.itemId, event.target.value)}
      />

      <button
        type="button"
        disabled={disabled}
        onClick={() => onPlaceOrder(item.itemId)}
      >
        Place Order
      </button>
    </>
  );
}
