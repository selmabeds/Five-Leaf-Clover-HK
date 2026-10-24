
package com.careeats.backend.ord.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MenuItem {

    private final Long itemId;
    private String name;
    private BigDecimal price;
    private MenuItemStatus status;

    private final List<String> ingredients;
    private final List<MenuItemTransitionRecord> transitionHistory;

    public MenuItem(Long itemId, String name, BigDecimal price,
                    List<String> ingredients) {

        if (itemId == null) {
            throw new IllegalArgumentException("Item ID is required");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name is required");
        }

        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        if (ingredients == null ||
        ingredients.stream().anyMatch(item -> item == null)) {
            throw new IllegalArgumentException("Valid ingredients are required");
        }

        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.ingredients = new ArrayList<>(ingredients);
        this.status = MenuItemStatus.AVAILABLE;
        this.transitionHistory = new ArrayList<>();
    }

    public Long getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public MenuItemStatus getStatus() {
        return status;
    }

    public List<String> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public List<MenuItemTransitionRecord> getTransitionHistory() {
        return Collections.unmodifiableList(transitionHistory);
    }

    public void markSoldOut() {
        changeStatus(MenuItemStatus.SOLD_OUT);
    }

    public void markAvailable() {
        changeStatus(MenuItemStatus.AVAILABLE);
    }

    private void changeStatus(MenuItemStatus newStatus) {

        if (status == newStatus) {
            throw new IllegalStateException(
                    "Menu item is already " + newStatus
            );
        }

        MenuItemStatus previousStatus = this.status;

        MenuItemTransitionRecord record = new MenuItemTransitionRecord(
                UUID.randomUUID().toString(),
                itemId,
                previousStatus,
                newStatus
        );

        this.status = newStatus;
        this.transitionHistory.add(record);
    }
}
