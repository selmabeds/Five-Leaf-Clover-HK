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
        return jdbc.query("SELECT * FROM careeats.restaurants WHERE id=?",(rs,n) -> Restaurant.restore(
            id,rs.getString("name"),rs.getDouble("latitude"),rs.getDouble("longitude"),
            RestaurantStatus.valueOf(rs.getString("status")),items(id),restaurantHistory(id)),id)
            .stream().findFirst().orElse(null);
    }
    @Override public List<Restaurant> findAll() {
        return jdbc.queryForList("SELECT id FROM careeats.restaurants ORDER BY id",Long.class)
            .stream().map(this::findById).toList();
    }
    private List<MenuItem> items(Long restaurantId) {
        return jdbc.query("SELECT * FROM careeats.menu_items WHERE restaurant_id=? ORDER BY position",(rs,n) -> {
            long itemId=rs.getLong("item_id");
            List<String> ingredients=jdbc.queryForList(
                "SELECT ingredient FROM careeats.menu_ingredients WHERE restaurant_id=? AND item_id=? ORDER BY position",
                String.class,restaurantId,itemId);
            List<MenuItemTransitionRecord> history=jdbc.query(
                "SELECT * FROM careeats.menu_transitions WHERE restaurant_id=? AND item_id=? ORDER BY position",
                (r,i) -> new MenuItemTransitionRecord(r.getString("event_id"),itemId,
                    MenuItemStatus.valueOf(r.getString("previous_status")),MenuItemStatus.valueOf(r.getString("new_status")),
                    r.getTimestamp("occurred_at").toLocalDateTime()),restaurantId,itemId);
            return MenuItem.restore(itemId,rs.getString("name"),rs.getBigDecimal("price"),ingredients,
                MenuItemStatus.valueOf(rs.getString("status")),history);
        },restaurantId);
    }
    private List<RestaurantTransitionRecord> restaurantHistory(Long id) {
        return jdbc.query("SELECT * FROM careeats.restaurant_transitions WHERE restaurant_id=? ORDER BY position",
            (r,n) -> new RestaurantTransitionRecord(r.getString("event_id"),id,
                RestaurantStatus.valueOf(r.getString("previous_status")),RestaurantStatus.valueOf(r.getString("new_status")),
                r.getTimestamp("occurred_at").toLocalDateTime()),id);
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
