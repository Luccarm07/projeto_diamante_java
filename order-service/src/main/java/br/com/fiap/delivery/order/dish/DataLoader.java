package br.com.fiap.delivery.order.dish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Seeds the menu. Dish #1 is the flash-sale dish and starts with only 10 units. */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final DishRepository dishRepository;

    public DataLoader(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Override
    public void run(String... args) {
        if (dishRepository.count() > 0) {
            return;
        }
        List<Dish> menu = List.of(
                new Dish("House Burger",
                        "Flash sale! Brioche bun, 180g beef patty, cheddar and caramelized onions",
                        new BigDecimal("39.90"), 10),
                new Dish("Garden Bowl",
                        "Vegetarian. Quinoa, roasted vegetables, chickpeas, feta cheese and tahini dressing",
                        new BigDecimal("34.90"), 40),
                new Dish("Margherita Pizza",
                        "Vegetarian. Tomato sauce, mozzarella and fresh basil on a thin crust",
                        new BigDecimal("54.90"), 30),
                new Dish("Coconut Chickpea Curry",
                        "Vegan. Chickpeas in coconut milk curry served with jasmine rice",
                        new BigDecimal("42.00"), 25),
                new Dish("Grilled Salmon",
                        "Salmon fillet with herb rice and sauteed asparagus",
                        new BigDecimal("72.50"), 20),
                new Dish("Chicken Caesar Salad",
                        "Romaine lettuce, grilled chicken, parmesan, croutons and caesar dressing",
                        new BigDecimal("36.90"), 35),
                new Dish("Chocolate Brownie",
                        "Dessert. Warm brownie with vanilla ice cream",
                        new BigDecimal("18.00"), 50)
        );
        dishRepository.saveAll(menu);
        log.info("Menu loaded with {} dishes (flash-sale dish '{}' has stock {})",
                menu.size(), menu.getFirst().getName(), menu.getFirst().getStock());
    }
}
