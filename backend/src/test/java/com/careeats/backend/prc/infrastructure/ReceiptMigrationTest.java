package com.careeats.backend.prc.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ReceiptMigrationTest {
    @Test void upgradesAnExistingOrderWithoutChangingItsPriceOrQuantity() {
        var source = new SingleConnectionDataSource("jdbc:h2:mem:receipt-upgrade-" + UUID.randomUUID()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "", true);
        try {
        Flyway.configure().dataSource(source).defaultSchema("careeats").schemas("careeats")
            .locations("classpath:db/migration").target("1").load().migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO careeats.app_users(id,name,email,password_hash,role) VALUES (1,'Customer','test@example.test','hash','CUSTOMER')");
        jdbc.update("INSERT INTO careeats.restaurants(id,name,latitude,longitude,status) VALUES (1,'Restaurant',45,-73,'OPEN')");
        jdbc.update("INSERT INTO careeats.menu_items(restaurant_id,item_id,position,name,price,status) VALUES (1,1,0,'Original dish',10.00,'AVAILABLE')");
        jdbc.update("INSERT INTO careeats.orders(id,customer_id,restaurant_id,status) VALUES (9,1,1,'PLACED')");
        jdbc.update("INSERT INTO careeats.order_lines(order_id,restaurant_id,position,item_id,quantity,unit_price) VALUES (9,1,0,1,2,10.00)");
        Flyway.configure().dataSource(source).defaultSchema("careeats").schemas("careeats")
            .locations("classpath:db/migration").load().migrate();
        jdbc.update("UPDATE careeats.menu_items SET name='New name',price=99.99 WHERE restaurant_id=1 AND item_id=1");
        assertEquals("Original dish", jdbc.queryForObject("SELECT item_name FROM careeats.order_lines WHERE order_id=9", String.class));
        assertEquals(new BigDecimal("10.00"), jdbc.queryForObject("SELECT unit_price FROM careeats.order_lines WHERE order_id=9", BigDecimal.class));
        assertEquals(2, jdbc.queryForObject("SELECT quantity FROM careeats.order_lines WHERE order_id=9", Integer.class));
        } finally { source.destroy(); }
    }
}
