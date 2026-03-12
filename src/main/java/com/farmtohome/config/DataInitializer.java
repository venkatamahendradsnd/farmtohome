package com.farmtohome.config;

import com.farmtohome.model.Category;
import com.farmtohome.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        // Define Categories with their default Units
        List<Category> categories = Arrays.asList(
                new Category("Vegetables", "Kg"),
                new Category("Fruits", "Kg"),
                new Category("Grains & Cereals", "Kg"),
                new Category("Pulses & Lentils", "Kg"),
                new Category("Leafy Greens", "Bundle"),
                new Category("Spices", "Gm"),
                new Category("Dairy Products", "Liter"),
                new Category("Dry Fruits & Nuts", "Kg"),
                new Category("Eggs", "Piece") // Added Eggs
        );

        for (Category initialCat : categories) {
            // Check if exists by name
            java.util.Optional<Category> existing = categoryRepository.findByName(initialCat.getName());
            if (existing.isPresent()) {
                // Update unit if missing or changed (optional/robustness)
                Category cat = existing.get();
                cat.setUnit(initialCat.getUnit());
                categoryRepository.save(cat);
            } else {
                categoryRepository.save(initialCat);
            }
        }
    }
}
