package com.careeats.backend.ord.infrastructure;

import com.careeats.backend.ord.domain.*;
import java.math.BigDecimal;
import java.util.List;

/** Original catalog retained with stable IDs for frontend compatibility. */
public final class DemoCatalog {
    private DemoCatalog() { }
    public static RestaurantRepository create() {
        RestaurantRepository repository = new RestaurantRepository();
        // Original test restaurant
        Restaurant test = new Restaurant(
            1L, "CARE Eats Test Restaurant",
            45.5017, -73.5673, RestaurantStatus.OPEN
        );
        test.addMenuItem(new MenuItem(1L, "Vegetable Bowl",
            new BigDecimal("12.99"),
            List.of("Rice", "Carrots", "Broccoli", "Olive Oil", "Lemon", "Garlic", "Salt", "Black Pepper")));
        test.addMenuItem(new MenuItem(2L, "Chicken Wrap",
            new BigDecimal("10.99"),
            List.of("Chicken", "Wheat Flour", "Water", "Canola Oil", "Lettuce", "Tomato", "Egg", "Lemon", "Mustard", "Salt", "Black Pepper")));
        repository.save(test);

        // GLUTEN-FREE (3)
        Restaurant gluten1 = new Restaurant(
            2L, "No Crumbs Attached",
            45.5080, -73.5700, RestaurantStatus.OPEN);
        gluten1.addMenuItem(new MenuItem(3L, "Chicken Rice Bowl",
            new BigDecimal("15.99"),
            List.of("Chicken", "Rice", "Carrots", "Olive Oil", "Lemon", "Garlic", "Paprika", "Salt", "Black Pepper")));
        gluten1.addMenuItem(new MenuItem(4L, "Salmon Rice Bowl",
            new BigDecimal("18.49"),
            List.of("Salmon", "Rice", "Cucumber", "Olive Oil", "Lemon", "Dill", "Salt", "Black Pepper")));
        MenuItem cake = new MenuItem(5L, "Flourless Chocolate Cake",
            new BigDecimal("7.99"),
            List.of("Dark Chocolate (Cocoa Mass, Cocoa Butter, Sugar)", "Egg", "Butter", "Sugar", "Cocoa Powder", "Salt"));
        cake.markSoldOut();
        gluten1.addMenuItem(cake);
        repository.save(gluten1);

        Restaurant gluten2 = new Restaurant(
            3L, "Gluten Tag",
            45.5090, -73.5750, RestaurantStatus.OPEN);
        gluten2.addMenuItem(new MenuItem(6L, "Gluten-Free Schnitzel",
            new BigDecimal("17.99"),
            List.of("Chicken", "Rice Flour", "Rice Breadcrumbs", "Egg", "Potatoes", "Canola Oil", "Lemon", "Salt", "Black Pepper")));
        gluten2.addMenuItem(new MenuItem(7L, "Bavarian Potato Salad",
            new BigDecimal("11.49"),
            List.of("Potatoes", "Mustard Seeds", "Onion", "Apple Cider Vinegar", "Canola Oil", "Parsley", "Sugar", "Salt", "Black Pepper")));
        repository.save(gluten2);

        Restaurant gluten3 = new Restaurant(
            4L, "Wheatless Wonders",
            45.5100, -73.5800, RestaurantStatus.PAUSED);
        gluten3.addMenuItem(new MenuItem(8L, "Gluten-Free Margherita Pizza",
            new BigDecimal("16.99"),
            List.of("Rice Flour", "Tapioca Starch", "Water", "Yeast", "Olive Oil", "Salt", "Tomato", "Mozzarella (Milk, Salt, Microbial Rennet)", "Basil", "Garlic")));
        gluten3.addMenuItem(new MenuItem(9L, "Quinoa Power Salad",
            new BigDecimal("13.49"),
            List.of("Quinoa", "Spinach", "Tomato", "Chickpeas", "Lemon", "Olive Oil", "Parsley", "Salt", "Black Pepper")));
        repository.save(gluten3);

        // HALAL (3)
        Restaurant halal1 = new Restaurant(
            5L, "Halal You Can Eat",
            45.5050, -73.5750, RestaurantStatus.OPEN);
        halal1.addMenuItem(new MenuItem(10L, "Chicken Shawarma Plate",
            new BigDecimal("16.99"),
            List.of("Halal Chicken", "Rice", "Garlic", "Lemon", "Canola Oil", "Cumin", "Paprika", "Turmeric", "Cucumber", "Tomato", "Salt", "Black Pepper")));
        halal1.addMenuItem(new MenuItem(11L, "Beef Kofta Wrap",
            new BigDecimal("13.49"),
            List.of("Halal Beef", "Wheat Flour", "Water", "Yeast", "Tomato", "Onion", "Parsley", "Cumin", "Coriander", "Olive Oil", "Salt", "Black Pepper")));
        halal1.addMenuItem(new MenuItem(12L, "Falafel Hummus Bowl",
            new BigDecimal("12.99"),
            List.of("Chickpeas", "Parsley", "Cilantro", "Onion", "Garlic", "Cumin", "Coriander", "Canola Oil", "Sesame Seeds", "Lemon", "Lettuce", "Salt")));
        repository.save(halal1);

        Restaurant halal2 = new Restaurant(
            6L, "Shawarma Drama",
            45.5120, -73.5820, RestaurantStatus.OPEN);
        halal2.addMenuItem(new MenuItem(13L, "Chicken Shawarma Wrap",
            new BigDecimal("12.99"),
            List.of("Halal Chicken", "Wheat Flour", "Water", "Yeast", "Cucumber", "Apple Cider Vinegar", "Garlic", "Lemon", "Canola Oil", "Paprika", "Cumin", "Salt")));
        halal2.addMenuItem(new MenuItem(14L, "Mixed Grill Platter",
            new BigDecimal("21.99"),
            List.of("Halal Beef", "Halal Chicken", "Rice", "Olive Oil", "Onion", "Garlic", "Lemon", "Paprika", "Cumin", "Parsley", "Salt", "Black Pepper")));
        MenuItem fries = new MenuItem(15L, "Loaded Shawarma Fries",
            new BigDecimal("10.99"),
            List.of("Potatoes", "Halal Chicken", "Canola Oil", "Garlic", "Lemon", "Paprika", "Cumin", "Salt"));
        fries.markSoldOut();
        halal2.addMenuItem(fries);
        repository.save(halal2);

        Restaurant halal3 = new Restaurant(
            7L, "The Halal Deal",
            45.5140, -73.5890, RestaurantStatus.OPEN);
        halal3.addMenuItem(new MenuItem(16L, "Halal Smash Burger",
            new BigDecimal("15.49"),
            List.of("Halal Beef", "Wheat Flour", "Water", "Yeast", "Sugar", "Canola Oil", "Cheddar (Milk, Salt, Microbial Rennet)", "Onion", "Cucumber", "Apple Cider Vinegar", "Salt", "Black Pepper")));
        halal3.addMenuItem(new MenuItem(17L, "Spicy Chicken Sandwich",
            new BigDecimal("14.99"),
            List.of("Halal Chicken", "Wheat Flour", "Water", "Yeast", "Canola Oil", "Cayenne", "Paprika", "Garlic", "Lettuce", "Apple Cider Vinegar", "Salt")));
        halal3.addMenuItem(new MenuItem(18L, "Seasoned Fries",
            new BigDecimal("5.49"),
            List.of("Potatoes", "Canola Oil", "Salt", "Paprika", "Garlic Powder")));
        repository.save(halal3);

        // VEGAN (2)
        Restaurant vegan1 = new Restaurant(
            8L, "Kale Me Maybe",
            45.5160, -73.5790, RestaurantStatus.OPEN);
        vegan1.addMenuItem(new MenuItem(19L, "Crispy Tofu Power Bowl",
            new BigDecimal("14.49"),
            List.of("Tofu (Soybeans, Water, Calcium Sulfate)", "Rice", "Avocado", "Broccoli", "Cornstarch", "Canola Oil", "Sesame Seeds", "Ginger", "Lime", "Salt")));
        vegan1.addMenuItem(new MenuItem(20L, "Beyond Belief Burger",
            new BigDecimal("17.99"),
            List.of("Pea Protein", "Beetroot", "Canola Oil", "Potato Starch", "Wheat Flour", "Water", "Yeast", "Sugar", "Lettuce", "Tomato", "Onion", "Mustard Seeds", "Salt")));
        vegan1.addMenuItem(new MenuItem(21L, "Chickpea Curry",
            new BigDecimal("15.49"),
            List.of("Chickpeas", "Coconut Milk (Coconut, Water)", "Tomato", "Onion", "Garlic", "Ginger", "Cumin", "Coriander", "Turmeric", "Canola Oil", "Salt")));
        repository.save(vegan1);

        Restaurant vegan2 = new Restaurant(
            9L, "Lettuce Be Friends",
            45.5200, -73.5900, RestaurantStatus.OPEN);
        vegan2.addMenuItem(new MenuItem(22L, "Rainbow Buddha Bowl",
            new BigDecimal("15.99"),
            List.of("Quinoa", "Sweet Potato", "Kale", "Chickpeas", "Carrots", "Red Cabbage", "Sesame Seeds", "Lemon", "Olive Oil", "Salt")));
        vegan2.addMenuItem(new MenuItem(23L, "BBQ Jackfruit Sandwich",
            new BigDecimal("16.49"),
            List.of("Jackfruit", "Tomato Paste", "Apple Cider Vinegar", "Brown Sugar", "Paprika", "Garlic", "Wheat Flour", "Water", "Yeast", "Canola Oil", "Red Cabbage", "Salt")));
        MenuItem mousse = new MenuItem(24L, "Chocolate Avocado Mousse",
            new BigDecimal("7.49"),
            List.of("Avocado", "Cocoa Powder", "Maple Syrup", "Vanilla Bean", "Salt"));
        mousse.markSoldOut();
        vegan2.addMenuItem(mousse);
        repository.save(vegan2);

        // DAIRY-FREE (3)
        Restaurant dairy1 = new Restaurant(
            10L, "Udderly Free",
            45.5150, -73.5850, RestaurantStatus.OPEN);
        dairy1.addMenuItem(new MenuItem(25L, "Oat-Milk Alfredo",
            new BigDecimal("16.49"),
            List.of("Wheat Semolina", "Water", "Oat Milk (Oats, Water)", "Olive Oil", "Wheat Flour", "Garlic", "Nutritional Yeast", "Lemon", "Salt", "Black Pepper")));
        dairy1.addMenuItem(new MenuItem(26L, "Tomato Basil Pasta",
            new BigDecimal("14.99"),
            List.of("Wheat Semolina", "Water", "Tomato", "Basil", "Garlic", "Olive Oil", "Salt", "Black Pepper")));
        dairy1.addMenuItem(new MenuItem(27L, "Coconut Mango Pudding",
            new BigDecimal("6.99"),
            List.of("Coconut Milk (Coconut, Water)", "Mango", "Sugar", "Cornstarch", "Lime", "Salt")));
        repository.save(dairy1);

        Restaurant dairy2 = new Restaurant(
            11L, "Moo-ve Over Milk",
            45.5190, -73.5830, RestaurantStatus.OPEN);
        dairy2.addMenuItem(new MenuItem(28L, "Coconut Curry Noodles",
            new BigDecimal("15.99"),
            List.of("Rice Noodles (Rice Flour, Water)", "Coconut Milk (Coconut, Water)", "Carrots", "Broccoli", "Bell Pepper", "Ginger", "Garlic", "Turmeric", "Cumin", "Lime", "Canola Oil", "Salt")));
        dairy2.addMenuItem(new MenuItem(29L, "Dairy-Free Mac",
            new BigDecimal("14.49"),
            List.of("Wheat Semolina", "Water", "Potatoes", "Carrots", "Olive Oil", "Nutritional Yeast", "Lemon", "Garlic", "Onion Powder", "Salt")));
        repository.save(dairy2);

        Restaurant dairy3 = new Restaurant(
            12L, "Dairy Not",
            45.5220, -73.5950, RestaurantStatus.OPEN);
        dairy3.addMenuItem(new MenuItem(30L, "Grilled Chicken Pita",
            new BigDecimal("14.99"),
            List.of("Chicken", "Wheat Flour", "Water", "Yeast", "Tomato", "Cucumber", "Lettuce", "Olive Oil", "Lemon", "Garlic", "Paprika", "Salt")));
        dairy3.addMenuItem(new MenuItem(31L, "Mango Coconut Smoothie",
            new BigDecimal("7.49"),
            List.of("Mango", "Coconut Milk (Coconut, Water)", "Ice", "Lime")));
        MenuItem sorbet = new MenuItem(32L, "Strawberry Sorbet",
            new BigDecimal("6.49"),
            List.of("Strawberries", "Sugar", "Water", "Lemon"));
        sorbet.markSoldOut();
        dairy3.addMenuItem(sorbet);
        repository.save(dairy3);

        // NUT-FREE CONCEPTS (2)
        Restaurant nut1 = new Restaurant(
            13L, "Nuts About Nothing",
            45.5250, -73.5880, RestaurantStatus.OPEN);
        nut1.addMenuItem(new MenuItem(33L, "Lemon Herb Chicken",
            new BigDecimal("17.49"),
            List.of("Chicken", "Lemon", "Olive Oil", "Garlic", "Parsley", "Thyme", "Salt", "Black Pepper")));
        nut1.addMenuItem(new MenuItem(34L, "Garden Quinoa Bowl",
            new BigDecimal("13.99"),
            List.of("Quinoa", "Cucumber", "Tomato", "Chickpeas", "Parsley", "Lemon", "Olive Oil", "Salt", "Black Pepper")));
        repository.save(nut1);

        Restaurant nut2 = new Restaurant(
            14L, "Nut Today!",
            45.5280, -73.5970, RestaurantStatus.PAUSED);
        nut2.addMenuItem(new MenuItem(35L, "Herb Roasted Chicken",
            new BigDecimal("18.49"),
            List.of("Chicken", "Potatoes", "Rosemary", "Olive Oil", "Garlic", "Lemon", "Salt", "Black Pepper")));
        nut2.addMenuItem(new MenuItem(36L, "Fresh Garden Salad",
            new BigDecimal("11.99"),
            List.of("Lettuce", "Tomato", "Cucumber", "Carrots", "Olive Oil", "Lemon", "Salt", "Black Pepper")));
        MenuItem brownie = new MenuItem(37L, "Chocolate Brownie",
            new BigDecimal("6.49"),
            List.of("Dark Chocolate (Cocoa Mass, Cocoa Butter, Sugar)", "Wheat Flour", "Egg", "Butter", "Sugar", "Cocoa Powder", "Salt"));
        brownie.markSoldOut();
        nut2.addMenuItem(brownie);
        repository.save(nut2);


        return repository;
    }
}
