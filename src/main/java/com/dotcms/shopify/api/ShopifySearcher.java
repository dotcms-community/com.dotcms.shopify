package com.dotcms.shopify.api;

import com.dotcms.shopify.api.ShopifyAPI.BEFORE_AFTER;
import com.dotmarketing.util.UtilMethods;

public class ShopifySearcher {

  public final BEFORE_AFTER before;
  public final int limit;
  public final String query;
  public final boolean reverse;
  public final SortKey sortKey;
  public final String cursor;

  public ShopifySearcher(BEFORE_AFTER before, int limit, String query, boolean reverse, SortKey sortKey, String cursor) {
    this.before = before;
    this.limit = limit;
    this.query = query;
    this.reverse = reverse;
    this.sortKey = sortKey;
    this.cursor = cursor;
  }
  boolean hasCursor() {
    return UtilMethods.isSet(cursor);
  }

  @Override
  public String toString() {
    return "ShopifySearcher:{" +
        "before:" + before.name() +
        ", limit:" + limit +
        ", query:'" + query + '\'' +
        ", reverse:" + reverse +
        ", sortKey:" + sortKey.name() +
        ", cursor:'" + cursor + '\'' +
        '}';
  }





  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private BEFORE_AFTER before = BEFORE_AFTER.AFTER;
    private int limit = 20;
    private String query = "";
    private boolean reverse = false;
    private SortKey sortKey = SortKey.RELEVANCE;
    private String cursor;

    public Builder before(BEFORE_AFTER before) {
      this.before = before;
      return this;
    }

    public Builder limit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder reverse(boolean reverse) {
      this.reverse = reverse;
      return this;
    }

    public Builder sortKey(SortKey sortKey) {
      this.sortKey = sortKey;
      return this;
    }

    public Builder cursor(String cursor) {
      this.cursor = cursor;
      return this;
    }

    public ShopifySearcher build() {
      return new ShopifySearcher(before, limit, query, reverse, sortKey, cursor);
    }
  }

}
