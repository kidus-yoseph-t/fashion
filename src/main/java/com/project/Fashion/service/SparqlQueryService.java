package com.project.Fashion.service;

import com.project.Fashion.config.RdfConfigProperties;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable; // For Sort info
import org.springframework.data.domain.Sort;    // For Sort info
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SparqlQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SparqlQueryService.class);

    private final RdfConversionService rdfConversionService;
    private final RdfConfigProperties rdfConfigProperties;

    @Autowired
    public SparqlQueryService(RdfConversionService rdfConversionService, RdfConfigProperties rdfConfigProperties) {
        this.rdfConversionService = rdfConversionService;
        this.rdfConfigProperties = rdfConfigProperties;
    }

    public List<Map<String, String>> executeSparqlQuery(String sparqlQueryString) {
        Model model = rdfConversionService.getApplicationRdfStore();
        if (model == null || model.isEmpty()) {
            logger.warn("RDF model is empty or null. Cannot execute SPARQL query.");
            return new ArrayList<>();
        }

        List<Map<String, String>> resultsList = new ArrayList<>();
        logger.debug("Executing SPARQL Query: {}", sparqlQueryString);

        try (QueryExecution qe = QueryExecutionFactory.create(sparqlQueryString, model)) {
            ResultSet results = qe.execSelect();
            List<String> resultVars = results.getResultVars();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Map<String, String> resultMap = new HashMap<>();
                for (String var : resultVars) {
                    RDFNode node = soln.get(var);
                    if (node != null) {
                        if (node.isLiteral()) {
                            resultMap.put(var, node.asLiteral().getLexicalForm());
                        } else if (node.isResource()) {
                            resultMap.put(var, node.asResource().getURI());
                        } else {
                            resultMap.put(var, node.toString());
                        }
                    } else {
                        resultMap.put(var, null);
                    }
                }
                resultsList.add(resultMap);
            }
        } catch (QueryParseException qpe) {
            logger.error("SPARQL Query Parse Exception: {} for query: [{}]", qpe.getMessage(), sparqlQueryString, qpe);
        } catch (Exception e) {
            logger.error("Error executing SPARQL query [{}]: {}", sparqlQueryString, e.getMessage(), e);
        }
        logger.info("Executed SPARQL query. Results count: {}", resultsList.size());
        return resultsList;
    }

    private String buildSemanticSearchWhereClause(String categoryName, String descriptionKeyword) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("  ?product a fash:FashionProduct . \n");
        // OPTIONAL clauses to ensure products are returned even if some properties are missing
        whereClause.append("  OPTIONAL { ?product schema:name ?productName . } \n");
        whereClause.append("  OPTIONAL { ?product schema:description ?description . } \n");

        if (StringUtils.hasText(categoryName)) {
            whereClause.append("  ?product fash:belongsToCategory ?categoryResource . \n");
            whereClause.append("  ?categoryResource schema:name ?catName . \n");
            whereClause.append(String.format("  FILTER (lcase(str(?catName)) = lcase(\"%s\")) \n",
                    categoryName.toLowerCase().replace("\"", "\\\"")));
        }

        if (StringUtils.hasText(descriptionKeyword)) {
            // Ensure description exists for filtering, but make overall description optional for select
            whereClause.append("  ?product schema:description ?descForFilter . \n"); // This description is for filtering
            whereClause.append(String.format("  FILTER regex(str(?descForFilter), \"%s\", \"i\") \n",
                    descriptionKeyword.replace("\"", "\\\"")));
        }
        return whereClause.toString();
    }


    /**
     * Searches for products semantically based on category name and/or a keyword in the description, with pagination and sorting.
     *
     * @param categoryName Optional. The name of the category to filter by (case-insensitive).
     * @param descriptionKeyword Optional. A keyword to search for in the product's description (case-insensitive).
     * @param pageable Pageable object containing limit, offset, and sort information.
     * @return A list of maps, where each map contains "product" (URI), "productName", and "description" for the current page.
     */
    public List<Map<String, String>> searchProductsSemantic(String categoryName, String descriptionKeyword, Pageable pageable) {
        if (!StringUtils.hasText(categoryName) && !StringUtils.hasText(descriptionKeyword)) {
            logger.warn("Semantic product search called with no category name or description keyword. Returning empty list.");
            return new ArrayList<>();
        }

        String whereClause = buildSemanticSearchWhereClause(categoryName, descriptionKeyword);

        StringBuilder orderByClause = new StringBuilder();
        if (pageable.getSort().isSorted()) {
            orderByClause.append("ORDER BY ");
            List<String> orders = new ArrayList<>();
            for (Sort.Order order : pageable.getSort()) {
                String property = order.getProperty();
                // Map DTO property names to SPARQL variable names if necessary
                // Example: if sorting by "name" (DTO), it maps to "?productName" in SPARQL
                String sparqlVar;
                if ("name".equalsIgnoreCase(property)) {
                    sparqlVar = "?productName"; // Assuming ?productName is selected
                } else if ("description".equalsIgnoreCase(property)) {
                    sparqlVar = "?description"; // Assuming ?description is selected
                } else {
                    // Add mappings for other sortable RDF properties if needed
                    // For now, skip sorting if property not directly mappable to selected SPARQL vars
                    logger.warn("Unsupported sort property for SPARQL semantic search: {}", property);
                    continue;
                }
                orders.add(order.isAscending() ? sparqlVar : "DESC(" + sparqlVar + ")");
            }
            if(!orders.isEmpty()){
                orderByClause.append(String.join(" ", orders));
            } else {
                orderByClause.setLength(0); // Clear if no valid sort orders
            }
        }


        String query = String.format(
                "PREFIX fash: <%s> " +
                        "PREFIX schema: <%s> " +
                        "SELECT DISTINCT ?product ?productName ?description " +
                        "WHERE { \n" +
                        whereClause +
                        "} \n" +
                        (orderByClause.length() > 0 ? orderByClause.toString() + "\n" : "") +
                        "LIMIT %d \n" +
                        "OFFSET %d",
                rdfConfigProperties.getOntologyBaseUri(),
                RdfConversionService.SCHEMA_NS,
                pageable.getPageSize(),
                pageable.getOffset()
        );

        logger.info("Executing paginated semantic product search with categoryName='{}', descriptionKeyword='{}', limit={}, offset={}",
                categoryName, descriptionKeyword, pageable.getPageSize(), pageable.getOffset());
        return executeSparqlQuery(query);
    }

    /**
     * Counts the total number of products matching the semantic search criteria.
     *
     * @param categoryName Optional. The name of the category.
     * @param descriptionKeyword Optional. A keyword for the description.
     * @return The total number of matching products.
     */
    public long countTotalSemanticSearchResults(String categoryName, String descriptionKeyword) {
        if (!StringUtils.hasText(categoryName) && !StringUtils.hasText(descriptionKeyword)) {
            // Consistent with searchProductsSemantic, if no criteria, count is 0 (or could be total products)
            logger.warn("Semantic count called with no criteria.");
            return 0;
        }

        String whereClause = buildSemanticSearchWhereClause(categoryName, descriptionKeyword);

        String countQueryString = String.format(
                "PREFIX fash: <%s> " +
                        "PREFIX schema: <%s> " +
                        "SELECT (COUNT(DISTINCT ?product) AS ?count) " +
                        "WHERE { \n" +
                        whereClause +
                        "}",
                rdfConfigProperties.getOntologyBaseUri(),
                RdfConversionService.SCHEMA_NS
        );

        logger.info("Executing SPARQL count query for semantic search with categoryName='{}', descriptionKeyword='{}'",
                categoryName, descriptionKeyword);

        Model model = rdfConversionService.getApplicationRdfStore();
        if (model == null || model.isEmpty()) {
            logger.warn("RDF model is empty or null. Cannot execute SPARQL count query.");
            return 0;
        }

        long totalCount = 0;
        try (QueryExecution qe = QueryExecutionFactory.create(countQueryString, model)) {
            ResultSet results = qe.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode countNode = soln.get("count");
                if (countNode != null && countNode.isLiteral()) {
                    totalCount = countNode.asLiteral().getLong();
                }
            }
        } catch (Exception e) {
            logger.error("Error executing SPARQL count query: {}", e.getMessage(), e);
        }
        logger.info("Total semantic search results count: {}", totalCount);
        return totalCount;
    }


    // --- Existing methods ---
    public List<Map<String, String>> getAllProductNamesAndUris() {
        String query = String.format(
                "PREFIX fash: <%s> " +
                        "PREFIX schema: <%s> " +
                        "SELECT ?product ?name " +
                        "WHERE { " +
                        "  ?product a fash:FashionProduct . " +
                        "  OPTIONAL { ?product schema:name ?name . } " +
                        "}",
                rdfConfigProperties.getOntologyBaseUri(),
                RdfConversionService.SCHEMA_NS
        );
        return executeSparqlQuery(query);
    }

    public List<Map<String, String>> findProductsByCategoryName(String categoryName) {
        String query = String.format(
                "PREFIX fash: <%s> " +
                        "PREFIX schema: <%s> " +
                        "SELECT ?product ?productName " +
                        "WHERE { " +
                        "  ?product a fash:FashionProduct ; " +
                        "           fash:belongsToCategory ?categoryResource ; " +
                        "           schema:name ?productName . " +
                        "  ?categoryResource schema:name ?catName . " +
                        "  FILTER (lcase(str(?catName)) = lcase(\"%s\")) " +
                        "}",
                rdfConfigProperties.getOntologyBaseUri(),
                RdfConversionService.SCHEMA_NS,
                categoryName.toLowerCase().replace("\"", "\\\"")
        );
        logger.info("Executing SPARQL query for category: {}", categoryName);
        return executeSparqlQuery(query);
    }
}
