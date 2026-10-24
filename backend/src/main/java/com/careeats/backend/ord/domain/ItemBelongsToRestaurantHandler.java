package com.careeats.backend.ord.domain;

public class ItemBelongsToRestaurantHandler extends BaseOrderPlacementHandler {

    @Override
    public void handle(OrderPlacementRequest request) {
        for (OrderLine line : request.getOrderLines()) {
            if (findMenuItem(request.getRestaurant(), line.getMenuItemId()) == null) {
                throw new IllegalArgumentException(
                        "Menu item does not belong to this restaurant: "
                                + line.getMenuItemId()
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