package com.project.Fashion.controller;

import com.project.Fashion.service.SparqlQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sparql")
@Tag(name = "SPARQL Queries", description = "APIs for executing SPARQL queries against RDF product data.")
public class SparqlController {

    private final SparqlQueryService sparqlQueryService;

    @Autowired
    public SparqlController(SparqlQueryService sparqlQueryService) {
        this.sparqlQueryService = sparqlQueryService;
    }

    @Operation(summary = "Get all product URIs and names via SPARQL",
            description = "Executes a predefined SPARQL query to retrieve the URI and name of all fashion products.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved product names and URIs",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "array", implementation = Map.class, example = "[{\"product\": \"http://fashion.example.com/data/product/1\", \"name\": \"Classic T-Shirt\"}]"))),
            @ApiResponse(responseCode = "500", description = "Error executing SPARQL query")
    })
    @GetMapping("/products/names")
    public ResponseEntity<List<Map<String, String>>> getAllProductNames() {
        try {
            List<Map<String, String>> results = sparqlQueryService.getAllProductNamesAndUris();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // Basic error handling, GlobalExceptionHandler would also catch this
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to execute SPARQL query: " + e.getMessage())));
        }
    }

    @Operation(summary = "Find products by category name using SPARQL",
            description = "Executes a SPARQL query to find products belonging to a specific category name (case-insensitive).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products for the category",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "array", implementation = Map.class, example = "[{\"product\": \"http://fashion.example.com/data/product/2\", \"productName\": \"Blue Jeans\"}]"))),
            @ApiResponse(responseCode = "400", description = "Category name parameter is missing"),
            @ApiResponse(responseCode = "500", description = "Error executing SPARQL query")
    })
    @GetMapping("/products/by-category")
    public ResponseEntity<List<Map<String, String>>> getProductsByCategory(
            @Parameter(description = "Name of the category to search for (e.g., 'Shirts', 'Accessories')", required = true, example = "Shirts")
            @RequestParam String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(List.of(Map.of("error", "categoryName parameter is required.")));
        }
        try {
            List<Map<String, String>> results = sparqlQueryService.findProductsByCategoryName(categoryName);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to execute SPARQL query for category: " + e.getMessage())));
        }
    }

    @Operation(summary = "Execute a custom SPARQL SELECT query (Admin/Dev only - use with caution)",
            description = "Allows execution of an arbitrary SPARQL SELECT query. " +
                    "NOTE: Exposing arbitrary SPARQL query execution directly via an API can be a security risk if not properly secured and sanitized. " +
                    "This endpoint is provided for testing/development or restricted admin use.",
            security = @SecurityRequirement(name = "bearerAuth") // Example: Secure if needed
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Query executed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "array", implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "SPARQL query parameter is missing or query parsing error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (if secured)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (if secured and user lacks permissions)"),
            @ApiResponse(responseCode = "500", description = "Error executing SPARQL query")
    })
    @PostMapping("/query")
    // @PreAuthorize("hasRole('ADMIN')") // Example: Secure this endpoint if it's for admin/dev use
    public ResponseEntity<List<Map<String, String>>> executeCustomQuery(
            @Parameter(description = "The SPARQL SELECT query string.", required = true,
                    example = "PREFIX schema: <http://schema.org/> SELECT ?s ?p ?o WHERE { ?s schema:name ?o . FILTER regex(?o, \"Shirt\", \"i\") } LIMIT 10")
            @org.springframework.web.bind.annotation.RequestBody String sparqlQuery) { // Changed to RequestBody for potentially long queries
        if (sparqlQuery == null || sparqlQuery.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(List.of(Map.of("error", "sparqlQuery parameter is required.")));
        }
        try {
            List<Map<String, String>> results = sparqlQueryService.executeSparqlQuery(sparqlQuery);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to execute custom SPARQL query: " + e.getMessage())));
        }
    }
}
