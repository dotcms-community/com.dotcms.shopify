package com.dotcms.shopify.api;

import com.dotcms.shopify.util.ShopifyApp;
import com.dotcms.shopify.util.ShopifyApp.AppKey;
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
import java.util.Iterator;
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


  public ShopifyService(Host host) {
    this.host = host;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(DEFAULT_TIMEOUT)
        .build();
  }

  /**
   * Load a GraphQL query from the resources directory
   *
   * @param queryFileName The name of the query file
   * @return The query string
   */

  private String loadGraphQLQuery(String queryFileName) {
    Host defaultSite = Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
        .getOrNull();


    String path = ShopifyApp.GRAPHQL_QUERY_FILES_PATH + "/" + queryFileName;
    FileAsset asset = APILocator.getFileAssetAPI()
        .getFileByPath(path, defaultSite,
            APILocator.getLanguageAPI().getDefaultLanguage().getId(), false);

    String query = null;

    try (InputStream inputStream = asset.getInputStream()) {
      query = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
    } catch (Exception e) {
      throw new DotRuntimeException("unable to load graphql query: " + queryFileName, e);
    }
    if (UtilMethods.isEmpty(query)) {
      throw new DotRuntimeException("graphql query is empty: " + queryFileName);
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
    try {
      // Get configuration
      Map<Object, String> config = getShopifyConfig();
      if (config.isEmpty()) {
        Logger.error(this, "No Shopify configuration found for host: " + host.getHostname());
        return new JSONObject();
      }

      String storeName = config.get(AppKey.STORE_NAME);
      String apiKey = config.get(AppKey.API_KEY);
      String apiVersion = config.get(AppKey.API_VERSION);

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
        requestBody.put("variables", new JSONObject(variables));
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
        return new JSONObject(responseBody);
      } else {
        Logger.error(this, "GraphQL request failed with status: " + response.statusCode() +
            ", body: " + response.body());
        return new JSONObject(Map.of("errors","GraphQL request failed with status: " + response.statusCode() +
            ", body: " + response.body()));
      }

    } catch (Exception e) {
      Logger.error(this, "Error executing GraphQL query", e);
      return new JSONObject();
    }
  }


  public Map<String, Object> getProductById(String productId) {

    return getProductById(Long.parseLong(productId));

  }


  public Map<String, Object> testConnection() {
    String query = loadGraphQLQuery("testConnection.graphql");
    String configStoreName = getShopifyConfig().get(AppKey.STORE_NAME);

    if(UtilMethods.isEmpty(configStoreName)){
      return Map.of("connection", "Error: No Shopify configuration found for host: " + host.getHostname());
    }


    JSONObject response = executeGraphQLQuery(query);
    if(response.toString().contains(configStoreName)){
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
  public Map<String, Object> getProductById(long productId) {
    String query = loadGraphQLQuery("getProductById.graphql");
    if (query.isEmpty()) {
      Logger.error(this, "Failed to load getProductById query");
      return Collections.emptyMap();
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("id", productId);

    Map<String, Object> response = executeGraphQLQuery(query, variables);
    return extractDataField(response, "product");
  }

  /**
   * Search products using GraphQL
   *
   * @param searchQuery The search query
   * @param limit       Number of products to return
   * @param after       Cursor for pagination
   * @return List of products
   */
  public List<Map<String, Object>> searchProducts(String searchQuery, int limit, String after) {
    String query = loadGraphQLQuery("searchProducts.graphql");
    if (query.isEmpty()) {
      Logger.error(this, "Failed to load searchProducts query");
      return Collections.emptyList();
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("query", searchQuery);
    variables.put("first", limit);
    if (UtilMethods.isSet(after)) {
      variables.put("after", after);
    }

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
    String query = loadGraphQLQuery("getCollectionById.graphql");
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
  public List<Map<String, Object>> searchCollections(String searchQuery, int limit, String after) {
    String query = loadGraphQLQuery("searchCollections.graphql");
    if (query.isEmpty()) {
      Logger.error(this, "Failed to load searchCollections query");
      return Collections.emptyList();
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("query", searchQuery);
    variables.put("first", limit);
    if (UtilMethods.isSet(after)) {
      variables.put("after", after);
    }

    Map<String, Object> response = executeGraphQLQuery(query, variables);
    return extractCollectionList(response);
  }

  /**
   * Get Shopify configuration for the host
   */
  private Map<Object, String> getShopifyConfig() {
    Map<Object, String> config = ShopifyApp.instance(host);
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
  private Map<String, Object> extractDataField(Map<String, Object> response, String field) {
    try {
      Map<String, Object> data = (Map<String, Object>) response.get("data");
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
  private List<Map<String, Object>> extractProductList(Map<String, Object> response) {
    try {
      Map<String, Object> data = (Map<String, Object>) response.get("data");
      if (data != null && data.containsKey("products")) {
        Map<String, Object> products = (Map<String, Object>) data.get("products");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) products.get("edges");
        if (edges != null) {
          return edges.stream()
              .map(edge -> (Map<String, Object>) edge.get("node"))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
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
  private List<Map<String, Object>> extractCollectionList(Map<String, Object> response) {
    try {
      Map<String, Object> data = (Map<String, Object>) response.get("data");
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

  /**
   * Convert JSONObject to Map recursively
   */
  private Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
    Map<String, Object> map = new HashMap<>();
    Iterator<String> keys = json.keys();

    while (keys.hasNext()) {
      String key = keys.next();
      Object value = json.get(key);

      if (value instanceof JSONObject) {
        map.put(key, jsonToMap((JSONObject) value));
      } else if (value instanceof JSONArray) {
        map.put(key, jsonArrayToList((JSONArray) value));
      } else {
        map.put(key, value);
      }
    }

    return map;
  }

  /**
   * Convert JSONArray to List recursively
   */
  private List<Object> jsonArrayToList(JSONArray array) throws JSONException {
    List<Object> list = new ArrayList<>();

    for (int i = 0; i < array.length(); i++) {
      Object value = array.get(i);

      if (value instanceof JSONObject) {
        list.add(jsonToMap((JSONObject) value));
      } else if (value instanceof JSONArray) {
        list.add(jsonArrayToList((JSONArray) value));
      } else {
        list.add(value);
      }
    }

    return list;
  }
}
