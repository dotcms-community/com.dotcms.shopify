package com.dotcms.shopify.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = SearchForm.Builder.class)
public class SearchForm {


    public final String searchTerm;

    public final int limit;
    public final int page;
    private SearchForm(Builder builder) {
        searchTerm = builder.searchTerm;
        limit = builder.limit < 1 
            ? 1 
            : builder.limit > 20 
                ? 20 
                : builder.limit;
        page = builder.page< 1 ? 1 : builder.page ;

    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public int getLimit() {
        return limit;
    }

    public int getPage() {
        return page;
    }

    public static final class Builder {

        @JsonProperty
        private String searchTerm;

        @JsonProperty
        private int limit=1;

        @JsonProperty
        private int page = 1;

        public Builder searchTerm(final String searchTerm) {
            this.searchTerm = searchTerm;
            return this;
        }

        public Builder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        public Builder page(final int page){
            this.page = page;
            return this;
        }

        public SearchForm build() {
            return new SearchForm(this);
        }
    }
}
