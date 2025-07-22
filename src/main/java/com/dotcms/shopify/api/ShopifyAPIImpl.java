package com.dotcms.shopify.api;

import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;

public class ShopifyAPIImpl implements ShopifyAPI {

  private final Host host;
  private final ShopifyService shopifyService;

  /**
   * Method to load the app secrets into the variables.
   *
   * @param host if is not sent will throw IllegalArgumentException, if sent will try to find the secrets for it, if
   *             there is no secrets for the host will use the ones for the System_Host
   */
  public ShopifyAPIImpl(final Host host) {
    this.host = host;
    this.shopifyService = new ShopifyService(host);
  }

  @Override
  public Map<String, Object> productById(String id) {
    Logger.info(this.getClass(), "Getting product by ID: " + id + " for host: " + host.getHostname());
    if (UtilMethods.isEmpty(id)) {
      return Map.of("errors", "no id provided ");
    }
    //long longId = Try.of(() -> Long.parseLong(id.replace("gid://shopify/Product/", ""))).getOrElse(0L);
    return shopifyService.getProductById(id);

  }

  @Override
  public Map<String, Object> getProductByHandle(String handle) {
    Logger.info(this.getClass(), "Getting product by handle: " + handle + " for host: " + host.getHostname());
    if(UtilMethods.isEmpty(handle)){
      return Map.of("errors", "no handle provided ");
    }

    return shopifyService.getProductByHandle(handle);

  }

  @Override
  public Map<String, Object> rawQuery(String query) {
    return this.rawQuery(query, Map.of());
  }

  @Override
  public Map<String, Object> rawQuery(String query, Map<String, Object> variables) {

    return shopifyService.executeGraphQLQuery(query, variables);
  }

  @Override
  public Map<String, Object> collectionById(String id) {
    Logger.info(this.getClass(), "Getting collection by ID: " + id + " for host: " + host.getHostname());

    // Ensure the ID is in the correct format for GraphQL
    // (gid://shopify/Collection/ID)
    String collectionId = id;
    if (!id.startsWith("gid://")) {
      collectionId = "gid://shopify/Collection/" + id;
    }

    return shopifyService.getCollectionById(collectionId);
  }

  @Override
  public List<Map<String, Object>> productSearch(String query, int limit, int page) {
    Logger.info(this.getClass(), "Searching products with query: " + query + " for host: " + host.getHostname());

    // For pagination, we'll use null for the first page
    // In a real implementation, you'd need to manage cursors for proper pagination
    String after = null;
    if (page > 1) {
      // This is simplified - in production you'd need to track cursors
      Logger.warn(this.getClass(),
          "Pagination beyond first page not fully implemented. Returning first page results.");
    }

    return shopifyService.searchProducts(query, limit, after);
  }

  @Override
  public List<Map<String, Object>> collectionSearch(String query, int limit, int page) {
    Logger.info(this.getClass(), "Searching collections with query: " + query + " for host: " + host.getHostname());

    // For pagination, we'll use null for the first page
    // In a real implementation, you'd need to manage cursors for proper pagination
    String after = null;
    if (page > 1) {
      // This is simplified - in production you'd need to track cursors
      Logger.warn(this.getClass(),
          "Pagination beyond first page not fully implemented. Returning first page results.");
    }

    return shopifyService.searchCollections(query, limit, after);
  }

  @Override
  public Map<String, Object> testConnection() {
    return shopifyService.testConnection();
  }


}
