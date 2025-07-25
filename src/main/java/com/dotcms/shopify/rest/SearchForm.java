package com.dotcms.shopify.rest;

import com.dotcms.shopify.api.ShopifyAPI.BEFORE_AFTER;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;



@JsonDeserialize(builder = SearchForm.Builder.class)
public class SearchForm {

  public final String searchTerm;
  public final String hostName;
  public final int limit;
  public final String cursor;
  public final String beforeAfter;

  private SearchForm(Builder builder) {
    searchTerm = builder.searchTerm;
    limit = builder.limit < 1
        ? 1
        : builder.limit > 20
            ? 20
            : builder.limit;
    cursor = builder.cursor;
    beforeAfter = builder.beforeAfter;
    hostName = builder.hostName;

    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      throw new IllegalArgumentException("searchTerm must be provided");
    }

    if (beforeAfter != null && !"BEFORE" .equalsIgnoreCase(beforeAfter) && !"AFTER" .equalsIgnoreCase(beforeAfter)) {
      throw new IllegalArgumentException("beforeAfter must be BEFORE or AFTER");
    }

  }

  public BEFORE_AFTER getBeforeAfter() {
    return BEFORE_AFTER.BEFORE.name().equalsIgnoreCase(beforeAfter) ? BEFORE_AFTER.BEFORE : BEFORE_AFTER.AFTER;
  }


  public static final class Builder {

    @JsonProperty
    private String searchTerm;

    @JsonProperty
    private String beforeAfter;

    @JsonProperty
    private String cursor;

    @JsonProperty
    private String hostName;

    @JsonProperty
    private int limit = 10;


    public Builder searchTerm(final String searchTerm) {
      this.searchTerm = searchTerm;
      return this;
    }

    public Builder hostName(final String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder beforeAfter(final String beforeAfter) {
      this.beforeAfter = beforeAfter;
      return this;
    }

    public Builder cursor(final String cursor) {
      this.cursor = cursor;
      return this;
    }

    public Builder limit(final int limit) {
      this.limit = limit;
      return this;
    }


    public SearchForm build() {
      return new SearchForm(this);
    }
  }
}
