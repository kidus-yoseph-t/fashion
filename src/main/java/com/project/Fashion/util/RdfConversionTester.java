package com.project.Fashion.util;

import com.project.Fashion.model.Product;
import com.project.Fashion.repository.ProductRepository; // Keep for individual test if needed
import com.project.Fashion.service.RdfConversionService;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RdfConversionTester implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RdfConversionTester.class);

    private final ProductRepository productRepository; // Keep if you still want to test individual product output
    private final RdfConversionService rdfConversionService;

    public RdfConversionTester(ProductRepository productRepository, RdfConversionService rdfConversionService) {
        this.productRepository = productRepository;
        this.rdfConversionService = rdfConversionService;
    }

    @Override
    @Transactional // Important for lazy loading if any part of this runner accesses lazy fields
    public void run(String... args) throws Exception {
        logger.info("--- Starting RDF Conversion Test and Store Population Runner ---");

        // Step 1: Populate the application-wide RDF store from the database
        try {
            logger.info("Attempting to populate the application RDF store...");
            rdfConversionService.populateRdfStoreFromDatabase();
            logger.info("Finished populating application RDF store. Store size: {}", rdfConversionService.getApplicationRdfStore().size());
        } catch (Exception e) {
            logger.error("Error during RDF store population: {}", e.getMessage(), e);
        }


        // Step 2 (Optional): Test conversion for a single product and print its RDF
        // This part can be kept for detailed verification of a specific product's RDF output.
        // You can comment it out if you only want to populate the store on startup.
        Long productIdToTest = 1L; // <<<< MAKE SURE THIS PRODUCT ID EXISTS IN YOUR DB
        Optional<Product> productOpt = productRepository.findById(productIdToTest);

        if (productOpt.isPresent()) {
            Product productToTest = productOpt.get();
            logger.info("Testing individual RDF conversion for Product ID: {}", productToTest.getId());
            // Log some details to verify inputs
            logger.info("Product Details: Name='{}', Category='{}', SellerID='{}', NumReviews='{}'",
                    productToTest.getName(),
                    productToTest.getCategory(),
                    productToTest.getSeller() != null ? productToTest.getSeller().getId() : "N/A",
                    productToTest.getReviews() != null ? productToTest.getReviews().size() : 0);


            try {
                Model individualProductRdfModel = rdfConversionService.convertProductToRdf(productToTest);
                if (individualProductRdfModel != null && !individualProductRdfModel.isEmpty()) {
                    logger.info("--- RDF Output for Individual Product ID: {} (Turtle Format) ---", productToTest.getId());
                    StringWriter out = new StringWriter();
                    individualProductRdfModel.write(out, "TTL");
                    logger.info("\n{}\n", out.toString());
                    logger.info("--- End of RDF Output for Individual Product ID: {} ---", productToTest.getId());
                } else {
                    logger.warn("Individual RDF conversion for Product ID: {} resulted in an empty or null model.", productToTest.getId());
                }
            } catch (Exception e) {
                logger.error("Error during individual RDF conversion test for Product ID: {}", productToTest.getId(), e);
            }
        } else {
            logger.warn("Product with ID {} not found for individual RDF conversion test.", productIdToTest);
            List<Long> availableProductIds = productRepository.findAll().stream()
                    .map(Product::getId)
                    .collect(Collectors.toList());
            if (availableProductIds.isEmpty()) {
                logger.warn("No products found in the database for individual testing.");
            } else {
                logger.warn("Available product IDs for individual testing: {}", availableProductIds);
            }
        }

        logger.info("--- RDF Conversion Test and Store Population Runner Finished ---");
    }
}
