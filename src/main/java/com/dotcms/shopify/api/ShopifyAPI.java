package com.dotcms.shopify.api;

import io.vavr.Lazy;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public interface ShopifyAPI {

    static ShopifyAPI api(Host host) {
        return new ShopifyAPIImpl(host);
    }
    public Map<String, Object> productById(String id);


    public List<Map<String, Object>> productSearch(String query, int limit, int page);

    public Map<String, Object> collectionById(String id);

    public List<Map<String, Object>> collectionSearch(String query, int limit, int page);

    public Map<String, Object> rawQuery(String query);

    public Map<String, Object> rawQuery(String query, Map<String, Object> variables);

    default void reload() {

    }



    boolean testConfig();
}
