package com.dotcms.shopify.api;

import com.dotcms.shopify.util.ShopifyCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import java.util.Map;

public interface ShopifyAPI {
    public static final String SHOPIFY_PRODUCT_PREFIX = "gid://shopify/Product/";
    public static final String SHOPIFY_COLLECTION_PREFIX = "gid://shopify/Collection/";

    static ShopifyAPI api(Host host) {
        return new ShopifyAPIImpl(host);
    }

    Map<String, Object> productByHandle(String handle, int variantLimit);

    public Map<String, Object> productById(String id);

    public Map<String, Object> productByHandle(String handle);

    public Map<String, Object> searchProducts(String query, int limit);

    public Map<String, Object> searchProducts(ProductSearcher searcher);

    Map<String, Object> searchProducts(String query, int limit, SortKey sortKey);

    public String linkToShopifyProduct(String productId);

    public String linkToShopifyCollection(String collectionId);

    public Map<String, Object> collectionById(String id);

    public Map<String, Object> rawQuery(String query);

    Map<String, Object> rawQuery(String query, String json);

    public Map<String, Object> rawQuery(String query, Map<String, Object> variables);

    public String getShopifyAdminUrl();

    default void reload() {
        CacheLocator.getCacheAdministrator().flushGroup(ShopifyCache.getInstance().getPrimaryGroup());
    }

    Map<String, Object> searchCollections(ProductSearcher searcher);

    public Map<String, Object> searchCollections(String query, int limit);

    Map<String, Object> testConnection();

    public enum BEFORE_AFTER {
        BEFORE, AFTER

    }

    public enum SORT_ORDER {
        ASC, DESC
    }

    public enum SORT_KEY {
    }
}
