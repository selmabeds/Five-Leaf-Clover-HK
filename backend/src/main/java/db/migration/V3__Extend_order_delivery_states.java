package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.SQLException;

public class V3__Extend_order_delivery_states extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws SQLException {
        String query = """
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.check_constraints cc
                  ON cc.constraint_schema = tc.constraint_schema
                 AND cc.constraint_name = tc.constraint_name
                WHERE LOWER(tc.table_schema) = 'careeats'
                  AND LOWER(tc.table_name) = 'orders'
                  AND UPPER(cc.check_clause) LIKE '%PLACED%'
                  AND UPPER(cc.check_clause) LIKE '%CANCELLED%'
                """;

        String constraintName;
        try (var statement = context.getConnection().prepareStatement(query);
             var result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Order status constraint was not found");
            }

            constraintName = result.getString(1);
        }

        String quotedName = "\"" + constraintName.replace("\"", "\"\"") + "\"";

        try (var statement = context.getConnection().createStatement()) {
            statement.executeUpdate(
                    "ALTER TABLE careeats.orders DROP CONSTRAINT " + quotedName);

            statement.executeUpdate("""
                    ALTER TABLE careeats.orders
                    ADD CONSTRAINT orders_status_check
                    CHECK (status IN (
                        'PLACED', 'ACCEPTED', 'PREPARING', 'READY',
                        'PICKED_UP', 'DELIVERED', 'REJECTED', 'CANCELLED'
                    ))
                    """);
        }
    }
}