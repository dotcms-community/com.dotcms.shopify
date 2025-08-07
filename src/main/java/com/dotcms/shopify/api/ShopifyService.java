package com.dotcms.shopify.api;

import com.dotcms.shopify.api.ShopifyAPI.BEFORE_AFTER;
import com.dotcms.shopify.osgi.ActivatorUtil;
import com.dotcms.shopify.util.ShopifyApp;
import com.dotcms.shopify.util.ShopifyApp.AppKey;
import com.dotcms.shopify.util.ShopifyCache;
import com.dotcms.shopify.util.ShopifyCache.CacheType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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
 * Service for executing GraphQL queries against Shopify's Storefront API using Java's native HTTP client.
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
        List<String> fragmentNames = ActivatorUtil.listFilesInPackage("graphql");
        fragmentNames
            .stream()
            .filter(n -> n.contains("fragment.gql"))
            .forEach(fragmentPath -> {
              String fragmentName = fragmentPath.replaceAll("graphql/", "");
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

    String path = ShopifyApp.GRAPHQL_QUERY_FILES_PATH + "/" + queryFileName;
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
        JSONObject jsonResponse =  new JSONObject(responseBody);
        return jsonResponse.has("data") && ! jsonResponse.has("errors") ? jsonResponse.getJSONObject("data") : parseGraphQLResponse(responseBody);
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

    return executeGraphQLQuery(query, variables);
    //return extractDataField(response, "product");
  }

  /**
   * Search products using GraphQL
   *
   * @param searchQuery The search query
   * @param limit       Number of products to return
   * @return List of products
   */
  public List<Map<String, Object>> searchProducts(String searchQuery, int limit) {
    String query = loadQueryFromFileasset("searchProducts.gql");
    if (query.isEmpty()) {
      Logger.error(this, "Failed to load searchProducts query");
      return Collections.emptyList();
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("query", searchQuery);
    variables.put("first", limit);

    Map<String, Object> response = executeGraphQLQuery(query, variables);
    return extractProductList(response);
  }


  public List<Map<String, Object>> searchProducts(String searchQuery, int limit, String cursor, BEFORE_AFTER beforeAfter) {

    String query = beforeAfter== BEFORE_AFTER.BEFORE ? loadQueryFromFileasset("searchProductsBefore.gql") : loadQueryFromFileasset("searchProductsAfter.gql");
    if (query.isEmpty()) {
      Logger.error(this, "Failed to load searchProducts query");
      return Collections.emptyList();
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("query", searchQuery);
    variables.put("limit", limit);
    variables.put("cursor", cursor);

    Map<String, Object> response = executeGraphQLQuery(query, variables);
    return extractProductList(response);


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
      return Collections.emptyMap();
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("id", collectionId);

    Map<String, Object> response = executeGraphQLQuery(query, variables);
    return extractDataField(response, "collection");
  }

  /**
   * Search collections using GraphQL
   *
   * @param searchQuery The search query
   * @param limit       Number of collections to return
   * @param after       Cursor for pagination
   * @return List of collections
   */
  public List<Map<String, Object>> searchCollections(String searchQuery, int limit) {
    String query = loadQueryFromFileasset("searchCollections.gql");
    if (query.isEmpty()) {
      Logger.error(this, "Failed to load searchCollections query");
      return Collections.emptyList();
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("query", searchQuery);
    variables.put("first", limit);

    Map<String, Object> response = executeGraphQLQuery(query, variables);
    return extractCollectionList(response);
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

  /**
   * Extract a specific data field from the GraphQL response
   */
  private Map<String, Object> extractDataField(Map<String, Object> data, String field) {
    try {
      if (data != null && data.containsKey(field)) {
        return (Map<String, Object>) data.get(field);
      }
    } catch (Exception e) {
      Logger.error(this, "Error extracting field: " + field, e);
    }
    return Collections.emptyMap();
  }

  /**
   * Extract product list from GraphQL response
   */
  private List<Map<String, Object>> extractProductList(Map<String, Object> data) {
    try {
      if (data.containsKey("products")) {
        Map<String, Object> products = (Map<String, Object>) data.get("products");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) products.get("edges");
        if (edges != null) {
          List<Map<String, Object>> productsToReturn = new ArrayList<>();
          for (Map<String, Object> edge : edges) {
            Map<String, Object> product = (Map<String, Object>) edge.get("node");
            product.put("cursor", edge.get("cursor"));
            productsToReturn.add(product);
          }
          return productsToReturn;

        }
      }
    } catch (Exception e) {
      Logger.error(this, "Error extracting product list", e);
    }
    return Collections.emptyList();
  }

  /**
   * Extract collection list from GraphQL response
   */
  private List<Map<String, Object>> extractCollectionList(Map<String, Object> data) {
    try {
      if (data != null && data.containsKey("collections")) {
        Map<String, Object> collections = (Map<String, Object>) data.get("collections");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) collections.get("edges");
        if (edges != null) {
          return edges.stream()
              .map(edge -> (Map<String, Object>) edge.get("node"))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
        }
      }
    } catch (Exception e) {
      Logger.error(this, "Error extracting collection list", e);
    }
    return Collections.emptyList();
  }


}
