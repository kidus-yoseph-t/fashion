package com.project.Fashion.service;

import com.project.Fashion.config.RdfConfigProperties;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.Review;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ProductRepository;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.datatypes.xsd.XSDDatatype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList; // For creating lists of statements to remove
import java.util.List;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class RdfConversionService {

    private static final Logger logger = LoggerFactory.getLogger(RdfConversionService.class);

    public static final String SCHEMA_NS = "http://schema.org/";
    public static final Property SCHEMA_name = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "name");
    public static final Property SCHEMA_description = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "description");
    public static final Property SCHEMA_image = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "image");
    public static final Property SCHEMA_Product = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "Product");
    public static final Property SCHEMA_Review = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "Review");
    public static final Property SCHEMA_Person = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "Person");
    public static final Property SCHEMA_Organization = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "Organization");
    public static final Property SCHEMA_itemReviewed = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "itemReviewed");
    public static final Property SCHEMA_reviewRating = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "reviewRating");
    public static final Property SCHEMA_Rating = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "Rating");
    public static final Property SCHEMA_ratingValue = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "ratingValue");
    public static final Property SCHEMA_reviewBody = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "reviewBody");
    public static final Property SCHEMA_datePublished = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "datePublished");
    public static final Property SCHEMA_email = ModelFactory.createDefaultModel().createProperty(SCHEMA_NS + "email");

    private final RdfConfigProperties rdfConfigProperties;
    private final ProductRepository productRepository;
    private Model ontologyModel;

    private Resource fashionProductClass;
    private Resource categoryClass;
    private Resource sellerClass;
    private Resource productReviewClass;

    private Property belongsToCategoryProp;
    private Property soldByProp;
    private Property hasReviewProp;
    private Property reviewedByProp;
    private Property hasPriceProp;
    private Property hasCurrencyProp;
    private Property averageRatingValueProp;
    private Property numberOfReviewsProp;
    private Property ratingValueProp;
    private Property commentTextProp;

    private Model applicationRdfStore;

    @Autowired
    public RdfConversionService(RdfConfigProperties rdfConfigProperties, ProductRepository productRepository) {
        this.rdfConfigProperties = rdfConfigProperties;
        this.productRepository = productRepository;
        this.applicationRdfStore = ModelFactory.createDefaultModel();
        this.applicationRdfStore.setNsPrefix("fash", rdfConfigProperties.getOntologyBaseUri());
        this.applicationRdfStore.setNsPrefix("schema", SCHEMA_NS);
        this.applicationRdfStore.setNsPrefix("xsd", XSDDatatype.XSD + "#");
        this.applicationRdfStore.setNsPrefix("data", rdfConfigProperties.getDataBaseUri());
    }

    @PostConstruct
    public void init() {
        try {
            ontologyModel = ModelFactory.createDefaultModel();
            InputStream ontologyStream = new ClassPathResource("ontology/fashion.ttl").getInputStream();
            ontologyModel.read(ontologyStream, null, "TTL");
            ontologyStream.close();
            logger.info("Fashion ontology loaded successfully from fashion.ttl");

            String ontBase = rdfConfigProperties.getOntologyBaseUri();
            fashionProductClass = ontologyModel.getResource(ontBase + "FashionProduct");
            categoryClass = ontologyModel.getResource(ontBase + "Category");
            sellerClass = ontologyModel.getResource(ontBase + "Seller");
            productReviewClass = ontologyModel.getResource(ontBase + "ProductReview");
            belongsToCategoryProp = ontologyModel.getProperty(ontBase + "belongsToCategory");
            soldByProp = ontologyModel.getProperty(ontBase + "soldBy");
            hasReviewProp = ontologyModel.getProperty(ontBase + "hasReview");
            reviewedByProp = ontologyModel.getProperty(ontBase + "reviewedBy");
            hasPriceProp = ontologyModel.getProperty(ontBase + "hasPrice");
            hasCurrencyProp = ontologyModel.getProperty(ontBase + "hasCurrency");
            averageRatingValueProp = ontologyModel.getProperty(ontBase + "averageRatingValue");
            numberOfReviewsProp = ontologyModel.getProperty(ontBase + "numberOfReviews");
            ratingValueProp = ontologyModel.getProperty(ontBase + "ratingValue");
            commentTextProp = ontologyModel.getProperty(ontBase + "commentText");

            if (fashionProductClass == null || !ontologyModel.containsResource(fashionProductClass)) {
                logger.error("Ontology class 'FashionProduct' not found. Check URI: {}ontology#FashionProduct", rdfConfigProperties.getOntologyBaseUri());
            }
            if (belongsToCategoryProp == null || !ontologyModel.contains(null, belongsToCategoryProp, (org.apache.jena.rdf.model.RDFNode) null) ) {
                logger.warn("Ontology property 'belongsToCategory' might not have been loaded. Check URI: {}ontology#belongsToCategory", rdfConfigProperties.getOntologyBaseUri());
            }
        } catch (Exception e) {
            logger.error("Failed to load fashion ontology: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public void populateRdfStoreFromDatabase() {
        logger.info("Starting to populate application RDF store from database...");
        List<Product> allProducts = productRepository.findAll();
        if (allProducts.isEmpty()) {
            logger.info("No products found in the database to populate RDF store.");
            return;
        }
        Model allProductsRdf = convertProductsToRdf(allProducts);
        synchronized (this.applicationRdfStore) {
            this.applicationRdfStore.add(allProductsRdf);
        }
        logger.info("Successfully populated application RDF store with {} products. Total statements: {}", allProducts.size(), this.applicationRdfStore.size());
    }

    public Model convertProductToRdf(Product product) {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("fash", rdfConfigProperties.getOntologyBaseUri());
        model.setNsPrefix("schema", SCHEMA_NS);
        model.setNsPrefix("xsd", XSDDatatype.XSD + "#");
        model.setNsPrefix("data", rdfConfigProperties.getDataBaseUri());

        if (product == null || product.getId() == null) {
            logger.warn("Product or Product ID is null, cannot convert to RDF.");
            return model;
        }

        String productUri = rdfConfigProperties.getProductUriPrefix() + product.getId();
        Resource productResource = model.createResource(productUri);

        if (fashionProductClass != null) productResource.addProperty(RDF.type, fashionProductClass);
        productResource.addProperty(RDF.type, SCHEMA_Product);

        if (StringUtils.hasText(product.getName())) {
            productResource.addProperty(SCHEMA_name, product.getName(), XSDDatatype.XSDstring);
        }
        if (StringUtils.hasText(product.getDescription())) {
            productResource.addProperty(SCHEMA_description, product.getDescription(), XSDDatatype.XSDstring);
        }
        if (StringUtils.hasText(product.getPhotoUrl())) {
            try {
                productResource.addProperty(SCHEMA_image, model.createResource(product.getPhotoUrl()));
            } catch (Exception e) {
                logger.warn("Invalid photoUrl for product {}: {}. Skipping schema:image.", product.getId(), product.getPhotoUrl());
            }
        }

        if (hasPriceProp != null) productResource.addProperty(hasPriceProp, model.createTypedLiteral(product.getPrice(), XSDDatatype.XSDfloat));
        if (hasCurrencyProp != null) productResource.addProperty(hasCurrencyProp, "USD", XSDDatatype.XSDstring);

        if (averageRatingValueProp != null) productResource.addProperty(averageRatingValueProp, model.createTypedLiteral(product.getAverageRating(), XSDDatatype.XSDfloat));
        if (numberOfReviewsProp != null) productResource.addProperty(numberOfReviewsProp, model.createTypedLiteral(product.getNumOfReviews(), XSDDatatype.XSDinteger));

        if (StringUtils.hasText(product.getCategory()) && belongsToCategoryProp != null && categoryClass != null) {
            String categorySlug = product.getCategory().trim().toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "");
            String categoryUri = rdfConfigProperties.getCategoryUriPrefix() + categorySlug;
            Resource categoryResource = model.createResource(categoryUri)
                    .addProperty(RDF.type, categoryClass)
                    .addProperty(SCHEMA_name, product.getCategory().trim(), XSDDatatype.XSDstring);
            productResource.addProperty(belongsToCategoryProp, categoryResource);
        }

        if (product.getSeller() != null && soldByProp != null && sellerClass != null) {
            User seller = product.getSeller();
            if (seller.getId() != null) {
                String sellerUri = rdfConfigProperties.getSellerUriPrefix() + seller.getId();
                Resource sellerResource = model.createResource(sellerUri)
                        .addProperty(RDF.type, sellerClass)
                        .addProperty(RDF.type, SCHEMA_Organization)
                        .addProperty(SCHEMA_name, (seller.getFirstName() + " " + seller.getLastName()).trim(), XSDDatatype.XSDstring);
                if (StringUtils.hasText(seller.getEmail())) {
                    sellerResource.addProperty(SCHEMA_email, seller.getEmail(), XSDDatatype.XSDstring);
                }
                productResource.addProperty(soldByProp, sellerResource);
            }
        }

        if (product.getReviews() != null && !product.getReviews().isEmpty() && hasReviewProp != null && productReviewClass != null) {
            for (Review review : product.getReviews()) {
                if (review == null || review.getId() == null || review.getUser() == null || review.getUser().getId() == null) {
                    logger.warn("Skipping incomplete review for product {} (ID: {}) during RDF conversion.", product.getName(), product.getId());
                    continue;
                }
                String reviewUri = rdfConfigProperties.getReviewUriPrefix() + review.getId();
                Resource reviewResource = model.createResource(reviewUri);

                if(productReviewClass != null) reviewResource.addProperty(RDF.type, productReviewClass);
                reviewResource.addProperty(RDF.type, SCHEMA_Review);
                reviewResource.addProperty(SCHEMA_itemReviewed, productResource);

                User reviewer = review.getUser();
                String reviewerUri = rdfConfigProperties.getUserUriPrefix() + reviewer.getId();
                Resource reviewerResource = model.createResource(reviewerUri)
                        .addProperty(RDF.type, SCHEMA_Person)
                        .addProperty(SCHEMA_name, (reviewer.getFirstName() + " " + reviewer.getLastName()).trim(), XSDDatatype.XSDstring);
                if(StringUtils.hasText(reviewer.getEmail())){
                    reviewerResource.addProperty(SCHEMA_email, reviewer.getEmail(), XSDDatatype.XSDstring);
                }
                if (reviewedByProp != null) reviewResource.addProperty(reviewedByProp, reviewerResource);

                if (ratingValueProp != null) reviewResource.addProperty(ratingValueProp, model.createTypedLiteral(review.getRating(), XSDDatatype.XSDfloat));

                Resource ratingSchemaResource = model.createResource();
                ratingSchemaResource.addProperty(RDF.type, SCHEMA_Rating);
                ratingSchemaResource.addProperty(SCHEMA_ratingValue, String.valueOf(review.getRating()));
                reviewResource.addProperty(SCHEMA_reviewRating, ratingSchemaResource);

                if (StringUtils.hasText(review.getComment())) {
                    if (commentTextProp != null) reviewResource.addProperty(commentTextProp, review.getComment(), XSDDatatype.XSDstring);
                    reviewResource.addProperty(SCHEMA_reviewBody, review.getComment(), XSDDatatype.XSDstring);
                }
                if (review.getDate() != null) {
                    String isoDateTime = review.getDate().toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    reviewResource.addProperty(SCHEMA_datePublished, model.createTypedLiteral(isoDateTime, XSDDatatype.XSDdateTime));
                }
                productResource.addProperty(hasReviewProp, reviewResource);
            }
        }
        return model;
    }

    public Model convertProductsToRdf(List<Product> products) {
        Model mainModel = ModelFactory.createDefaultModel();
        mainModel.setNsPrefix("fash", rdfConfigProperties.getOntologyBaseUri());
        mainModel.setNsPrefix("schema", SCHEMA_NS);
        mainModel.setNsPrefix("xsd", XSDDatatype.XSD + "#");
        mainModel.setNsPrefix("data", rdfConfigProperties.getDataBaseUri());

        if (products == null || products.isEmpty()) return mainModel;
        for (Product product : products) {
            if (product != null) {
                Model productModel = convertProductToRdf(product);
                mainModel.add(productModel);
            }
        }
        return mainModel;
    }

    public Model getApplicationRdfStore() {
        return this.applicationRdfStore;
    }

    @Transactional(readOnly = true)
    public synchronized void refreshRdfStore() {
        logger.info("Refreshing application RDF store...");
        this.applicationRdfStore.removeAll();
        populateRdfStoreFromDatabase();
        logger.info("Application RDF store refreshed. Total statements: {}", this.applicationRdfStore.size());
    }

    /**
     * Adds or updates a product's RDF representation in the main in-memory store.
     * If the product already exists, its old triples are removed before adding new ones.
     * This method needs to be called when a product is created or updated in the relational DB.
     *
     * @param product The Product entity to add or update in the RDF store.
     * It's crucial that this Product object has its ID and any
     * lazy-loaded associations (like reviews, seller) already fetched if they are
     * to be included in the RDF. This method itself should be called within a transaction
     * if it needs to trigger lazy loading from the product object passed to it,
     * or the product object should be fully populated before being passed.
     */
    public synchronized void addOrUpdateProductInRdfStore(Product product) {
        if (product == null || product.getId() == null) {
            logger.warn("Cannot add/update null product or product with null ID to RDF store.");
            return;
        }
        // Ensure product is fully loaded, especially lazy collections like reviews.
        // This might require fetching it again with joins or ensuring the calling context is transactional.
        // For simplicity here, we assume `product` passed in is sufficiently populated.
        // If ProductService calls this, it should pass the fully saved/updated Product entity.

        String productUri = rdfConfigProperties.getProductUriPrefix() + product.getId();
        Resource productResource = applicationRdfStore.getResource(productUri);

        // Remove existing statements for this product to handle updates correctly.
        // This includes statements where the product is a subject, and also
        // statements where related resources (like reviews specific to this product) are subjects.
        // A simple way for reviews is to also remove by their URIs if they are derived from product ID.
        if (applicationRdfStore.containsResource(productResource)) {
            logger.debug("Removing existing RDF for product URI: {}", productUri);
            // Remove all statements where productResource is the subject
            applicationRdfStore.removeAll(productResource, null, null);
            // Remove all statements where productResource is the object (less common for product updates but good practice)
            applicationRdfStore.removeAll(null, null, productResource);

            // More complex: also remove associated review resources if their URIs are known/derivable
            // For example, if reviews are http://.../review/{productId}/{reviewerId} or /review/{reviewId}
            // This requires careful handling to not remove unrelated data.
            // For now, convertProductToRdf recreates review nodes, so old review nodes linked
            // from an old version of the product will be orphaned if not explicitly removed.
            // A better approach for review removal: iterate statements `?s hasReview productResource`
            // and then remove all statements about `?s`.
            // For now, we rely on the fact that `convertProductToRdf` will re-add current reviews.
        }

        Model productRdf = convertProductToRdf(product); // This will generate RDF for the product and its current reviews
        applicationRdfStore.add(productRdf);
        logger.info("RDF data for product ID {} (URI: {}) added/updated in application RDF store. Store size: {}",
                product.getId(), productUri, applicationRdfStore.size());
    }

    /**
     * Removes a product's RDF representation from the main in-memory store.
     * This method needs to be called when a product is deleted from the relational DB.
     *
     * @param productId The ID of the product to remove.
     */
    public synchronized void removeProductFromRdfStore(Long productId) {
        if (productId == null) {
            logger.warn("Cannot remove product with null ID from RDF store.");
            return;
        }
        String productUri = rdfConfigProperties.getProductUriPrefix() + productId;
        Resource productResource = applicationRdfStore.getResource(productUri);

        if (applicationRdfStore.containsResource(productResource)) {
            // Before removing the product resource itself, remove associated resources like reviews that are linked to it
            // This avoids orphaned review data if reviews are only linked via hasReview from the product
            StmtIterator reviewStmts = applicationRdfStore.listStatements(productResource, hasReviewProp, (RDFNode) null);
            List<Resource> reviewsToRemove = new ArrayList<>();
            while(reviewStmts.hasNext()){
                Resource reviewNode = reviewStmts.nextStatement().getResource();
                reviewsToRemove.add(reviewNode);
            }
            reviewStmts.close();

            for(Resource reviewToRemove : reviewsToRemove){
                logger.debug("Removing associated review RDF: {}", reviewToRemove.getURI());
                applicationRdfStore.removeAll(reviewToRemove, null, null); // Remove all statements about the review
                applicationRdfStore.removeAll(null, null, reviewToRemove); // Remove statements where review is object
            }

            // Now remove all statements about the product resource itself
            applicationRdfStore.removeAll(productResource, null, null);
            applicationRdfStore.removeAll(null, null, productResource); // Also where it's an object

            logger.info("RDF data for product ID {} (URI: {}) removed from application RDF store. Store size: {}",
                    productId, productUri, applicationRdfStore.size());
        } else {
            logger.warn("Product URI {} not found in RDF store for removal.", productUri);
        }
    }
}