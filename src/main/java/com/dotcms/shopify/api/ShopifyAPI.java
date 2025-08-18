package com.dotcms.shopify.api;

import com.dotcms.shopify.util.ShopifyCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import java.util.List;
import java.util.Map;

public interface ShopifyAPI {


  static ShopifyAPI api(Host host) {
    return new ShopifyAPIImpl(host);
  }

  public Map<String, Object> productById(String id);

  public Map<String, Object> productByHandle(String handle);


  public List<Map<String, Object>> searchProducts(String query, int limit);

  public List<Map<String, Object>> searchProducts(ShopifySearcher searcher);

  public List<Map<String, Object>> searchProducts(String query, int limit,String sortBy);

  List<Map<String, Object>> searchProducts(String query, int limit, SortKey sortKey);

  public List<Map<String, Object>> searchProducts(String query, int limit, String cursor,
          BEFORE_AFTER beforeAfterCursor,SortKey sortBy,boolean reverse);

  public Map<String, Object> collectionById(String id);

  public List<Map<String, Object>> searchCollections(String query, int limit);

  Map<String, Object> getProductByHandle(String handle);

  public Map<String, Object> rawQuery(String query);

  public Map<String, Object> rawQuery(String query, Map<String, Object> variables);

  default void reload() {
    CacheLocator.getCacheAdministrator().flushGroup(ShopifyCache.getInstance().getPrimaryGroup());
  }

  List<Map<String, Object>> searchCollections(String query, int limit, String cursor,
      BEFORE_AFTER beforeAfterCursor);

  Map<String, Object> testConnection();

  public enum BEFORE_AFTER {
    BEFORE, AFTER

  }


  public enum SORT_ORDER {
    ASC, DESC
  }

  public enum SORT_KEY {}
}
