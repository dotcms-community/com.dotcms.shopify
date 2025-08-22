package com.dotcms.shopify.api;

import com.dotcms.shopify.api.ShopifyAPI.BEFORE_AFTER;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;

/**
 * JAX-RS parameter class for product search operations.
 * Converts to immutable ProductSearcher using builder pattern.
 */
public class ProductSearchParams {

    @QueryParam("before")
    @DefaultValue("AFTER")
    public BEFORE_AFTER before;

    @QueryParam("limit")
    @DefaultValue("20")
    public int limit;

    @QueryParam("query")
    @DefaultValue("")
    public String query;

    @QueryParam("reverse")
    @DefaultValue("false")
    public boolean reverse;

    @QueryParam("sortKey")
    @DefaultValue("RELEVANCE")
    public SortKey sortKey;

    @QueryParam("cursor")
    public String cursor;

    @QueryParam("id")
    public String id;

    @QueryParam("hostName")
    public String hostName;

    /**
     * Default constructor required by JAX-RS
     */
    public ProductSearchParams() {
        // JAX-RS will set values via field injection
    }

    /**
     * Converts these parameters to an immutable ProductSearcher using the builder
     * pattern
     */
    public ProductSearcher toProductSearcher() {
        return ProductSearcher.builder()
                .before(before != null ? before : BEFORE_AFTER.AFTER)
                .limit(limit)
                .query(query != null ? query : "")
                .reverse(reverse)
                .sortKey(sortKey != null ? sortKey : SortKey.RELEVANCE)
                .cursor(cursor)
                .id(id)
                .hostName(hostName)
                .build();
    }
}
