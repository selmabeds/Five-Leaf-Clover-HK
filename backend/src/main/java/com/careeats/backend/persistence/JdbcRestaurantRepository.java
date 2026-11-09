package com.careeats.backend.persistence;

import com.careeats.backend.ord.domain.*;
import com.careeats.backend.ord.infrastructure.RestaurantRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Transactional
public class JdbcRestaurantRepository extends RestaurantRepository {
    private final JdbcTemplate jdbc;
    public JdbcRestaurantRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void lockForWrite() { DatabaseLock.acquire(jdbc); }
    @Override public boolean existsById(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM careeats.restaurants WHERE id=?",Long.class,id)>0;
    }
    @Override public Restaurant findById(Long id) {
        if (id == null) return null;
        return readRestaurants(id).stream().findFirst().orElse(null);
    }
    @Override public List<Restaurant> findAll() { return readRestaurants(null); }

    private record RestaurantRow(long id, String name, double latitude, double longitude, RestaurantStatus status) { }
    private record ItemKey(long restaurantId, long itemId) { }

    /** Load the catalog in five queries rather than querying inside each row mapper. */
    private List<Restaurant> readRestaurants(Long id) {
        Object[] args = id == null ? new Object[0] : new Object[]{id};
        String headerWhere = id == null ? "" : " WHERE id=?";
        String childWhere = id == null ? "" : " WHERE restaurant_id=?";
        List<RestaurantRow> rows = jdbc.query("SELECT * FROM careeats.restaurants" + headerWhere + " ORDER BY id",
            (rs,n) -> new RestaurantRow(rs.getLong("id"),rs.getString("name"),rs.getDouble("latitude"),
                rs.getDouble("longitude"),RestaurantStatus.valueOf(rs.getString("status"))),args);
        if (rows.isEmpty()) return List.of();

        Map<ItemKey,List<String>> ingredients = new HashMap<>();
        jdbc.query("SELECT * FROM careeats.menu_ingredients" + childWhere + " ORDER BY restaurant_id,item_id,position", rs -> {
            ItemKey key = new ItemKey(rs.getLong("restaurant_id"),rs.getLong("item_id"));
            ingredients.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rs.getString("ingredient"));
        },args);
        Map<ItemKey,List<MenuItemTransitionRecord>> itemHistories = new HashMap<>();
        jdbc.query("SELECT * FROM careeats.menu_transitions" + childWhere + " ORDER BY restaurant_id,item_id,position",rs -> {
            ItemKey key = new ItemKey(rs.getLong("restaurant_id"),rs.getLong("item_id"));
            itemHistories.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new MenuItemTransitionRecord(
                rs.getString("event_id"), key.itemId(), MenuItemStatus.valueOf(rs.getString("previous_status")),
                MenuItemStatus.valueOf(rs.getString("new_status")),rs.getTimestamp("occurred_at").toLocalDateTime()));
        },args);
        Map<Long,List<MenuItem>> items = new HashMap<>();
        jdbc.query("SELECT * FROM careeats.menu_items" + childWhere + " ORDER BY restaurant_id,position",rs -> {
            ItemKey key = new ItemKey(rs.getLong("restaurant_id"),rs.getLong("item_id"));
            items.computeIfAbsent(key.restaurantId(),ignored -> new ArrayList<>()).add(MenuItem.restore(
                key.itemId(),rs.getString("name"),rs.getBigDecimal("price"),ingredients.getOrDefault(key,List.of()),
                MenuItemStatus.valueOf(rs.getString("status")),itemHistories.getOrDefault(key,List.of())));
        },args);
        Map<Long,List<RestaurantTransitionRecord>> histories = new HashMap<>();
        jdbc.query("SELECT * FROM careeats.restaurant_transitions" + childWhere + " ORDER BY restaurant_id,position",rs -> {
            long restaurantId = rs.getLong("restaurant_id");
            histories.computeIfAbsent(restaurantId,ignored -> new ArrayList<>()).add(new RestaurantTransitionRecord(
                rs.getString("event_id"),restaurantId,RestaurantStatus.valueOf(rs.getString("previous_status")),
                RestaurantStatus.valueOf(rs.getString("new_status")),rs.getTimestamp("occurred_at").toLocalDateTime()));
        },args);
        return rows.stream().map(row -> Restaurant.restore(row.id(),row.name(),row.latitude(),row.longitude(),row.status(),
            items.getOrDefault(row.id(),List.of()),histories.getOrDefault(row.id(),List.of()))).toList();
    }
    @Override public void save(Restaurant restaurant) {
        if (restaurant==null) throw new IllegalArgumentException("Restaurant cannot be null");
        lockForWrite();
        long id=restaurant.getRestaurantId();
        int updated=jdbc.update("UPDATE careeats.restaurants SET name=?,latitude=?,longitude=?,status=? WHERE id=?",
            restaurant.getName(),restaurant.getLatitude(),restaurant.getLongitude(),restaurant.getStatus().name(),id);
        if(updated==0) jdbc.update("INSERT INTO careeats.restaurants(id,name,latitude,longitude,status) VALUES (?,?,?,?,?)",
            id,restaurant.getName(),restaurant.getLatitude(),restaurant.getLongitude(),restaurant.getStatus().name());
        int position=0;
        for(MenuItem item:restaurant.getMenuItems()) {
            int changed=jdbc.update("UPDATE careeats.menu_items SET name=?,price=?,status=? WHERE restaurant_id=? AND item_id=?",
                item.getName(),item.getPrice(),item.getStatus().name(),id,item.getItemId());
            if(changed==0) jdbc.update("INSERT INTO careeats.menu_items(restaurant_id,item_id,position,name,price,status) VALUES (?,?,?,?,?,?)",
                id,item.getItemId(),position,item.getName(),item.getPrice(),item.getStatus().name());
            jdbc.update("DELETE FROM careeats.menu_ingredients WHERE restaurant_id=? AND item_id=?",id,item.getItemId());
            int ingredientPosition=0;
            for(String ingredient:item.getIngredients()) jdbc.update(
                "INSERT INTO careeats.menu_ingredients(restaurant_id,item_id,position,ingredient) VALUES (?,?,?,?)",
                id,item.getItemId(),ingredientPosition++,ingredient);
            int eventPosition=0;
            for(MenuItemTransitionRecord event:item.getTransitionHistory()) {
                if(jdbc.queryForObject("SELECT COUNT(*) FROM careeats.menu_transitions WHERE event_id=?",Long.class,event.getEventId())==0)
                    jdbc.update("INSERT INTO careeats.menu_transitions(event_id,restaurant_id,item_id,position,previous_status,new_status,occurred_at) VALUES (?,?,?,?,?,?,?)",
                        event.getEventId(),id,item.getItemId(),eventPosition,event.getPreviousStatus().name(),event.getNewStatus().name(),event.getTimestamp());
                eventPosition++;
            }
            position++;
        }
        int eventPosition=0;
        for(RestaurantTransitionRecord event:restaurant.getTransitionHistory()) {
            if(jdbc.queryForObject("SELECT COUNT(*) FROM careeats.restaurant_transitions WHERE event_id=?",Long.class,event.getEventId())==0)
                jdbc.update("INSERT INTO careeats.restaurant_transitions(event_id,restaurant_id,position,previous_status,new_status,occurred_at) VALUES (?,?,?,?,?,?)",
                    event.getEventId(),id,eventPosition,event.getPreviousStatus().name(),event.getNewStatus().name(),event.getTimestamp());
            eventPosition++;
        }
    }
}
