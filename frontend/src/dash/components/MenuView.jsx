
import OrderView from './OrderView';

/**
 * DASH UI layer: Restaurant Menu (UC-ORD-01 Browse Restaurant Menu).
 *
 * Displays the menu items received from ORD for the selected restaurant,
 * with one OrderView per item to place an order.
 */
export default function MenuView({
  selectedRestaurant,
  selectedRestaurantId,
  menu,
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
      {selectedRestaurant && restaurantClosed && (
        <p className="menu-notice">
          This restaurant is {selectedRestaurant.status.toLowerCase()}. Ordering is unavailable.
        </p>
      )}

      {menu.map(item => {
        const soldOut = item.status !== 'AVAILABLE';

        return (
          <div key={item.itemId}>
            <p>
              {item.name} - ${item.price} - {item.status}
            </p>

            <OrderView
              item={item}
              quantity={quantities[item.itemId] ?? 1}
              disabled={restaurantClosed || soldOut}
              onQuantityChange={onQuantityChange}
              onPlaceOrder={onPlaceOrder}
            />

            {soldOut && !restaurantClosed && (
              <span className="menu-item-note">Sold out</span>
            )}
          </div>
        );
      })}
    </section>
  );
}
