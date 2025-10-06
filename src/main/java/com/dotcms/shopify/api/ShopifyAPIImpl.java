package com.dotcms.shopify.api;

import com.dotcms.shopify.util.AppKey;
import com.dotcms.shopify.util.ShopifyApp;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import java.util.Map;

public class ShopifyAPIImpl implements ShopifyAPI {

    private final Host host;
    private final ShopifyService shopifyService;

    /**
     * Method to load the app secrets into the variables.
     *
     * @param host if is not sent will throw IllegalArgumentException, if sent will
     *             try to find the secrets for it, if
     *             there is no secrets for the host will use the ones for the
     *             System_Host
     */
    public ShopifyAPIImpl(final Host host) {
        this.host = host;
        this.shopifyService = new ShopifyService(host);
    }

    @Override
    public Map<String, Object> productByHandle(String handle) {
       return this.productByHandle(handle, 1);

    }
    @Override
    public Map<String, Object> productByHandle(String handle, int variantLimit) {
        Logger.info(this.getClass(), "Getting product by ID: " + handle + ", variants:" + variantLimit +" for host: " + host.getHostname());
        if (UtilMethods.isEmpty(handle)) {
            return Map.of("errors", "no handle provided ");
        }
        return shopifyService.getProductByHandle(handle, variantLimit);
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

        try {
            JSONObject jsonObject = new JSONObject(query);
            if(jsonObject.has("query") && jsonObject.has("variables")){
                return this.rawQuery(jsonObject.getString("query"), jsonObject.getJSONObject("variables"));

            }
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "no variables in query, moving on:" + e.getMessage(), e);
        }

        return this.rawQuery(query, Map.of());
    }

    @Override
    public Map<String, Object> rawQuery(String query, String json) {
        JSONObject jsonObject = new JSONObject(json);
        return this.rawQuery(query, jsonObject);
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
    public String linkToShopifyCollection(String collectionId) {
        String id = collectionId;
        id = id.replace(SHOPIFY_COLLECTION_PREFIX, "");
        return getShopifyAdminUrl() + "/collections/" + id;
    }

    @Override
    public Map<String, Object> collectionById(String id) {
        Logger.debug(this.getClass(), "Getting collection by ID: " + id + " for host: " + host.getHostname());

        String collectionId = id;
        if (!id.startsWith(SHOPIFY_COLLECTION_PREFIX)) {
            collectionId = SHOPIFY_COLLECTION_PREFIX + id;
        }

        return shopifyService.getCollectionById(collectionId);
    }

    @Override
    public Map<String, Object> searchCollections(String query, int limit) {
        Logger.debug(this.getClass(), "Searching collections with query: " + query + " for host: " + host.getHostname());

        ProductSearcher searcher = ProductSearcher.builder().limit(limit).query(query).sortKey(SortKey.RELEVANCE)
                .build();

        return this.searchCollections(searcher);
    }

    @Override
    public Map<String, Object> searchCollections(ProductSearcher searcher) {
        Logger.debug(this.getClass(),
                "Searching collections with query: " + searcher + " for host: " + host.getHostname());

        return shopifyService.searchCollections(searcher);
    }

    @Override
    public Map<String, Object> testConnection() {
        return shopifyService.testConnection();
    }

}
