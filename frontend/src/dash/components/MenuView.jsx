
import OrderView from './OrderView';

/**
 * DASH UI layer: Restaurant Menu (UC-ORD-01 Browse Restaurant Menu).
 *
 * Displays the menu items received from ORD for the selected restaurant,
 * with one OrderView per item to place an order. Only customers may place
 * orders (Spec Appendix A, R-DASH-01), so `canOrder` is false for restaurant
 * staff viewing their own menu: no ordering controls are shown at all.
 *
 * Restaurant staff (`canManage`) instead manage their own menu: item
 * availability (UC-ORD-05) and pausing / reopening the restaurant (UC-ORD-06).
 */
export default function MenuView({
  selectedRestaurant,
  selectedRestaurantId,
  menu,
  canOrder,
  canManage,
  busy,
  onToggleAvailability,
  onRestaurantStatus,
  quantities,
  onQuantityChange,
  onPlaceOrder
}) {
  const restaurantClosed = selectedRestaurant?.status !== 'OPEN';

  return (
    <section>
      <h3>{selectedRestaurant ? 'Menu: ' + selectedRestaurant.name : 'Menu'}</h3>

      {selectedRestaurantId === null && (
        <p>Select a restaurant from the Restaurants tab.</p>
      )}

      {/* R-DASH-04: no ordering from a paused or closed restaurant */}
      {canOrder && selectedRestaurant && restaurantClosed && (
        <p className="menu-notice">
          This restaurant is {selectedRestaurant.status.toLowerCase()}. Ordering is unavailable.
        </p>
      )}

      {/* UC-ORD-06: restaurant staff pause / reopen their own restaurant */}
      {canManage && selectedRestaurant && (
        <div className="menu-notice restaurant-status-bar">
          <span>Restaurant status: {selectedRestaurant.status}</span>
          {selectedRestaurant.status !== 'CLOSED' && (
            <button
              type="button"
              disabled={busy}
              onClick={() => onRestaurantStatus(selectedRestaurant)}
            >
              {selectedRestaurant.status === 'OPEN' ? 'Pause Restaurant' : 'Reopen Restaurant'}
            </button>
          )}
        </div>
      )}

      {menu.map(item => {
        const soldOut = item.status !== 'AVAILABLE';

        return (
          <div key={item.itemId}>
            <p>
              {item.name} - ${item.price} - {item.status}
            </p>

            {canOrder && (
              <OrderView
                item={item}
                quantity={quantities[item.itemId] ?? 1}
                disabled={restaurantClosed || soldOut}
                onQuantityChange={onQuantityChange}
                onPlaceOrder={onPlaceOrder}
              />
            )}

            {canOrder && soldOut && !restaurantClosed && (
              <span className="menu-item-note">Sold out</span>
            )}

            {/* UC-ORD-05: restaurant staff mark their items sold out / available */}
            {canManage && (
              <button
                type="button"
                disabled={busy}
                onClick={() => onToggleAvailability(item)}
              >
                {soldOut ? 'Mark Available' : 'Mark Sold Out'}
              </button>
            )}
          </div>
        );
      })}
    </section>
  );
}
