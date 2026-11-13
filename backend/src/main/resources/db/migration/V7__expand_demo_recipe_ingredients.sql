-- Expand the original fictional demo recipes. Preserve menu IDs, prices and availability.
-- Existing ingredient lists are replaced only when they match the original seeded list.
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 1 AND item_id = 1
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 1 AND item_id = 1 AND name = 'Vegetable Bowl')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 1 AND item_id = 1)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 1 AND item_id = 1
        AND ((position = 0 AND ingredient = 'Rice') OR (position = 1 AND ingredient = 'Carrots') OR (position = 2 AND ingredient = 'Broccoli')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 1, 1, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Rice'), (1, 'Carrots'), (2, 'Broccoli'), (3, 'Olive Oil'), (4, 'Lemon'), (5, 'Garlic'), (6, 'Salt'), (7, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 1 AND item_id = 1 AND name = 'Vegetable Bowl')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 1 AND item_id = 1);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 1 AND item_id = 2
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 1 AND item_id = 2 AND name = 'Chicken Wrap')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 1 AND item_id = 2)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 1 AND item_id = 2
        AND ((position = 0 AND ingredient = 'Chicken') OR (position = 1 AND ingredient = 'Lettuce') OR (position = 2 AND ingredient = 'Tortilla')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 1, 2, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Chicken'), (1, 'Wheat Flour'), (2, 'Water'), (3, 'Canola Oil'), (4, 'Lettuce'), (5, 'Tomato'), (6, 'Egg'), (7, 'Lemon'), (8, 'Mustard'), (9, 'Salt'), (10, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 1 AND item_id = 2 AND name = 'Chicken Wrap')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 1 AND item_id = 2);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 3
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 2 AND item_id = 3 AND name = 'Chicken Rice Bowl')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 3)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 3
        AND ((position = 0 AND ingredient = 'Chicken') OR (position = 1 AND ingredient = 'Rice') OR (position = 2 AND ingredient = 'Carrots')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 2, 3, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Chicken'), (1, 'Rice'), (2, 'Carrots'), (3, 'Olive Oil'), (4, 'Lemon'), (5, 'Garlic'), (6, 'Paprika'), (7, 'Salt'), (8, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 2 AND item_id = 3 AND name = 'Chicken Rice Bowl')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 3);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 4
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 2 AND item_id = 4 AND name = 'Salmon Rice Bowl')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 4)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 4
        AND ((position = 0 AND ingredient = 'Salmon') OR (position = 1 AND ingredient = 'Rice') OR (position = 2 AND ingredient = 'Cucumber')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 2, 4, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Salmon'), (1, 'Rice'), (2, 'Cucumber'), (3, 'Olive Oil'), (4, 'Lemon'), (5, 'Dill'), (6, 'Salt'), (7, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 2 AND item_id = 4 AND name = 'Salmon Rice Bowl')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 4);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 5
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 2 AND item_id = 5 AND name = 'Flourless Chocolate Cake')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 5)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 5
        AND ((position = 0 AND ingredient = 'Chocolate') OR (position = 1 AND ingredient = 'Eggs') OR (position = 2 AND ingredient = 'Butter')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 2, 5, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Dark Chocolate (Cocoa Mass, Cocoa Butter, Sugar)'), (1, 'Egg'), (2, 'Butter'), (3, 'Sugar'), (4, 'Cocoa Powder'), (5, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 2 AND item_id = 5 AND name = 'Flourless Chocolate Cake')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 2 AND item_id = 5);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 3 AND item_id = 6
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 3 AND item_id = 6 AND name = 'Gluten-Free Schnitzel')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 3 AND item_id = 6)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 3 AND item_id = 6
        AND ((position = 0 AND ingredient = 'Chicken') OR (position = 1 AND ingredient = 'Rice Flour') OR (position = 2 AND ingredient = 'Potatoes')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 3, 6, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Chicken'), (1, 'Rice Flour'), (2, 'Rice Breadcrumbs'), (3, 'Egg'), (4, 'Potatoes'), (5, 'Canola Oil'), (6, 'Lemon'), (7, 'Salt'), (8, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 3 AND item_id = 6 AND name = 'Gluten-Free Schnitzel')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 3 AND item_id = 6);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 3 AND item_id = 7
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 3 AND item_id = 7 AND name = 'Bavarian Potato Salad')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 3 AND item_id = 7)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 3 AND item_id = 7
        AND ((position = 0 AND ingredient = 'Potatoes') OR (position = 1 AND ingredient = 'Mustard') OR (position = 2 AND ingredient = 'Onions')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 3, 7, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Potatoes'), (1, 'Mustard Seeds'), (2, 'Onion'), (3, 'Apple Cider Vinegar'), (4, 'Canola Oil'), (5, 'Parsley'), (6, 'Sugar'), (7, 'Salt'), (8, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 3 AND item_id = 7 AND name = 'Bavarian Potato Salad')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 3 AND item_id = 7);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 4 AND item_id = 8
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 4 AND item_id = 8 AND name = 'Gluten-Free Margherita Pizza')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 4 AND item_id = 8)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 4 AND item_id = 8
        AND ((position = 0 AND ingredient = 'Gluten-Free Crust') OR (position = 1 AND ingredient = 'Tomato') OR (position = 2 AND ingredient = 'Mozzarella')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 4, 8, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Rice Flour'), (1, 'Tapioca Starch'), (2, 'Water'), (3, 'Yeast'), (4, 'Olive Oil'), (5, 'Salt'), (6, 'Tomato'), (7, 'Mozzarella (Milk, Salt, Microbial Rennet)'), (8, 'Basil'), (9, 'Garlic')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 4 AND item_id = 8 AND name = 'Gluten-Free Margherita Pizza')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 4 AND item_id = 8);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 4 AND item_id = 9
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 4 AND item_id = 9 AND name = 'Quinoa Power Salad')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 4 AND item_id = 9)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 4 AND item_id = 9
        AND ((position = 0 AND ingredient = 'Quinoa') OR (position = 1 AND ingredient = 'Spinach') OR (position = 2 AND ingredient = 'Tomato')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 4, 9, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Quinoa'), (1, 'Spinach'), (2, 'Tomato'), (3, 'Chickpeas'), (4, 'Lemon'), (5, 'Olive Oil'), (6, 'Parsley'), (7, 'Salt'), (8, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 4 AND item_id = 9 AND name = 'Quinoa Power Salad')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 4 AND item_id = 9);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 10
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 5 AND item_id = 10 AND name = 'Chicken Shawarma Plate')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 10)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 10
        AND ((position = 0 AND ingredient = 'Halal Chicken') OR (position = 1 AND ingredient = 'Rice') OR (position = 2 AND ingredient = 'Garlic Sauce')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 5, 10, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Halal Chicken'), (1, 'Rice'), (2, 'Garlic'), (3, 'Lemon'), (4, 'Canola Oil'), (5, 'Cumin'), (6, 'Paprika'), (7, 'Turmeric'), (8, 'Cucumber'), (9, 'Tomato'), (10, 'Salt'), (11, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 5 AND item_id = 10 AND name = 'Chicken Shawarma Plate')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 10);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 11
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 5 AND item_id = 11 AND name = 'Beef Kofta Wrap')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 11)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 11
        AND ((position = 0 AND ingredient = 'Halal Beef') OR (position = 1 AND ingredient = 'Pita') OR (position = 2 AND ingredient = 'Tomato')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 5, 11, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Halal Beef'), (1, 'Wheat Flour'), (2, 'Water'), (3, 'Yeast'), (4, 'Tomato'), (5, 'Onion'), (6, 'Parsley'), (7, 'Cumin'), (8, 'Coriander'), (9, 'Olive Oil'), (10, 'Salt'), (11, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 5 AND item_id = 11 AND name = 'Beef Kofta Wrap')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 11);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 12
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 5 AND item_id = 12 AND name = 'Falafel Hummus Bowl')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 12)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 12
        AND ((position = 0 AND ingredient = 'Chickpeas') OR (position = 1 AND ingredient = 'Hummus') OR (position = 2 AND ingredient = 'Lettuce')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 5, 12, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Chickpeas'), (1, 'Parsley'), (2, 'Cilantro'), (3, 'Onion'), (4, 'Garlic'), (5, 'Cumin'), (6, 'Coriander'), (7, 'Canola Oil'), (8, 'Sesame Seeds'), (9, 'Lemon'), (10, 'Lettuce'), (11, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 5 AND item_id = 12 AND name = 'Falafel Hummus Bowl')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 5 AND item_id = 12);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 13
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 6 AND item_id = 13 AND name = 'Chicken Shawarma Wrap')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 13)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 13
        AND ((position = 0 AND ingredient = 'Halal Chicken') OR (position = 1 AND ingredient = 'Pita') OR (position = 2 AND ingredient = 'Pickles')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 6, 13, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Halal Chicken'), (1, 'Wheat Flour'), (2, 'Water'), (3, 'Yeast'), (4, 'Cucumber'), (5, 'Apple Cider Vinegar'), (6, 'Garlic'), (7, 'Lemon'), (8, 'Canola Oil'), (9, 'Paprika'), (10, 'Cumin'), (11, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 6 AND item_id = 13 AND name = 'Chicken Shawarma Wrap')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 13);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 14
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 6 AND item_id = 14 AND name = 'Mixed Grill Platter')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 14)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 14
        AND ((position = 0 AND ingredient = 'Halal Beef') OR (position = 1 AND ingredient = 'Halal Chicken') OR (position = 2 AND ingredient = 'Rice')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 6, 14, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Halal Beef'), (1, 'Halal Chicken'), (2, 'Rice'), (3, 'Olive Oil'), (4, 'Onion'), (5, 'Garlic'), (6, 'Lemon'), (7, 'Paprika'), (8, 'Cumin'), (9, 'Parsley'), (10, 'Salt'), (11, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 6 AND item_id = 14 AND name = 'Mixed Grill Platter')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 14);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 15
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 6 AND item_id = 15 AND name = 'Loaded Shawarma Fries')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 15)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 15
        AND ((position = 0 AND ingredient = 'Potatoes') OR (position = 1 AND ingredient = 'Halal Chicken') OR (position = 2 AND ingredient = 'Garlic Sauce')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 6, 15, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Potatoes'), (1, 'Halal Chicken'), (2, 'Canola Oil'), (3, 'Garlic'), (4, 'Lemon'), (5, 'Paprika'), (6, 'Cumin'), (7, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 6 AND item_id = 15 AND name = 'Loaded Shawarma Fries')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 6 AND item_id = 15);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 16
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 7 AND item_id = 16 AND name = 'Halal Smash Burger')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 16)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 16
        AND ((position = 0 AND ingredient = 'Halal Beef') OR (position = 1 AND ingredient = 'Bun') OR (position = 2 AND ingredient = 'Cheese')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 7, 16, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Halal Beef'), (1, 'Wheat Flour'), (2, 'Water'), (3, 'Yeast'), (4, 'Sugar'), (5, 'Canola Oil'), (6, 'Cheddar (Milk, Salt, Microbial Rennet)'), (7, 'Onion'), (8, 'Cucumber'), (9, 'Apple Cider Vinegar'), (10, 'Salt'), (11, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 7 AND item_id = 16 AND name = 'Halal Smash Burger')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 16);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 17
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 7 AND item_id = 17 AND name = 'Spicy Chicken Sandwich')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 17)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 17
        AND ((position = 0 AND ingredient = 'Halal Chicken') OR (position = 1 AND ingredient = 'Bun') OR (position = 2 AND ingredient = 'Hot Sauce')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 7, 17, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Halal Chicken'), (1, 'Wheat Flour'), (2, 'Water'), (3, 'Yeast'), (4, 'Canola Oil'), (5, 'Cayenne'), (6, 'Paprika'), (7, 'Garlic'), (8, 'Lettuce'), (9, 'Apple Cider Vinegar'), (10, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 7 AND item_id = 17 AND name = 'Spicy Chicken Sandwich')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 17);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 18
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 7 AND item_id = 18 AND name = 'Seasoned Fries')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 18)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 18
        AND ((position = 0 AND ingredient = 'Potatoes') OR (position = 1 AND ingredient = 'Salt') OR (position = 2 AND ingredient = 'Paprika')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 7, 18, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Potatoes'), (1, 'Canola Oil'), (2, 'Salt'), (3, 'Paprika'), (4, 'Garlic Powder')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 7 AND item_id = 18 AND name = 'Seasoned Fries')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 7 AND item_id = 18);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 19
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 8 AND item_id = 19 AND name = 'Crispy Tofu Power Bowl')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 19)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 19
        AND ((position = 0 AND ingredient = 'Tofu') OR (position = 1 AND ingredient = 'Rice') OR (position = 2 AND ingredient = 'Avocado')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 8, 19, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Tofu (Soybeans, Water, Calcium Sulfate)'), (1, 'Rice'), (2, 'Avocado'), (3, 'Broccoli'), (4, 'Cornstarch'), (5, 'Canola Oil'), (6, 'Sesame Seeds'), (7, 'Ginger'), (8, 'Lime'), (9, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 8 AND item_id = 19 AND name = 'Crispy Tofu Power Bowl')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 19);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 20
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 8 AND item_id = 20 AND name = 'Beyond Belief Burger')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 20)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 20
        AND ((position = 0 AND ingredient = 'Plant-Based Patty') OR (position = 1 AND ingredient = 'Lettuce') OR (position = 2 AND ingredient = 'Vegan Bun')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 8, 20, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Pea Protein'), (1, 'Beetroot'), (2, 'Canola Oil'), (3, 'Potato Starch'), (4, 'Wheat Flour'), (5, 'Water'), (6, 'Yeast'), (7, 'Sugar'), (8, 'Lettuce'), (9, 'Tomato'), (10, 'Onion'), (11, 'Mustard Seeds'), (12, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 8 AND item_id = 20 AND name = 'Beyond Belief Burger')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 20);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 21
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 8 AND item_id = 21 AND name = 'Chickpea Curry')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 21)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 21
        AND ((position = 0 AND ingredient = 'Chickpeas') OR (position = 1 AND ingredient = 'Coconut Milk') OR (position = 2 AND ingredient = 'Spices')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 8, 21, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Chickpeas'), (1, 'Coconut Milk (Coconut, Water)'), (2, 'Tomato'), (3, 'Onion'), (4, 'Garlic'), (5, 'Ginger'), (6, 'Cumin'), (7, 'Coriander'), (8, 'Turmeric'), (9, 'Canola Oil'), (10, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 8 AND item_id = 21 AND name = 'Chickpea Curry')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 8 AND item_id = 21);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 22
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 9 AND item_id = 22 AND name = 'Rainbow Buddha Bowl')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 22)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 22
        AND ((position = 0 AND ingredient = 'Quinoa') OR (position = 1 AND ingredient = 'Sweet Potato') OR (position = 2 AND ingredient = 'Kale')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 9, 22, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Quinoa'), (1, 'Sweet Potato'), (2, 'Kale'), (3, 'Chickpeas'), (4, 'Carrots'), (5, 'Red Cabbage'), (6, 'Sesame Seeds'), (7, 'Lemon'), (8, 'Olive Oil'), (9, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 9 AND item_id = 22 AND name = 'Rainbow Buddha Bowl')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 22);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 23
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 9 AND item_id = 23 AND name = 'BBQ Jackfruit Sandwich')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 23)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 23
        AND ((position = 0 AND ingredient = 'Jackfruit') OR (position = 1 AND ingredient = 'BBQ Sauce') OR (position = 2 AND ingredient = 'Vegan Bun')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 9, 23, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Jackfruit'), (1, 'Tomato Paste'), (2, 'Apple Cider Vinegar'), (3, 'Brown Sugar'), (4, 'Paprika'), (5, 'Garlic'), (6, 'Wheat Flour'), (7, 'Water'), (8, 'Yeast'), (9, 'Canola Oil'), (10, 'Red Cabbage'), (11, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 9 AND item_id = 23 AND name = 'BBQ Jackfruit Sandwich')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 23);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 24
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 9 AND item_id = 24 AND name = 'Chocolate Avocado Mousse')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 24)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 24
        AND ((position = 0 AND ingredient = 'Avocado') OR (position = 1 AND ingredient = 'Cocoa') OR (position = 2 AND ingredient = 'Maple Syrup')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 9, 24, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Avocado'), (1, 'Cocoa Powder'), (2, 'Maple Syrup'), (3, 'Vanilla Bean'), (4, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 9 AND item_id = 24 AND name = 'Chocolate Avocado Mousse')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 9 AND item_id = 24);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 25
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 10 AND item_id = 25 AND name = 'Oat-Milk Alfredo')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 25)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 25
        AND ((position = 0 AND ingredient = 'Pasta') OR (position = 1 AND ingredient = 'Oat Milk') OR (position = 2 AND ingredient = 'Garlic')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 10, 25, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Wheat Semolina'), (1, 'Water'), (2, 'Oat Milk (Oats, Water)'), (3, 'Olive Oil'), (4, 'Wheat Flour'), (5, 'Garlic'), (6, 'Nutritional Yeast'), (7, 'Lemon'), (8, 'Salt'), (9, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 10 AND item_id = 25 AND name = 'Oat-Milk Alfredo')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 25);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 26
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 10 AND item_id = 26 AND name = 'Tomato Basil Pasta')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 26)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 26
        AND ((position = 0 AND ingredient = 'Pasta') OR (position = 1 AND ingredient = 'Tomato') OR (position = 2 AND ingredient = 'Basil')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 10, 26, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Wheat Semolina'), (1, 'Water'), (2, 'Tomato'), (3, 'Basil'), (4, 'Garlic'), (5, 'Olive Oil'), (6, 'Salt'), (7, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 10 AND item_id = 26 AND name = 'Tomato Basil Pasta')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 26);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 27
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 10 AND item_id = 27 AND name = 'Coconut Mango Pudding')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 27)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 27
        AND ((position = 0 AND ingredient = 'Coconut Milk') OR (position = 1 AND ingredient = 'Mango') OR (position = 2 AND ingredient = 'Sugar')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 10, 27, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Coconut Milk (Coconut, Water)'), (1, 'Mango'), (2, 'Sugar'), (3, 'Cornstarch'), (4, 'Lime'), (5, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 10 AND item_id = 27 AND name = 'Coconut Mango Pudding')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 10 AND item_id = 27);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 11 AND item_id = 28
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 11 AND item_id = 28 AND name = 'Coconut Curry Noodles')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 11 AND item_id = 28)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 11 AND item_id = 28
        AND ((position = 0 AND ingredient = 'Rice Noodles') OR (position = 1 AND ingredient = 'Coconut Milk') OR (position = 2 AND ingredient = 'Vegetables')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 11, 28, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Rice Noodles (Rice Flour, Water)'), (1, 'Coconut Milk (Coconut, Water)'), (2, 'Carrots'), (3, 'Broccoli'), (4, 'Bell Pepper'), (5, 'Ginger'), (6, 'Garlic'), (7, 'Turmeric'), (8, 'Cumin'), (9, 'Lime'), (10, 'Canola Oil'), (11, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 11 AND item_id = 28 AND name = 'Coconut Curry Noodles')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 11 AND item_id = 28);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 11 AND item_id = 29
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 11 AND item_id = 29 AND name = 'Dairy-Free Mac')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 11 AND item_id = 29)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 11 AND item_id = 29
        AND ((position = 0 AND ingredient = 'Pasta') OR (position = 1 AND ingredient = 'Cashew-Free Vegan Sauce') OR (position = 2 AND ingredient = 'Garlic')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 11, 29, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Wheat Semolina'), (1, 'Water'), (2, 'Potatoes'), (3, 'Carrots'), (4, 'Olive Oil'), (5, 'Nutritional Yeast'), (6, 'Lemon'), (7, 'Garlic'), (8, 'Onion Powder'), (9, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 11 AND item_id = 29 AND name = 'Dairy-Free Mac')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 11 AND item_id = 29);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 30
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 12 AND item_id = 30 AND name = 'Grilled Chicken Pita')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 30)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 30
        AND ((position = 0 AND ingredient = 'Chicken') OR (position = 1 AND ingredient = 'Pita') OR (position = 2 AND ingredient = 'Tomato')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 12, 30, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Chicken'), (1, 'Wheat Flour'), (2, 'Water'), (3, 'Yeast'), (4, 'Tomato'), (5, 'Cucumber'), (6, 'Lettuce'), (7, 'Olive Oil'), (8, 'Lemon'), (9, 'Garlic'), (10, 'Paprika'), (11, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 12 AND item_id = 30 AND name = 'Grilled Chicken Pita')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 30);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 31
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 12 AND item_id = 31 AND name = 'Mango Coconut Smoothie')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 31)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 31
        AND ((position = 0 AND ingredient = 'Mango') OR (position = 1 AND ingredient = 'Coconut Milk') OR (position = 2 AND ingredient = 'Ice')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 12, 31, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Mango'), (1, 'Coconut Milk (Coconut, Water)'), (2, 'Ice'), (3, 'Lime')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 12 AND item_id = 31 AND name = 'Mango Coconut Smoothie')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 31);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 32
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 12 AND item_id = 32 AND name = 'Strawberry Sorbet')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 32)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 32
        AND ((position = 0 AND ingredient = 'Strawberries') OR (position = 1 AND ingredient = 'Sugar') OR (position = 2 AND ingredient = 'Water')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 12, 32, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Strawberries'), (1, 'Sugar'), (2, 'Water'), (3, 'Lemon')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 12 AND item_id = 32 AND name = 'Strawberry Sorbet')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 12 AND item_id = 32);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 13 AND item_id = 33
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 13 AND item_id = 33 AND name = 'Lemon Herb Chicken')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 13 AND item_id = 33)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 13 AND item_id = 33
        AND ((position = 0 AND ingredient = 'Chicken') OR (position = 1 AND ingredient = 'Lemon') OR (position = 2 AND ingredient = 'Herbs')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 13, 33, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Chicken'), (1, 'Lemon'), (2, 'Olive Oil'), (3, 'Garlic'), (4, 'Parsley'), (5, 'Thyme'), (6, 'Salt'), (7, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 13 AND item_id = 33 AND name = 'Lemon Herb Chicken')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 13 AND item_id = 33);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 13 AND item_id = 34
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 13 AND item_id = 34 AND name = 'Garden Quinoa Bowl')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 13 AND item_id = 34)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 13 AND item_id = 34
        AND ((position = 0 AND ingredient = 'Quinoa') OR (position = 1 AND ingredient = 'Cucumber') OR (position = 2 AND ingredient = 'Tomato')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 13, 34, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Quinoa'), (1, 'Cucumber'), (2, 'Tomato'), (3, 'Chickpeas'), (4, 'Parsley'), (5, 'Lemon'), (6, 'Olive Oil'), (7, 'Salt'), (8, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 13 AND item_id = 34 AND name = 'Garden Quinoa Bowl')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 13 AND item_id = 34);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 35
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 14 AND item_id = 35 AND name = 'Herb Roasted Chicken')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 35)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 35
        AND ((position = 0 AND ingredient = 'Chicken') OR (position = 1 AND ingredient = 'Potatoes') OR (position = 2 AND ingredient = 'Rosemary')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 14, 35, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Chicken'), (1, 'Potatoes'), (2, 'Rosemary'), (3, 'Olive Oil'), (4, 'Garlic'), (5, 'Lemon'), (6, 'Salt'), (7, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 14 AND item_id = 35 AND name = 'Herb Roasted Chicken')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 35);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 36
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 14 AND item_id = 36 AND name = 'Fresh Garden Salad')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 36)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 36
        AND ((position = 0 AND ingredient = 'Lettuce') OR (position = 1 AND ingredient = 'Tomato') OR (position = 2 AND ingredient = 'Cucumber')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 14, 36, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Lettuce'), (1, 'Tomato'), (2, 'Cucumber'), (3, 'Carrots'), (4, 'Olive Oil'), (5, 'Lemon'), (6, 'Salt'), (7, 'Black Pepper')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 14 AND item_id = 36 AND name = 'Fresh Garden Salad')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 36);
DELETE FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 37
    AND EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 14 AND item_id = 37 AND name = 'Chocolate Brownie')
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 37)
    AND 3 = (SELECT COUNT(*) FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 37
        AND ((position = 0 AND ingredient = 'Chocolate') OR (position = 1 AND ingredient = 'Flour') OR (position = 2 AND ingredient = 'Eggs')));
INSERT INTO careeats.menu_ingredients (restaurant_id, item_id, position, ingredient)
SELECT 14, 37, recipe.pos, recipe.ingredient
FROM (VALUES (0, 'Dark Chocolate (Cocoa Mass, Cocoa Butter, Sugar)'), (1, 'Wheat Flour'), (2, 'Egg'), (3, 'Butter'), (4, 'Sugar'), (5, 'Cocoa Powder'), (6, 'Salt')) AS recipe(pos, ingredient)
WHERE EXISTS (SELECT 1 FROM careeats.menu_items WHERE restaurant_id = 14 AND item_id = 37 AND name = 'Chocolate Brownie')
    AND NOT EXISTS (SELECT 1 FROM careeats.menu_ingredients WHERE restaurant_id = 14 AND item_id = 37);
