ALTER TABLE careeats.order_lines ADD COLUMN item_name VARCHAR(255);
UPDATE careeats.order_lines SET item_name = (
    SELECT m.name FROM careeats.menu_items m
    WHERE m.restaurant_id=careeats.order_lines.restaurant_id AND m.item_id=careeats.order_lines.item_id
);
ALTER TABLE careeats.order_lines ALTER COLUMN item_name SET NOT NULL;
