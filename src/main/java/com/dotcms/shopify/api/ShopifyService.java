package com.dotcms.shopify.api;

import com.dotcms.shopify.api.ShopifyAPI.BEFORE_AFTER;
import com.dotcms.shopify.osgi.ActivatorUtil;
import com.dotcms.shopify.util.AppKey;
import com.dotcms.shopify.util.ShopifyApp;
import com.dotcms.shopify.util.ShopifyApp;
import com.dotcms.shopify.util.ShopifyCache;
import com.dotcms.shopify.util.ShopifyCache.CacheType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for executing GraphQL queries against Shopify's Storefront API using
 * Java's native HTTP client.
 */
public class ShopifyService {

    private static final String STOREFRONT_API_PATH = "/api/%s/graphql.json";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient httpClient;
    private final Host host;

    private final ShopifyCache cache = ShopifyCache.getInstance();

    public ShopifyService(Host host) {
        this.host = host;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
    }

    private Map<String, String> getFragmentMap() {
        Map<String, String> cacheMap = (Map<String, String>) cache.get(CacheType.GRAPHQL, "FRAGMENT_MAP");
        if (cacheMap != null) {
            return cacheMap;
        }

        synchronized (this.getClass()) {
            cacheMap = (Map<String, String>) cache.get(CacheType.GRAPHQL, "FRAGMENT_MAP");
            if (cacheMap == null) {
                Map<String, String> fragmentMap = new HashMap<>();
                List<String> fragmentNames = ActivatorUtil.listFilesInPackage("/application/shopify/gql/").stream().filter(e->!e.isDirectory()).map(e->e.getName()).collect(Collectors.toList());
                fragmentNames
                        .stream()
                        .filter(n -> n.contains("fragment.gql"))
                        .forEach(fragmentPath -> {
                            String fragmentName = fragmentPath.substring(fragmentPath.lastIndexOf("/")+1,fragmentPath.length());
                            String query = loadQueryFromFileasset(fragmentName);
                            fragmentMap.put(fragmentName, query);
                        });
                cache.put(CacheType.GRAPHQL, "FRAGMENT_MAP", Map.copyOf(fragmentMap));
            }
        }
        return (Map<String, String>) cache.get(CacheType.GRAPHQL, "FRAGMENT_MAP");
    }

    /**
     * Load a GraphQL query from the resources directory
     *
     * @param queryFileName The name of the query file
     * @return The query string
     */

    private String loadQueryFromFileasset(String queryFileName) {
        Host defaultSite = Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                .getOrNull();

        String path =  "/application/shopify/gql/" + queryFileName;
        FileAsset asset = APILocator.getFileAssetAPI()
                .getFileByPath(path, defaultSite,
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(), false);

        String query;

        try (InputStream inputStream = asset.getInputStream()) {
            query = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new DotRuntimeException("unable to load.gql query: " + queryFileName, e);
        }
        if (UtilMethods.isEmpty(query)) {
            throw new DotRuntimeException(".gql query is empty " + queryFileName);
        }

        return query;
    }

    public JSONObject executeGraphQLQuery(String query) {

        return executeGraphQLQuery(query, Map.of());
    }

    /**
     * Execute a GraphQL query against Shopify's Storefront API
     *
     * @param query     The GraphQL query string
     * @param variables Optional variables for the query
     * @return The response data as a Map
     */
    public JSONObject executeGraphQLQuery(String query, Map<String, Object> variables) {

        for (Map.Entry<String, String> entry : getFragmentMap().entrySet()) {
            query = query.replace("$" + entry.getKey(), entry.getValue());
        }
        System.out.println("query:" + query);

        try {
            // Get configuration
            Map<String, String> config = getShopifyConfig();
            if (config.isEmpty()) {
                Logger.error(this, "No Shopify configuration found for host: " + host.getHostname());
                return new JSONObject();
            }

            String storeName = config.get(AppKey.STORE_NAME.name());
            String apiKey = config.get(AppKey.API_KEY.name());
            String apiVersion = config.get(AppKey.API_VERSION.name());

            if (!UtilMethods.isSet(storeName) || !UtilMethods.isSet(apiKey) || !UtilMethods.isSet(apiVersion)) {
                Logger.error(this, "Missing required Shopify configuration values");
                return new JSONObject();
            }

            // Build the URL
            String url = String.format("https://%s.myshopify.com" + STOREFRONT_API_PATH,
                    storeName, apiVersion);

            // Build the request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("query", query);

            if (variables != null && !variables.isEmpty()) {
                requestBody.put("variables", variables);
            }

            final String requestBodyString = requestBody.toString(2);

            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Shopify-Storefront-Private-Token", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            // Execute the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, List<String>> headers = response.headers().map();
            if (response.statusCode() == 200) {
                String responseBody = response.body();

                JSONObject jsonResponse = new JSONObject(responseBody);

                if (jsonResponse.has("errors")) {
                    Logger.error(this, "GraphQL request failed");
                    Logger.error(this, "GraphQL request failed: Original Request: \n" + requestBodyString);

                    JSONArray errors = jsonResponse.optJSONArray("errors");

                    if (errors != null && errors.length() > 0) {
                        for (int i = 0; i < errors.length(); i++) {
                            Logger.error(this, "GraphQL request error:" + errors.getString(i));
                        }
                    }
                    return jsonResponse;
                }

                return parseGraphQLResponse(responseBody);
            } else {
                Logger.error(this, "GraphQL request failed with status: " + response.statusCode() +
                        ", body: " + response.body());
                Logger.error(this, " Failed GraphQL query:" + query);
                return new JSONObject(Map.of("errors", "GraphQL request failed with status: " + response.statusCode() +
                        ", body: " + response.body()));
            }

        } catch (Exception e) {
            Logger.error(this, "Error executing GraphQL query", e);
            return new JSONObject();
        }
    }

    public Map<String, Object> getProductByHandle(String productHandle) {
        String query = loadQueryFromFileasset("getProductByHandle.gql");
        Map<String, Object> variables = Map.of("handle", productHandle);
        return executeGraphQLQuery(query, variables);

    }

    public Map<String, Object> testConnection() {
        String query = loadQueryFromFileasset("testConnection.gql");
        String configStoreName = getShopifyConfig().get(AppKey.STORE_NAME.name());

        if (UtilMethods.isEmpty(configStoreName)) {
            return Map.of("connection", "Error: No Shopify configuration found for host: " + host.getHostname());
        }

        JSONObject response = executeGraphQLQuery(query);
        if (response.toString().contains(configStoreName)) {
            return new JSONObject(Map.of("connection", "Success", "response", response));
        }
        return new JSONObject(Map.of("connection", "Error", "response", response));

    }

    /**
     * Get product by ID using GraphQL
     *
     * @param productId The Shopify product ID (gid format)
     * @return Product data as a Map
     */
    public Map<String, Object> getProductById(String productId) {
        String query = loadQueryFromFileasset("getProductById.gql");
        if (query.isEmpty()) {
            Logger.error(this, "Failed to load getProductById query");
            return Collections.emptyMap();
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", productId);

        Map<String, Object> response = executeGraphQLQuery(query, variables);
        return response;
    }

    /**
     * Search products using GraphQL
     *
     * @param searcher
     * @return List of products
     */
    public Map<String, Object> searchProducts(ProductSearcher searcher) {

        if (searcher.hasCursor()) {
            String query = searcher.before == BEFORE_AFTER.BEFORE
                    ? loadQueryFromFileasset("searchProductsBefore.gql")
                    : loadQueryFromFileasset("searchProductsAfter.gql");

            if (query.isEmpty()) {
                Logger.error(this, "Failed to load searchProducts query");
                return Map.of("errors", "Failed to load searchProducts query");
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("query", searcher.query);
            variables.put("limit", searcher.limit);
            variables.put("cursor", searcher.cursor);

            return executeGraphQLQuery(query, variables);

        } else {
            String query = loadQueryFromFileasset("searchProducts.gql");
            if (query.isEmpty()) {
                Logger.error(this, "Failed to load searchProducts query");
                return Map.of("errors", "Failed to load searchProducts query");
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("query", searcher.query);
            variables.put("first", searcher.limit);
            variables.put("sortKey", searcher.sortKey.name());

            Map<String, Object> response = executeGraphQLQuery(query, variables);
            return response;

        }

    }

    /**
     * Get collection by ID using GraphQL
     *
     * @param collectionId The Shopify collection ID (gid format)
     * @return Collection data as a Map
     */
    public Map<String, Object> getCollectionById(String collectionId) {
        String query = loadQueryFromFileasset("getCollectionById.gql");
        if (query.isEmpty()) {
            Logger.error(this, "Failed to load getCollectionById query");
            return Map.of("errors", "Failed to load searchCollections query");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", collectionId);

        return executeGraphQLQuery(query, variables);
    }


   /**
    * Get collection by ID using GraphQL
    *
    * @param collectionId The Shopify collection ID (gid format)
    * @return Collection data as a Map
    */
   public Map<String, Object> getCollectionByIdWithOptions(ProductSearcher searcher) {
      String query = loadQueryFromFileasset("getCollectionById.gql");
      if (query.isEmpty()) {
         Logger.error(this, "Failed to load getCollectionById query");
         return Map.of("errors", "Failed to load searchCollections query");
      }

      Map<String, Object> variables = new HashMap<>();
      variables.put("id", searcher.id);

      return executeGraphQLQuery(query, variables);

   }




    /**
     *
     * @param searchQuery
     * @param limit
     * @param sortKey
     * @return
     */
    public Map<String, Object> searchCollections(ProductSearcher searcher) {
        String query = loadQueryFromFileasset("searchCollections.gql");
        if (query.isEmpty()) {
            Logger.error(this, "Failed to load searchCollections query");
            return Map.of("errors", "Failed to load searchCollections query");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("query", searcher.query);
        variables.put("first", searcher.limit);
        variables.put("sortKey", searcher.sortKey.name());
        Map<String, Object> response = executeGraphQLQuery(query, variables);
        return response;
    }

    /**
     * Get Shopify configuration for the host
     */
    private Map<String, String> getShopifyConfig() {
        Map<String, String> config = ShopifyApp.instance(host);
        if (config == null || !config.containsKey(AppKey.API_KEY.name())) {
            return Map.of();
        }
        return config;
    }

    /**
     * Parse GraphQL response JSON string into a Map
     */
    private JSONObject parseGraphQLResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json;
        } catch (JSONException e) {
            Logger.error(this, "Error parsing GraphQL response", e);
            return new JSONObject();
        }
    }

}
