package com.careeats.backend.ord.domain;

// R-ORD-02: no orders containing a sold-out item
public class ItemAvailableHandler extends BaseOrderPlacementHandler {

    @Override
    public void handle(OrderPlacementRequest request) {
        for (OrderLine line : request.getOrderLines()) {
            MenuItem item = findMenuItem(request.getRestaurant(), line.getMenuItemId());

            if (item.getStatus() != MenuItemStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "Menu item is sold out: " + line.getMenuItemId()
                );
            }
        }

        super.handle(request);
    }

    private MenuItem findMenuItem(Restaurant restaurant, Long itemId) {
        for (MenuItem item : restaurant.getMenuItems()) {
            if (item.getItemId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }
}