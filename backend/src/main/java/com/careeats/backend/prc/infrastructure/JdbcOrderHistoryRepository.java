package com.careeats.backend.prc.infrastructure;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.ord.domain.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Transactional(readOnly = true)
public class JdbcOrderHistoryRepository implements OrderHistoryRepository {
    private final JdbcTemplate jdbc;
    public JdbcOrderHistoryRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    private static final String SELECT = """
        SELECT o.id,o.customer_id,o.restaurant_id,o.status,o.rejection_reason,r.name AS restaurant_name,
               t.placed_at,d.courier_id,d.delivered_at,c.amount AS total_charge,c.recorded_at AS charged_at,
               f.amount AS refunded_amount,f.recorded_at AS refunded_at
        FROM careeats.orders o
        JOIN careeats.restaurants r ON r.id=o.restaurant_id
        LEFT JOIN (SELECT order_id,MIN(occurred_at) AS placed_at FROM careeats.order_transitions
                   WHERE previous_status IS NULL GROUP BY order_id) t ON t.order_id=o.id
        LEFT JOIN careeats.delivered_orders d ON d.order_id=o.id
        LEFT JOIN careeats.ledger_entries c ON c.order_id=o.id AND c.entry_type='CHARGE'
        LEFT JOIN careeats.ledger_entries f ON f.order_id=o.id AND f.entry_type='REFUND'
        """;

    // To fulfill R-PRC-04, we need a method where each type of user is restricted to the orders they are allowed to see.
    @Override public List<OrderHistoryRecord> findVisibleTo(User user,Long restaurantId) {
        return switch(user.getRole()) {
            case CUSTOMER -> query("o.customer_id=?",user.getId());
            case RESTAURANT -> query("o.restaurant_id=?",restaurantId);
            case COURIER -> query("d.courier_id=?",user.getId());
            case OPERATOR -> query("1=1");
        };
    }
    @Override public OrderHistoryRecord findById(Long id) {
        return query("o.id=?",id).stream().findFirst().orElse(null);
    }
    private List<OrderHistoryRecord> query(String condition,Object... args) {
        return jdbc.query(SELECT+" WHERE "+condition+" ORDER BY o.id DESC",(rs,n)->new OrderHistoryRecord(
            rs.getLong("id"),rs.getLong("customer_id"),rs.getLong("restaurant_id"),rs.getString("restaurant_name"),
            OrderStatus.valueOf(rs.getString("status")),rs.getString("rejection_reason"),
            rs.getTimestamp("placed_at").toLocalDateTime(),rs.getObject("courier_id",Long.class),
            instant(rs,"delivered_at"),rs.getBigDecimal("total_charge"),instant(rs,"charged_at"),
            rs.getBigDecimal("refunded_amount"),instant(rs,"refunded_at")),args);
    }
    private Instant instant(ResultSet rs,String column) throws SQLException {
        OffsetDateTime value=rs.getObject(column,OffsetDateTime.class);
        return value==null?null:value.toInstant();
    }
    @Override public List<ReceiptLine> findLines(Long id) {
        return jdbc.query("SELECT item_id,item_name,quantity,unit_price FROM careeats.order_lines WHERE order_id=? ORDER BY position",
            (rs,n)->new ReceiptLine(rs.getLong(1),rs.getString(2),rs.getInt(3),rs.getBigDecimal(4)),id);
    }
    @Override public boolean isCourier(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM careeats.app_users WHERE id=? AND role='COURIER'",Long.class,id)>0;
    }
}
