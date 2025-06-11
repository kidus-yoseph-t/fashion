package com.project.Fashion.config;

import com.project.Fashion.model.Product;
import com.project.Fashion.model.Review;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.service.RdfConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProductInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProductInitializer.class);

    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RdfConversionService rdfConversionService;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String SELLER_1_EMAIL = "alice@sellershop.com";
    private static final String SELLER_2_EMAIL = "bro@fashion.com";
    private static final String DUMMY_REVIEWER_EMAIL = "seeded-reviewer@fashion.com";

    private record ReviewSeedData(float rating, String comment) {}
    private record ProductSeedData(String name, String description, float price, String category, String image, List<ReviewSeedData> reviews) {}

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("--- Starting Product Initializer (24 Products, 2 Per Category Per Seller) ---");

        User seller1 = findOrCreateUser(SELLER_1_EMAIL, "Alice", "Seller", "SELLER");
        User seller2 = findOrCreateUser(SELLER_2_EMAIL, "Bro", "Fashion", "SELLER");
        User dummyReviewer = findOrCreateUser(DUMMY_REVIEWER_EMAIL, "Review", "User", "BUYER");

        // Group the seed data by category
        Map<String, List<ProductSeedData>> productsByCategory = getProductSeedData().stream()
                .collect(Collectors.groupingBy(ProductSeedData::category));

        for (Map.Entry<String, List<ProductSeedData>> entry : productsByCategory.entrySet()) {
            List<ProductSeedData> productsInCategory = entry.getValue();
            if (productsInCategory.size() < 4) {
                logger.warn("Not enough products in category '{}' to assign 2 to each seller. Skipping category.", entry.getKey());
                continue;
            }

            // Assign first 2 to seller1, next 2 to seller2
            processProductsForSeller(productsInCategory.subList(0, 2), seller1, dummyReviewer);
            processProductsForSeller(productsInCategory.subList(2, 4), seller2, dummyReviewer);
        }

        logger.info("--- Product Initializer Finished ---");
    }

    private void processProductsForSeller(List<ProductSeedData> products, User seller, User reviewer) {
        for (ProductSeedData data : products) {
            if (productRepository.findByName(data.name()).isPresent()) {
                logger.info("Product '{}' already exists. Skipping.", data.name());
                continue;
            }

            Product product = createProductEntity(data, seller, reviewer);
            Product savedProduct = productRepository.save(product);
            logger.info("Created product: '{}' for seller: {}", savedProduct.getName(), savedProduct.getSeller().getEmail());

            rdfConversionService.addOrUpdateProductInRdfStore(savedProduct);
        }
    }

    private Product createProductEntity(ProductSeedData data, User seller, User reviewer) {
        Product product = new Product();
        product.setName(data.name());
        product.setDescription(data.description());
        product.setPrice(data.price());
        product.setCategory(data.category());
        product.setPhotoUrl("/api/products/image/" + data.image());
        product.setSeller(seller);
        product.setStock(new Random().nextInt(50) + 20);

        List<Review> reviews = new ArrayList<>();
        if (data.reviews() != null) {
            for (ReviewSeedData reviewData : data.reviews()) {
                Review review = new Review();
                review.setProduct(product);
                review.setUser(reviewer);
                review.setRating(reviewData.rating());
                review.setComment(reviewData.comment());
                review.setDate(new Date());
                reviews.add(review);
            }
        }
        product.setReviews(reviews);

        if (!reviews.isEmpty()) {
            product.setNumOfReviews(reviews.size());
            double avgRating = reviews.stream().mapToDouble(Review::getRating).average().orElse(0.0);
            product.setAverageRating((float) avgRating);
        } else {
            product.setNumOfReviews(0);
            product.setAverageRating(0.0f);
        }
        return product;
    }

    private User findOrCreateUser(String email, String firstName, String lastName, String role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setRole(role);
            newUser.setPassword(passwordEncoder.encode("Password123"));
            logger.info("Creating user: {} with role {}", email, role);
            return userRepository.save(newUser);
        });
    }

    // This method provides 4 products from each of the six category
    private List<ProductSeedData> getProductSeedData() {
        return List.of(
                // --- Dress Products ---
                new ProductSeedData("Vibrant Sundress", "Bright and colorful sundress for vacations and sunny days.", 129.99f, "Dress", "dress-6.jpg", List.of()),
                new ProductSeedData("Casual Summer Dress", "Lightweight summer dress ideal for warm days.", 89.50f, "Dress", "dress-2.jpg", List.of(new ReviewSeedData(4, "Great for casual outings."))),
                new ProductSeedData("Classic Green Dress", "Timeless green dress for formal occasions.", 149.00f, "Dress", "dress-3.jpg", List.of(new ReviewSeedData(5, "Perfect little black dress."))),
                new ProductSeedData("Floral Maxi Dress", "Colorful maxi dress with floral patterns.", 105.75f, "Dress", "dress-4.jpg", List.of()),

                // --- Jacket Products ---
                new ProductSeedData("Denim Brown Jacket", "Classic denim jacket with a rugged look.", 89.99f, "Jacket", "jacket-1.jpg", List.of(new ReviewSeedData(4.5f, "Love this jacket!"))),
                new ProductSeedData("Leather Biker Jacket", "Tough leather jacket for bikers and style lovers.", 220.00f, "Jacket", "jacket-2.jpg", List.of()),
                new ProductSeedData("Casual Bomber Jacket", "Trendy bomber jacket for casual outings.", 120.00f, "Jacket", "jacket-3.jpg", List.of(new ReviewSeedData(4.5f, "Stylish and lightweight."))),
                new ProductSeedData("Winter Parka", "Heavy-duty parka jacket for cold winters.", 249.99f, "Jacket", "jacket-4.jpg", List.of(new ReviewSeedData(5, "Extremely warm and very high quality."))),

                // --- Kids Products ---
                new ProductSeedData("Kids Floral Dress", "Lovely floral dress for little girls.", 25.00f, "Kids", "kids-1.jpg", List.of(new ReviewSeedData(5, "My kid loves the bright colors!"))),
                new ProductSeedData("Kids Overalls", "Cute denim overalls for kids.", 35.99f, "Kids", "kids-2.jpg", List.of(new ReviewSeedData(4, "Adorable and sturdy."))),
                new ProductSeedData("Kids Winter Jacket", "Warm jacket for cold days at the playground.", 59.99f, "Kids", "kids-3.jpg", List.of(new ReviewSeedData(5, "Keeps my little one warm."))),
                new ProductSeedData("Kids Pink T-Shirt", "Perfect blend of style and comfort.", 48.00f, "Kids", "kids-4.jpg", List.of()),

                // --- Shirt Products ---
                new ProductSeedData("Striped Office Shirt", "Professional shirt with striped pattern.", 59.99f, "Shirt", "shirt-1.jpg", List.of(new ReviewSeedData(4, "Good quality cotton, fits well."))),
                new ProductSeedData("Plaid Flannel Shirt", "Warm flannel shirt with plaid design.", 68.50f, "Shirt", "shirt-2.jpg", List.of()),
                new ProductSeedData("Denim Shirt", "Casual denim shirt with button details.", 70.00f, "Shirt", "shirt-3.jpg", List.of()),
                new ProductSeedData("Slim Fit Shirt", "Modern slim fit shirt for a sharp look.", 80.00f, "Shirt", "shirt-4.jpg", List.of(new ReviewSeedData(4, "Great fit, looks very professional."))),

                // --- T-shirt Products ---
                new ProductSeedData("Graphic Print T-shirt", "Trendy t-shirt with modern print.", 35.00f, "T-shirt", "t-shirt-1.png", List.of(new ReviewSeedData(4.2f, "Cool design."))),
                new ProductSeedData("Plain Black T-shirt", "Simple, everyday basic black tee.", 20.00f, "T-shirt", "t-shirt-2.jpg", List.of()),
                new ProductSeedData("Vintage Rock T-shirt", "Retro rock band inspired t-shirt.", 42.00f, "T-shirt", "t-shirt-3.jpg", List.of(new ReviewSeedData(4.8f, "Awesome vintage vibe!"))),
                new ProductSeedData("Sports Jersey T-shirt", "Athletic inspired jersey t-shirt.", 38.00f, "T-shirt", "t-shirt-4.jpg", List.of()),

                // --- Trouser Products ---
                new ProductSeedData("Classic Black Trousers", "Formal trousers for office and events.", 99.00f, "Trouser", "trouser-1.jpg", List.of()),
                new ProductSeedData("Casual Jogger Pants", "Comfortable jogger pants for casual wear.", 55.00f, "Trouser", "trouser-2.jpg", List.of(new ReviewSeedData(4.7f, "Super comfy joggers."))),
                new ProductSeedData("Slim Fit Chinos", "Modern chinos for smart casual outfits.", 80.00f, "Trouser", "trouser-3.jpg", List.of()),
                new ProductSeedData("High Waist Trousers", "Stylish high waist trousers for women.", 92.50f, "Trouser", "trouser-4.jpg", List.of(new ReviewSeedData(5, "Very flattering and comfortable.")))
        );
    }
}