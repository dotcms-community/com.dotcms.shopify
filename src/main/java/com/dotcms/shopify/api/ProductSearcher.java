package com.dotcms.shopify.api;

import com.dotcms.shopify.api.ShopifyAPI.BEFORE_AFTER;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;

@JsonDeserialize(builder = ProductSearcher.Builder.class)
public class ProductSearcher {

    public final BEFORE_AFTER before;
    public final int limit;
    public final String query;
    public final boolean reverse;
    public final SortKey sortKey;
    public final String cursor;
    public final String id;
    public final String hostName;
    public final String handle;
    public final int variantLimit;

    public ProductSearcher(BEFORE_AFTER before, int limit, String query, boolean reverse, SortKey sortKey,
            String cursor,
            String id, String hostName, String handle,int variantLimit) {
        this.before = before;
        this.limit = limit;
        this.query = query;
        this.reverse = reverse;
        this.sortKey = sortKey;
        this.cursor = cursor;
        this.id = id;
        this.hostName = hostName;
        this.handle = handle;
        this.variantLimit = variantLimit;
    }

    public static Builder builder() {
        return new Builder();
    }

    boolean hasCursor() {
        return UtilMethods.isSet(cursor);
    }

    @Override
    public String toString() {
        return "ShopifySearcher:{" +
                "id:'" + id + '\'' +
                "before:" + before.name() +
                ", limit:" + limit +
                ", query:'" + query + '\'' +
                ", reverse:" + reverse +
                ", sortKey:" + sortKey.name() +
                ", cursor:'" + cursor + '\'' +
                ", hostName:'" + hostName + '\'' +
                ", handle:'" + handle + '\'' +
                ", variantLimit:" + variantLimit +
                '}';
    }

    public static class Builder {

        private BEFORE_AFTER before = BEFORE_AFTER.AFTER;
        private int limit = 20;
        private String query = "";
        private boolean reverse = false;
        private SortKey sortKey = SortKey.RELEVANCE;
        private String cursor;
        private String id;
        private String hostName;
        private String handle;
        private int variantLimit = 10;

        @JsonProperty
        public Builder before(BEFORE_AFTER before) {
            this.before = before;
            return this;
        }

        @JsonProperty
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        @JsonProperty
        public Builder query(String query) {
            this.query = query;
            return this;
        }

        @JsonProperty
        public Builder reverse(boolean reverse) {
            this.reverse = reverse;
            return this;
        }

        @JsonProperty
        public Builder sortKey(SortKey sortKey) {
            this.sortKey = sortKey;
            return this;
        }

        @JsonProperty
        public Builder cursor(String cursor) {
            this.cursor = cursor;
            return this;
        }

        @JsonProperty
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @JsonProperty
        public Builder hostName(String hostName) {
            this.hostName = hostName;
            return this;
        }
        @JsonProperty
        public Builder handle(String handle) {
            this.handle = handle;
            return this;
        }

        @JsonProperty
        public Builder variantLimit(int variantLimit) {
            this.variantLimit = variantLimit;
            return this;
        }

        public ProductSearcher build() {
            return new ProductSearcher(before, limit, query, reverse, sortKey, cursor, id, hostName,handle,variantLimit);
        }

        public Builder copy(ProductSearcher searcher) {
            this.before = searcher.before;
            this.limit = searcher.limit;
            this.query = searcher.query;
            this.reverse = searcher.reverse;
            this.sortKey = searcher.sortKey;
            this.cursor = searcher.cursor;
            this.id = searcher.id;
            this.hostName = searcher.hostName;
            this.handle = searcher.handle;
            this.variantLimit = searcher.variantLimit;
            return this;
        }
    }

}
