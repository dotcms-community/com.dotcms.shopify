package com.dotcms.shopify.api;

import com.dotcms.shopify.util.AppKey;
import com.dotcms.shopify.util.ShopifyApp;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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
  public Map<String, Object> productByHandle(String handle) {
    Logger.info(this.getClass(), "Getting product by ID: " + handle + " for host: " + host.getHostname());
    if (UtilMethods.isEmpty(handle)) {
      return Map.of("errors", "no handle provided ");
    }

    return shopifyService.getProductByHandle(handle);

  }

  @Override
  public Map<String, Object> productById(String id) {
    Logger.info(this.getClass(), "Getting product by ID: " + id + " for host: " + host.getHostname());
    if (UtilMethods.isEmpty(id)) {
      return Map.of("errors", "no id provided ");
    }

    if (!id.startsWith(SHOPIFY_PRODUCT_PREFIX)) {
      id = SHOPIFY_PRODUCT_PREFIX + id;
    }

    return shopifyService.getProductById(id);

  }

  @Override
  public Map<String, Object> searchProducts(String query, int limit) {
    return this.searchProducts(query, limit, SortKey.RELEVANCE);

  }

  @Override
  public Map<String, Object> searchProducts(String query, int limit, SortKey sortKey) {

    return shopifyService
        .searchProducts(ProductSearcher.builder().query(query).limit(limit).sortKey(sortKey).build());

  }

  @Override
  public Map<String, Object> searchProducts(ProductSearcher searcher) {
    Logger.info(this.getClass(), "Searching products with query: " + searcher + " for host: " + host.getHostname());

    return shopifyService.searchProducts(searcher);

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
  public String getShopifyAdminUrl() {

    String shopifyHost = ShopifyApp.instance(host).get(AppKey.STORE_NAME.appValue);

    if (UtilMethods.isEmpty(shopifyHost)) {
      throw new DotRuntimeException("Unable to find valid shopify config ");
    }
    return "https://admin.shopify.com/store/" + shopifyHost;
  }

  @Override
  public String linkToShopifyProduct(String productId) {
    String id = productId;
    id = id.replace(SHOPIFY_PRODUCT_PREFIX, "");
    return getShopifyAdminUrl() + "/products/" + id;
  }

  @Override
  public Map<String, Object> collectionById(String id) {
    Logger.info(this.getClass(), "Getting collection by ID: " + id + " for host: " + host.getHostname());

    String collectionId = id;
    if (!id.startsWith(SHOPIFY_COLLECTION_PREFIX)) {
      collectionId = SHOPIFY_COLLECTION_PREFIX + id;
    }

    return shopifyService.getCollectionById(collectionId);
  }

  @Override
  public Map<String, Object> searchCollections(String query, int limit) {
    Logger.info(this.getClass(), "Searching collections with query: " + query + " for host: " + host.getHostname());

    return this.searchCollections(query, limit, SortKey.RELEVANCE);
  }

  @Override
  public Map<String, Object> searchCollections(String query, int limit, SortKey sortKey) {
    Logger.info(this.getClass(), "Searching collections with query: " + query + " for host: " + host.getHostname());

    return shopifyService.searchCollections(query, limit, sortKey);
  }

  @Override
  public Map<String, Object> testConnection() {
    return shopifyService.testConnection();
  }

}
