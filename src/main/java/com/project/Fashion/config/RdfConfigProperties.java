package com.project.Fashion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RdfConfigProperties {

    // Base URI for your ontology (defined in fashion.ttl)
    @Value("${fashion.ontology.base-uri:http://fashion.example.com/ontology#}")
    private String ontologyBaseUri;

    // Base URI for your instance data (e.g., specific products, categories)
    @Value("${fashion.data.base-uri:http://fashion.example.com/data/}")
    private String dataBaseUri;

    public String getOntologyBaseUri() {
        return ontologyBaseUri;
    }

    // Getter for dataBaseUri that was missing
    public String getDataBaseUri() {
        return dataBaseUri;
    }

    public String getProductUriPrefix() {
        return dataBaseUri + "product/";
    }

    public String getCategoryUriPrefix() {
        return dataBaseUri + "category/";
    }

    public String getSellerUriPrefix() {
        return dataBaseUri + "seller/";
    }

    public String getReviewUriPrefix() {
        return dataBaseUri + "review/";
    }

    public String getUserUriPrefix() { // For reviewers
        return dataBaseUri + "user/";
    }
}
