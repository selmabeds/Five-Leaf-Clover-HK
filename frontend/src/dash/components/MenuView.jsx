
import OrderView from './OrderView';

/**
 * DASH UI layer: Restaurant Menu (UC-ORD-01 Browse Restaurant Menu).
 *
 * Displays the menu items received from ORD for the selected restaurant,
 * with one OrderView per item to place an order. Only customers may place
 * orders (Spec Appendix A, R-DASH-01), so `canOrder` is false for restaurant
 * staff viewing their own menu: no ordering controls are shown at all.
 */
export default function MenuView({
  selectedRestaurant,
  selectedRestaurantId,
  menu,
  canOrder,
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

      {!canOrder && selectedRestaurant && (
        <p className="menu-notice">Restaurant status: {selectedRestaurant.status}</p>
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
          </div>
        );
      })}
    </section>
  );
}
