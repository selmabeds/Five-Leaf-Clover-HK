package com.careeats.backend.ord.infrastructure;

import com.careeats.backend.ord.application.MenuService;
import com.careeats.backend.ord.application.OrderService;
import com.careeats.backend.ord.application.RestaurantService;
import com.careeats.backend.ord.domain.MenuItem;
import com.careeats.backend.ord.domain.Restaurant;
import com.careeats.backend.ord.domain.RestaurantStatus;
import com.careeats.backend.reg.domain.User;
import com.careeats.backend.reg.infrastructure.UserRepo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OrderConfiguration {

    
@Bean
public RestaurantRepository restaurantRepository() {
        RestaurantRepository repository = new RestaurantRepository();

        // Original test restaurant
        Restaurant test = new Restaurant(
            1L, "CARE Eats Test Restaurant",
            45.5017, -73.5673, RestaurantStatus.OPEN
        );
        test.addMenuItem(new MenuItem(1L, "Vegetable Bowl",
            new BigDecimal("12.99"),
            List.of("Rice", "Carrots", "Broccoli")));
        test.addMenuItem(new MenuItem(2L, "Chicken Wrap",
            new BigDecimal("10.99"),
            List.of("Chicken", "Lettuce", "Tortilla")));
        repository.save(test);

        // GLUTEN-FREE (3)
        Restaurant gluten1 = new Restaurant(
            2L, "No Crumbs Attached",
            45.5080, -73.5700, RestaurantStatus.OPEN);
        gluten1.addMenuItem(new MenuItem(3L, "Chicken Rice Bowl",
            new BigDecimal("15.99"),
            List.of("Chicken", "Rice", "Carrots")));
        gluten1.addMenuItem(new MenuItem(4L, "Salmon Rice Bowl",
            new BigDecimal("18.49"),
            List.of("Salmon", "Rice", "Cucumber")));
        MenuItem cake = new MenuItem(5L, "Flourless Chocolate Cake",
            new BigDecimal("7.99"),
            List.of("Chocolate", "Eggs", "Butter"));
        cake.markSoldOut();
        gluten1.addMenuItem(cake);
        repository.save(gluten1);

        Restaurant gluten2 = new Restaurant(
            3L, "Gluten Tag",
            45.5090, -73.5750, RestaurantStatus.OPEN);
        gluten2.addMenuItem(new MenuItem(6L, "Gluten-Free Schnitzel",
            new BigDecimal("17.99"),
            List.of("Chicken", "Rice Flour", "Potatoes")));
        gluten2.addMenuItem(new MenuItem(7L, "Bavarian Potato Salad",
            new BigDecimal("11.49"),
            List.of("Potatoes", "Mustard", "Onions")));
        repository.save(gluten2);

        Restaurant gluten3 = new Restaurant(
            4L, "Wheatless Wonders",
            45.5100, -73.5800, RestaurantStatus.PAUSED);
        gluten3.addMenuItem(new MenuItem(8L, "Gluten-Free Margherita Pizza",
            new BigDecimal("16.99"),
            List.of("Gluten-Free Crust", "Tomato", "Mozzarella")));
        gluten3.addMenuItem(new MenuItem(9L, "Quinoa Power Salad",
            new BigDecimal("13.49"),
            List.of("Quinoa", "Spinach", "Tomato")));
        repository.save(gluten3);

        // HALAL (3)
        Restaurant halal1 = new Restaurant(
            5L, "Halal You Can Eat",
            45.5050, -73.5750, RestaurantStatus.OPEN);
        halal1.addMenuItem(new MenuItem(10L, "Chicken Shawarma Plate",
            new BigDecimal("16.99"),
            List.of("Halal Chicken", "Rice", "Garlic Sauce")));
        halal1.addMenuItem(new MenuItem(11L, "Beef Kofta Wrap",
            new BigDecimal("13.49"),
            List.of("Halal Beef", "Pita", "Tomato")));
        halal1.addMenuItem(new MenuItem(12L, "Falafel Hummus Bowl",
            new BigDecimal("12.99"),
            List.of("Chickpeas", "Hummus", "Lettuce")));
        repository.save(halal1);

        Restaurant halal2 = new Restaurant(
            6L, "Shawarma Drama",
            45.5120, -73.5820, RestaurantStatus.OPEN);
        halal2.addMenuItem(new MenuItem(13L, "Chicken Shawarma Wrap",
            new BigDecimal("12.99"),
            List.of("Halal Chicken", "Pita", "Pickles")));
        halal2.addMenuItem(new MenuItem(14L, "Mixed Grill Platter",
            new BigDecimal("21.99"),
            List.of("Halal Beef", "Halal Chicken", "Rice")));
        MenuItem fries = new MenuItem(15L, "Loaded Shawarma Fries",
            new BigDecimal("10.99"),
            List.of("Potatoes", "Halal Chicken", "Garlic Sauce"));
        fries.markSoldOut();
        halal2.addMenuItem(fries);
        repository.save(halal2);

        Restaurant halal3 = new Restaurant(
            7L, "The Halal Deal",
            45.5140, -73.5890, RestaurantStatus.OPEN);
        halal3.addMenuItem(new MenuItem(16L, "Halal Smash Burger",
            new BigDecimal("15.49"),
            List.of("Halal Beef", "Bun", "Cheese")));
        halal3.addMenuItem(new MenuItem(17L, "Spicy Chicken Sandwich",
            new BigDecimal("14.99"),
            List.of("Halal Chicken", "Bun", "Hot Sauce")));
        halal3.addMenuItem(new MenuItem(18L, "Seasoned Fries",
            new BigDecimal("5.49"),
            List.of("Potatoes", "Salt", "Paprika")));
        repository.save(halal3);

        // VEGAN (2)
        Restaurant vegan1 = new Restaurant(
            8L, "Kale Me Maybe",
            45.5160, -73.5790, RestaurantStatus.OPEN);
        vegan1.addMenuItem(new MenuItem(19L, "Crispy Tofu Power Bowl",
            new BigDecimal("14.49"),
            List.of("Tofu", "Rice", "Avocado")));
        vegan1.addMenuItem(new MenuItem(20L, "Beyond Belief Burger",
            new BigDecimal("17.99"),
            List.of("Plant-Based Patty", "Lettuce", "Vegan Bun")));
        vegan1.addMenuItem(new MenuItem(21L, "Chickpea Curry",
            new BigDecimal("15.49"),
            List.of("Chickpeas", "Coconut Milk", "Spices")));
        repository.save(vegan1);

        Restaurant vegan2 = new Restaurant(
            9L, "Lettuce Be Friends",
            45.5200, -73.5900, RestaurantStatus.OPEN);
        vegan2.addMenuItem(new MenuItem(22L, "Rainbow Buddha Bowl",
            new BigDecimal("15.99"),
            List.of("Quinoa", "Sweet Potato", "Kale")));
        vegan2.addMenuItem(new MenuItem(23L, "BBQ Jackfruit Sandwich",
            new BigDecimal("16.49"),
            List.of("Jackfruit", "BBQ Sauce", "Vegan Bun")));
        MenuItem mousse = new MenuItem(24L, "Chocolate Avocado Mousse",
            new BigDecimal("7.49"),
            List.of("Avocado", "Cocoa", "Maple Syrup"));
        mousse.markSoldOut();
        vegan2.addMenuItem(mousse);
        repository.save(vegan2);

        // DAIRY-FREE (3)
        Restaurant dairy1 = new Restaurant(
            10L, "Udderly Free",
            45.5150, -73.5850, RestaurantStatus.OPEN);
        dairy1.addMenuItem(new MenuItem(25L, "Oat-Milk Alfredo",
            new BigDecimal("16.49"),
            List.of("Pasta", "Oat Milk", "Garlic")));
        dairy1.addMenuItem(new MenuItem(26L, "Tomato Basil Pasta",
            new BigDecimal("14.99"),
            List.of("Pasta", "Tomato", "Basil")));
        dairy1.addMenuItem(new MenuItem(27L, "Coconut Mango Pudding",
            new BigDecimal("6.99"),
            List.of("Coconut Milk", "Mango", "Sugar")));
        repository.save(dairy1);

        Restaurant dairy2 = new Restaurant(
            11L, "Moo-ve Over Milk",
            45.5190, -73.5830, RestaurantStatus.OPEN);
        dairy2.addMenuItem(new MenuItem(28L, "Coconut Curry Noodles",
            new BigDecimal("15.99"),
            List.of("Rice Noodles", "Coconut Milk", "Vegetables")));
        dairy2.addMenuItem(new MenuItem(29L, "Dairy-Free Mac",
            new BigDecimal("14.49"),
            List.of("Pasta", "Cashew-Free Vegan Sauce", "Garlic")));
        repository.save(dairy2);

        Restaurant dairy3 = new Restaurant(
            12L, "Dairy Not",
            45.5220, -73.5950, RestaurantStatus.OPEN);
        dairy3.addMenuItem(new MenuItem(30L, "Grilled Chicken Pita",
            new BigDecimal("14.99"),
            List.of("Chicken", "Pita", "Tomato")));
        dairy3.addMenuItem(new MenuItem(31L, "Mango Coconut Smoothie",
            new BigDecimal("7.49"),
            List.of("Mango", "Coconut Milk", "Ice")));
        MenuItem sorbet = new MenuItem(32L, "Strawberry Sorbet",
            new BigDecimal("6.49"),
            List.of("Strawberries", "Sugar", "Water"));
        sorbet.markSoldOut();
        dairy3.addMenuItem(sorbet);
        repository.save(dairy3);

        // NUT-FREE CONCEPTS (2)
        Restaurant nut1 = new Restaurant(
            13L, "Nuts About Nothing",
            45.5250, -73.5880, RestaurantStatus.OPEN);
        nut1.addMenuItem(new MenuItem(33L, "Lemon Herb Chicken",
            new BigDecimal("17.49"),
            List.of("Chicken", "Lemon", "Herbs")));
        nut1.addMenuItem(new MenuItem(34L, "Garden Quinoa Bowl",
            new BigDecimal("13.99"),
            List.of("Quinoa", "Cucumber", "Tomato")));
        repository.save(nut1);

        Restaurant nut2 = new Restaurant(
            14L, "Nut Today!",
            45.5280, -73.5970, RestaurantStatus.PAUSED);
        nut2.addMenuItem(new MenuItem(35L, "Herb Roasted Chicken",
            new BigDecimal("18.49"),
            List.of("Chicken", "Potatoes", "Rosemary")));
        nut2.addMenuItem(new MenuItem(36L, "Fresh Garden Salad",
            new BigDecimal("11.99"),
            List.of("Lettuce", "Tomato", "Cucumber")));
        MenuItem brownie = new MenuItem(37L, "Chocolate Brownie",
            new BigDecimal("6.49"),
            List.of("Chocolate", "Flour", "Eggs"));
        brownie.markSoldOut();
        nut2.addMenuItem(brownie);
        repository.save(nut2);

        return repository;
    }


    @Bean
    public OrderRepository orderRepository() {
        return new OrderRepository();
    }

   @Bean
    public Map<Long, Long> restaurantAssignments() {
        Map<Long, Long> assignments = new HashMap<>();

        assignments.put(2L, 1L);

        return assignments;
    }

    @Bean
    public RestaurantService restaurantService(
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments) {

        return new RestaurantService(
            restaurantRepository, userRepo, restaurantAssignments
        );
    }

    @Bean
    public MenuService menuService(
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments) {

        return new MenuService(
            restaurantRepository, userRepo, restaurantAssignments
        );
    }

    @Bean
    public OrderService orderService(
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            UserRepo userRepo,
            Map<Long, Long> restaurantAssignments) {

        return new OrderService(
            orderRepository,
            restaurantRepository,
            userRepo,
            restaurantAssignments
        );
    }
}
